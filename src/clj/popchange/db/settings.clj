(ns popchange.db.settings
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/settings.sql")
