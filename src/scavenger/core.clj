(ns scavenger.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [response header]])
  (:use [datomic.api :only [db q] :as d]))

(def uri "datomic:free://localhost:4334/items")

(def conn (d/connect uri))

(defn get-all-items []
  (map first (q '[:find (pull ?c [*]) :where [?c item/name]] (db conn))))

(defroutes app-routes
  (OPTIONS "/items" []
    (-> (response "")
      (header "Access-Control-Allow-Methods" "GET")
      (header "Access-Control-Allow-Headers" "content-type")
      (header "Access-Control-Allow-Origin" "*")))
  (GET "/items" []
    (-> (response (str (into [] (get-all-items))))
      (header "Access-Control-Allow-Origin" "*")))
  (POST "/items" {body :body}
    (let [tempid (d/tempid :items)
          data (merge (read-string (slurp body)) {:db/id tempid})
          tx @(d/transact conn [data])
          id (d/resolve-tempid (db conn) (:tempids tx) tempid)]
      (-> (response (str (d/touch (d/entity (db conn) id))))
        (header "Access-Control-Allow-Origin" "*"))))
  (route/not-found "Page not found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security nil)))
