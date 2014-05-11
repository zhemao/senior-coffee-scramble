(ns senior-coffee-scramble.forms)

(def MAX_INVITEES 10)

(def UNI_RE (re-pattern "^[a-z]{2,3}[0-9]{1,4}$"))

(defrecord InvitationBatch [name uni invitations])

(defrecord Invitation [uni message])

(defn truncate [n s]
  (if (> (count s) n)
    (subs s 0 n) s))

(defn form-to-invitation-list [form]
  (remove nil?
    (for [i (range MAX_INVITEES)]
      (let [invitee (form (str "invitee" i))
            message (form (str "message" i))]
        (if (re-matches UNI_RE invitee)
          (Invitation. invitee (truncate 140 message)) nil)))))

(defn form-to-invitation-batch [form]
  (let [invite-list (form-to-invitation-list form)
        inviter-name (form "name")
        inviter-uni  (form "uni")]
    (if-not (or (empty? inviter-name)
                (> (count inviter-name) 64)
                (not (re-matches UNI_RE inviter-uni))
                (empty? invite-list))
      (InvitationBatch. inviter-name inviter-uni invite-list) nil)))
