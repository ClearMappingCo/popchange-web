(ns popchange.lib.validation
  (:require [bouncer.core :as b]
            [bouncer.validators :as v :refer [defvalidator]]
            [popchange.lib.user :as user]))

(defvalidator equal
  [first second]
  (= first second))

(defvalidator sector-exists
  [id]
  (let [sectors (user/user-sectors)]
    (not (nil? (some #{id} (map #(:id %) sectors))))))

(defvalidator username-taken
  [username]
  (not (user/username-exists? username)))

(defvalidator email-taken
  [email]
  (not (user/email-exists? email)))

(defvalidator token-valid
  [token]
  (user/token-exists? token))

(def password-validation [v/required [v/min-count 8]])

(defn password-confirm-validation
  [password]
  [[equal password :message "Passwords don't match"]])


(defn user
  [params]
  (first
   (b/validate
    params
    :username [v/required [v/matches #"^((?!@).)*$" :message "Username cannot be an email address"] [username-taken :message "Username has been taken"]]
    :name v/required
    :password password-validation
    :password-confirm (password-confirm-validation (:password params))
    :email [v/email [email-taken :message "Email address has been used (reset your password?)"]]
    :sector [v/required [sector-exists :message "Sector doesn't exist"]])))

(defn password-reset
  [params]
  (first
   (b/validate
    params
    :token [v/required [token-valid :message "Token is invalid and may have expired, please try resetting your password again"]]
    :password password-validation
    :password-confirm (password-confirm-validation (:password params)))))
