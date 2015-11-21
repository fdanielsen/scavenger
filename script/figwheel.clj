(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!
  {:figwheel-options {}
   :build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel true
     :source-paths ["src"]
     :compiler {:main 'scavenger.client.main
                :asset-path "js"
                :foreign-libs [{:file "node_modules/google-map-react/dist/GoogleMapReact.js"
                                :provides ["google.maps.react"]}]
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js"
                :verbose true}}]})

(ra/cljs-repl)
