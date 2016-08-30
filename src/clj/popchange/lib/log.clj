(ns popchange.lib.log
  (:require [popchange.db.conn :as conn]
            [popchange.db.log :as db]))

(defn create-user-login!
  [user-id remote-ip]
  (db/create-user-login conn/db {:uid user-id :ip remote-ip}))

(defn create-attrib-comparison!
  [user-id remote-ip src-attrib-id dst-attrib-id excl-low-count-cells counts-table]
  (db/create-attrib-comparison conn/db {:uid user-id
                                        :ip remote-ip
                                        :src-attrib-id src-attrib-id
                                        :dst-attrib-id dst-attrib-id
                                        :excl-low-count-cells (if excl-low-count-cells true false)
                                        :counts-table counts-table}))
