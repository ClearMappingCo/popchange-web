(ns popchange.db.log
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/log.sql")

;; (hugsql/def-sqlvec-fns "sql/user.sql") ;; useful for debugging
