(ns distillery.view.word
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.data :refer [key-comp any?]])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.transcript :as transcript]))

(defn- in-video-occurrences
  [video word]
  (filter #(= (:video-id %) (:id video)) (:occurrences word)))

(defn- render-word-statistic
  [{:keys [word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :main-word-occurrences) (:occurrence-count word)))
          (list-item (str (txt :main-word-mean-confidence) (format "%1.1f%%" (* 100 (:mean-confidence word)) "%")))]))

(defn- render-video-word-statistic
  [{:keys [video word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :video-word-occurrences) (count (in-video-occurrences video word))))
          (list-item (str (txt :video-word-mean-confidence) (format "%1.1f%%" (* 100 (:mean-confidence word)) "%")))]))

(defn- render-category-word-statistic
  [{:keys [category word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :category-word-occurrences) (:occurrence-count word)))]))

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
  [{:keys [categories word] :as args}]
  (let [word-id (:id word)
        tmp (map
             (fn [{:keys [matches] :as category}]
                (vals matches))
             categories)
        cats (filter
              (fn [{:keys [matches] :as category}]
                (any?
                 (fn [{:keys [word-scores] :as match}]
                   (contains? word-scores word-id))
                 (vals matches)))
              categories)
        items (map
               (fn [{:keys [id name] :as category}]
                 (list-item
                  (link (str "categories/" id "/index.html?word=" word-id)
                        name)))
               cats)]
    (if (> (count items) 0)
      (ulist items)
      (paragraph (txt :main-word-category-list-empty)))))

(defn- render-category-word-video-list
  [{:keys [videos category word max-word-score] :as args}]
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
                             [(format "%.4f  " (/ score max-word-score))
                              (link (str "../../videos/" video-id "/index.html?word=" word-id)
                                    (:name video))])))))]
    (if (> (count items) 0)
      (ulist items)
      (paragraph (txt :category-word-video-list-empty)))))

(defn- render-video-word-category-list
  [{:keys [categories video word max-word-score] :as args}]
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
                             [(format "%.4f  " (/ score max-word-score))
                              (link (str "../../categories/" category-id "/index.html?word=" word-id)
                                    (:name category))])))))]
    (if (> (count items) 0)
      (ulist items)
      (paragraph (txt :video-word-category-list-empty)))))

(defn render-word-include
  "Renders the include part for the word frame."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-word-statistic args)
     (headline 4 (txt :main-word-videos-h))
     (render-word-video-list args)
     (headline 4 (txt :main-word-categories-h))
     (render-word-category-list args)]))

(defn render-video-word-include
  "Renders the include part for the word frame of a video page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-video-word-statistic args)
     (headline 4 (txt :video-word-phrases-h))
     (render-video-word-phrases args)
     (headline 4 (txt :video-word-categories-h))
     (render-video-word-category-list args)]))

(defn render-category-word-include
  "Renders the include part for the word frame of a category page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form]} word]
    [(headline 4 "word_headline" lexical-form)
     (render-category-word-statistic args)
     (headline 4 (txt :category-word-videos-h))
     (render-category-word-video-list args)]))
