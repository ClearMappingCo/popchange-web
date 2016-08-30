(ns popchange.db.user
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/user.sql")

;; (hugsql/def-sqlvec-fns "sql/user.sql") ;; useful for debugging
