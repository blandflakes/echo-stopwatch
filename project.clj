(defproject stopwatch "0.1.0-SNAPSHOT"
  :description "This is an Echo app server that hosts a stopwatch application."
  :url "https://github.com/blandflakes/echo-stopwatch"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.taoensso/timbre "4.3.1"]
                 [echo-chamber "0.4.0"]
                 [echo-chamber-server "0.1.0"]
                 [org.clojure/data.json "0.2.6"]
                 [simple-time "0.2.0"]]
  :main stopwatch.core)
