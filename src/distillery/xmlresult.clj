(ns distillery.xmlresult
  (:import [java.util.Locale])
  (:import [java.io.FileWriter])
  (:require [clojure.java.io :refer (writer)])
  (:require [clojure.data.xml :as xml]))

(defn- tag-list
  [tag-name f xs]
  (let [tags (map f xs)]
    (vec (concat [tag-name {:length (count tags)}] tags))))

(defn- format-invariant
  [fmt & args]
  (String/format java.util.Locale/US fmt (to-array args)))

(defn- category-resource-tag
  [job category resource]
  [:Resource {:type (name (:type resource))} (:url resource)])

(defn- category-match-tag
  [job category [video-id match]]
  [:Match {:video-id video-id
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

(defn- video-match-tag
  [job video [category-id match]]
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
  [job video]
  [:SpeechRecognition {:profile (:recognition-profile video)
                       :profile-name (:recognition-profile-name video)}
   (tag-list :PhraseList
             (partial recognized-phrase job)
             (:results video))])

(defn- video-tag
  [job video]
  [:Media {:id (:id video)}
   [:Name {} (:name video)]
   (tag-list :MatchList
             (partial video-match-tag job video)
             (filter
              (fn [[id match]] (>= (:score match) (get-in job [:configuration :min-match-score])))
              (:matches video)))
   [:Source {} (:video-file video)]
   (speech-recognition-tag job video)])

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
              (partial video-tag job)
              (:videos job))]))

(defn save-result
  [path job]
  (let [tags (job-result-tags job)]
    (with-open [w (writer path)]
      (xml/indent tags w))))





