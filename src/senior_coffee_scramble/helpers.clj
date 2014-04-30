(ns senior-coffee-scramble.helpers
  (:require [clojure.string :as string]))

(def MAX_INVITEES 10)

(defn form-to-invitee-list [request]
  (remove string/blank?
    (for [i (range MAX_INVITEES)]
      (request (str "invitee" i)))))
