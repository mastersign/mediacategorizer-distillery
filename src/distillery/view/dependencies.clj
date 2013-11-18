(ns distillery.view.dependencies
  (:import java.nio.file.Files)
  (:require [clojure.java.io :as io])
  (:require [distillery.files :refer :all]))

(def static-resources
  ["reset.css"
   "base.css"
   "layout.css"
   "distillery.css"
   "jquery.js"
   "distillery.js"
;   "video.js"
;   "video-js.min.css"
;   "video-js.png"
;   "video-js.swf"
;   "vjs.eot"
;   "vjs.woff"
;   "vjs.ttf"
;   "vjs.svg"
   ])

(defn- copy-stream-to-file
  [^java.io.InputStream s ^java.nio.file.Path p]
  (java.nio.file.Files/copy s p (into-array java.nio.file.CopyOption [java.nio.file.StandardCopyOption/REPLACE_EXISTING])))

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
