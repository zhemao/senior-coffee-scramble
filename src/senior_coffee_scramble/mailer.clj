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

(defn create-message [recipient subject text-body html-body]
  {:to recipient
   :from EMAIL_FROM
   :subject subject
   :body (if (nil? html-body)
           ; if the html body is nil, send plaintext email
           text-body
           ; otherwise send multipart alternative
           [:alternative
            {:type "text/plain"
             :content text-body}
            {:type "text/html"
             :content html-body}])})

(def RETRY_DELAY 500)
(def SENDMSG_RETRIES 2)

(defn attempt-send [message]
  (try
    (postal/send-message EMAIL_CONFIG message)
    (catch Exception e {:code -1, :error :EXCEPTION, :message (str e)})))

(defn retry-function [action check exc-format retries dly]
  (loop [res (action)
         retries-left retries]
    (cond
      (check res) nil
      (zero? retries-left) (throw (Exception. (exc-format res)))
      :else (do
              (Thread/sleep dly)
              (recur (action) (dec retries-left))))))

(defn send-email [recipient subject text-body html-body]
  (let [message (create-message recipient subject text-body html-body)]
    (retry-function #(attempt-send message)
                    (comp zero? :code)
                    #(str "Failed sending to " recipient ", "
                          "Error message: " (:message %))
                    SENDMSG_RETRIES RETRY_DELAY)))

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

(defn send-feedback-email [uni message]
  (send-email (getenv "ADMIN_EMAIL")
              (str "CU Senior Coffee Scramble Feedback from " uni)
              (str uni " says,\n\n" message) nil))
