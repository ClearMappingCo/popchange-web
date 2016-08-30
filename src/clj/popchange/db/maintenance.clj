(ns popchange.db.maintenance
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/maintenance.sql")

(hugsql/def-sqlvec-fns "sql/maintenance.sql") ;; useful for debugging
