(ns distillery.view.word
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.transcript :as transcript]))

(defn- in-video-occurrences
  [video word]
  (filter #(= (:video-id %) (:id video)) (:occurrences word)))

(defn- render-statistic
  [{:keys [video word] :as args}]
  (ulist "word_statistic" [(list-item (str "Vorkommen: " (count (in-video-occurrences video word))))
           (list-item (format "Mittlere Erkennungssicherheit: %1.1f%%" (* 100 (:mean-confidence word)) "%"))]))

(defn- render-phrases
  [{:keys [video word] :as args}]
  (let [occurrences (in-video-occurrences video word)
        results (into
                 (sorted-set-by #(< (:start %1) (:start %2)))
                 (map #(get (:results video) (:result-no %)) occurrences))]
    (transcript/render-result-list results :index (:index video) :pivot (:lexical-form word))))

(defn render-video-word-include
  "Renders the include part for the word frame of a video page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-statistic args)
     (render-phrases args)]))