(ns popchange.lib.email
  (:require [postal.core :as postal]
            [popchange.config :as cfg]))

(def config (:mail cfg/config))

(defn send-message!
  [to subject body]
  (postal/send-message
   config
   {:from (:sender config)
    :to to
    :subject subject
    :body body}))

(defn password-reset-body
  [name url]
  (str
   "Hi " name ",\n\n"
   "Please use the following link to reset your password:\n\n"
   url "\n\n"
   "If you didn't request to reset your password please disregard this message.\n\n"
   "Many thanks,\n"
   "The PopChange Team"))

(defn send-password-reset-message!
  [to name url]
  (send-message! to "PopChange password reset" (password-reset-body name url)))

(defn feedback-body
  [from name comments]
  (str
   "Feedback from " name " <" from "> on " (.format (java.text.SimpleDateFormat. "dd/MM/yyyy HH:mm:ss") (new java.util.Date)) "\n\n"
   comments))

(defn send-feedback-message!
  [from name comments copy-sender]
  (let [recips (:feedback-recips cfg/config)]
    (doseq [to (if copy-sender (conj recips from) recips)]
      (send-message! to "PopChange feedback" (feedback-body from name comments)))))
