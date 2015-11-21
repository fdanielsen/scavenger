(defproject scavenger "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [org.omcljs/om "1.0.0-alpha22"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]
                 [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]
                 [joda-time "1.6"]]
  :main ^:skip-aot scavenger.core
  :target-path "target/%s"
  :plugins [[lein-ring "0.8.11"]]
  :profiles {:uberjar {:aot :all}}
  :ring {:handler scavenger.core/handler})
