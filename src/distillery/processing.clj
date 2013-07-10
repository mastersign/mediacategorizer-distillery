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

(defn word-group-stats
  "Returns a map with statistics for a grouped word."
  [word-group]
  (->> word-group
       (map-group-items :confidence)
       (map-pair-value (fn [xs] {:squared-sum (squared-sum xs) :appearance (count xs)}))
       (apply merge)))

(defn grouped-words
  "Returns ordered statistics over a sequence of phrases."
  [results filters]
  (->> (words results :best-phrases true :filters filters)
       (group-by #(dissoc % :confidence))
       (map word-group-stats)))

(defn format-word-stat
  [{:keys [lexical-form pronunciation appearance squared-sum]}]
  (format "%4d %8.3f: %s (%s)" appearance squared-sum lexical-form pronunciation))

