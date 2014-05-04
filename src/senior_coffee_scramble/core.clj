(ns senior-coffee-scramble.core
  (:gen-class)
  (:use senior-coffee-scramble.handler
        senior-coffee-scramble.helpers
        senior-coffee-scramble.mailer
        senior-coffee-scramble.database)
  (:require [ring.adapter.jetty :as ring]))

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
