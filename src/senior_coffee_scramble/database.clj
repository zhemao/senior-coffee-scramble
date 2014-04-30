(ns senior-coffee-scramble.database
  (:require [clojure.java.jdbc :as sql]))

(defn getenv
  ([varname] (getenv varname ""))
  ([varname default] (or (System/getenv varname) default)))

(def postgres-conf
  {:subprotocol "postgresql"
   :subname (getenv "POSTGRES_URL" "//localhost:5432/senior-coffee-scramble")
   :user (getenv "POSTGRES_USER" (getenv "USER"))
   :password (getenv "POSTGRES_PASSWD")})

(defn insert-if-needed [trans table object where-clause]
  (let [select-statement (vec (cons (str "SELECT * FROM "
                                         (name table)
                                         " WHERE "
                                         (first where-clause))
                                    (rest where-clause)))
        existing (sql/query trans select-statement)]
    (if (empty? existing)
      (sql/insert! trans table object)
      existing)))

(defn add-invitations [inviter-name inviter-uni invitee-unis]
  (sql/with-db-transaction [trans postgres-conf]
    ; concatenate the results of the insertions together
    (concat
      (insert-if-needed trans :students
                        {:name inviter-name
                         :uni inviter-uni
                         :confirmed false}
                        ["uni = ?" inviter-uni])
      (flatten
        (for [invitee-uni invitee-unis]
          (insert-if-needed trans :invitations
            {:inviter inviter-uni
             :invitee invitee-uni
             :confirmed false}
            ["inviter = ? AND invitee = ?"
             inviter-uni invitee-uni]))))))
