(ns popchange.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [popchange.layout-selma :refer [error-page]]
            [popchange.routes.home :refer [home-routes]]
            [popchange.routes.raster-calculation :refer [raster-calculation-routes]]
            [compojure.route :as route]
            [popchange.env :refer [defaults]]
            [mount.core :as mount]
            [popchange.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    (-> #'raster-calculation-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-restricted)
        (wrap-routes middleware/wrap-formats))
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
