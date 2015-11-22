(ns scavenger.client.maps
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.object :as gobj]))

; Accessor of React component children
(defn children [component]
  (.. component -props -children))

; Create a Google Map LatLng object
(defn lat-lng [lat lng]
  (js/google.maps.LatLng. lat lng))

; Create a Google Map instance for a component
(defn create-map [component el]
  (when-not (:gmap (om.next/get-state component))
    (let [{:keys [lat lng]} (:center (om/props component))
          zoom (:zoom (om/props component))
          gmap (js/google.maps.Map.
               el #js {:center (lat-lng lat lng)
                       :zoom zoom})]
      (om.next/set-state! component {:gmap gmap}))))

(defui GoogleMap
  Object
  (render [this]
    (dom/div (clj->js {:ref #(create-map this %)
                       :style {:width "500px" :height "500px"}})
      (js/React.Children.map (children this)
        #(js/React.cloneElement % #js {:map-cref this})))))

(def google-map (om/factory GoogleMap))

(defui Marker
  Object
  (componentDidMount [this]
    (let [{:keys [lat lng]} (om/props this)
          marker (js/google.maps.Marker.
                   #js {:position (lat-lng lat lng)})]
      (om.next/set-state! this {:marker marker})))
  (render [this]
    (if-let [marker (:marker (om.next/get-state this))]
      (let [map-component (gobj/get (. this -props) "map-cref")
            gmap (:gmap (om.next/get-state map-component))]
        (.setMap marker gmap)))
    (dom/div nil)))

(def marker (om/factory Marker))
