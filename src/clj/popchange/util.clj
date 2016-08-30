(ns popchange.util
  (:require  [ring.util.response :as response]))

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
