(ns senior-coffee-scramble.handler
  (:use compojure.core
        senior-coffee-scramble.helpers
        senior-coffee-scramble.database
        senior-coffee-scramble.middleware)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.string :as string]
            [ring.util.response :refer [redirect]]))

(defn invite-handler [request]
  (let [inviter-name (-> request :params :name)
        inviter-uni  (-> request :params :uni)
        invitee-unis (form-to-invitee-list (:form-params request))]
    (println invitee-unis)
    (if (or (string/blank? inviter-name)
            (string/blank? inviter-uni)
            (empty? invitee-unis))
      (redirect "/error.html")
      (do
        (add-invitations inviter-name inviter-uni invitee-unis)
        (redirect "/recorded.html")))))

(defroutes app-routes
  (GET "/" [] (redirect "/index.html"))
  (POST "/invite" request (try-handler invite-handler request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
