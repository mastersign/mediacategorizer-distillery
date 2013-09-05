(ns distillery.view.word
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.data :refer [key-comp]])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.transcript :as transcript]))

(defn- in-video-occurrences
  [video word]
  (filter #(= (:video-id %) (:id video)) (:occurrences word)))

(defn- render-word-statistic
  [{:keys [word] :as args}]
  (ulist "word_statistic"
         [(list-item (str "Vorkommen: " (:occurrence-count word)))
          (list-item (format "Mittlere Erkennungssicherheit: %1.1f%%" (* 100 (:mean-confidence word)) "%"))]))

(defn- render-video-word-statistic
  [{:keys [video word] :as args}]
  (ulist "word_statistic"
         [(list-item (str "Vorkommen: " (count (in-video-occurrences video word))))
          (list-item (format "Mittlere Erkennungssicherheit: %1.1f%%" (* 100 (:mean-confidence word)) "%"))]))

(defn- render-category-word-statistic
  [{:keys [category word] :as args}]
  (ulist "word_statistic"
         [(list-item (str "Vorkommen: " (:occurrence-count word)))]))

(defn- render-video-word-phrases
  [{:keys [video word] :as args}]
  (let [occurrences (in-video-occurrences video word)
        results (into
                 (sorted-set-by (key-comp :start))
                 (map #(get (:results video) (:result-no %)) occurrences))]
    (transcript/render-transcript results :index (:index video) :pivot (:lexical-form word))))

(defn- render-video-list-item
  [word-id {:keys [id name] :as video}]
  (list-item (link (str "videos/" id "/index.html?word=" word-id) name)))

(defn- render-word-video-list
  [{:keys [word] :as args}]
  (let [word-id (:id word)
        video-ids (set (map :video-id (:occurrences word)))
        videos (filter #(contains? video-ids (:id %)) (:videos args))]
    (ulist (map (partial render-video-list-item word-id) videos))))

(defn- render-word-category-list
  [{:keys [word] :as args}]
  (TODO "Word Category List"))

(defn- render-category-word-video-list
  [{:keys [videos category word] :as args}]
  (let [video-fn (fn [id] (first (filter #(= id (:id %)) videos)))
        category-id (:id category)
        word-id (:id word)
        items (->> (vals (:matches category))
                   (map (fn [{:keys [video-id word-scores] :as match}]
                          [video-id
                           (get word-scores word-id)]))
                   (filter #(not (nil? (second %))))
                   (map (fn [[video-id score]]
                          (let [video (video-fn video-id)]
                            (list-item
                             [(format "%2.4f  " score)
                              (link (str "../../videos/" video-id "/index.html?word=" word-id)
                                    (:name video))])))))]
    (if (> (count items) 0)
      (ulist items)
      (paragraph "Dieses Wort kommt in keinem Video vor."))))

(defn- render-video-word-category-list
  [{:keys [categories video word] :as args}]
  (let [category-fn (fn [id] (first (filter #(= id (:id %)) categories)))
        video-id (:id video)
        word-id (:id word)
        items (->> (vals (:matches video))
                   (map (fn [{:keys [category-id word-scores] :as match}]
                          [category-id
                           (get word-scores word-id)]))
                   (filter #(not (nil? (second %))))
                   (map (fn [[category-id score]]
                          (let [category (category-fn category-id)]
                            (list-item
                             [(format "%2.4f  " score)
                              (link (str "../../categories/" category-id "/index.html?word=" word-id)
                                    (:name category))])))))]
    (if (> (count items) 0)
      (ulist items)
      (paragraph "Dieses Wort kommt in keiner Kategorie vor."))))

(defn render-word-include
  "Renders the include part for the word frame."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-word-statistic args)
     (headline 4 "Videos")
     (render-word-video-list args)
     (headline 4 "Kategorien")
     (render-word-category-list args)]))

(defn render-video-word-include
  "Renders the include part for the word frame of a video page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-video-word-statistic args)
     (headline 4 "Phrasen")
     (render-video-word-phrases args)
     (headline 4 "Kategorien")
     (render-video-word-category-list args)]))

(defn render-category-word-include
  "Renders the include part for the word frame of a category page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form]} word]
    [(headline 4 "word_headline" lexical-form)
     (render-category-word-statistic args)
     (headline 4 "Videos")
     (render-category-word-video-list args)]))


