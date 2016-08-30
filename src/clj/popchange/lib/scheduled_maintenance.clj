(ns popchange.lib.scheduled-maintenance
  (:require [immutant.scheduling :refer [schedule stop in every]]
            [mount.core :refer [defstate]]
            [clojure.java.io :as io]
            [popchange.lib.maintenance :as m]))

(def db-trigger-filename "/tmp/yu4z6cfzpnuu2hqx") ;; $ > /tmp/yu4z6cfzpnuu2hqx

(defn db-maintenance
  []
  (if (and (-> db-trigger-filename io/as-file .exists) (empty? (slurp db-trigger-filename)))
    (do
      (m/delete-auto-comparison-group-combinations)
      (m/create-comparison-group-combinations)
      (spit db-trigger-filename "DONE"))))

(defstate db-job
  :start (schedule db-maintenance
                   {:in [10 :seconds]
                    :every [10 :minutes]
                    :id ::db
                    :singleton true})
  :stop (stop db-job))




