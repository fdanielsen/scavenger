(ns scavenger.client.main
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [google.maps.react]))

; Local app state
(def app-state
  (atom
    {:items ["Multebær" "Steinsopp" "Ramsløk"]
     :sightings [{:location [59.980504 10.752347]
                  :item "Multebær"}
                 {:location [59.992540 10.796283]
                  :item "Steinsopp"}]}))

; Read function for parser
(defn read [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      {:value :not-found})))

; Scavenger items list
(defui ItemsList
  Object
  (render [this]
    (let [items (om/props this)]
      (apply dom/ul nil
        (map
          (fn [name]
            (dom/li nil name))
          items)))))

(def items-list (om/factory ItemsList))

(def google-map (js/React.createFactory js/GoogleMapReact))

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
        (dom/div (clj->js {:style {:width "500px" :height "500px"}})
          (google-map (clj->js {:center {:lat 59.974289 :lng 10.728749}
                                :zoom 13})))))))

(def app (om/factory App))

; Create reconciler with parser for app state
(def reconciler
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read})}))

; Start render loop!
(om/add-root! reconciler
  App (gdom/getElement "app"))
