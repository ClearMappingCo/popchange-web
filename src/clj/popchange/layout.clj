(ns popchange.layout
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))


(def pages
  (let [p (fn [title url] {:title title :url url})]
    {:raster-calculation
     (p "Raster Calculation" "/raster-calc")
     :resources
     (p "Resources" "/resources")
     :faq
     (p "FAQ" "/faq")
     :data-dl
     (p "Data" "http://popchange-data.liverpool.ac.uk")
     :feedback
     (p "Feedback" "/feedback")
     :logout
     (p "Logout" "/logout")}))

(defn navigation
  [active-page]
  [:div#navbar.navbar-collapse.collapse
   ;; https://getbootstrap.com/examples/navbar-static-top/
   [:ul.nav.navbar-nav
    (map
     (fn [k]
       (let [page (k pages)
             params (if (= active-page k) {:class "active"})]
         [:li params
          [:a {:href (:url page)} (:title page)]]))
     (keys pages))]])

(defn render
  [panel & [params]]
  (let [params
        (assoc params
               :csrf-token *anti-forgery-token*)]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "PopChange"]
      [:link
       {:rel "stylesheet"
        :href "/css/bootstrap.min.css"
        :type "text/css"}]
      [:link
       {:rel "stylesheet"
        :href "/css/ie10-viewport-bug-workaround.css"
        :type "text/css"}]
      [:link
       {:rel "stylesheet"
        :href "/css/sticky-footer-navbar.css"
        :type "text/css"}]
      [:link
       {:rel "stylesheet"
        :href "/css/popchange.css"
        :type "text/css"}]
      [:script {:type "text/javascript" :src "/js/jquery.min.js"}]
      [:script {:type "text/javascript" :src "/js/bootstrap.min.js"}]]
     [:body
      [:nav.navbar.navbar-default.navbar-static-top
       [:div.uol-logo
        [:div.container
         [:a {:href "https://www.liverpool.ac.uk/"}
          [:img {:src "/img/university_of_liverpool_logo.png"}]]]]

       (if (:logo-centred params)
         [:div.container
          [:div.navbar-header.logo-centred.col-md-2.col-md-offset-5
           [:a.navbar-brand {:href "/"}
            [:img {:src "/img/popchange_logo.png"}]]]]
         
         [:div.container
          [:div.navbar-header
           [:button.navbar-toggle.collapsed {:type "button"
                                             :aria-controls "navbar"
                                             :aria-expanded "false"
                                             :data-target "#navbar"
                                             :data-toggle "collapse"}]
           [:a.navbar-brand {:href "/"}
            [:img {:src "/img/popchange_logo.png"}]]]
          (navigation (:active-page params))])]

      [:div.wrapper (panel params)]

      [:footer.footer
       [:div.supporters-bg
        [:div.container.supporters
         [:div.col-md-3
          [:div.col-md-6.esrc-logo.text-center
           [:a {:href "http://www.esrc.ac.uk/"}
            [:img {:src "/img/esrc_logo.png"}]]]
          [:div.col-md-6.sdai-logo.text-center
           [:a {:href "http://www.esrc.ac.uk/research/our-research/secondary-data-analysis-initiative/"}
            [:img {:src "/img/sdai_logo.jpg"}]]]]
         [:div.col-md-6.text-center
          [:p "PopChange was funded by the Economic and Social Research Council (ESRC) under the Secondary Data Analysis Initiative (SDAI), Phase 2 (project ES/L014769/1) and this support is acknowledged gratefully."]]
         [:div.col-md-3.csdr-logo.text-center
          [:div.col-md-6.col-md-offset-3
           [:a {:href "https://www.liverpool.ac.uk/spatial-demographics/"}
            [:img {:src "/img/csdr_logo.png"}]]]]]]
       [:div.container.text-center
        [:p.text-muted "University of Liverpool PopChange by " 
         [:a {:href "http://www.clearmapping.co.uk/"} "Clear Mapping Co"] ". "
         "Source code available on "
         [:a {:href "https://github.com/ClearMappingCo/popchange-web"} "GitHub"] "."]]]])))
