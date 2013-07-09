(ns distillery.processing
  (:import [java.lang.Character])
  (:require [distillery.data :refer :all]))


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

(defn words
  "Returns words from a result sequence."
  [results & {:keys [filters best-phrases]}]
  (let [predicate    (partial multi-filter (vec filters))
        phrase-src   (if best-phrases
                       (partial map best-alternate)
                       (partial mapcat :alternates))]
    (->> results
         (phrase-src)
         (mapcat :words)
         (filter predicate))))

(defn grouped-words
  "Returns ordered statistics over a sequence of phrases."
  [results filters]
  (let
    [source      (words results :best-phrases true :filters filters)
     groups      (group-by
                  #(dissoc % :confidence)
                  source) ;; Group words by text
     confidences (map
                  (partial map-group-items :confidence)
                  groups) ;; Reject additional text per confidence
     squaresums  (map
                  (partial map-pair-value (fn [xs] {:squared-sum (squared-sum xs) :appearance (count xs)}))
                  confidences)] ;; Build square sums of confidence per word
    (reverse (sort-by #(:squared-sum (get % 1)) squaresums)))) ;; Sort the words by sum of squared confidence

(defn format-word-stat
  [[{:keys [lexical-form pronunciation]} {:keys [appearance squared-sum]}]]
  (format "%4d %8.3f: %s (%s)" appearance squared-sum lexical-form pronunciation))
