(ns scavenger.client.main
  (:require [cljs.reader :as reader]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [datascript.core :as d]
            [goog.dom :as gdom]
            [goog.events :as events]
            [scavenger.client.maps :as maps])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

(def api-url "http://localhost:3449")

; Utility function to send XHR requests with correct content type and parse
; the EDN response appropriately.
; TODO: Use core.async to avoid on-complete callback
(defn edn-xhr [{:keys [method path data on-complete]}]
  (let [xhr (XhrIo.)
        url (str api-url path)]
    (events/listen xhr goog.net.EventType.COMPLETE
      (fn [e]
        (on-complete (reader/read-string (.getResponseText xhr)))))
    (. xhr
       (send url method (when data (pr-str data))
         #js {"Content-Type" "application/edn"}))))

; Local datascript state storage
(defonce conn (d/create-conn {}))

; Reader for state
(defmulti read om/dispatch)

(defmethod read :sorts
  [{:keys [state query]} key params]
  {:value (d/q '[:find [(pull ?e [*]) ...]
                 :where [?e :sort/name]]
               (d/db state) query)})

; Mutator for state
(defmulti mutate om/dispatch)

(defmethod mutate 'observations/add [{:keys [state]} _ entity]
  {:value {:keys [:observations]}
   :action
   (fn []
     (edn-xhr
      {:method "POST"
       :path "/observations"
       :data (merge entity {:observation/lat 60.029427 :observation/lng 9.955025})
       :on-complete (fn [data] (d/transact! state [data]))}))})

; Scavenger sorts list
(defui SortsList
  Object
  (render [this]
    (let [{:keys [sorts onSelect]} (om/props this)]
      (apply
       dom/select #js
       {:onChange
        (fn [e]
          (let [value (.. e -target -value)]
            (onSelect (if (empty? value) nil (reader/read-string value)))))}
       (cons
        (dom/option #js {:value ""} "-- Select sort --")
        (map
         (fn [sort]
           (dom/option #js {:value (:db/id sort)} (:sort/name sort)))
         sorts))))))

(def sorts-list (om/factory SortsList))

(defui AddObservation
  static om/IQuery
  (query [this] [:observations])
  Object
  (render [this]
    (let [sorts (om/props this)]
      (dom/div
       nil
       (sorts-list
        {:sorts sorts
         :onSelect (fn [sort-id] (om/set-state! this {:sort-id sort-id}))})
       (dom/button
        #js {:onClick
             (fn [e]
               (let [{:keys [sort-id]} (om/get-state this)]
                 (om/transact!
                  this `[(observations/add {:observation/sort ~sort-id})])))}
        "New observation")))))

(def add-observation (om/factory AddObservation))

; Keep state property for name text in AddSort component instance up to date
(defn update-state [component event]
  (om/update-state! component assoc
    :name-text (.. event -target -value)))

; Main application component
(defui App
  static om/IQuery
  (query [this]
    [:sorts])
  Object
  (render [this]
    (let [{:keys [sorts]} (om/props this)]
      (dom/div nil
        (dom/h1 nil "Scavenger sorts")
        (add-observation sorts)
        (maps/google-map {:center {:lat 60 :lng 10},
                     :zoom 12}
          (maps/marker {:lat 60 :lng 10})
          (maps/marker {:lat 60.01 :lng 10.01}))))))

(def app (om/factory App))

; Create reconciler with parser for app state
(def reconciler
  (om/reconciler
    {:state conn
     :parser (om/parser {:read read :mutate mutate})}))

; Load all sorts from API
(edn-xhr {:method "GET"
          :path "/sorts"
          :on-complete (fn [data]
                         (d/transact! conn data))})

; Start render loop!
(om/add-root! reconciler
  App (gdom/getElement "app"))
