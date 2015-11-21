(ns scavenger.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(defroutes app-routes
  (GET "/" []
       "Let's go scavenging!")
  (GET "/:name" [name]
       (str "Hello, " name ", let's go scavenging!"))
  (route/not-found "Page not found"))

(def app
  (wrap-defaults app-routes site-defaults))
