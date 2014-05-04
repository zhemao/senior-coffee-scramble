(ns senior-coffee-scramble.core
  (:use [senior-coffee-scramble.handler :only [app]]
        [senior-coffee-scramble.batch :only [invitation-batch-send]]
        [senior-coffee-scramble.helpers :only [getenv]])
  (:require [ring.adapter.jetty :as ring]))

(defn start [port]
  (ring/run-jetty app {:port port :join? false}))

(defn -main [& args]
  (if-not (empty? args)
    (case (first args)
      "batch"  (invitation-batch-send)
      "server" (start (Integer/parseInt (getenv "PORT" "3000")))
      (println "Unrecognized subcommand"))
    (println "Please enter subcommand")))
