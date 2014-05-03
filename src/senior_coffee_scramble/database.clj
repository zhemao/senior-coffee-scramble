(ns senior-coffee-scramble.database
  (:require [senior-coffee-scramble.helpers :refer [getenv]]
            [clojure.java.jdbc :as sql]))

(def postgres-conf
  {:subprotocol "postgresql"
   :subname (getenv "POSTGRES_URL" "//localhost:5432/senior-coffee-scramble")
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
            (concat
              (sql/update! trans :students student
                           ["uni = ?" inviter-uni])
              (insert-invitations trans inviter-uni invitee-unis)))
          ; otherwise, throw an exception
          (throw (Exception. "Invitations already sent.")))))))

(defn confirm-invitations [id]
  (sql/with-db-transaction [trans postgres-conf]
    (let [student (find-student-by-id trans id)]
      (when-not (nil? student)
        (sql/update! trans :students {:confirmed true} ["id = ?" id])
        (sql/update! trans :invitations {:confirmed true}
                     ["inviter = ?" (:uni student)]))
      student)))
