(ns distillery.text
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.data :refer (load-data)]))

(def resources (load-data (resource "text.edn")))

(defn txt
  [id]
  (let [v (get resources id)]
    (if (keyword? v) (txt v) v)))
