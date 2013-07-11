(ns distillery.processing
  (:require [distillery.data :refer :all]))


(defn word-text
  "Returns the text of a word. The word can be a string or a map with a the key :lexical-form."
  [word]
  (if (map? word) (:lexical-form word) word))

(defn no-point?
  "Checks whether the given string ends with a colon."
  [^String word]
  (= \. (last word)))

(defn not-short?
  "Checks whether a word is long enough to be relevant."
  [^String word]
  (not (or (nil? word) (< (count word) 3))))

(defn noun?
  "Checks whether the given word is a noun."
  [^String word]
  (java.lang.Character/isUpperCase (or (first word) \a)))

(defn best-alternate
  "Return the best alternate phrase of a recgnition result."
  [result]
  (apply max-key :confidence (:alternates result)))

(defn words
  "Returns words from a result sequence."
  [results & {:keys [filters best-phrases]}]
  (let [filters      (map #(comp % word-text) filters)
        predicate    (partial multi-filter (vec filters))
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

