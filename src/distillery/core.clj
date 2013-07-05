(ns distillery.core
  (:import [java.lang.Character])
  (:import [java.io.PushbackReader])
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.edn :as edn])
  (:require [clojure.pprint :refer (pprint)]))


(defn load-data
  "Loads the content of a file as EDN formatted data structure."
  [path]
  (with-open
    [r (java.io.PushbackReader. (reader path))]
    (edn/read r)))

(defn best-alternate
  "Return the best alternate phrase of a recgnition result."
  [result]
  (apply max-key :confidence (:alternates result)))

(defn noun?
  "Checks wheter the given word is a noun.
  The given word can be the word map from a recognition result with key :text
  or can be a plain string."
  [word]
  (let [word (if (map? word) (:text word) word)]
    (if (or (nil? word) (< (count word) 3))
      false
      (Character/isUpperCase (first word)))))

(defn map-group-items [f [k xs]] [k (map f xs)])

(defn map-pair-value [f [k v]] [k (f v)])

(defn sum [xs] (apply + xs))

(defn squared-sum [xs] (apply + (map #(* % %) xs)))


;----------------------------

(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\11.01 Theoretische, technische, praktische, angewandte Informatik.clj")

(def results (load-data path))
(def phrases (map best-alternate results))
;(def phrases (mapcat :alternates results))
(def words (mapcat :words phrases))
(def nouns (filter noun? words))

(def grouped-nouns
  (let
    [groups (group-by :text nouns)
     confidences (map (partial map-group-items :confidence) groups)
     squaresums (map (partial map-pair-value squared-sum) confidences)
     ordered (reverse (sort-by #(get % 1) squaresums))]
    ordered))

(pprint (take 20 grouped-nouns))
