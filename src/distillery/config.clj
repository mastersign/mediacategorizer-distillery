;; # Application Configuration
;; The application configuration is a map with parameters
;; controlling the analysis and result generation process.
;; The application configuration is build by merging the
;; default configuration from `resources/default.cfg`
;; with the [Configuration](data-structures.html#Configuration)
;; in the [Job Description](data-structures.html#JobDescription).

(ns distillery.config
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.data :refer (load-data)]))

;; A public var with a the default configuration.
(def default (load-data (resource "default.cfg")))

(defn value
  "Retrieves the value of a configuration parameter by its key.

  The first argument `k` is the key of the parameter,
  the optional second argument `cfg` is the job configuration
  overlaying the default configuration."
  ([k]
   (let [ks (if (coll? k) (vec k) [k])]
     (get-in default ks)))
  ([k cfg]
   (let [ks (if (coll? k) (vec k) [k])]
     (get-in cfg ks (get-in default ks nil)))))



