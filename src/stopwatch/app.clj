(ns stopwatch.app
  (:require [echo.core :as echo]
            [echo.response :as response]
            [simple-time.core :as t]))

; Map of userId to timestamp
(def ^:private watches (ref {}))

(defn watches->strs
  "Returns all current watches as a map of user-id to formatted datetime strings ready for serialization."
  []
  (reduce (fn [acc [user-id watch]] (assoc acc user-id (t/format watch))) {} @watches))

(defn strs->watches
  "Converts a map of user-id to formatted datetime strings to a map of user-id to actual datetimes."
  [watch-strings]
  (dosync
   (ref-set watches (reduce (fn [acc [user-id watch-string]] (assoc acc user-id (t/parse watch-string))) {} watch-strings))))

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
  []
  (response/respond {:should-end? true
                     :speech (response/plaintext-speech
                              "Starting a new stopwatch. Launch stopwatch any time to get the status.")
                     :card (response/simple-card "Started Stopwatch" nil)}))

(defn- stopped-watch
  [watch now]
  (response/respond {:should-end? true
                     :speech (response/plaintext-speech
                              (str "Stopwatch ended at " (verbal-status watch now)))
                     :card (response/simple-card
                            "Stopwatch Ended"
                            (str "Duration: " (format-duration watch now)))}))

(defn- watch-status
  [watch now]
  (response/respond {:should-end? true
                     :speech (response/plaintext-speech
                              (str "Stopwatch duration is " (verbal-status watch now)))
                     :card (response/simple-card
                            "Stopwatch Status"
                            (str "Duration: " (format-duration watch now)))}))

(defn- already-watch
  []
  (response/respond {:should-end? true
                     :speech (response/plaintext-speech "You already have a stopwatch set.")}))

(defn- no-watch
  []
  (response/respond {:should-end? true
                     :speech (response/plaintext-speech "No stopwatch is set.")}))

(defn- restarted-watch
  ([] (response/respond {:should-end? true
                         :speech (response/plaintext-speech "No stopwatch is set, but I started a new one.")
                         :card (response/simple-card
                                "Started Stopwatch" nil)}))
  ([watch now ] (response/respond {:should-end? true
                                   :speech (response/plaintext-speech
                                            (str "Stopwatch restarted. Previous duration was " (verbal-status watch now)))
                                   :card (response/simple-card
                                          "Stopwatch Restarted"
                                          (str "Duration: " (format-duration watch now)))})))

(defn- launch
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (watch-status existing-watch now)
      (do
       (dosync
        (alter watches assoc user-id now))
       (new-watch)))))

(defmulti handle-intent (fn [request session] (get-in request ["intent" "name"])))

(defmethod handle-intent "StartStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (already-watch)
      (do
       (dosync
        (alter watches assoc user-id now))
       (new-watch)))))

(defmethod handle-intent "StopwatchStatus"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (watch-status existing-watch now)
      (no-watch))))

(defmethod handle-intent "StopStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (do
       (dosync
        (alter watches dissoc user-id))
       (stopped-watch existing-watch now))
      (no-watch))))

(defmethod handle-intent "ResetStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    ; In either case, we're going to overwrite the existing watch
    (dosync
     (alter watches assoc user-id now))
    ; All that differs is the response
    (if existing-watch
      (restarted-watch existing-watch now)
      (restarted-watch))))

(deftype StopwatchApp []
  echo/IEchoApp
  (on-launch [this request session] (launch request session))
  (on-intent [this request session] (handle-intent request session))
  (on-end [this request session] (response/respond {:should-end? true})))

(def app-handler (echo/request-dispatcher (StopwatchApp.)))
