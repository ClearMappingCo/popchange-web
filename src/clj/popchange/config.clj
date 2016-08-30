(ns popchange.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)]))

(def config-resource "conf/popchange.edn")
(def config-file "/etc/popchange")

(def config
  (edn/read-string
   (slurp
    (if (.exists (io/file config-file))
      config-file
      (io/resource config-resource)))))

(def working-dir (:working-dir config))
