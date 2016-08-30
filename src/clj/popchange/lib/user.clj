(ns popchange.lib.user
  (:require [popchange.db.user :as db]
            [popchange.db.conn :as conn]
            [buddy.hashers :as hashers]))

(def hasher-algorithm :bcrypt+sha512)

(defn user-sectors
  []
  (db/user-sectors conn/db))

(defn username-exists?
  [username]
  (not (empty? (db/username-exists conn/db {:username username}))))

(defn email-exists?
  [email]
  (not (empty? (db/email-exists conn/db {:email email}))))

(defn user-by-username-or-email
  [username-or-email]
  (first (db/user-by-username-or-email conn/db {:username-or-email username-or-email})))

(defn users-by-id
  [ids]
  (db/users-by-id conn/db {:ids ids}))

(defn user-by-id
  [id]
  (first (users-by-id [id])))

(defn check
  "Validate user and return if login valid"
  [username-or-email password]
  (if-let [user (user-by-username-or-email username-or-email)]
    (if (hashers/check password (:password_hash user))
      (dissoc user :password_hash))))

(defn create!
  [username password fullname email mailing-list sector-id]
  (db/create-user conn/db {:username username
                           :password-hash (hashers/derive password {:alg hasher-algorithm})
                           :fullname fullname
                           :email email
                           :mailing-list mailing-list
                           :sector-id sector-id}))

(defn update-password!
  [token password]
  (do
    (db/update-password conn/db {:token token
                                 :password-hash (hashers/derive password {:alg hasher-algorithm})})
    (db/delete-token conn/db {:token token})))


(def token-chars "0123456789abcdefghijklmnopqrstuvwxyz")
(def token-length 8)

(defn token-exists?
  [token]
  (not (empty? (db/token-exists conn/db {:token token}))))

(defn token
  []
  (apply
   str
   (take token-length (repeatedly #(rand-nth token-chars)))))

(defn create-token!
  "Create a token for the user and return it"
  [user-id]
  (let [token (token)]
    (do
      (db/delete-expired-tokens conn/db)
      (db/delete-expired-tokens-by-user-id conn/db {:uid user-id})
      (db/create-token conn/db {:uid user-id :token token}))
    token))
