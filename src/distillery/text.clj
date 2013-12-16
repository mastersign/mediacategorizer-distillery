(ns distillery.text
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.data :refer (load-data)]))

(def resources (load-data (resource "text.edn")))

(defn txt
  [id]
  (let [v (get resources id (format "TEXT WITH ID '%s' NOT FOUND!" (name id)))]
    (if (keyword? v) (txt v) v)))
