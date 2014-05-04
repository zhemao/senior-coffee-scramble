(ns senior-coffee-scramble.core
  (:use [senior-coffee-scramble.handler :only [app]]
  (:require [ring.adapter.jetty :as ring]))

(defn start [port]
  (ring/run-jetty app {:port port :join? false}))

(defn -main [& args]
  (let [port (if (empty? args) 8080 (Integer/parseInt (first args)))]
    (start port)))
