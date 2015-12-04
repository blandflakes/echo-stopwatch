(defproject stopwatch "0.1.0-SNAPSHOT"
  :description "FIXME: This is an Echo app server that..."
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [echo-chamber "0.0.1"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-json "0.3.1"]
                 [ring-logger-timbre "0.7.5"]
                 [simple-time "0.2.0"]]
  :main stopwatch.core)
