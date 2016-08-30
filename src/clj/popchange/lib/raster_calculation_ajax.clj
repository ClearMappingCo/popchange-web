(ns popchange.lib.raster-calculation-ajax
  (:require [popchange.config :refer [config]]
            [popchange.lib.raster-calculation :refer [colour-map-stop-values]]))

(defn conj-default
  [options]
  (conj
   options
   [:option {:value -1} "Please select"]))

(defn set-years->options
  [years]
  ;; (html)
  (conj-default
   (map
    (fn [year]
      [:option {:value year} year])
    years))
)

(defn set-attribs->options
  [attribs]
  (conj-default
   (map
    (fn [attrib]
      [:option {:value (:id attrib)} (:title attrib)])
    attribs)))

(defn set2-attribs->options
  [attribs]
  (conj-default
   (map
    (fn [attrib]
      [:option {:value (:id attrib) :style (str "color:" (:colour attrib))} (:title attrib)])
    attribs)))

(defn map-zoom
  [raster-height]
  (cond
    (nil? raster-height) 0
    (< raster-height 1200) 10
    :else 6))

(defn counts-or-rates
  [enabled]
  (let [dp (if-not enabled {:disabled "disabled"} {})]
    [:div
     [:label.radio-inline
      [:input (merge
               dp
               {:type "radio"
                :name "countsorrates"
                :value "counts"
                :checked "checked"})]
      "Counts"]
     [:label.radio-inline
      [:input (merge
               dp
               {:type "radio"
                :name "countsorrates"
                :value "rates"})]
      "Percentage"]]))

(defn comparison-quality
  [description colour]
  (let [d (if description description "N/A")
        c (if colour colour "#CCCCCC")]
    [:em {:style (str "color: " c)} d]))

(defn visualisation-js
  [params]
  (str
   "var rasterCalc = " (if (:raster-calc params) "true" "false") ";\n"
   "var rasterUrl = \"" (:img-src-tiff params) "\";\n"
   "var rasterPlotDomain = [" (-> params :cumulative-cut :min) ", " (-> params :cumulative-cut :max) "];\n"
   "var rasterTopLeft = [" (-> params :info :upper-left-x) ", " (-> params :info :upper-left-y) "];\n"
   "var rasterBottomRight = [" (-> params :info :lower-right-x) ", " (-> params :info :lower-right-y) "];\n"
   "var mapCentre = [" (-> params :info :centre-x) ", " (-> params :info :centre-y) "];\n"
   ;; "var rasterTopLeft = [50000.0, 1190000.0];\n"
   ;; "var rasterBottomRight = [600000.0, 2300.0];\n"
   ;; "var mapCentre = [370000.0, 612000.0];\n"
   "var mapZoom = " (map-zoom (-> params :info :height)) ";\n"
   "initRasterCalcVisual(" (:raster-calc-id params) ");\n"))

(comment
  (defn visualisation-legend-steps
    [min max steps]
    (let [diff (- max min)]
      (for [i (range steps)]
        i))))

(def visualisation-legend-steps (->> (map #(:value %) colour-map-stop-values) (drop 1) drop-last))

(defn format-step
  [value]
  (format "%.2f" value))

(defn visualisation-legend ;; TODO: Read values from :cumulative-cut
  [params]
  (let [cc-max (-> params :cumulative-cut :max)
        cc-min (-> params :cumulative-cut :min)
        cc-diff (- cc-max cc-min)]
    [:div
     [:div.col-md-offset-3.col-md-9.legend-steps
      [:div.legend-step.legend-cc-max
       [:span "&nbsp;"]
       (format-step cc-max)]

      (reverse
       (for [i (range (count visualisation-legend-steps))]
         (let [step (nth visualisation-legend-steps i)]
           [:div.legend-step {:class (str "legend-cc-" (+ i 1))}
            [:span "&nbsp;"]
            (format-step (+ (* cc-diff step) cc-min))])))
      
      [:div.legend-step.legend-cc-min
       [:span "&nbsp;"]
       (format-step cc-min)]]
     [:div.col-md-offset-3.col-md-9.summary-stats
      "Max: " (-> params :info :maximum) [:br]
      "Min: " (-> params :info :minimum) [:br]
      "Mean: " (-> params :info :mean) [:br]
      "StdDev: " (-> params :info :std-dev)
      ]]))

(def source-data-url (partial str (:source-data-host config) "/"))

(defn source-data-links
  [set-num {:keys [asc csv csvt counts-table] :as params}]
  (if (some #(if % true) (-> params (dissoc :counts-table) vals))
    [:div
     (format "Set %d source data: " set-num)
     (if asc
       [:a {:href (source-data-url asc) :target "_blank" :title (str "Download source ASCII grid data for set " set-num)} "ASC"]) " "
     (if csv
       [:a {:href (source-data-url csv) :target "_blank" :title (str "Download source lookup CSV data for set " set-num)} "CSV"]) " "
     (if csvt
       [:a {:href (source-data-url csvt) :target "_blank" :title (str "Download source lookup CSV types data for set " set-num)} "CSVT"])

     (if-not counts-table
       ;; No lookup CSVs for rates (discussed during call with Nick Bearman 24/08/2016)
       [:span [:br] [:em "(CSV and CSVT are for counts)"]])]))

(defn visualisation-exports
  [params]
  [:div
   [:hr]
   [:div.col-md-offset-3.col-md-9
    [:strong
     [:a {:href (:pdf-url params) :target "_blank"} "Download PDF"]]
    [:br]
    [:a {:href (:img-src-tiff params) :target "_blank"} "Download GeoTIFF"]
    [:br]
    [:a {:href (:img-src-png params) :target "_blank"} "Download PNG"]
    [:br]
    [:a {:href (:img-src-jpeg params) :target "_blank"} "Download JPEG (greyscale)"]
    [:br] [:br]
    (source-data-links 1 (-> params :source-data :set1))
    (source-data-links 2 (-> params :source-data :set2))]])