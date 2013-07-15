(ns distillery.processing
  (:require [clojure.string :as string])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all]))


(defn word-text
  "Returns the text of a word. The word can be a string or a map with a the key :lexical-form."
  [word]
  (or (if (map? word) (:lexical-form word) word) ""))

(defn no-punctuation?
  "Checks whether the given word ends with a colon."
  [word]
  (let [text (:text word)]
    (not (or
          (.endsWith text ".")
          (.endsWith text ",")
          (.endsWith text ":")
          (.endsWith text ";")
          (= text "(")
          (= text ")")))))

(defn not-short?
  "Checks whether a word is long enough to be relevant."
  [word]
  (let [text (word-text word)]
    (not (< (count text) 3))))

(defn noun?
  "Checks whether the given word is a noun."
  [word]
  (let [text (word-text word)]
    (java.lang.Character/isUpperCase (or (first text) \a))))

(defn min-confidence?
  "Checks wheter the given word was recognized with a minimum of confidence."
  [word]
  (-> word
      :confidence
      (>= cfg/min-confidence)))

(defn good-confidence?
  "Checks wheter the given word was recognized with good confidence."
  [word]
  (-> word
      :confidence
      (>= cfg/good-confidence)))

(defn best-alternate-phrase
  "Returns the best alternate phrase of a recgnition result."
  [result]
  (apply max-key :confidence (:alternates result)))

(defn- words
  "Returns words without statistic infos from a result sequence."
  [results & {:keys [predicate best-phrases]}]
  (let [predicate    (or predicate (fn [x] true))
        phrase-src   (if best-phrases
                       (partial map best-alternate-phrase)
                       (partial mapcat :alternates))]
    (->> results
         (phrase-src)
         (mapcat :words)
         (filter predicate))))

(defn- process-word-group
  "Returns a word with statistic infos for a grouped word."
  [word-group]
  (->> word-group
       (map-group-items :confidence)
       (map-pair-value (fn [xs] {:confidence (mean xs)
                                 :scs (squared-sum xs)
                                 :appearance (count xs)}))
       (apply merge)))

(defn grouped-words
  "Returns ordered statistics over a sequence of phrases."
  [results word-filters group-filters]
  (let [word-filter (partial multi-filter (vec word-filters))
        group-filter (partial multi-filter (vec group-filters))]
    (->> (words results :best-phrases cfg/best-phrases-only :predicate word-filter)
         (group-by #(dissoc % :confidence))
         (map process-word-group)
         (filter group-filter))))

(defn most-frequent-word
  "Returns the most frequent word from all words in the results."
  [results]
  (->> (words results :best-phrases cfg/best-phrases-only)
       (group-by :lexical-form)
       (map (fn [[text group]] [text (count group)]))
       (apply max-key #(get % 1))))

(defn max-appearance
  "The appearance of the most frequent word in the given words."
  [words]
  (->> words
       (map :appearance)
       (apply max)))

(defn correction-candidates
  "Returns frequent words that are most likely false recognized."
  [words]
  (let [norm (max-appearance words)]
    (->> words
         (map #(assoc % :relative-appearance (if (> (:appearance %) 1) (/ (:appearance %) norm) 0)))
         (filter #(and
                   (< (:confidence %) cfg/good-confidence)
                   (>= (:relative-appearance %) cfg/min-relative-appearance))))))

(defn format-word-stat
  "Creates a formatted string for the given word."
  [{:keys [lexical-form pronunciation appearance scs confidence]}]
  (format "%4d %8.3f %5.1f %%: %s (%s)"
          appearance
          scs
          (* 100 confidence)
          lexical-form
          pronunciation))

(defn print-word-list
  "Prints a list of words."
  [words]
  (->> words
       (map format-word-stat)
       (cons "---------------------------------------------------")
       (cons "#  N      SCS    CONF: Lexical Form (Pronunciation)")
       (string/join "\n")
       println))
