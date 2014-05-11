(ns senior-coffee-scramble.handler
  (:import clojure.lang.ExceptionInfo)
  (:use compojure.core
        senior-coffee-scramble.mailer
        senior-coffee-scramble.helpers
        senior-coffee-scramble.database
        senior-coffee-scramble.forms)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.string :as string]
            [selmer.parser :refer [render-file]]
            [ring.util.response :refer [redirect]]
            [ring.middleware.cookies :refer [wrap-cookies]]))

(defn invite-handler [request]
  (if (csrf-validate request)
    (try
      (let [batch (form-to-invitation-batch
                    (:form-params request))]
        (if-let [results (add-invitations batch)]
          (do
            (pool-do try-function send-confirmation-email
                     (first results) (rest results))
            (redirect (str "/recorded/" (:uni batch))))
          (redirect (str "/already-sent/" (:uni batch)))))
      (catch ExceptionInfo e
        (redirect (build-url-string "/"
                    (assoc (:form-params request) "error" (.getMessage e))))))
    {:status 400, :body "Invalid CSRF Token"}))

(defn resend-handler [request]
  (if (csrf-validate request)
    (let [uni (get-in request [:params :uni])
          results (find-student-invitations uni)]
      (if results
        (do
          (pool-do try-function send-confirmation-email
                   (first results) (rest results))
          (redirect (str "/recorded/" uni)))
        {:status 400, :body "No such UNI recorded"}))
    {:status 400, :body "Invalid CSRF Token"}))

(defn feedback-handler [request]
  (let [params (:params request)]
    (pool-do send-feedback-email (:uni params) (:message params))
    (redirect (str "/feedback-sent/" (:uni params)))))

(defn confirm-handler [id]
  (if (valid-id? id)
    (let [student (confirm-invitations (deobfuscate id))]
      (if-not (nil? student)
        (render-file "confirmed.html" student)
        {:status 404
         :body (render-file "invalid-link.html" {})}))
    {:status 404
     :body (render-file "invalid-link.html" {})}))

(defn index-handler [request]
  (let [get-param (fn [pname i] (get-in request [:query-params (str pname i)]))
        csrf-token (csrf-generate)
        invitations (for [i (range 0 MAX_INVITEES)]
                      [i (get-param "invitee" i) (get-param "message" i)])]
    {:status 200
     :headers {}
     :cookies {"csrf-token" {:value csrf-token}}
     :body (render-file "index.html"
                        {:name (get-in request [:params :name])
                         :uni  (get-in request [:params :uni])
                         :error (get-in request [:params :error])
                         :invitations invitations
                         :csrf-token csrf-token})}))

(defn recorded-handler [uni]
  (let [csrf-token (csrf-generate)]
    {:status 200
     :headers {}
     :cookies {"csrf-token" {:value csrf-token}}
     :body (render-file "recorded.html"
                        {:uni uni, :csrf-token csrf-token})}))

(defn admin-handler [success]
  (let [csrf-token (csrf-generate)]
    (render-file "admin.html"
                 {:signups-total (count-total-students)
                  :signups-confirmed (count-confirmed-students)
                  :invitations-total (count-total-invitations)
                  :invitations-confirmed (count-confirmed-invitations)
                  :invitations-sent (count-sent-invitations)
                  :unconfirmed (get-unconfirmed-students)
                  :csrf-token csrf-token
                  :revoke-success (= success "revoke")})))

(defn revoke-handler [request]
  (if (csrf-validate request)
    (let [uni (-> request :params :uni)]
      (revoke-confirmations uni)
      (redirect "/admin?success=revoke"))
    {:status 400, :body "Invalid CSRF Token"}))

(defroutes app-routes
  (GET "/" request (index-handler request))
  (POST "/invite" request (invite-handler request))
  (POST "/resend" request (resend-handler request))
  (POST "/feedback" request (feedback-handler request))
  (GET "/recorded/:uni" [uni] (recorded-handler uni))
  (GET "/already-sent/:uni" [uni]
       (render-file "already-sent.html" {:uni uni}))
  (GET "/feedback" [] (render-file "feedback.html" {}))
  (GET "/feedback-sent/:uni" [uni]
       (render-file "feedback-sent.html" {:uni uni}))
  (GET "/confirm/:id" [id] (confirm-handler id))
  (GET "/admin" [success] (admin-handler success))
  (POST "/revoke" request (revoke-handler request))
  (GET "/exception" [] (throw (Exception. "Error")))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-errors [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e {:status 500, :body (exception-stacktrace e)}))))

(def app
  (-> (handler/site app-routes) wrap-cookies wrap-errors))
