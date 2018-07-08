(ns stopwatch.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [stopwatch.app :as app]
            [echo-chamber-server.server :refer [app-server]]
            [echo-chamber-server.verifiers.timestamp :as timestamp]
            [echo-chamber-server.verifiers.signature :as signature]
            [taoensso.timbre :refer [error info]]
            [taoensso.timbre :as timbre])
  (:gen-class :main true)
  (:import (java.io File)))

(def ^:private watches-location (System/getProperty "watchesfile" "watches.json"))

(defn- load-watches
  []
  (if (.exists ^File (io/as-file watches-location))
    (try
     (app/primitives->watches (json/read-str (slurp watches-location)))
     (catch Exception e
       (error "Exception reading watches." e)))
    (info (str "No persisted watches found at '" watches-location "'"))))

(defn- dump-watches
  []
  (let [watches-to-serialize (app/watches->primitives)]
    (info "Persisting" (count watches-to-serialize) "watches to '" watches-location "'")
    (try
     (spit watches-location (json/write-str watches-to-serialize))
     (catch Exception e
       (error (str "Exception persisting existing watches. All watches will be lost. Attempted to write to '" watches-location "'") e)))))

(defn -main
  []
  (timbre/set-level! :warn)
  (let [ip (get (System/getenv) "HTTP_IP" "127.0.0.1")
        port (Integer/parseInt (get (System/getenv) "HTTP_PORT" "8080"))]
    (load-watches)
    (let [backend (app-server ip port {:skill-fn app/skill :verifiers [(signature/verifier) (timestamp/verifier)]})]
      (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable (fn []
                                                        (dump-watches)
                                                        ((:stop backend)))))
      ((:start backend)))))
