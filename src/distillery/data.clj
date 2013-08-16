(ns distillery.data
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.string :as string])
  (:require [clojure.edn :as edn]))

(defmacro key-comp
  "Creates a comparator, defined by a function which takes an element and returns the key."
  [f-key]
  `(fn [a# b#] (compare (~f-key a#) (~f-key b#))))

(defn load-data
  "Loads the content of a file as EDN formatted data structure."
  [path & opts]
  (with-open
    [r (java.io.PushbackReader. (apply reader (cons path opts)))]
    (edn/read r)))

(defn load-list
  "Loads the content of a file as word list. Whitespace and commas are word separators."
  [path & opts]
  (let [text (apply slurp (cons path opts))]
    (string/split text #"[\s,]+")))

(defn map-group-items
  "Applies a function to the items of the group collection."
  [f [k xs]] [k (map f xs)])

(defn map-pair-value
  "Applies a function to the value of a key-value-pair."
  [f [k v]] [k (f v)])

(defn map-values
  "Applies a function to the values of a map and returns a map with associating the original keys with the transformed values."
  [f m]
  (apply (if (sorted? m) sorted-map hash-map) (apply concat (map #(map-pair-value f %) m))))

(defn reduce-by
  "Groups a collection by a key, computed by key-fn, and reduces the values of each group with f."
  [key-fn f init coll]
  (reduce (fn [summaries x]
            (let [k (key-fn x)]
              (assoc summaries k (f (summaries k init) x))))
          {} coll))

(defn reduce-by-sorted
  "Groups a collection by a key, computed by key-fn, and reduces the values of each group with f.
   Returns a sorted map."
  [key-fn f init coll]
  (reduce (fn [summaries x]
            (let [k (key-fn x)]
              (assoc summaries k (f (summaries k init) x))))
          (sorted-map) coll))

(defn multi-filter
  "Applies a number of predicates to a value and returns true if all predicates are true."
  [predicates x]
  (if (empty? predicates)
    true
    (every? true? ((apply juxt predicates) x))))

(defn sum
  "Computes the sum of a numeric sequence."
  [xs] (apply + xs))

(defn mean
  "Computes the mean value of a numeric sequence."
  [xs]
  (let [cnt (count xs)]
    (if (> cnt 0) (/ (apply + xs) cnt) 0)))

(defn squared-sum
  "Computes the sum of the squared items of a sequence."
  [xs] (apply + (map #(* % %) xs)))
