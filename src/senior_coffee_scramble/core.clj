(ns senior-coffee-scramble.core
  (:use [senior-coffee-scramble.handler :only [app]]
        senior-coffee-scramble.helpers
        senior-coffee-scramble.mailer
        senior-coffee-scramble.database)
  (:require [ring.adapter.jetty :as ring]))

(defn start [port]
  (ring/run-jetty app {:port port :join? false}))

(defn invitation-batch-send []
  (let [invitations (find-unsent-invitations)]
    (do-grouped :invitee invitations
      (fn [invites]
        (let [recipient (:invitee (first invites))]
          (try
            (send-invitation-digest recipient invites)
            (mark-invitations-sent recipient)
            (catch Exception e
              (.printStackTrace e))))))))

(defn -main [& args]
  (let [port (if (empty? args) 8080 (Integer/parseInt (first args)))]
    (start port)))
