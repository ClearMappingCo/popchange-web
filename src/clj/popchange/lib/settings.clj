(ns popchange.lib.settings
  (:require [popchange.db.conn :as conn]
            [popchange.db.settings :as db]))

(defn server-monitoring-hash
  []
  (:val (first (db/server-monitoring-hash conn/db))))
