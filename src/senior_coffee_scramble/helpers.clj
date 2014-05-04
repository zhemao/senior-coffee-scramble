(ns senior-coffee-scramble.helpers
  (:require [clojure.string :as string]))

(defn getenv
  ([varname] (getenv varname ""))
  ([varname default] (or (System/getenv varname) default)))

(def MAX_INVITEES 10)

(defn form-to-invitee-list [request]
  (remove string/blank?
    (for [i (range MAX_INVITEES)]
      (request (str "invitee" i)))))

(defn exception-stacktrace [e]
  (apply str (cons (str e "\n")
                   (map #(str "\tat " % "\n") (.getStackTrace e)))))

(def OBFUSCATED_ID_RE (re-pattern "^[0-9a-f]{8}$"))
(def SECRET_NUMBER (Integer/parseInt (getenv "SECRET_NUMBER") 16))

; See http://stackoverflow.com/questions/8554286/obfuscating-an-id
(def MASK1 0x00550055)
(def SHIFT1 7)
(def MASK2 0x0000cccc)
(def SHIFT2 14)

(defn valid-id? [obfuscated-id]
  (re-matches OBFUSCATED_ID_RE obfuscated-id))

(defn obfuscate [id]
  (let [t (bit-and (bit-xor id (bit-shift-right id SHIFT1)) MASK1)
        u (bit-xor id t (bit-shift-left t SHIFT1))
        v (bit-and (bit-xor u (bit-shift-right u SHIFT2)) MASK2)
        y (bit-xor u v (bit-shift-left v SHIFT2))]
    (format "%08x" (bit-xor y SECRET_NUMBER))))

(defn deobfuscate [obfuscated-id]
  (let [enc-number (Integer/parseInt obfuscated-id 16)
        y (bit-xor enc-number SECRET_NUMBER)
        v (bit-and (bit-xor y (bit-shift-right y SHIFT2)) MASK2)
        u (bit-xor y v (bit-shift-left v SHIFT2))
        t (bit-and (bit-xor u (bit-shift-right u SHIFT1)) MASK1)]
    (bit-xor u t (bit-shift-left t SHIFT1))))

(defn try-function [f & args]
  (try
    (apply f args)
    (catch Exception e
      (println (exception-stacktrace e)))))
