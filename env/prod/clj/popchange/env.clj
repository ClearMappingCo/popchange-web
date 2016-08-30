(ns popchange.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[popchange started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[popchange has shut down successfully]=-"))
   :middleware identity})
