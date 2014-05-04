(ns senior-coffee-scramble.mailer
  (:use senior-coffee-scramble.helpers)
  (:require [postal.core :as postal]
            [selmer.parser :refer [render-file]]))

(def SITE_NAME (getenv "SITE_NAME"))
(def EMAIL_USER (getenv "EMAIL_USER" (getenv "USER")))
(def EMAIL_FROM (getenv "EMAIL_FROM" (str EMAIL_USER "@" SITE_NAME)))

(def EMAIL_CONFIG
  {:host (getenv "SMTP_HOST" "localhost")
   :user EMAIL_USER
   :pass (getenv "EMAIL_PASSWD")
   :port (Integer/parseInt (getenv "SMTP_PORT" "25"))
   :tls (= "true" (getenv "SMTP_TLS"))})

(defn send-email [recipient subject text-body html-body]
  (postal/send-message EMAIL_CONFIG
    {:to recipient
     :from EMAIL_FROM
     :subject subject
     :body [:alternative
            {:type "text/plain"
             :content text-body}
            {:type "text/html"
             :content html-body}]}))

(defn send-confirmation-email [inviter invitations]
  (let [obfuscated-id (obfuscate (:id inviter))
        invitees (map :invitee invitations)
        template-args {:obfuscated-id obfuscated-id
                       :name (:name inviter)
                       :invitees invitees
                       :site-name SITE_NAME}
        text-body (render-file "confirmation-email.txt" template-args)
        html-body (render-file "confirmation-email.html" template-args)]
    (send-email (str (:uni inviter) "@columbia.edu")
                "Senior Coffee Scramble Confirmation"
                text-body html-body)))

(defn send-invitation-digest [recipient invites]
  (let [template-args {:invitations invites}
        html-body (render-file "invitation-digest.html" template-args)
        text-body (render-file "invitation-digest.txt" template-args)]
    (send-email (str recipient "@columbia.edu")
                "Senior Coffee Scramble Invitations"
                text-body html-body)))
