(ns popchange.lib.maintenance
  (:require [clojure.string :as s]
            [popchange.db.conn :as conn]
            [popchange.db.maintenance :as db]
            [clojure.math.combinatorics :as combo]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.edn :as edn]))

(def unknown-comparison-id 3)

(defn comparison-groups
  "Groups of manually created attribute comparisons"
  []
  (let [joins (db/manual-attrib-comparisons conn/db)]
    (reduce
     (fn [groups join]
       (let [group [(:src join)]]
         (conj groups (cons (:src join) (map #(Integer/parseInt %) (s/split (:dsts join) #","))))))
     '()
     joins)))

(defn comparison-group-combinations
  "All combinations for groups of comparisons"
  []
  (->>
   (reduce
    (fn [combs group]
      (let [gc (combo/combinations group 2)]
        (conj combs (concat gc (map #(reverse %) gc)))))
    '()
    (comparison-groups))
   (mapcat identity) ;; Flatten first level of nesting
   (distinct))) ;; Remove duplicates

;; Call this via the REPL to create completementary comparisons
(defn create-comparison-group-combinations
  []
  (doseq [[src dst] (comparison-group-combinations)]
    ;; Use reversed comparison quality if available, otherwise unknown
    (let [params {:src src :dst dst}
          rq (db/reversed-quality conn/db params)
          cid (if-not (empty? rq) (:comparison_quality_id (first rq)) unknown-comparison-id)
          description (if-not (empty? rq) (:description (first rq)) nil)]
      (db/create-attrib-comparison conn/db (conj params {:cid cid :description description})))))

(defn delete-auto-comparison-group-combinations
  []
  (db/delete-auto-attrib-comparisons conn/db))


;; EXPORTS FOR NICK

(defn write-attribs
  []
  (let [attribs (db/attribs-all conn/db)
        av (into [] (map #(into [] (vals %)) attribs))]
    ;; (def t av)
    (with-open [out-file (io/writer "/tmp/attribs.csv")]
      (csv/write-csv out-file av))))


(defn update-attrib
  [id cenus-year title]
  (db/update-attrib conn/db {:id id :cyear cenus-year :title title}))

(defn update-attribs
  []
  (with-open [in-file (io/reader "/tmp/nick_attribs/attribs-without-headers.csv")]
    (let [rows (doall (drop 8 (csv/read-csv in-file)))]
      (doseq [row rows]
        (let [id (Integer/parseInt (first row))
              cenus-year (Integer/parseInt (second row))
              title (nth row 2)]
          (update-attrib id cenus-year title))))))

(defn create-comparison
  [src dst cid description]
  (db/create-comparison conn/db {:src src :dst dst :cid cid :description description}))

(defn create-comparisons
  []
  (with-open [in-file (io/reader "/tmp/nick_attribs/comparisions-without-tmpcols.csv")]
    (let [rows (doall (csv/read-csv in-file))]
      (doseq [row rows]
        (let [src (Integer/parseInt (first row))
              dst (Integer/parseInt (second row))
              cid (Integer/parseInt (nth row 3))
              description (nth row 4)]
          (create-comparison src dst cid description))))))


;; PROCESS DATA COLLECTED DURING WORKSHOP

(def workshop-data-dir "/tmp/popchange")

(defn workshop-files
  [re]
  (let [dir (io/file workshop-data-dir)
        fs (file-seq dir)] ;; All files in directory
    (filter #(re-find re (.getName %)) fs)))

(defn feedback-files
  []
  (workshop-files #"feedback.*\.edn"))

(defn edn-reader
  [file]
  (edn/read-string
   {:readers {'object (fn [o])}} ;; Return nil for unknown types (e.g. object)
   (slurp file)))

(defn feedback-files->txt
  []
  (doseq [file (feedback-files)]
    (let [feedback (edn-reader file)]
      (spit
       (s/replace (.getPath file) ".edn" ".txt")
       (str
        "Name: " (-> feedback :params :name) "\n"
        "Email: " (-> feedback :params :email) "\n"
        "IP Address: " (-> feedback :remote-addr) "\n\n"
        (-> feedback :params :comments))))))

(defn registrations
  []
  (map #(edn-reader %) (workshop-files #"registration.*\.edn")))

(defn parse-registrations
  []
  (reduce
   (fn [registrations registration]
     (conj registrations {:name (-> registration :params :name)
                          :email (-> registration :params :email)
                          :sector (-> registration :params :sector)
                          :mailing-list (if (-> registration :params :mailinglist) true false)
                          :ip-address (-> registration :remote-addr)}))
   '()
   (registrations)))

(defn registrations->csv
  []
  (with-open [out-file (io/writer (str workshop-data-dir "/registrations.csv"))]
    (csv/write-csv out-file (into [] (map #(into [] (vals %)) (parse-registrations)))))
  )



;; DOWNLOADABLE FILES


(defn downloadable-files
  []
  (map
   #(assoc
     %
     :counts-table (str
                    "ag_"
                    (-> % :filename
                        (s/replace #"\.asc$" "")
                        (s/replace #"\." "_")
                        ;; (s/replace #"5a_ascii_grid" "ag_")
                        ;; (s/lower-case)
                        ))
     :path (-> % :path (subs 2)) ;; Remove ./ at beginning of path
     )
   (edn/read-string (slurp (io/resource "maintenance/popchange_data_index.edn")))))

(defn attrib-files
  [attrib downloadable-file]
  (let [filename (:filename downloadable-file)
        path (:path downloadable-file)
        dir (partial str (subs path 0 (- (count path) (count filename))))
        filename-no-ext (s/replace filename #"\.asc$" "")
        base-filename (partial str filename-no-ext)
        base-lookup-filename (partial str (s/replace filename-no-ext #"5a_ascii_grid" "lookup_"))

        counts-lookup-filename (base-lookup-filename ".csv")
        counts-lookup-types-filename (base-lookup-filename ".csvt")
        rates-filename (base-filename "_pc.asc")]
    {:counts-filename filename
     :counts-path path
     :counts-lookup-filename counts-lookup-filename
     :counts-lookup-path (dir counts-lookup-filename)
     :counts-lookup-types-filename counts-lookup-types-filename
     :counts-lookup-types-path (dir counts-lookup-types-filename)
     :rates-filename rates-filename
     :rates-path (dir rates-filename)}))

(defn existing-attrib-files
  [attrib-files downloadable-files]
  (reduce
   (fn [existing-files k]
     (let [df-k (keyword (last (s/split (str k) #"-")))]
       (assoc existing-files k (some #(if (= (k attrib-files) (df-k %)) (df-k %)) downloadable-files))))
   {}
   (keys attrib-files)))

(defn attrib-downloadable-files
  []
  (let [dfs (downloadable-files)]
    (reduce
     (fn [attribs attrib]
       (if-let [df (some #(if (= (:counts-table %) (:counts_table attrib)) %) dfs)]
         (conj
          attribs
          (merge attrib (->
                         (attrib-files attrib df)
                         (existing-attrib-files dfs)
                         )))
         
         attribs))
     []
     (db/attribs-all conn/db))))


(defn update-attrib-files
  []
  (map #(db/update-attrib-files conn/db %) (attrib-downloadable-files)))

