(ns senior-coffee-scramble.handler
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
    (if-let [batch (form-to-invitation-batch
                     (:form-params request))]
      (if-let [results (add-invitations batch)]
        (do
          (pool-do try-function send-confirmation-email
                   (first results) (rest results))
          (redirect (str "/recorded/" (:uni batch))))
        (redirect (str "/already-sent/" (:uni batch))))
      (redirect "/?error=true"))
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

(defn confirm-handler [id]
  (if (valid-id? id)
    (let [student (confirm-invitations (deobfuscate id))]
      (if-not (nil? student)
        (render-file "confirmed.html" student)
        {:status 404
         :body (render-file "invalid-link.html" {})}))
    {:status 404
     :body (render-file "invalid-link.html" {})}))

(defn index-handler [error]
  (let [csrf-token (csrf-generate)]
    {:status 200
     :headers {}
     :cookies {"csrf-token" {:value csrf-token}}
     :body (render-file "index.html"
                        {:error error
                         :numrange (range 0 MAX_INVITEES)
                         :csrf-token csrf-token})}))

(defn recorded-handler [uni]
  (let [csrf-token (csrf-generate)]
    {:status 200
     :headers {}
     :cookies {"csrf-token" {:value csrf-token}}
     :body (render-file "recorded.html"
                        {:uni uni, :csrf-token csrf-token})}))

(defroutes app-routes
  (GET "/" [error] (index-handler error))
  (POST "/invite" request (invite-handler request))
  (POST "/resend" request (resend-handler request))
  (GET "/recorded/:uni" [uni] (recorded-handler uni))
  (GET "/already-sent/:uni" [uni]
       (render-file "already-sent.html" {:uni uni}))
  (GET "/confirm/:id" [id] (confirm-handler id))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-cookies (handler/site app-routes)))
