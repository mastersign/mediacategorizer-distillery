(ns distillery.processing
  (:require [clojure.string :as string])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all]))

(defn reverse-index-results
  "Annotates the result data structures with indices pointing upwards."
  [results]
  (let [t-rword (fn [word result]
                  (assoc word
                    :result-no (:no result)))
        t-pword (fn [word alt result]
                  (assoc word
                    :alt-no (:no alt)
                    :result-no (:no result)))
        t-alt (fn [alt result]
                   (assoc alt
                     :result-no (:no result)
                     :words (vec (map #(t-pword % alt result) (:words alt)))))
        t-result (fn [result]
                   (-> result
                       (assoc :words (vec (map #(t-rword % result) (:words result))))
                       (assoc :alternates (vec (map #(t-alt % result) (:alternates result))))))]
    (vec (map t-result results))))

(defn strip-alternates
  "Removes potential alternate phrases from results for memory optimization."
  [results]
  (map #(dissoc % :alternates) results))

(defn result-of-word
  "Returns the result which contains the given word."
  [word results]
  (get results (:result-no word)))

(defn phrase-of-word
  "Returns the phrase which contains the given word.
   The phrase can be a result."
  [word results]
  (if (contains? word :alt-no)
    (get (get results (:result-no word)) (:alt-no word))
    (get results (:result-no word))))

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
          (= text "@")
          (= text "(")
          (= text ")")
          (= text "[")
          (= text "]")
          (= text "{")
          (= text "}")
          (= text "§")
          (= text "$")))))

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
      (>= (cfg/value :min-confidence))))

(defn good-confidence?
  "Checks wheter the given word was recognized with good confidence."
  [word]
  (-> word
      :confidence
      (>= (cfg/value :good-confidence))))

(defn alternate-phrases
  "Returns alternate phrases of the recognition results."
  [results]
  (mapcat :alternates results))

(defn words
  "Returns words without statistic infos from a result sequence."
  [phrases & {:keys [predicate]}]
  (let [predicate (or predicate (fn [x] true))]
    (->> phrases
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
    (->> (words results :predicate word-filter)
         (group-by #(dissoc % :no :result-no :alt-no :confidence))
         (map process-word-group)
         (filter group-filter))))

(defn most-frequent-word
  "Returns the most frequent word from all words in the results."
  [results]
  (->> (words results)
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
                   (< (:confidence %) (cfg/value :good-confidence))
                   (>= (:relative-appearance %) (cfg/value :min-relative-appearance)))))))

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

(defn word-identifier
  "Creates an identifier for a word.
   The identifier can be used as HTML/XML ID or as filename."
  [{:keys [lexical-form] :as word}]
  (comment "TODO Needs to be improved for arbitrary characters!")
  (-> lexical-form
      (.replace " " "_")
      (.toLowerCase)
      (.replace "ß" "ss")
      (.replace "ä" "ae")
      (.replace "ö" "oe")
      (.replace "ü" "ue")))

(defn- compute-index-entry-stats
  [{:keys [occurrences] :as entry}]
  (assoc entry
    :occurrence-count (count occurrences)
    :mean-confidence (mean (map :confidence occurrences))))

(defn merge-index-entries
  "Merges two index entries for the same word into one."
  [a b]
  (let [res (-> a
      (assoc :occurrences (concat (:occurrences a) (:occurrences b)))
      compute-index-entry-stats)]
    res))

(defn video-word-index
  "Builds an index of words for a sequence of recognition results."
  [video & {:keys [predicate]}]
  (let [ws (words (:results video) :predicate predicate)
        add-occurrence (fn [props word]
                         (let [props (or props {:id (word-identifier word)
                                                :lexical-form (:lexical-form word)
                                                :pronunciation (:pronunciation word)})]
                           (assoc props :occurrences
                             (conj (:occurrences props [])
                                   {:video-id (:id video)
                                    :result-no (:result-no word)
                                    :word-no (:no word)
                                    :confidence (:confidence word)}))))
        index (reduce-by-sorted :lexical-form add-occurrence nil ws)]
    (map-values compute-index-entry-stats index)))

(defn- char-to-index-letter
  "Converts every alphabetic character in its upper case and all other characters into '?'."
  [c]
  (let [c (java.lang.Character/toUpperCase c)
        n (int c)]
    (if (and (>= n 65) (<= n 91)) c \?)))

(defn partition-index
  "Partitions an index into sections.
   One section for every letter and one section for other symbols.
   The partitioned index is a map with a letter as key and the
   index partiton as value."
  [index]
  (->> index
       (group-by (comp char-to-index-letter first first))
       (map-values #(apply sorted-map (apply concat %)))))

