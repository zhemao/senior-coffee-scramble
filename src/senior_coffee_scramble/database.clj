(ns senior-coffee-scramble.database
  (:require [clojure.java.jdbc :as sql]))

(defn getenv
  ([varname] (getenv varname ""))
  ([varname default] (or (System/getenv varname) default)))

(def postgres-conf
  {:subprotocol "postgresql"
   :subname (getenv "POSTGRES_URL" "//localhost:5432/senior-coffee-scramble")
   :user (getenv "POSTGRES_USER")
   :password (getenv "POSTGRES_PASSWD")})

(defn add-invitations [inviter-name inviter-uni invitee-unis]
  (sql/with-db-transaction [trans postgres-conf]
    ; concatenate the results of the insertions together
    (concat
      (sql/insert! trans :students {:name inviter-name
                                    :uni inviter-uni
                                    :confirmed false})
      (apply (partial sql/insert! trans :invitations)
             (for [invitee invitee-unis]
               {:inviter inviter-uni
                :invitee invitee
                :confirmed false})))))
