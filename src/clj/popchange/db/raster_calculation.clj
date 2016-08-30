(ns popchange.db.raster-calculation
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/raster_calculation.sql")

(hugsql/def-sqlvec-fns "sql/raster_calculation.sql") ;; useful for debugging
