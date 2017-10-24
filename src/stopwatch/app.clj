(ns stopwatch.app
  (:require [echo.core :as echo]
            [stopwatch.responses :as responses]
            [simple-time.core :as t]
            [echo.response :as response]))

; total app state:
; {"user-id"
;   { :started <datetime>
;     :duration <timespan> }}
(def ^:private watches (ref {}))

(defn watch->primitives
  [watch]
  (let [started (:started watch)
        duration (:duration watch)]
    {"started"  (if started (t/format started) started)
     "duration" (if duration (t/timespan->total-milliseconds duration) duration)}))

(defn watches->primitives
  "Returns all current watches as a map of user-id to formatted datetime strings ready for serialization.
  We will serialize this as:
  {string-user-id: {'started': '<formatted-timestamp>', 'duration': timespan-ms}}"
  []
  (reduce (fn [acc [user-id watch]] (assoc acc user-id (watch->primitives watch))) {} @watches))

(defn primitives->watch
  [watch-primitives]
  (let [started (get watch-primitives "started")
        duration (get watch-primitives "duration")]
    {:started  (if started (t/parse started) started)
     :duration (if duration (t/timespan duration) duration)}))

(defn primitives->watches
  "Converts a map of user-id to formatted watches to a map of user-id to map with started-datetime and
  duration-timespan. Input should look like:
  {'started': '<formatted-timestamp>', 'duration': timespan-ms}}"
  [watches-primitives]
  (dosync
    (ref-set watches (reduce (fn [acc [user-id watch-primitives]] (assoc acc user-id (primitives->watch watch-primitives))) {} watches-primitives))))

(defn- elapsed
  [watch now]
  (let [prev-duration (if-let [duration (:duration watch)] duration (t/timespan))
        current-elapsed (if-let [started (:started watch)] (t/- now started) (t/timespan))]
    (t/+ current-elapsed prev-duration)))

(defn- launch
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (responses/watch-status (elapsed existing-watch now) (:started existing-watch))
      (do
        (dosync
          (alter watches assoc user-id {:started now}))
        (responses/new-watch)))))

(defmulti handle-intent (fn [request session] (get-in request ["intent" "name"])))

(defmethod handle-intent "StartStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (responses/already-watch (elapsed existing-watch now))
      (do
        (dosync
          (alter watches assoc user-id {:started now}))
        (responses/new-watch)))))

(defmethod handle-intent "StopwatchStatus"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (responses/watch-status (elapsed existing-watch now) (:started existing-watch))
      (responses/no-watch))))

(defmethod handle-intent "StopStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (do
        (dosync
          (alter watches dissoc user-id))
        (responses/stopped-watch (elapsed existing-watch now)))
      (responses/no-watch))))

(defmethod handle-intent "ResetStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    ; In either case, we're going to overwrite the existing watch
    (dosync
      (alter watches assoc user-id {:started now}))
    ; All that differs is the response
    (if existing-watch
      (responses/restarted-watch (elapsed existing-watch now))
      (responses/no-watch true))))

(defmethod handle-intent "PauseStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (if (:started existing-watch)
        (let [elapsed-time (elapsed existing-watch now)]
          (dosync (alter watches assoc user-id {:duration elapsed-time}))
          (responses/paused-watch elapsed-time))
        (responses/already-paused-watch (:duration existing-watch)))
      (responses/no-watch))))

(defmethod handle-intent "ResumeStopwatch"
  [request session]
  (let [now (t/utc-now)
        user-id (get-in session ["user" "userId"])
        existing-watch (get @watches user-id)]
    (if existing-watch
      (if (:started existing-watch)
        (responses/already-watch (elapsed existing-watch now))
        (do
          (dosync (alter watches assoc-in [user-id :started] now))
          (responses/resumed-watch (elapsed existing-watch now))))
      ; if there's no watch, we may as well start one for them.
      (do
        (dosync (alter watches assoc user-id {:started now}))
        (responses/no-watch true)))))

(deftype StopwatchApp []
  echo/IEchoApp
  (on-launch [this request session] (launch request session))
  (on-intent [this request session] (handle-intent request session))
  (on-end [this request session] (response/respond {:should-end? true})))

(def app-handler (echo/request-dispatcher (StopwatchApp.)))