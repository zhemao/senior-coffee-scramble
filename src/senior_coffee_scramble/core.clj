(ns senior-coffee-scramble.core
  (:use [senior-coffee-scramble.handler :only [app]]
        [senior-coffee-scramble.batch :only [invitation-batch-send]]
        [senior-coffee-scramble.helpers :only [getenv]])
  (:require [ring.adapter.jetty :as ring]))

(defn start [host port]
  (ring/run-jetty app {:host host :port port :join? false}))

(defn -main [& args]
  (if-not (empty? args)
    (case (first args)
      "batch"  (invitation-batch-send)
      "server" (start (getenv "HOST" "127.0.0.1")
                      (Integer/parseInt (getenv "PORT" "3000")))
      (println "Unrecognized subcommand"))
    (println "Please enter subcommand")))
