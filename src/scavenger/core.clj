(ns scavenger.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [header content-type response resource-response]])
  (:use [datomic.api :only [db q] :as d]))

(def uri "datomic:free://localhost:4334/items")

(def conn (d/connect uri))

(defn get-all-items []
  (map first (q '[:find (pull ?c [*]) :where [?c item/name]] (db conn))))

(defroutes app-routes
  (GET "/items" []
    (response (str (into [] (get-all-items)))))
  (POST "/items" {body :body}
    (let [tempid (d/tempid :items)
          data (merge (read-string (slurp body)) {:db/id tempid})
          tx @(d/transact conn [data])
          id (d/resolve-tempid (db conn) (:tempids tx) tempid)]
      (response (str (d/touch (d/entity (db conn) id))))))
  (GET "/" []
    (-> (resource-response "index.html" {:root "public"})
        (content-type "text/html")))
  (route/not-found "Page not found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security nil)))
