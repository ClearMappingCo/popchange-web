(ns popchange.util
  (:require  [ring.util.response :as response]
             [clj-commons-exec :as exec]))

(defn md5
  [s]
  (let [hash-bytes
        (doto (java.security.MessageDigest/getInstance "MD5")
          (.reset)
          (.update (.getBytes s))
          )]
    (.toString
     (new java.math.BigInteger 1 (.digest hash-bytes)) ;; Positive and the size of the number
     16))) ;; Use base16 i.e. hex

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))

(defn byte-array-response
  "Ring response to send byte array to browser"
  [filename content-type]
  (let [byte-array (slurp-bytes filename)]
    (-> (response/response (java.io.ByteArrayInputStream. byte-array))
        (response/content-type content-type)
        (response/header "Content-Length" (alength byte-array)))))


;; https://github.com/matlux/stevedore-example/blob/master/src/stevedoretest/core.clj

(defn write-file
  "Writes a value to a file"
  [value out-file]
  (spit out-file "" :append false)
  (with-open [out-data (clojure.java.io/writer out-file)]
      (.write out-data (str value))))

(defn read-file [in-file]
  (with-open [rdr (clojure.java.io/reader in-file)]
    (reduce conj [] (line-seq rdr))))

(defn run-script [s]
  (let [filename (str "/tmp/" (System/currentTimeMillis) ".sh")]
    (write-file s filename)
    @(exec/sh ["chmod" "u+x" filename])
    (let [out @(exec/sh [filename])]
      @(exec/sh ["rm" filename])
      out)))
