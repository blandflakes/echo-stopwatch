(ns stopwatch.core
  (:require [stopwatch.app :as app]
            [ring.adapter.jetty :as jetty]
            [stopwatch.ring :refer [wrap-handler]])
  (:gen-class :main true))

(def handler (wrap-handler app/app-handler))

(defn -main
  []
  (let [ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")
        port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))]
    (jetty/run-jetty handler {:host ip :port port})))
