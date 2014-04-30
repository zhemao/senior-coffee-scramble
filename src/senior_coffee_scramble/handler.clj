(ns senior-coffee-scramble.handler
  (:use compojure.core
        senior-coffee-scramble.helpers
        senior-coffee-scramble.database)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]))

(defn invite-handler [request]
  (let [inviter-name (request "name")
        inviter-uni  (request "uni")
        invitee-unis (form-to-invitee-list request)]
    (add-invitations inviter-name inviter-uni invitee-unis)))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (POST "/invite" request (invite-handler request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
