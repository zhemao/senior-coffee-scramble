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
        (cond
          (empty? invitee) nil
          (not (re-matches UNI_RE invitee))
            (throw (ex-info (str "Invalid UNI " invitee) {}))
          :else (Invitation. invitee (truncate 140 message)))))))

(defn form-to-invitation-batch [form]
  (let [invite-list (form-to-invitation-list form)
        inviter-name (form "name")
        inviter-uni  (form "uni")]
    (cond
      (empty? inviter-name)
        (throw (ex-info "name field is blank" {}))
      (> (count inviter-name) 64)
        (throw (ex-info "name field too long (limit is 64 characters)" {}))
      (not (re-matches UNI_RE inviter-uni))
        (throw (ex-info (str "invalid uni " inviter-uni) {}))
      (empty? invite-list)
        (throw (ex-info "No invitees" {}))
      :else
        (InvitationBatch. inviter-name inviter-uni invite-list))))
