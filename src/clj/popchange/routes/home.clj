(ns popchange.routes.home
  (:require [popchange.config :as cfg]
            [popchange.routes.raster-calculation :as rc-routes]
            [popchange.layout :as layout]
            [popchange.views.user-login :as user-login-view]
            [popchange.views.user-password-reset :as user-password-reset-view]
            [popchange.views.user-password-reset-confirm :as user-password-reset-confirm-view]
            [popchange.views.user-registration :as user-reg-view]
            [popchange.lib.user :as user]
            [popchange.lib.validation :as validate]
            [popchange.lib.settings :as settings]
            [popchange.lib.email :as email]
            [popchange.lib.log :as log]
            [popchange.util :as util]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [buddy.auth :refer [authenticated?]]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]))

(defn login
  [{:keys [session params flash]}]
  (if (authenticated? session)
    (response/found rc-routes/main-uri)
    
    (layout/render
     user-login-view/main-panel
     (merge params
            {:logo-centred true}
            (select-keys flash [:errors])))))

(defn user-registration
  [{:keys [flash]}]
  (layout/render
   user-reg-view/main-panel
   (merge {:sectors (user/user-sectors) ;; Database data for view
           :logo-centred true}
          (select-keys flash [:username :name :email :sector :mailinglist :errors])))) ;; Fields to populate from submitted data and any errors

(defn authenticate-user!
  [{{register "register" username "username" password "password"} :form-params
    session :session :as req
    remote-addr :remote-addr}]
  (if register
    (response/found "/register")
    
    (if-let [user (user/check username password)]

      ;; If authenticated
      (do
        (log/create-user-login! (:id user) remote-addr)
        (assoc (response/found rc-routes/main-uri)
               :session (assoc session :identity (:id user))))

      ;; Otherwise
      (-> (response/found "/")
          (assoc :flash {:errors ["Username or password incorrect"]})))))

(defn user-logout!
  [{session :session}]
  (assoc (response/found "/")
         :session (dissoc session :identity)))

(defn create-user!
  [{:keys [params]}]
  (let [params (merge params
                      {:sector (Integer/parseInt (:sector params))})] ;; Sanitize params
    (if-let [errors (validate/user params)]
      (-> (response/found "/register")
          (assoc :flash (assoc params :errors errors)))
      (do
        (user/create! (:username params)
                      (:password params)
                      (:name params)
                      (:email params)
                      (if (:mailinglist params) true false)
                      (:sector params))
        (response/found "/")))))

(defn password-reset
  [{:keys [params flash]}]
  (let [token (:t params)
        view (if-not token
               user-password-reset-view/main-panel
               user-password-reset-confirm-view/main-panel)]
    (if (and token (not (user/token-exists? token)))

      ;; Token is invalid
      (-> (response/found "/password-reset")
          (assoc :flash {:errors ["Token is invalid or expired, please try again"]}))

      ;; Token is valid
      (layout/render
       view
       (merge params
              {:logo-centred true}
              (select-keys flash [:errors :messages]))))))

(defn send-password-reset-link!
  [{{username "username"} :form-params}]
  (if-let [user (user/user-by-username-or-email username)]

    ;; If user found
    (do
      (let [token (user/create-token! (:id user))]
        (email/send-password-reset-message!
         (:email user)
         (:fullname user)
         (str (:host cfg/config) "/password-reset?t=" token)))
      (-> (response/found "/password-reset")
          (assoc :flash {:messages ["Password reset link sent, please check your inbox"]})))

    ;; Otherwise
    (-> (response/found "/password-reset")
        (assoc :flash {:errors ["Username or email not found"]}))))

(defn update-password!
  [{:keys [params]}]
  (if-let [errors (validate/password-reset params)]
    (-> (response/found (str "/password-reset?t=" (:token params)))
        (assoc :flash (assoc params :errors errors)))
    (do
      (user/update-password! (:token params) (:password params))
      
      (response/found "/"))))



(defroutes home-routes
  (GET "/" request (login request))
  (POST "/login" request (authenticate-user! request))

  (GET "/logout" request (user-logout! request))
  
  (GET "/register" request (user-registration request))
  (POST "/register" request (create-user! request))

  (GET "/password-reset" request (password-reset request))
  (POST "/password-reset" request (send-password-reset-link! request))
  (POST "/password-reset-confirm" request (update-password! request))

  (GET "/server-monitoring-hash" [] (html (settings/server-monitoring-hash))))

