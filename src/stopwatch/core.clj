(ns stopwatch.core
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [stopwatch.app :as app]
            [stopwatch.ring :refer [wrap-handler]]
            [taoensso.timbre :refer [error info]])
  (:gen-class :main true))

(def handler (wrap-handler app/app-handler))

(def ^:private DEFAULT-WATCH-FILE "watches.json")

(defn- load-watches
  []
  (let [path (get (System/getenv) "OPENSHIFT_DATA_DIR" DEFAULT-WATCH-FILE)]
    (if (.exists (io/as-file path))
      (try
       (app/strs->watches (json/read-str (slurp path)))
       (catch Exception e
         (error "Exception reading watches." e)))
      (info (str "No persisted watches found at '" path "'")))))

(defn- dump-watches
  []
  (let [path (get (System/getenv) "OPENSHIFT_DATA_DIR" DEFAULT-WATCH-FILE)
        watches-to-serialize (app/watches->strs)]
    (info "Persisting " (count watches-to-serialize) " watches.")
    (try
     (spit path (json/write-str watches-to-serialize))
     (catch Exception e
       (error (str "Exception persisting existing watches. All watches will be lost. Attempted to write to '" path "'") e)))))

(defn -main
  []
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (load-watches)
    (.addShutdownHook (Runtime/getRuntime) (Thread. dump-watches))
    (jetty/run-jetty handler {:host ip :port port})))
