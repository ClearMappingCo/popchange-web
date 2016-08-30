-- :name user-sectors
-- :doc Get all user sectors
SELECT id, title FROM user_sectors ORDER BY ordering, title;


-- :name username-exists
-- :doc Username exists
SELECT id, username, fullname FROM users WHERE username=:username;

-- :name email-exists
-- :doc Email exists
SELECT id, username, fullname FROM users WHERE email=:email;

-- :name user-by-username-or-email :1
-- :doc Get user by username or email
SELECT id, username, fullname, email, password_hash
FROM users
WHERE username=:username-or-email OR email=:username-or-email

-- :name users-by-id :? :*
-- :doc Get users by their id
SELECT id, username, fullname, email FROM users WHERE id IN (:v*:ids);

-- :name create-user :!
-- :doc Create a new user
INSERT INTO users (user_sector_id, username, password_hash, fullname, email, mailing_list, created) VALUES
       (:sector-id, :username, :password-hash, :fullname, :email, :mailing-list, now());

-- :name update-password :!
-- :doc Update user's password
UPDATE users SET password_hash=:password-hash, modified=now()
WHERE id=(SELECT user_id FROM user_password_reset_tokens WHERE token=:token);


-- :name token-exists
-- :doc Token exists
SELECT user_id, token, expires FROM user_password_reset_tokens WHERE token=:token AND expires >= (now() - interval '3 days');

-- :name delete-token :!
-- :doc Delete token
DELETE FROM user_password_reset_tokens WHERE token=:token;

-- :name delete-expired-tokens :!
-- :doc Delete expired tokens for all users
DELETE FROM user_password_reset_tokens WHERE expires <= (now() - interval '3 days');

-- :name delete-expired-tokens-by-user-id :!
-- :doc Delete expired tokens for user
DELETE FROM user_password_reset_tokens WHERE user_id=:uid;

-- :name create-token :!
-- :doc Create a new for user
INSERT INTO user_password_reset_tokens (user_id, token, expires) VALUES (:uid, :token, now() + interval '3 days');
