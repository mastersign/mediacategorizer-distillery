(ns distillery.view.dependencies
  (:import [java.io InputStream])
  (:import [java.nio.file Path Files CopyOption StandardCopyOption])
  (:require [clojure.java.io :as io])
  (:require [mastersign.files :refer :all]))

(def static-resources
  ["reset.css"
   "base.css"
   "layout.css"
   "distillery.css"
   "jquery.js"
   "distillery.js"
   ])

(defn- copy-stream-to-file
  [^InputStream s ^Path p]
  (Files/copy s p (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn- save-dependency
  "Saves a static resource dependency relative to the given HTML file path."
  [^String target-dir ^String rn]
  (let [src-s (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) rn)
        trg (get-path target-dir rn)]
    (copy-stream-to-file src-s trg)
    (.close src-s)))

(defn save-dependencies
  "Saves all static resource dependencies relative to the given HTML file path."
  [^String target-dir]
  (doseq [rn static-resources]
    (save-dependency target-dir rn)))
