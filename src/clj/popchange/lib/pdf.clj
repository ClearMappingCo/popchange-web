(ns popchange.lib.pdf
  (:require [clj-pdf.core :refer [pdf]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [popchange.config :as cfg]))

(def pdf-dir "pdf")
(def working-dir (str cfg/working-dir "/" pdf-dir))

(def legend-steps-text-only-filename "legend_steps_text_only.svg")
(def legend-gradient-filename "legend_gradient.svg")

(defn working-dir-file
  [filename]
  (str working-dir "/" filename))

(defn prep-working-dir!
  []
  (let [wd (io/file working-dir)
        files (conj
               (map #(into [] [pdf-dir (format "step_%d.svg" %)]) (range 6))
               ["public/img" "popchange_logo.png"])]
    (if-not (.isDirectory wd)
      (do
        (.mkdir wd)
        (doseq [[src-dir filename] files]
          (with-open [in (io/input-stream (io/resource (str src-dir "/" filename)))] ;; Extract from running WAR
            (io/copy in (io/file (working-dir-file filename)))))))))

(def footer-colour [128 128 128])

(defn pdf-meta
  [generated-by]
  {:title "PopChange raster calculation"
   :pages false
   :bottom-margin 0

   :footer {:page-numbers false
            :align :center
            :start-page 2 ;; Hides page numbers when using table
            :y 106
            :table [:pdf-table {:border false :page-numbers false}
                    [1 1]
                    ;; [[:pdf-cell {:colspan 2} [:line]]]
                    [[:pdf-cell
                      [:paragraph {:color footer-colour} (str "Generated on " (.format (java.text.SimpleDateFormat. "dd/MM/yyyy") (new java.util.Date)) " by " generated-by)]]
                     [:pdf-cell {:align :right}
                      [:paragraph {:color footer-colour} "Details at " [:anchor {:target "https://popchange.liverpool.ac.uk"} "https://popchange.liverpool.ac.uk"]]]]
                    [[:pdf-cell {:align :center :colspan 2}
                      [:paragraph {:color footer-colour} "PopChange was funded by the Economic and Social Research Council (ESRC) under the Secondary Data Analysis Initiative (SDAI), Phase 2 (project ES/L014769/1) and this support is acknowledged gratefully."]
                      [:spacer]
                      [:paragraph {:color footer-colour} "Supplied by " [:anchor {:target "http://www.clearmapping.co.uk"} "Clear Mapping Co"]]]]]
            }})

(defn set-comparison
  [set1 set2]
  (if (= (:title set1) (:title set2))
    (str (:title set1) " from " (:cenus-year set1) " and " (:cenus-year set2))
    (str (:title set1) " from " (:cenus-year set1) " and " (:title set2) " from " (:cenus-year set2))))

(defn legend-steps-svg
  [steps]
  (reduce
   (fn [svg i]
     (s/replace svg (str "LSTEP" i) (format "%5.2f" (nth steps i))))
   (slurp (io/resource (str pdf-dir "/" legend-steps-text-only-filename)))
   (range (count steps))))


(defn save!
  [filename raster-calculation-filename area set1 set2 legend-steps generated-by]
  (prep-working-dir!)
  (pdf
   [(pdf-meta generated-by)
    [:image {:scale 25 :align :center} (javax.imageio.ImageIO/read (io/file (working-dir-file "popchange_logo.png")))]

    [:spacer]
    [:paragraph {:align :center} "This is a comparison on 1km" [:superscript "2"] " grid cells for "
     area " on "
     (set-comparison set1 set2) "."]

    [:spacer]
    [:image {:yscale 0.7 :xscale 0.7 :align :center} raster-calculation-filename]

    [:svg {:translate [40 628] :scale [0.7 1]} (slurp (io/resource (str pdf-dir "/" legend-gradient-filename)))]
    [:svg {:translate [70 630] :scale [0.3 0.3]} (legend-steps-svg legend-steps)]




    (comment
      [:paragraph {:style :bold} "Legend"]
      
      (for [i (reverse (range (count legend-steps)))]
        [:svg {:under true :translate [42 (+ (- 636 (* i 16)) 80)] :scale 1}
         (io/file (working-dir-file (str "step_" i ".svg")))])

      (reverse
       (map
        (fn [step]
          [:paragraph {:keep-together true :indent 20} (format "%.2f" step)])
        legend-steps)))

    ]
   filename))
