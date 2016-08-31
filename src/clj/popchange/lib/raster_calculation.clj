(ns popchange.lib.raster-calculation
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.conch :refer [programs]]
            [clj-commons-exec :as exec]
            [popchange.config :as cfg]
            [popchange.db.conn :as conn]
            [popchange.db.raster-calculation :as db]
            [popchange.util :as util]))

(programs gdal_translate gdalinfo python) ;; requires gdal-bin package on host

(def working-dir cfg/working-dir)

(def wdir (partial str working-dir))

(defn working-dir-file
  ([id]
   (working-dir-file id "tiff"))
  ([id ext]
   (wdir "/rcalc_" id "." ext)))

(defn make-working-dir!
  "Creates working directory if doesn't exist"
  []
  (let [wd (io/file working-dir)]
    (if-not (.isDirectory wd)
      (.mkdir wd))))


;; TODO Refactor extracting scripts
(def cumulative-cut-script-src "scripts/cumulative_cut_min_max.py")
(def cumulative-cut-script (str working-dir "/cumulative_cut_min_max.py"))
(def raster-to-png-script-src "scripts/raster_to_png.py")
(def raster-to-png-script (str working-dir "/raster_to_png.py"))
(def raster-median-script-src "scripts/raster_median.py")
(def raster-median-script (str working-dir "/raster_median.py"))

(def raster-style-template "scripts/raster_style_template.qml")

(defn prep-scripts!
  "Prepares supporting scripts (useful when packaged as a JAR / WAR)"
  []
  (do
    (make-working-dir!)
    (if-not (.exists (io/as-file cumulative-cut-script))
      (spit cumulative-cut-script (slurp (io/resource cumulative-cut-script-src))))
    (if-not (.exists (io/as-file raster-to-png-script))
      (spit raster-to-png-script (slurp (io/resource raster-to-png-script-src))))
    (if-not (.exists (io/as-file raster-median-script))
      (spit raster-median-script (slurp (io/resource raster-median-script-src))))))

(defn sets
  "Data sets available for generating raster calculations"
  []
  (map #(:table %) (db/ag-tables conn/db))) ;; Flatten data set

(defn flatten-set-years
  [years]
  (map #(:cenus_year %) years))

(defn set-years
  []
  (flatten-set-years (db/set-years conn/db)))

(defn set-attribs
  [year]
  (db/set-attribs conn/db {:cyear year}))

(defn set2-years
  "Years with acceptable matches based on set 1 attribute"
  [attrib-id]
  (flatten-set-years (db/set2-years conn/db {:aid attrib-id})))

(defn set2-attribs
  "Attributes with acceptable matches based on set 1 attribute and year"
  [attrib-id year]
  (db/set2-attribs conn/db {:aid attrib-id :cyear year}))

(defn attribs-by-id
  [ids]
  (map
   #(-> %
        (assoc :cenus-year (:cenus_year %))
        (dissoc :cenus_year))
   (db/attribs-by-id conn/db {:ids ids})))

(defn attrib-counts-tables
  [set1-attrib-id set2-attrib-id]
  (first (db/attrib-counts-tables conn/db {:a1 set1-attrib-id :a2 set2-attrib-id})))

(defn attrib-rates-tables
  [set1-attrib-id set2-attrib-id]
  (first (db/attrib-rates-tables conn/db {:a1 set1-attrib-id :a2 set2-attrib-id})))

(defn low-count-cell-mask
  "Name of low count cell mask for set"
  [s]
  (let [set-year (:cenus_year (first (db/set-year conn/db {:tbl s})))]
    (str "am_" set-year "_mask")))

(defn cumulative-cut
  [filename]
  (do
    (prep-scripts!)
    (let [vals (s/split (python cumulative-cut-script filename) #" ")]
      {:min (Double. (first vals))
       :max (Double. (second vals))})))

(defn median
  [filename]
  (do
    (prep-scripts!)
    (Double. (python raster-median-script filename))))

;; https://color.adobe.com/create/color-wheel/?base=2&rule=Analogous&selected=4&name=My%20Color%20Theme&mode=rgb&rgbvalues=0.050000000000000044,0.4863328728314287,1,0.04550000000000004,0.91,0.4170136015589975,0.8899342843958493,1,0,0.91,0.565626307834159,0,1,0.045165788032136334,0&swatchOrder=0,1,2,3,4
(def colour-map-stop-values
  [{:value 0 ;; Blue
    :colour "#0D7CFF"}
   {:value 0.25
    :colour "#0CE86A"}
   {:value 0.5
    :colour "#E3FF00"}
   {:value 0.75
    :colour "#E89000"}
   {:value 1 ;; Red
    :colour "#FF0C00"}])

(defn colour-map-stops
  [filename]
  (let [cc (cumulative-cut filename)
        cc-max (:max cc)
        cc-min (:min cc)
        cc-diff (- cc-max cc-min)]
    (map
     (fn [stop]
       (assoc
        stop
        :value
        (cond
          (= (:value stop) 1) cc-max
          (= (:value stop) 0) cc-min
          :else (+ (* cc-diff (:value stop)) cc-min))))
     colour-map-stop-values)))

(def colour-map-stop-format "%.5f")

(defn colour-map-stops-xml
  [stops]
  (map
   (fn [stop]
     (let [value-frmt (format colour-map-stop-format (:value stop))]
       (str "<item alpha=\"255\" value=\"" value-frmt "\" label=\"" value-frmt "\" color=\"" (s/lower-case (:colour stop)) "\"/>\n")))
   stops))

(defn info
  [filename]
  (let [info (gdalinfo filename)
        size (re-find #"Size is (\d+), (\d+)" info)
        origin (re-find #"Origin = \((.*\d+\.\d+),(.*\d+\.\d+)\)" info)
        pixel-size (re-find #"Pixel Size = \((\d+\.\d+)" info)
        upper-left (re-find #"Upper Left.*\((.*\.\d+),(.*\.\d+)\)" info)
        lower-right (re-find #"Lower Right.*\((.*\.\d+),(.*\.\d+)\)" info)
        centre (re-find #"Center.*\((.*\.\d+),(.*\.\d+)\)" info)
        summary-stats (re-find #"Minimum=(\-*\d+\.\d+), Maximum=(\-*\d+\.\d+), Mean=(\-*\d+\.\d+), StdDev=(\-*\d+\.\d+)" info)]
    {:width (Integer/parseInt (second size))
     :height (Integer/parseInt (last size))
     :origin-x (Double. (second origin))
     :origin-y (Double. (last origin))
     :pixel-size (Double. (second pixel-size))
     :upper-left-x (Double. (s/trim (second upper-left)))
     :upper-left-y (Double. (s/trim (last upper-left)))
     :lower-right-x (Double. (s/trim (second lower-right)))
     :lower-right-y (Double. (s/trim (last lower-right)))
     :centre-x (Double. (s/trim (second centre)))
     :centre-y (Double. (s/trim (last centre)))
     :minimum (Double. (s/trim (nth summary-stats 1)))
     :maximum (Double. (s/trim (nth summary-stats 2)))
     :mean (Double. (s/trim (nth summary-stats 3)))
     :std-dev (Double. (s/trim (nth summary-stats 4)))
     :median (median filename)}))

(defn source-data-path-map
  [set-key set-paths counts-table]
  {set-key
   ;; No lookup CSVs for rates (discussed during call with Nick Bearman 24/08/2016)
   {:asc ((if counts-table :counts_path :rates_path) set-paths)
    :csv (:counts_lookup_path set-paths)
    :csvt (:counts_lookup_types_path set-paths)
    :counts-table counts-table
    :title (format "%d %s" (:cenus_year set-paths) (:title set-paths))}})

(defn source-data-paths
  [set1 set2 counts-table]
  (let [aps (db/attribs-paths conn/db {:ids [set1 set2]})
        sp1 (some #(if (= (:id %) set1) %) aps)
        sp2 (some #(if (= (:id %) set2) %) aps)]
    (merge (source-data-path-map :set1 sp1 counts-table)
           (source-data-path-map :set2 sp2 counts-table))))

(defn all-attribs-have-rates-tables?
  [ids]
  (:tf (db/all-attribs-have-rates-tables conn/db {:ids ids})))

(defn comparison-quality
  [src dst]
  (first (db/comparison-quality conn/db {:src src :dst dst}))) ;; TODO: Set single return on hugsql? Check others?

(defn area-uk?
  [area]
  (nil? area))

(def clip-area-uk {:id 0 :title "Great Britain"})

(defn clip-areas
  []
  (conj
   (db/clip-areas conn/db)
   clip-area-uk)) ;; Prepend default area

(defn clip-areas-by-id
  [ids]
  (let [uk (if (some #(= % (:id clip-area-uk)) ids) clip-area-uk)]
    (conj
     (db/clip-areas-by-id conn/db {:ids ids})
     uk)))

(defn clip-areas-by-id
  [ids]
  (let [clip-areas (db/clip-areas-by-id conn/db {:ids ids})]
    (if (some #(= % (:id clip-area-uk)) ids)
      (conj clip-areas clip-area-uk)
      clip-areas)))

(defn clip-area-shapefile-table
  [id]
  (-> (db/clip-area-shapefile-table conn/db {:id id}) first :shapefile_table))

(defn create-raster-calc-fn
  "Function to create raster calculation"
  [area excl-low-count-cells]
  (cond
    (and (not (area-uk? area)) excl-low-count-cells) db/lo-create-raster-calc-clipped-excl-low-count-cells
    (and (area-uk? area) excl-low-count-cells) db/lo-create-raster-calc-excl-low-count-cells
    (not (area-uk? area)) db/lo-create-raster-calc-clipped

    :else db/lo-create-raster-calc))

(defn create-raster-calc!
  "Create raster calculation in database"
  [area set1 set2 excl-low-count-cells]
  (let [f (create-raster-calc-fn area excl-low-count-cells)
        ;; Define all params for all database functions to save complexity here
        db-params {:area area
                   :set1 set1
                   :set2 set2
                   :mask1 (low-count-cell-mask set1)
                   :mask2 (low-count-cell-mask set2)}]
    (f conn/db db-params)))

(defn cache-filename
  [checksum]
  (str working-dir "/cache_" checksum ".tiff"))

(defn cache-checksum
  [area set1 set2 excl-low-count-cells]
  (let [excl-lcc-s (if excl-low-count-cells "excl-lcc" "incl-lcc")]
    (util/md5 (str area set1 set2 excl-lcc-s))))

(defn copy-cache
  [cache-filename out-filename]
  (do
    (io/copy (java.io.File. cache-filename) (java.io.File. out-filename))
    out-filename))

(defn tiff!
  "Export raster calculation visual from database and save as GeoTIFF"
  [filename area set1 set2 excl-low-count-cells]
  (make-working-dir!)
  (let [checksum (cache-checksum area set1 set2 excl-low-count-cells)
        cache-filename (cache-filename checksum)]
    (if (.exists (io/as-file cache-filename))
      (copy-cache cache-filename filename)

      ;; New raster calculation...
      (let [oid (:oid (create-raster-calc! area set1 set2 excl-low-count-cells))]
        (do
          (io/copy
           (:dat (db/lo-readall conn/db {:oid oid}))
           (java.io.File. cache-filename))
          (db/lo-unlink conn/db {:oid oid})
          (copy-cache cache-filename filename)
          filename)))))

(defn tiff->jpeg!
  "Convert a GeoTIFF to JPEG"
  [tiff jpeg]
  ;; A project for converting GeoTIFF to JPEG can be found here:
  ;; https://github.com/jburnett31/convert-geotiff
  ;; However, it uses im4clj (https://github.com/neatonk/im4clj) which is not under active development.
  ;; The recommendation of im4clj is to use ImageMagick, etc, directly on command line.
  (gdal_translate "-of" "JPEG" "-scale" "-co" "worldfile=yes" tiff jpeg))

(defn tiff->png!
  "Convert a GeoTIFF to PNG"
  [tiff png]
  (let [style-filename (str tiff ".qml")
        style-template (slurp (io/resource raster-style-template))
        stops (colour-map-stops tiff)]
    (do
      (spit
       style-filename
       (-> style-template
           (s/replace "{REPLACE_CC_MAX}" (format colour-map-stop-format (:value (last stops))))
           (s/replace "{REPLACE_CC_MIN}" (format colour-map-stop-format (:value (first stops))))
           (s/replace "{REPLACE_COLOUR_RAMP}" (apply str (colour-map-stops-xml stops)))))
      (python raster-to-png-script tiff style-filename png))))

(defn tiff->vector-zip!
  [tiff dst vector-file-ext ogr-format]
  (let [basename (util/md5 tiff)
        wd (wdir "/" basename "_tmp")
        tmpf (partial str wd "/")
        vector-file (str basename vector-file-ext)]
    (do
      @(exec/sh ["mkdir" wd])
      @(exec/sh ["gdal_polygonize.py" tiff "-f" ogr-format (tmpf vector-file)])
      @(exec/sh ["zip" "-r" dst "."] {:dir wd})
      @(exec/sh ["rm" "-Rf" wd]))))

(defn tiff->shapefile-zip!
  [tiff dst]
  (tiff->vector-zip! tiff dst ".shp" "ESRI Shapefile"))

(defn tiff->mapinfo-zip!
  [tiff dst]
  (tiff->vector-zip! tiff dst ".tab" "Mapinfo File"))
