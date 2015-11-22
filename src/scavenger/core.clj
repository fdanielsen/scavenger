(ns scavenger.core
  (:require [compojure.core :refer :all]
            [compojure.coercions :refer [as-int]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer [header content-type response resource-response]])
  (:use [datomic.api :only [db q] :as d]))

(def uri "datomic:free://localhost:4334/items")

(def conn (d/connect uri))

(defn get-sorts []
  (map first (q '[:find (pull ?c [*]) :where [?c sort/name]] (db conn))))

(defn get-observations [& [sort]]
  (let [query
        (if sort
          '[:find (pull ?c [*])
            :in $ ?sort
            :where [?c observation/sort ?sort]]
          '[:find (pull ?c [*])
            :where [?c observation/sort]])]
    (map first (q query (db conn) sort))))

(defn add-observation [edn-data]
  (let [tempid (d/tempid :observations)
        tx @(d/transact conn [(merge {:db/id tempid} edn-data)])
        id (d/resolve-tempid (db conn) (:tempids tx) tempid)]
    (d/touch (d/entity (db conn) id))))

(defn gen-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (GET "/sorts" []
    (gen-response (vec (get-sorts))))
  (GET "/observations" []
    (gen-response (vec (get-observations))))
  (GET "/observations/:sort" [sort :<< as-int]
    (gen-response (vec (get-observations sort))))
  (POST "/observations" {body :body}
    (gen-response (add-observation (read-string (slurp body)))))
  (GET "/" []
    (-> (resource-response "index.html" {:root "public"})
        (content-type "text/html")))
  (route/not-found "Page not found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security nil)))
