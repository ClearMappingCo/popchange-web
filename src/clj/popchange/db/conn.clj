(ns popchange.db.conn
  (:require [popchange.config :as cfg]))

(def config cfg/config)

;; Any additionial keys are passed to the driver as driver-specific properties
(def db {:subprotocol "postgresql"
         :subname (str "//" (-> config :db :host) ":" (-> config :db :port) "/" (-> config :db :name))
         :user (-> config :db :user)
         :password (-> config :db :pass)})


;; To deploy to live db from local REPL, define below pointing to VPN IP and push to REPL
;; (def db {:subprotocol "postgresql", :subname "//VPN-IP:5432/db", :user "user", :password "pass"})
