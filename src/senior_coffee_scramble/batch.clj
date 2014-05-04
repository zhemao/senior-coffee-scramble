(ns senior-coffee-scramble.batch
  (:use senior-coffee-scramble.helpers
        senior-coffee-scramble.mailer
        senior-coffee-scramble.database))

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
  (invitation-batch-send))
