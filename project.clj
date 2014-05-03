(defproject senior-coffee-scramble "0.1.0-SNAPSHOT"
  :description "Senior Coffee Scramble App"
  :url "http://senior-coffee-scramble.herokuapp.com/"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]
                 [compojure "1.1.6"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [ring/ring-core "1.2.2"]
                 [selmer "0.6.6"]
                 [com.draines/postal "1.11.1"]
                 [postgresql "9.1-901.jdbc4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler senior-coffee-scramble.handler/app}
  :main ^:skip-aot senior-coffee-scramble.core
  :uberjar-name "senior-coffee-scramble.jar"
  :resource-paths ["resources" "resources/templates"]
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}})
