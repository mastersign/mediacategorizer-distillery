(ns distillery.data
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.edn :as edn]))

(defn load-data
  "Loads the content of a file as EDN formatted data structure."
  [path]
  (with-open
    [r (java.io.PushbackReader. (reader path))]
    (edn/read r)))

(defn map-group-items
  "Applies a function to the items of the group collection."
  [f [k xs]] [k (map f xs)])

(defn map-pair-value
  "Applies a function to the value of a key-value-pair."
  [f [k v]] [k (f v)])

(defn multi-filter
  "Applies a number of predicates to a value and returns true if all predicates are true."
  [predicates x]
  (if (empty? predicates)
    true
    (every? true? ((apply juxt predicates) x))))

(defn sum
  "Computes the sum of a sequence."
  [xs] (apply + xs))

(defn squared-sum
  "Computes the sum of the squared items of a sequence."
  [xs] (apply + (map #(* % %) xs)))
