(ns distillery.view.word
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.data :refer [key-comp any?]])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.transcript :as transcript]))

(defn- in-medium-occurrences
  [medium word]
  (filter #(= (:medium-id %) (:id medium)) (:occurrences word)))

(defn- render-word-statistic
  [{:keys [word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :main-word-occurrences) (:occurrence-count word)))
          (list-item (str (txt :main-word-mean-confidence) (format "%1.1f%%" (* 100 (:mean-confidence word)) "%")))]))

(defn- render-medium-word-statistic
  [{:keys [medium word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :medium-word-occurrences) (count (in-medium-occurrences medium word))))
          (list-item (str (txt :medium-word-mean-confidence) (format "%1.1f%%" (* 100 (:mean-confidence word)) "%")))]))

(defn- render-category-word-statistic
  [{:keys [category word] :as args}]
  (ulist "word_statistic"
         [(list-item (str (txt :category-word-occurrences) (:occurrence-count word)))]))

(defn- render-medium-word-phrases
  [{:keys [medium word] :as args}]
  (let [occurrences (in-medium-occurrences medium word)
        results (into
                 (sorted-set-by (key-comp :start))
                 (map #(get (:results medium) (:result-no %)) occurrences))]
    (transcript/render-transcript results :index (:index medium) :pivot (:lexical-form word))))

(defn- render-medium-list-item
  [word-id {:keys [id name] :as medium}]
  (list-item (link (str "media/" id "/index.html?word=" word-id) name)))

(defn- render-word-medium-list
  [{:keys [word] :as args}]
  (let [word-id (:id word)
        medium-ids (set (map :medium-id (:occurrences word)))
        media (filter #(contains? medium-ids (:id %)) (:media args))]
    (ulist (map (partial render-medium-list-item word-id) media))))

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

(defn- render-category-word-medium-list
  [{:keys [media category word max-word-score] :as args}]
  (let [medium-fn (fn [id] (first (filter #(= id (:id %)) media)))
        category-id (:id category)
        word-id (:id word)
        items (->> (vals (:matches category))
                   (map (fn [{:keys [medium-id word-scores] :as match}]
                          [medium-id
                           (get word-scores word-id)]))
                   (filter #(not (nil? (second %))))
                   (map (fn [[medium-id score]]
                          (let [medium (medium-fn medium-id)]
                            (list-item
                             [(format "%.4f  " (/ score max-word-score))
                              (link (str "../../media/" medium-id "/index.html?word=" word-id)
                                    (:name medium))])))))]
    (if (> (count items) 0)
      (ulist items)
      (paragraph (txt :category-word-medium-list-empty)))))

(defn- render-medium-word-category-list
  [{:keys [categories medium word max-word-score] :as args}]
  (let [category-fn (fn [id] (first (filter #(= id (:id %)) categories)))
        medium-id (:id medium)
        word-id (:id word)
        items (->> (vals (:matches medium))
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
      (paragraph (txt :medium-word-category-list-empty)))))

(defn render-word-include
  "Renders the include part for the word frame."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-word-statistic args)
     (headline 4 (txt :main-word-media-h))
     (paragraph "explanation" (txt :main-word-media-d))
     (render-word-medium-list args)
     (headline 4 (txt :main-word-categories-h))
     (paragraph "explanation" (txt :main-word-categories-d))
     (render-word-category-list args)]))

(defn render-medium-word-include
  "Renders the include part for the word frame of a medium page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form pronunciation]} word]
    [(headline 4 "word_headline" [lexical-form (span "pronunciation" pronunciation)])
     (render-medium-word-statistic args)
     (headline 4 (txt :medium-word-phrases-h))
     (paragraph "explanation" (txt :medium-word-phrases-d))
     (render-medium-word-phrases args)
     (headline 4 (txt :medium-word-categories-h))
     (paragraph "explanation" (txt :medium-word-categories-d))
     (render-medium-word-category-list args)]))

(defn render-category-word-include
  "Renders the include part for the word frame of a category page."
  [{:keys [word] :as args}]
  (let [{:keys [lexical-form]} word]
    [(headline 4 "word_headline" lexical-form)
     (render-category-word-statistic args)
     (headline 4 (txt :category-word-media-h))
     (paragraph "explanation" (txt :category-word-media-d))
     (render-category-word-medium-list args)]))
