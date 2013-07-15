(ns distillery.view.dependencies
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.files :refer :all]))

(def static-resources
  ["reset.css"
   "base.css"
   "layout.css"
   "distillery.css"
   "jquery.js"
   "video.js"
   "video-js.min.css"
   "video-js.png"
   "video-js.swf"
   "vjs.eot"
   "vjs.woff"
   "vjs.ttf"
   "vjs.svg"])

(defn save-dependency
  "Saves a static resource dependency relative to the given HTML file path."
  [^String target-dir ^String resource]
  (let [src (get-path resource)
        trg (get-path target-dir (file-name resource))]
    (copy-file src trg)))

(defn save-dependencies
  "Saves all static resource dependencies relative to the given HTML file path."
  [^String target-dir]
  (doseq [rn static-resources]
    (save-dependency target-dir (resource rn))))
