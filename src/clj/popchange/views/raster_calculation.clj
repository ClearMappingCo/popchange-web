(ns popchange.views.raster-calculation
  (:require [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [clojure.string :as s]))

(defn raster-calc-set-name
  "Friendly name for raster calculation set based on table name"
  [s]
  (let [words (-> (subs s 3) (s/split #"_"))]
    (s/join " " (map s/capitalize words))))

(defn area-select
  [raster-calc-areas current-area]
  [:select.form-control {:name "area" :id "area"}
   (map
    (fn [raster-calc-area]
      (let [attrs {:value (:id raster-calc-area)}]
        [:option
         (if (= (:value attrs) current-area)
           (assoc attrs :selected "selected")
           attrs)
         (:title raster-calc-area)]))
    raster-calc-areas)])

(defn set-select
  [set-name raster-calc-sets current-set]
  [:select.form-control {:name set-name :id set-name}
   (map
    (fn [raster-calc-set]
      (let [attrs {:value raster-calc-set}]
        [:option
         (if (= raster-calc-set current-set)
           (assoc attrs :selected "selected")
           attrs)
         (raster-calc-set-name raster-calc-set)]))
    raster-calc-sets)])

(defn raster-calc-form
  [params]
  [:form.form-horizontal {:method "post" :action "/raster-calc-gen" :id "non-js-form"}
   [:div.form-group
    [:label.col-md-3.control-label {:for "area"} "Area"]
    [:div.col-md-9
     (area-select (:areas params) (:area params))]]
   [:div.form-group
    [:label.col-md-3.control-label {:for "set1"} "Set 1"]
    [:div.col-md-9
     (set-select "set1" (:sets params) (:set1 params))]]
   [:div.form-group
    [:label.col-md-3.control-label {:for "set2"} "Set 2"]
    [:div.col-md-9
     (set-select "set2" (:sets params) (:set2 params))]]
   [:div.form-group
    [:div.checkbox.col-md-offset-3.col-md-9
     [:label
      (let [excl-low-count-cells-attrs {:type "checkbox"
                                        :id "excl-lcc"
                                        :name "excl-lcc"}]
        [:input (if (empty? (:excl-lcc params))
                  excl-low-count-cells-attrs
                  (assoc excl-low-count-cells-attrs :checked "checked"))])
      "Exclude low count cells"]]]
   [:div.form-group
    [:div.col-md-offset-3.col-md-9
     [:input {:type "submit"
              :class "btn btn-lg btn-success"
              :value "Generate"}]]]
   (anti-forgery-field)])


(defn area-select-alt
  [raster-calc-areas current-area]
  [:select.form-control.selectpicker {:name "area"
                                      :id "area"
                                      :data-live-search "true"}
   (map
    (fn [raster-calc-area]
      (let [attrs {:value (:id raster-calc-area)}]
        [:option
         (if (= (:value attrs) current-area)
           (assoc attrs :selected "selected")
           attrs)
         (:title raster-calc-area)]))
    raster-calc-areas)])

(defn set-select-alt
  ([set-name]
   (let [select-name-year (str set-name "-year")
         select-name-attrib (str set-name "-attrib")]
     [:div
      [:select.form-control.selectpicker {:name select-name-year
                                          :id select-name-year
                                          :disabled "disabled"}
       [:option "---"]]
      [:select.form-control.selectpicker {:name select-name-attrib
                                          :id select-name-attrib
                                          :data-live-search "true"
                                          :disabled "disabled"}
       [:option "---"]]
      [:input {:type "hidden"
               :id set-name
               :name set-name
               :value "-1"}]])))

(defn raster-calc-form-alt
  [params]
  [:div.form-horizontal
   [:div.form-group
    [:label.col-md-3.control-label {:for "area"} "Area"]
    [:div.col-md-9
     (area-select-alt (:areas params) (:area params))]]
   [:div.form-group
    [:label.col-md-3.control-label "Set 1"]
    [:div.col-md-9
     (set-select-alt "set1")]]
   [:div.form-group
    [:label.col-md-3.control-label "Set 2"]
    [:div.col-md-9
     (set-select-alt "set2")]]
   [:div.form-group
    [:div#countsorrates.col-md-offset-3.col-md-9
     [:label.radio-inline
      [:input {:type "radio"
               :name "countsorrates"
               :disabled "disabled"
               :checked "checked"}]
      "Counts"]
     [:label.radio-inline
      [:input {:type "radio"
               :name "countsorrates"
               :disabled "disabled"}]
      "Percentage"]]]
   [:div.form-group
    [:div#comparisonquality.col-md-offset-3.col-md-9
     [:em {:style "color: #cccccc"} "N/A"]]]
   [:div.form-group
    [:div.checkbox.col-md-offset-3.col-md-9
     [:label
      (let [excl-low-count-cells-attrs {:type "checkbox"
                                        :id "excl-lcc"
                                        :name "excl-lcc"}]
        [:input (if (empty? (:excl-lcc params))
                  excl-low-count-cells-attrs
                  (assoc excl-low-count-cells-attrs :checked "checked"))])
      "Exclude low count cells"]]]
   [:div.form-group
    [:div.col-md-offset-3.col-md-9
     [:button {:id "form-submit" :class "btn btn-lg btn-success"}
      "Generate"]]]
   (anti-forgery-field)])

(defn raster-calc-display
  [img-src]
  [:img.img-rounded {:src img-src}])

(defn main-panel
  [params]
  [:div.container
   [:div.page-header
    [:h1 "Raster Calculation Visualisation"]]
   [:p.lead "Generate a visualisation based on the calculation between two data sets (" [:code "[set 1] - [set 2]"] "). The visualisation can be used to identity areas of positive or negative growth by looking for \"hot spots\" and vice-versa."]

   [:div.col-md-4
    [:div.non-js
     (raster-calc-form params)]
    [:div#rasterui.hidden
     (raster-calc-form-alt params)]
    
    (if (:raster-calc params)
      [:div#rasterexports.non-js
       [:hr]
       [:div.col-md-offset-3.col-md-9
        [:a {:href (:img-src-tiff params) :target "_blank"} "Download GeoTIFF"]
        [:br]
        [:a {:href (:img-src-jpeg params) :target "_blank"} "Download JPEG (greyscale)"]]])

    [:div#rasterlegend.clearfix.hidden]

    [:div#rasterexports.hidden]]



   ;; Non-JS
   [:div.col-md-8.text-right.non-js
    (raster-calc-display (:img-src-jpeg params))
    [:div
     [:p "Please enable JavaScript to best explore the raster calculation."]]]

   [:div#rastercanvascontrols.col-md-8.hidden
    [:input.opacity {:id "opacity-slider"
                     :type "text"
                     :style "width: 100%"
                     :data-slider-min "0"
                     :data-slider-max "1"
                     :data-slider-step "0.01"
                     :data-slider-tooltip "hide"}]]
   [:div#rastercanvas.col-md-8.hidden]
   
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/ol3/3.13.1/ol-debug.js"}]
   [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/ol3/3.13.1/ol.min.css"}]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/proj4js/2.3.14/proj4.js"}]
   [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-slider/7.1.0/css/bootstrap-slider.min.css"}]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-slider/7.1.0/bootstrap-slider.min.js"}]
   [:link {:rel "stylesheet" :href "/css/rastercalc.css"}]
   [:script {:src "/js/rastercalc.js"}]

   [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.10.0/css/bootstrap-select.min.css"}]
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.10.0/js/bootstrap-select.min.js"}]])

