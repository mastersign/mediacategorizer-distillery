(ns distillery.config
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.data :refer (load-data)]))

;; The default configuration
(def default (load-data (resource "default.cfg")))

(defn value
  "Retrieves a configuration value by its key."
  ([k]
   (let [ks (if (coll? k) (vec k) [k])]
     (get-in default ks)))
  ([k cfg]
   (let [ks (if (coll? k) (vec k) [k])]
     (get-in cfg ks (get-in default ks nil)))))

