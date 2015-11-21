(ns scavenger.client.main
  (:require [cljs.reader :as reader]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [datascript.core :as d]
            [goog.dom :as gdom]
            [goog.events :as events]
            [google.maps.react])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

(def api-url "http://localhost:3000")

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

(defmethod read :items
  [{:keys [state query]} key params]
  {:value (d/q '[:find [(pull ?e [*]) ...]
                 :where [?e :item/name]]
               (d/db state) query)})

; Mutator for state
(defmulti mutate om/dispatch)

; Add a single item
; Persists to the backend API first, before adding the complete
; item from the API response to the locale state cache.
; TODO: Add to local state cache immediately, updating with response
; data and reverting local transaction if persisting fails
(defmethod mutate 'items/add
  [{:keys [state]} _ entity]
  {:value {:keys [:items]}
   :action (fn []
             (edn-xhr {:method "POST"
                       :path "/items"
                       :data entity
                       :on-complete
                       (fn [data]
                         (d/transact! state [data]))}))})

; Scavenger items list
(defui ItemsList
  Object
  (render [this]
    (let [items (om/props this)]
      (apply dom/ul nil
        (map
          (fn [item]
            (dom/li nil (:item/name item)))
          items)))))

(def items-list (om/factory ItemsList))

(def google-map (js/React.createFactory js/GoogleMapReact))

; Keep state property for name text in AddItem component instance up to date
(defn update-state [component event]
  (om/update-state! component assoc
    :name-text (.. event -target -value)))

(defui AddItem
  static om/IQuery
  (query [this]
    [:items])
  Object
  (render [this]
    (dom/div nil
      (dom/input #js {:onChange #(update-state this %)
                      :value (om/get-state this :name-text)})
      (dom/button
        #js {:onClick
             (fn [e]
               (let [value (om/get-state this :name-text)]
                 (om/transact! this
                    `[(items/add {:item/name ~value})])))}
        "Add item!"))))

(def add-item (om/factory AddItem))

; Main application component
(defui App
  static om/IQuery
  (query [this]
    [:items])
  Object
  (render [this]
    (let [{:keys [items]} (om/props this)]
      (dom/div nil
        (dom/h1 nil "Scavenger items")
        (items-list items)
        (add-item)
        (dom/div (clj->js {:style {:width "500px" :height "500px"}})
          (google-map (clj->js {:center {:lat 59.974289 :lng 10.728749}
                                :zoom 13})))))))

(def app (om/factory App))

; Create reconciler with parser for app state
(def reconciler
  (om/reconciler
    {:state conn
     :parser (om/parser {:read read :mutate mutate})}))

; Load all items from API
(edn-xhr {:method "GET"
          :path "/items"
          :on-complete (fn [data]
                         (d/transact! conn data))})

; Start render loop!
(om/add-root! reconciler
  App (gdom/getElement "app"))
