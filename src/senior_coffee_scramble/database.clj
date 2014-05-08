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

(defn find-invitations-by-inviter [trans uni]
  (sql/query trans ["SELECT * FROM invitations WHERE inviter = ?" uni]))

(defn insert-invitations [trans inviter-uni invitations]
  (apply (partial sql/insert! trans :invitations)
     (for [invite invitations]
       {:inviter inviter-uni
        :invitee (:uni invite)
        :message (:message invite)})))

(defn add-invitations [batch]
  (sql/with-db-transaction [trans postgres-conf]
    ; concatenate the results of the insertions together
    (let [existing-student (find-student-by-uni trans (:uni batch))
          student {:name (:name batch)
                   :uni (:uni batch)
                   :confirmed false}]
      (if (nil? existing-student)
        ; if the student doesn't exist create it and invitations
        (concat
          (sql/insert! trans :students student)
          (insert-invitations trans (:uni batch) (:invitations batch)))
        ; if the student exists but is not confirmed
        ; update the student and recreate the invitations
        (if-not (:confirmed existing-student)
          (do
            (sql/delete! trans :invitations ["inviter = ?" (:uni batch)])
            (cons (assoc student :id
                         (first (sql/update! trans :students student
                                             ["uni = ?" (:uni batch)])))
                  (insert-invitations trans (:uni batch)
                                      (:invitations batch))))
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
    ["SELECT i.id, i.invitee, i.message, s.uni, s.name FROM invitations i
        LEFT JOIN students s ON i.inviter = s.uni
        WHERE i.confirmed AND NOT i.email_sent ORDER BY i.invitee"]))

(defn mark-invitations-sent [invitee]
  (sql/update! postgres-conf :invitations {:email_sent true}
               ["invitee = ?" invitee]))

(defn cleanup-sent-invitations []
  (sql/delete! postgres-conf :invitations ["email_sent"]))

(defn find-student-invitations [uni]
  (sql/with-db-transaction [trans postgres-conf]
    (if-let [student (find-student-by-uni trans uni)]
      (cons
        student
        (sql/query trans ["SELECT * FROM invitations WHERE inviter = ?" uni]))
      nil)))
