(defproject stopwatch "0.1.0-SNAPSHOT"
  :description "This is an Echo app server that hosts a stopwatch application."
  :url "https://github.com/blandflakes/echo-stopwatch"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.3.1"]
                 [echo-chamber "0.3.1"]
                 [echo-chamber-middleware "0.1.6"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-json "0.3.1"]
                 [ring-logger-timbre "0.7.5"]
                 [simple-time "0.2.0"]]
  :main stopwatch.core)
