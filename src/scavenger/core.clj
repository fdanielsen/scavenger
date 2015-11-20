(ns scavenger.core
  (:require
    [ring.util.response :as r]))

(defn handler [request]
  (-> (r/response "Let's go scavenging!")
      (r/content-type "text/html")))
