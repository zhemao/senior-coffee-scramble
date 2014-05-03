(ns senior-coffee-scramble.mailer
  (:use senior-coffee-scramble.helpers)
  (:require [postal.core :as postal]
            [selmer.parser :refer [render-file]]))

(def EMAIL-USER (getenv "EMAIL_USER"))
(def SITE-NAME (getenv "SITE_NAME"))

(def EMAIL-CONFIG
  {:host (getenv "SMTP_HOST")
   :user EMAIL-USER
   :pass (getenv "EMAIL_PASSWD")
   :port (Integer/parseInt (getenv "SMTP_PORT" "25"))
   :tls (= "true" (getenv "SMTP_TLS"))})

(defn send-email [message]
  (postal/send-message EMAIL-CONFIG
                       (assoc message :from EMAIL-USER)))

(defn send-confirmation-email [inviter invitations]
  (let [obfuscated-id (obfuscate (:id inviter))
        invitees (map :invitee invitations)
        template-args {:obfuscated-id obfuscated-id
                       :name (:name inviter)
                       :invitees invitees
                       :site-name SITE-NAME}
        text-body (render-file "confirmation-email.txt" template-args)
        html-body (render-file "confirmation-email.html" template-args)]
    (send-email {:to (str (:uni inviter) "@columbia.edu")
                 :subject "Senior Coffee Match Confirmation"
                 :body [:alternative
                        {:type "text/plain"
                         :content text-body}
                        {:type "text/html"
                         :content html-body}]})))
