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
(def conn (d/create-conn {}))

; Reader for state
(defmulti read om/dispatch)

(defmethod read :sorts
  [{:keys [state query]} key params]
  {:value (d/q '[:find [(pull ?e [*]) ...]
                 :where [?e :sort/name]]
               (d/db state) query)})

; Mutator for state
(defmulti mutate om/dispatch)

; Scavenger sorts list
(defui SortsList
  Object
  (render [this]
    (let [sorts (om/props this)]
      (apply dom/ul nil
        (map
          (fn [sort]
            (dom/li nil (:sort/name sort)))
          sorts)))))

(def sorts-list (om/factory SortsList))

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
        (sorts-list sorts)
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
