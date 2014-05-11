(ns senior-coffee-scramble.helpers
  (:import (java.util.concurrent Executors))
  (:require [clojure.string :as string]
            [ring.util.codec :refer [url-encode]]))

(defn getenv
  ([varname] (getenv varname ""))
  ([varname default] (or (System/getenv varname) default)))

(defn exception-stacktrace [e]
  (apply str (cons (str e "\n")
                   (map #(str "\tat " % "\n") (.getStackTrace e)))))

(def OBFUSCATED_ID_RE (re-pattern "^[0-9a-f]{8}$"))
(def SECRET_NUMBER (Integer/parseInt (getenv "SECRET_NUMBER" "0") 16))

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

(defn do-grouped [keyfn ungrouped action!]
  (when-not (empty? ungrouped)
    ; group holds the current group we are collecting
    ; stream holds the remainder of the lazy sequence
    (loop [group (list (first ungrouped))
           stream (rest ungrouped)]
      (if (empty? stream)
        ; if the stream is empty, do the action on the last group
        (action! group)
        (let [top (first stream)]
          (if (= (keyfn (first group)) (keyfn top))
            ; if the keys match, push the object on top and move on
            (recur (cons top group) (rest stream))
            ; otherwise, clear the group
            (do
              (action! group)
              (recur (list top) (rest stream)))))))))

(def MAINPOOL (Executors/newFixedThreadPool
                (Integer/parseInt (getenv "NWORKERS" "1"))))

(defn pool-do [f & args]
  (.submit MAINPOOL #(apply f args)))

(defn csrf-generate []
  (format "%07x" (rand-int 0xfffffff)))

(defn csrf-validate [request]
  (let [cookie-value (get-in request ["cookies" "csrf-token" :value])
        form-value (get-in request ["form-params" "csrf-token"])]
    (= cookie-value form-value)))

(defn build-url-string [base params]
  (str base "?"
    (string/join "&"
      (for [[k v] (seq params)]
        (str (url-encode k) "=" (url-encode v))))))
