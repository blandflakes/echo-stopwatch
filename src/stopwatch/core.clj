(ns stopwatch.core
  (:require [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [stopwatch.app :as app]
            [stopwatch.ring :refer [wrap-handler]]
            [taoensso.timbre :refer [error info]])
  (:gen-class :main true))

(def handler (wrap-handler app/app-handler))

(def ^:private DEFAULT-WATCH-FILE "watches.edn")

(defn- load-watches
  []
  (let [path (get (System/getenv) "OPENSHIFT_DATA_DIR" DEFAULT-WATCH-FILE)]
    (if (.exists (io/as-file path))
      (try
       (dosync
        (ref-set app/watches (read-string (slurp path))))
       (catch Exception e
         (error "Exception reading watches." e)))
      (info (str "No persisted watches found at '" path "'")))))

(defn- dump-watches
  []
  (let [path (get (System/getenv) "OPENSHIFT_DATA_DIR" DEFAULT-WATCH-FILE)
        watches-snapshot @app/watches]
    (info "Persisting " (count watches-snapshot) " watches.")
    (try
     (spit path (pr-str watches-snapshot))
     (catch Exception e
       (error "Exception persisting existing watches. All watches will be lost. Attempted to write to '" path "'")))))

(defn -main
  []
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (load-watches)
    (.addShutdownHook (Runtime/getRuntime) (Thread. dump-watches))
    (jetty/run-jetty handler {:host ip :port port})))
