(ns stopwatch.app
  (:require [echo.core :as echo]
            [echo.response :as response]
            [simple-time.core :as t]))

; Map of userId to timestamp
(def watches (ref {}))

(defn- include-part?
  "A predicate for whether to include a part of a timespan in the verbal summary of a watch."
  [[_part-label value]]
  (pos? value))

(defn- part-summary
  "Returns a summary of the given part (really it just concatenates them, but this was messy when nesting)"
  [[part-label value]]
  (if (= 1 value)
    (str value " " part-label)
    (str value " " part-label "s")))

(defn- verbal-status
  "Generates an English summary of the time that has elapsed since the provided watch was started."
  [watch now]
  (let [diff (t/- now watch)
        days (t/timespan->days diff)
        hours (t/timespan->hours diff)
        minutes (t/timespan->minutes diff)
        seconds (t/timespan->seconds diff)
        values [["day" days] ["hour" hours] ["minute" minutes] ["second" seconds]]]
    (clojure.string/join ", " (map part-summary (filter include-part? values)))))

(defn- format-duration
  "Generates a formatted output of the duration of the provided watch. Could in theory be collapsed
   with verbal status."
  [watch now]
  (let [diff (t/- now watch)
        days (t/timespan->days diff)
        hours (t/timespan->hours diff)
        minutes (t/timespan->minutes diff)
        seconds (t/timespan->seconds diff)]
    (format "%02d:%02d:%02d:%02d" days hours minutes seconds)))

(defn- new-watch
  [user-id now session]
  (dosync
   (alter watches assoc user-id now))
  (response/respond session {:should-end? true
                             :speech (response/plaintext-speech "Starting a new stopwatch.")
                             :card (response/simple-card "Started stopwatch" nil nil)}))

(defn- watch-status
  [watch now session]
  (response/respond session {:should-end? true
                              :speech (response/plaintext-speech
                                      (str "Stopwatch duration is " (verbal-status watch now)))
                              :card (response/simple-card
                                     "Stopwatch Status"
                                     "Duration"
                                     (format-duration watch now))}))

(defn already-watch
  [session]
  (response/respond session {:should-end? true
                             :speech (response/plaintext-speech "You already have a stopwatch set.")}))

(defn- no-watch
  [session]
  (response/respond session {:should-end? true
                             :speech (response/plaintext-speech "No stopwatch is set.")}))

(defn launch
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (watch-status existing-watch now session)
      (new-watch user-id now session))))

(defmulti handle-intent (fn [request session] (get-in request ["intent" "name"])))

(defmethod handle-intent "StartStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (already-watch session)
      (new-watch user-id now session))))

(defmethod handle-intent "StopwatchStatus"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (watch-status existing-watch now session)
      (no-watch session))))
       
(defmethod handle-intent "StopStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (do
       (dosync
        (alter watches dissoc user-id))
        (watch-status existing-watch now session))
      (no-watch))))

(deftype StopwatchApp []
  echo/IEchoApp
  (on-launch [this request session] (launch request session))
  (on-intent [this request session] (handle-intent request session))
  (on-end [this request session] :default))

(def app-handler (echo/request-dispatcher (StopwatchApp.)))
