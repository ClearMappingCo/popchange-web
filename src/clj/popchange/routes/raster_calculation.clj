(ns popchange.routes.raster-calculation
  (:require [popchange.layout :as layout]
            [popchange.views.feedback :as feedback-view]
            [popchange.views.markdown :as markdown-view]
            [popchange.views.raster-calculation :as raster-calc-view]
            [popchange.lib.raster-calculation :as raster-calc]
            [popchange.lib.raster-calculation-ajax :as rc-ajax]
            [popchange.lib.user :as user]
            [popchange.lib.pdf :as pdf]
            [popchange.lib.email :as email]
            [popchange.lib.log :as log]
            [popchange.util :as util]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]))

(def main-uri "/raster-calc")

(def raster-calc-img-src "img/raster_place_holder.jpg")

(def local-doc-location "/var/opt/popchange/source/resources") ;; TODO: Move to config file

(defn doc-page-content
  [filename]
  (let [local-path (str local-doc-location "/" filename)]
    (slurp
     (if (-> local-path io/as-file .exists)
       local-path
       (io/resource filename)))))

(defn resources
  [{:keys [params]}]
  (layout/render
   markdown-view/main-panel
   (merge
    params
    {:page-title "Resources"
     :page-content (doc-page-content "docs/resources.md")})))

(defn faq
  [{:keys [params]}]
  (layout/render
   markdown-view/main-panel
   (merge
    params
    {:page-title "Frequently Asked Questions"
     :page-content (doc-page-content "docs/faq.md")})))

(defn feedback
  [{:keys [params flash]}]
  (layout/render
   feedback-view/main-panel
   (merge params
          (select-keys flash [:errors :messages]))))

(defn save-feeback! ;; TODO Properly destructure request
  [request]
  (let [params (:params request)
        session (:session request)
        user (user/user-by-id (:identity session))]
    (do
      (raster-calc/make-working-dir!)
      (spit (str raster-calc/working-dir "/feedback_" (System/currentTimeMillis) ".edn") request)
      (email/send-feedback-message! (:email user) (:fullname user) (:comments params) (not (nil? (:copyme params)))))
    (-> (response/found "/feedback")
        (assoc :flash {:messages ["Thanks for your feedback"]}))))

(defn raster-calculation
  [{:keys [params]}]
  (layout/render
   raster-calc-view/main-panel
   (conj
    params
    {:areas (raster-calc/clip-areas)
     :sets (raster-calc/sets)
     :set-years (raster-calc/set-years)})))

(defn rates-table?
  [params]
  (= (:countsorrates params) "rates"))

(def counts-table? (complement rates-table?))

(defn raster-calculation-params
  "Append additional parameters for raster calcuation view"
  [params calc-id]
  (let [filename (raster-calc/working-dir-file calc-id)
        cc (raster-calc/cumulative-cut filename)
        info (raster-calc/info filename)]
    {:params
     (merge
      params
      {:raster-calc true
       :raster-calc-id calc-id
       :img-src-tiff (str "/raster-calc-tiff?id=" calc-id)
       :img-src-jpeg (str "/raster-calc-jpeg?id=" calc-id)
       :img-src-png (str "/raster-calc-png?id=" calc-id)
       :img-src-shp (str "/raster-calc-shp?id=" calc-id)
       :img-src-tab (str "/raster-calc-tab?id=" calc-id)
       :cumulative-cut cc
       :info info}
      (if-let [pdf (:pdf params)]
        {:pdf-url (format
                   "/raster-calc-pdf?id=%s&set1=%d&set2=%d&area=%d"
                   (:id pdf)
                   (:set1 pdf)
                   (:set2 pdf)
                   (:area pdf))})
      (if-let [sd (:source-data params)]
        {:source-data
         (raster-calc/source-data-paths
          (:set1 sd)
          (:set2 sd)
          (counts-table? sd))}))}))

(defn assoc-tables
  "Swap ids for table names"
  [request]
  (let [params (:params request)
        attrib-ids (map
                    #(Integer/parseInt %)
                    (vals (select-keys params [:set1 :set2])))
        tables ((if (counts-table? params)
                  raster-calc/attrib-counts-tables
                  raster-calc/attrib-rates-tables)
                (first attrib-ids) (second attrib-ids))
        clip-area (raster-calc/clip-area-shapefile-table (Integer/parseInt (:area params)))]
    (assoc request :params (merge
                            params
                            tables
                            {:area clip-area}))))

(defn log-raster-calcation!
  [{:keys [params session remote-addr]}]
  (log/create-attrib-comparison!
   (:identity session)
   remote-addr
   (Integer/parseInt (:set1 params))
   (Integer/parseInt (:set2 params))
   (:excl-lcc params)
   (counts-table? params)))

(defn raster-calculation!
  [{:keys [params]}]
  (let [calc-id (System/currentTimeMillis)
        filename (raster-calc/working-dir-file calc-id)
        excl-low-count-cells (not (empty? (:excl-lcc params)))]
    (do
      (raster-calc/tiff! filename (:area params) (:set1 params) (:set2 params) excl-low-count-cells)
      (raster-calc/tiff->jpeg! filename (raster-calc/working-dir-file calc-id "jpeg"))
      (raster-calc/tiff->png! filename (raster-calc/working-dir-file calc-id "png"))
      calc-id)))

(defn pdf!
  [{:keys [params session]}]
  (let [set1 (Integer/parseInt (:set1 params))
        set2 (Integer/parseInt (:set2 params))
        attribs (raster-calc/attribs-by-id [set1 set2])
        clip-area (first (raster-calc/clip-areas-by-id [(Integer/parseInt (:area params))]))
        user (first (user/users-by-id [(:identity session)]))
        filename (raster-calc/working-dir-file (:id params) "pdf")]
    (pdf/save!
     filename
     (raster-calc/working-dir-file (:id params) "png")
     (:title clip-area)
     (some #(if (= (:id %) set1) %) attribs) ;; Need specific order
     (some #(if (= (:id %) set2) %) attribs) ;; Need specific order
     (map #(:value %) (raster-calc/colour-map-stops (raster-calc/working-dir-file (:id params))))
     (:fullname user))
    (util/byte-array-response filename "application/pdf")))

(defn vector-zip!
  [convert-fn id]
  (let [filename (raster-calc/working-dir-file id "zip")]
    (convert-fn (raster-calc/working-dir-file id) filename)
    (util/byte-array-response filename "application/zip")))

(defn counts-or-rates
  [src dst]
  (let [enabled (raster-calc/all-attribs-have-rates-tables? [(Integer/parseInt src) (Integer/parseInt dst)])]
    (rc-ajax/counts-or-rates enabled)))

(defn comparison-quality
  [src dst]
  (let [cq (raster-calc/comparison-quality (Integer/parseInt src) (Integer/parseInt dst))]
    (rc-ajax/comparison-quality (:description cq) (:colour cq))))

(defroutes raster-calculation-routes
  (GET main-uri [] (raster-calculation {:params {:img-src raster-calc-img-src}})) ;; Default params
  (POST "/raster-calc-gen" request (raster-calculation (raster-calculation-params (:params request) (raster-calculation! request)))) ;; TODO: Temporarily disable (untested)
  (POST "/raster-calc-gen-alt" request #(do (log-raster-calcation! %) (html (raster-calculation! (assoc-tables %)))))

  (GET "/raster-calc-tiff" [id] (util/byte-array-response (raster-calc/working-dir-file id) "image/tiff"))
  (GET "/raster-calc-jpeg" [id] (util/byte-array-response (raster-calc/working-dir-file id "jpeg") "image/jpeg"))
  (GET "/raster-calc-png" [id] (util/byte-array-response (raster-calc/working-dir-file id "png") "image/png"))

  (GET "/raster-calc-shp" [id] (vector-zip! raster-calc/tiff->shapefile-zip! id))
  (GET "/raster-calc-tab" [id] (vector-zip! raster-calc/tiff->mapinfo-zip! id))

  (GET "/raster-calc-pdf" request (pdf! request))

  ;; AJAX
  (GET "/set-years" [] (html (rc-ajax/set-years->options (raster-calc/set-years))))
  (GET "/set-attribs" [year] (html (rc-ajax/set-attribs->options (raster-calc/set-attribs (Integer/parseInt year)))))
  (GET "/set2-years" [id] (html (rc-ajax/set-years->options (raster-calc/set2-years (Integer/parseInt id)))))
  (GET "/set2-attribs" [id year] (html (rc-ajax/set2-attribs->options (raster-calc/set2-attribs (Integer/parseInt id) (Integer/parseInt year)))))
  (GET "/counts-or-rates" [src dst] (html (counts-or-rates src dst)))
  (GET "/comparison-quality" [src dst] (html (comparison-quality src dst)))
  (GET "/visualisation-js" [id] (html (rc-ajax/visualisation-js (:params (raster-calculation-params {} id)))))
  (GET "/visualisation-legend" [id] (html (rc-ajax/visualisation-legend (:params (raster-calculation-params {} id)))))
  (GET "/visualisation-exports" [id set1 set2 area countsorrates]
       (let [set1 (Integer/parseInt set1)
             set2 (Integer/parseInt set2)
             area (Integer/parseInt area)]
         (html
          (rc-ajax/visualisation-exports
           (:params
            (raster-calculation-params
             {:source-data
              {:set1 set1
               :set2 set2
               :countsorrates countsorrates}
              :pdf
              {:id id
               :set1 set1
               :set2 set2
               :area area}} id))))))

  ;; Docs, etc
  (GET "/resources" request (resources request))
  (GET "/faq" request (faq request))
  

  (GET "/feedback" request (feedback request))
  (POST "/feedback" request (save-feeback! request))
  )

