(ns popchange.views.markdown
  (:require [me.raynes.cegdown :as md]))

(defn main-panel
  [params]
  [:div.container
   [:div.page-header
    [:h1 (:page-title params)]]
   (md/to-html (:page-content params))])
