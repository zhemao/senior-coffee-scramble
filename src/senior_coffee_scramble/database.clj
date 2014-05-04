(ns senior-coffee-scramble.database
  (:require [senior-coffee-scramble.helpers :refer [getenv]]
            [clojure.java.jdbc :as sql]))

(def postgres-conf
  {:subprotocol "postgresql"
   :subname (getenv "POSTGRES_URL" "//localhost:5432/senior-scramble")
   :user (getenv "POSTGRES_USER" (getenv "USER"))
   :password (getenv "POSTGRES_PASSWD")})

(defn first-or-nil [lst]
  (if (empty? lst) nil (first lst)))

(defn find-student-by-uni [trans uni]
  (first-or-nil
    (sql/query trans ["SELECT * FROM students WHERE uni = ?" uni])))

(defn find-student-by-id [trans id]
  (first-or-nil
    (sql/query trans ["SELECT * FROM students WHERE id = ?" id])))

(defn insert-invitations [trans inviter-uni invitee-unis]
  (apply (partial sql/insert! trans :invitations)
     (for [invitee-uni invitee-unis]
       {:inviter inviter-uni :invitee invitee-uni})))

(defn add-invitations [inviter-name inviter-uni invitee-unis]
  (sql/with-db-transaction [trans postgres-conf]
    ; concatenate the results of the insertions together
    (let [existing-student (find-student-by-uni trans inviter-uni)
          student {:name inviter-name
                   :uni inviter-uni
                   :confirmed false}]
      (if (nil? existing-student)
        ; if the student doesn't exist create it and invitations
        (concat
          (sql/insert! trans :students student)
          (insert-invitations trans inviter-uni invitee-unis))
        ; if the student exists but is not confirmed
        ; update the student and recreate the invitations
        (if-not (:confirmed existing-student)
          (do
            (sql/delete! trans :invitations ["inviter = ?" inviter-uni])
            (cons (assoc student :id
                         (first (sql/update! trans :students student
                                             ["uni = ?" inviter-uni])))
                  (insert-invitations trans inviter-uni invitee-unis)))
          ; otherwise, return nil
          nil)))))

(defn confirm-invitations [id]
  (sql/with-db-transaction [trans postgres-conf]
    (let [student (find-student-by-id trans id)]
      (when-not (nil? student)
        (sql/update! trans :students {:confirmed true} ["id = ?" id])
        (sql/update! trans :invitations {:confirmed true}
                     ["inviter = ?" (:uni student)]))
      student)))

(defn find-unsent-invitations []
  (sql/query postgres-conf
    ["SELECT i.id, i.invitee, s.uni, s.name FROM invitations i
        LEFT JOIN students s ON i.inviter = s.uni
        WHERE i.confirmed AND NOT i.email_sent ORDER BY i.invitee"]))

(defn mark-invitations-sent [invitee]
  (sql/update! postgres-conf :invitations {:email_sent true}
               ["invitee = ?" invitee]))

(defn cleanup-sent-invitations []
  (sql/delete! postgres-conf :invitations ["email_sent"]))
