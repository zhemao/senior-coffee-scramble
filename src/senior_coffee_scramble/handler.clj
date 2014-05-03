(ns senior-coffee-scramble.handler
  (:use compojure.core
        senior-coffee-scramble.helpers
        senior-coffee-scramble.database)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.string :as string]
            [selmer.parser :refer [render-file]]
            [ring.util.response :refer [redirect]]))

(defn invite-handler [request]
  (let [inviter-name (-> request :params :name)
        inviter-uni  (-> request :params :uni)
        invitee-unis (form-to-invitee-list (:form-params request))]
    (if (or (string/blank? inviter-name)
            (string/blank? inviter-uni)
            (empty? invitee-unis))
      (redirect "/?error=true")
      (try
        (add-invitations inviter-name inviter-uni invitee-unis)
        (redirect (str "/recorded/" inviter-uni))
        (catch Exception e
          (if (System/getenv "DEBUG")
            {:status 500
             :body (exception-stacktrace e)}
            (redirect (str "/already-sent/" inviter-uni))))))))

(defn confirm-handler [id]
  (if (valid-id? id)
    (let [student (confirm-invitations (deobfuscate id))]
      (if-not (nil? student)
        (render-file "confirmed.html" student)
        {:status 404
         :body (render-file "invalid-link.html" {})}))
    {:status 404
     :body (render-file "invalid-link.html" {})}))

(defroutes app-routes
  (GET "/" [error] (render-file "index.html" {:error error}))
  (POST "/invite" request (invite-handler request))
  (GET "/recorded/:uni" [uni] (render-file "recorded.html" {:uni uni}))
  (GET "/already-sent/:uni" [uni]
       (render-file "already-sent.html" {:uni uni}))
  (GET "/confirm/:id" [id] (confirm-handler id))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
