(ns stopwatch.ring
  (:require [echo-chamber-middleware.authentication :refer [wrap-signature-verifier wrap-timestamp-verifier]]
            [ring.logger.timbre :as logger.timbre]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.util.response :as ring-response]))

(defn wrap-head
  "Wrapper that intercepts head requests and returns 200 OK."
  [handler]
  (fn [request]
    (if (= :head (:request-method request))
      {:status 200}
      (handler request))))

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
      wrap-timestamp-verifier
      wrap-json-response
      (wrap-defaults api-defaults)
      wrap-json-body
      wrap-signature-verifier
      wrap-head
      logger.timbre/wrap-with-logger))
