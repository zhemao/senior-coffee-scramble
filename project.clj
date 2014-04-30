(defproject senior-coffee-scramble "0.1.0-SNAPSHOT"
  :description "Senior Coffee Scramble App"
  :url "http://senior-coffee-scramble.herokuapp.com/"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [postgresql "9.1-901.jdbc4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler senior-coffee-scramble.handler/app}
  :main senior-coffee-scramble.core
  :uberjar-name "senior-coffee-scramble.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
