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
  "Returns the text of a word.
  The word can be a string or a map with a the key `:lexical-form.`"
  [word]
  (or (if (map? word) (:lexical-form word) word) ""))

(defn no-punctuation?
  "Checks whether the given word ends with a colon."
  [word]
  (let [^String text (:text word)]
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
          (= text "<")
          (= text ">")
          (= text "=")
          (= text "§")
          (= text "$")
          (= text "&")
          (= text "%")
          (= text "?")
          (= text "!")
          (= text "/")
          (= text "\\")))))

(defn not-short?
  "Checks whether a word is long enough to be relevant."
  [word]
  (let [text (word-text word)]
    (not (< (count text) 3))))

(defn noun?
  "Checks whether the given word is a noun."
  [word]
  (let [text (word-text word)
        ^Character c (or (first text) \a)]
    (java.lang.Character/isUpperCase c)))

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
       (safe-max)))

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
  [{:keys [^String lexical-form] :as word}]
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

(defn- compute-index-entry-match-value
  [{:keys [max-occurrence-count] :as index-stats}
   {:keys [mean-confidence occurrence-count] :as entry}]
  (assoc entry
    :match-value (* (double mean-confidence)
                    (/ (double occurrence-count)
                       (double max-occurrence-count)))))

(defn merge-index-entries
  "Merges two index entries for the same word into one."
  [a b]
  (let [res (-> a
      (assoc :occurrences (concat (:occurrences a) (:occurrences b)))
      compute-index-entry-stats)]
    res))

(defn add-video-word-index
  "Builds an index of words for a sequence of recognition results."
  [video & {:keys [predicate]}]
  (let [ws (words (:results video) :predicate predicate)
        add-occurrence (fn [props word]
                         (let [props (or props {:id (:id word)
                                                :lexical-form (:lexical-form word)
                                                :pronunciation (:pronunciation word)})]
                           (assoc props :occurrences
                             (conj (:occurrences props [])
                                   {:video-id (:id video)
                                    :result-no (:result-no word)
                                    :word-no (:no word)
                                    :confidence (:confidence word)}))))
        index (->> ws
                   (map #(assoc % :id (word-identifier %)))
                   (reduce-by-sorted :id add-occurrence nil)
                   (map-values compute-index-entry-stats))
        index-stats {:count (count index)
                     :max-occurrence-count (safe-max (map :occurrence-count (vals index)))}
        index (map-values (partial compute-index-entry-match-value index-stats) index)]
    (assoc video
      :index index
      :index-stats index-stats)))

(defn- category-words
  [{:keys [words] :as category} & {:keys [predicate]}]
  (filter predicate words))

(defn add-category-word-index
  "Builds an index for a sequence of category words."
  [category & {:keys [predicate]}]
  (let [ws (category-words category :predicate predicate)
        add-occurrence (fn [props word]
                        (let [props (or props {:id (:id word)
                                               :lexical-form (:lexical-form word)})]
                          (assoc props :occurrences
                            (conj (:occurrences props [])
                                  {:category-id (:id category)
                                   :no (:no word)
                                   :confidence 1}))))
        index (->> ws
                   (map #(assoc % :id (word-identifier %)))
                   (reduce-by-sorted :id add-occurrence nil)
                   (map-values compute-index-entry-stats))
        index-stats {:count (count index)
                     :max-occurrence-count (safe-max (map :occurrence-count (vals index)))}
        index (map-values (partial compute-index-entry-match-value index-stats) index)]
    (assoc category
      :index index
      :index-stats index-stats)))

(defn- char-to-index-letter
  "Converts every alphabetic character in its upper case and all other characters into '?'."
  [^Character c]
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

(defn- compute-matching-score
  "Builds a [Category Match](data-structures.html#CategoryMatch) between
  the given `video` and `category`."
  [video category]
  (let [cwidx (:index category)
        mwidx (:index video)
        word-scores (->> (keys mwidx)
                         (map (fn [wid] [wid
                                         (* (:match-value (get mwidx wid) 0.0)
                                            (:match-value (get cwidx wid) 0.0))]))
                         (filter #(> (second %) 0))
                         (apply concat)
                         (apply hash-map))
        score (double (apply + (vals word-scores)))]
    {:category-id (:id category)
     :word-scores word-scores
     :score score}))

(defn match-video
  "Builds the matches between all categories in the
  [Analysis Results](data-structures.html#AnalysisResults) `job`
  and the given `video`."
  [{:keys [categories configuration] :as job} video]
  (let [matches (if (not (empty? categories))
                  (->> categories
                       (map #(compute-matching-score video %))
                       (filter #(>= (:score %) 0.0))
                       (map #(vector (:category-id %) %))
                       (apply concat)
                       (apply sorted-map))
                  {})]
  (assoc video
    :matches matches
    :max-score (safe-max (map :score (vals matches))))))

(defn- lookup-matching-score
  [category video]
  (-> video
      (get-in [:matches (:id category)] {:word-scores {} :score 0.0})
      (dissoc :category-id)
      (assoc :video-id (:id video))))

(defn lookup-category-match
  [{:keys [videos] :as job} category]
  (let [matches (->> videos
                  (map #(lookup-matching-score category %))
                  (filter #(>= (:score %) 0.0))
                  (map #(vector (:video-id %) %))
                  (apply concat)
                  (apply sorted-map))]
  (assoc category
    :matches matches
    :max-score (safe-max (map :score (vals matches))))))

