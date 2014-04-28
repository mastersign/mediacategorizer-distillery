(ns distillery.xmlresult
  (:import [java.util Locale])
  (:require [clojure.java.io :refer (writer)])
  (:require [clojure.data.xml :as xml]))

(defn- tag-list
  [tag-name f xs]
  (let [tags (map f xs)]
    (vec (concat [tag-name {:length (count tags)}] tags))))

(defn- format-invariant
  [fmt & args]
  (String/format Locale/US fmt (to-array args)))

(defn- category-resource-tag
  [job category resource]
  [:Resource {:type (name (:type resource))} (:url resource)])

(defn- category-match-tag
  [job category [medium-id match]]
  [:Match {:medium-id medium-id
           :score (format-invariant "%.6f" (:score match))}])

(defn- category-tag
  [job category]
  [:Category {:id (:id category)}
   [:Name {} (:name category)]
   (tag-list :MatchList
             (partial category-match-tag job category)
             (filter
              (fn [[id match]] (>= (:score match) (get-in job [:configuration :min-match-score])))
              (:matches category)))
   (tag-list :ResourceList
             (partial category-resource-tag job category)
             (:resources category))])

(defn- medium-match-tag
  [job medium [category-id match]]
  [:Match {:category-id category-id
           :score (format-invariant "%.6f" (:score match))}])

(defn- recognized-word
  [job phrase word]
  [:Word {:no (:no word)
          :confidence (:confidence word)}
   (:text word)])

(defn- recognized-phrase
  [job result]
  [:Phrase {:no (:no result)
            :start (:start result)
            :duration (:duration result)
            :confidence (:confidence result)}
   [:Text {} (:text result)]
   ;(tag-list :WordList
   ;          (partial recognized-word job result)
   ;          (:words result))
   ])

(defn- speech-recognition-tag
  [job medium]
  [:SpeechRecognition {:profile (:recognition-profile medium)
                       :profile-name (:recognition-profile-name medium)}
   (tag-list :PhraseList
             (partial recognized-phrase job)
             (:results medium))])

(defn- medium-tag
  [job medium]
  [:Media {:id (:id medium)}
   [:Name {} (:name medium)]
   (tag-list :MatchList
             (partial medium-match-tag job medium)
             (filter
              (fn [[id match]] (>= (:score match) (get-in job [:configuration :min-match-score])))
              (:matches medium)))
   [:Source {} (:medium-file medium)]
   (speech-recognition-tag job medium)])

(defn- occurrence-tag
  [job word {:keys [medium-id result-no word-no] :as occurrence}]
  (let [medium (first (filter #(= medium-id (:id %)) (:media job)))
        phrase-start (get-in medium [:results result-no :start])]
    [:Occurrence {:medium medium-id
                  :phrase result-no
                  :no word-no
                  :phrase-start (format-invariant "%.2f" (double phrase-start))}]))

(defn- word-tag
  [job {:keys [lexical-form pronunciation occurrence-count occurrences] :as word}]
  [:Word {:lexical-form lexical-form
          :pronunciation pronunciation}
   (tag-list :OccurrenceList
             (partial occurrence-tag job word)
             occurrences)])

(defn- job-result-tags
  [job]
  (xml/sexp-as-element
   [:MediaCategorizerResults {:tool-version (:media-categorizer-version job)}
    [:JobInfo {}
     [:Name {} (:job-name job)]
     [:Description {} (:job-description job)]]
    (tag-list :CategoryList
              (partial category-tag job)
              (:categories job))
    (tag-list :MediaList
              (partial medium-tag job)
              (:media job))
    (tag-list :Glossary
              (partial word-tag job)
              (map val (:words job)))]))

(defn save-result
  [path job]
  (let [tags (job-result-tags job)]
    (with-open [w (writer path)]
      (xml/indent tags w))))
