(ns senior-coffee-scramble.core
  (:use [senior-coffee-scramble.handler :only [app]])
  (:require [ring.adapter.jetty :as ring]))

(defn start [port]
  (ring/run-jetty app {:port port :join? false}))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))]
    (start port)))
