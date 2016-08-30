(ns user
  (:require [mount.core :as mount]
            [popchange.figwheel :refer [start-fw stop-fw cljs]]
            popchange.core))

(defn start []
  (mount/start-without #'popchange.core/repl-server))

(defn stop []
  (mount/stop-except #'popchange.core/repl-server))

(defn restart []
  (stop)
  (start))


