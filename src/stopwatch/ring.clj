(ns stopwatch.ring
  (:require [ring.logger.timbre :as logger.timbre]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :as ring-response]))

(defn contact-point
  "Manages the contact point between ring middleware and the echo app router.
   Extracts the echo request from the ring request's body, and wraps the echo
   response in a ring response."
  [handler]
  (fn [request]
    (-> request :body handler ring-response/response)))


(defn wrap-handler
  "Generates a ring handler that parses HTTP requests and sends the parsed JSON body
   to the provided app-handler.
   app-handler should be a handler that takes an echo request map."
  [app-handler]
  (-> app-handler
      contact-point
      wrap-json-response
      (wrap-defaults api-defaults)
      wrap-json-body
      logger.timbre/wrap-with-logger))
