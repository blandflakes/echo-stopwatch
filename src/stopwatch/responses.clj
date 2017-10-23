(ns stopwatch.responses
  (:require [echo.response :as response]
            [simple-time.core :as t]))

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

(defn- elapsed-parts
  [elapsed-time]
  (let [days (t/timespan->days elapsed-time)
        hours (t/timespan->hours elapsed-time)
        minutes (t/timespan->minutes elapsed-time)
        seconds (t/timespan->seconds elapsed-time)]
    {:days days :hours hours :minutes minutes :seconds seconds}))

(defn- verbal-status
  "Generates an English summary of the time that has elapsed since the provided watch was started."
  [elapsed-time]
  (let [parts (elapsed-parts elapsed-time)
        values [["day" (:days parts)] ["hour" (:hours parts)] ["minute" (:minutes parts)] ["second" (:seconds parts)]]
        non-zero-parts (filter include-part? values)]
    (if (empty? non-zero-parts)
      "0 seconds"
      (clojure.string/join ", " (map part-summary non-zero-parts)))))

(defn- format-duration
  "Generates a formatted output of the duration of the provided watch. Could in theory be collapsed
   with verbal status."
  [elapsed-time]
  (let [parts (elapsed-parts elapsed-time)]
    (format "%02d:%02d:%02d:%02d" (:days parts) (:hours parts) (:minutes parts) (:seconds parts))))

(defn new-watch
  []
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech
                                    "Starting a new stopwatch. Launch stopwatch any time to get the status.")
                     :card        (response/simple-card "Started Stopwatch" nil)}))

(defn stopped-watch
  [elapsed-time]
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech
                                    (str "Stopwatch ended at " (verbal-status elapsed-time)))
                     :card        (response/simple-card
                                    "Stopwatch Ended"
                                    (str "Duration: " (format-duration elapsed-time)))}))

(defn watch-status
  [elapsed-time running?]
  (let [speech-prefix (if running? "Stopwatch is running at " "Stopwatch is paused at ")
        card-prefix (if running? "Running at: " "Paused at: ")]
    (response/respond {:should-end? true
                       :speech      (response/plaintext-speech
                                      (str speech-prefix (verbal-status elapsed-time)))
                       :card        (response/simple-card
                                      "Stopwatch Status"
                                      (str card-prefix (format-duration elapsed-time)))})))

(defn already-watch
  [elapsed-time]
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech "Your stopwatch is already running.")
                     :card        (response/simple-card
                                    "Stopwatch Already Running"
                                    (str "Duration: " (format-duration elapsed-time)))}))

(defn no-watch
  ([] (no-watch false))
  ([started?]
   (let [speech-text (if started? "No stopwatch is set, but I started a new one." "No stopwatch is set.")
         card-text (if started? "Started a new stopwatch." "No stopwatch is set.")]
     (response/respond {:should-end? true
                        :speech      (response/plaintext-speech speech-text)
                        :card        (response/simple-card
                                       "No Stopwatch Running"
                                       card-text)}))))

(defn restarted-watch
  [elapsed-time] (response/respond {:should-end? true
                                    :speech      (response/plaintext-speech
                                                   (str "Stopwatch restarted. Previous duration was " (verbal-status elapsed-time)))
                                    :card        (response/simple-card
                                                   "Stopwatch Restarted"
                                                   (str "Duration: " (format-duration elapsed-time)))}))
(defn paused-watch
  [elapsed-time]
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech
                                    (str "Stopwatch paused. Current duration is " (verbal-status elapsed-time)))
                     :card        (response/simple-card
                                    "Stopwatch Paused"
                                    (str "Duration: " (format-duration elapsed-time)))}))

(defn already-paused-watch [elapsed-time]
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech (str "Stopwatch already paused at " (verbal-status elapsed-time)))
                     :card        (response/simple-card
                                    "Stopwatch Already Paused"
                                    (str "Duration: " (format-duration elapsed-time)))}))

(defn resumed-watch [elapsed-time]
  (response/respond {:should-end? true
                     :speech      (response/plaintext-speech "Resuming your stopwatch.")
                     :card        (response/simple-card
                                    "Stopwatch Resumed"
                                    (str "Duration: " (format-duration elapsed-time)))}))