(ns senior-coffee-scramble.helpers
  (:require [clojure.string :as string]))

(def MAX_INVITEES 10)

(defn form-to-invitee-list [request]
  (remove string/blank?
    (for [i (range MAX_INVITEES)]
      (request (str "invitee" i)))))

(defn exception-stacktrace [e]
  (apply str (cons (str e "\n")
                   (map #(str "\tat " % "\n") (.getStackTrace e)))))

(def OBFUSCATION_NUMBER 0x3af7e159)
(def OBFUSCATED_ID_RE (re-pattern "^[0-9a-f]{8}$"))

(defn valid-id? [obfuscated-id]
  (re-matches OBFUSCATED_ID_RE obfuscated-id))

(defn obfuscate [id]
  (format "%08x" (bit-xor id OBFUSCATION_NUMBER)))

(defn deobfuscate [obfuscated-id]
  (bit-xor (Integer/parseInt obfuscated-id 16) OBFUSCATION_NUMBER))

(defn getenv
  ([varname] (getenv varname ""))
  ([varname default] (or (System/getenv varname) default)))
