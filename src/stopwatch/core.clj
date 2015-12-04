(ns stopwatch.core
  (:require [stopwatch.app :as app]
            [ring.adapter.jetty :as jetty]
            [stopwatch.ring :refer [wrap-handler]])
  (:gen-class :main true))

(def handler (wrap-handler app/app-handler))

(defn -main
  []
  (jetty/run-jetty handler {:port 8080}))
