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
  (map first (q '[:find (pull ?s [*]) :where [?s sort/name]] (db conn))))

(defn get-observations
  ([]
   (let [query '[:find (pull ?o [*]) :where [?o observation/sort]]]
     (map first (q query (db conn)))))
  ([sort]
   (let [query '[:find (pull ?o [*]) :in $ ?sort :where
                 [?o observation/sort ?sort]]]
     (map first (q query (db conn) sort))))
  ([sort lat1 lng1 lat2 lng2]
   (let [[lat-min lat-max] (clojure.core/sort (list lat1 lat2))
         [lng-min lng-max] (clojure.core/sort (list lng1 lng2))
         query '[:find (pull ?o [*])
                 :in $ ?sort ?lat-min ?lat-max ?lng-min ?lng-max
                 :where
                 [?o observation/sort ?sort]
                 [?o observation/lat ?lat]
                 [?o observation/lng ?lng]
                 [(< ?lat-min ?lat)] [(< ?lat ?lat-max)]
                 [(< ?lng-min ?lng)] [(< ?lng ?lng-max)]]]
     (map first (q query (db conn) sort lat-min lat-max lng-min lng-max)))))

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
  (GET "/observations/:sort/:lat1/:lng1/:lat2/:lng2" [sort lat1 lng1 lat2 lng2]
    (let [args (map read-string (list sort lat1 lng1 lat2 lng2))]
      (gen-response (vec (apply get-observations args)))))
  (POST "/observations" {body :body}
    (gen-response (add-observation (read-string (slurp body)))))
  (GET "/" []
    (-> (resource-response "index.html" {:root "public"})
        (content-type "text/html")))
  (route/not-found "Page not found"))

(def app
  (wrap-defaults app-routes (assoc site-defaults :security nil)))
