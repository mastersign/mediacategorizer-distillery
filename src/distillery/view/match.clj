(ns distillery.view.match
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [distillery.data :refer [key-comp any?]])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all]))

(defn- render-category-match-word-list
  [{:keys [video match max-score max-word-score] :as args}]
  (let [index (:index video)
        items (->> (:word-scores match)
                   (sort-by second)
                   reverse
                   (map (fn [[word-id score]]
                          (assoc (get index word-id) :score score)))
                   (map (fn [{:keys [id lexical-form score]}]
                          (list-item [(format "%.4f  " (/ score max-word-score))
                                      (jslink
                                       (str "word('" id "');")
                                       lexical-form)]))))]
    (div (ulist items))))

(defn render-category-match-include
  "Renders the include part for the match frame of a category page."
  [{:keys [videos category match max-score] :as args}]
  (let [{:keys [video-id]} match
        video (first (filter #(= (:id %) video-id) videos))]
    [(paragraph [(str (txt :video) ": ") (strong (:name video)) {:tag :br}
                 (str (txt :category) ": ") (strong (:name category))])
     (paragraph (str (txt :match-normalized) (format "%.4f" (/ (:score match) max-score))))
     (paragraph "explanation" (txt :match-d))
     (render-category-match-word-list (assoc args :video video))]))

(defn- render-match-matrix-head-cell
  [{:keys [id name] :as category}]
  {:tag :th
   :content [(link (str "categories/" id "/index.html")
                   name)]})

(defn- render-match-matrix-head-row
  [categories]
  {:tag :tr
   :content (vec (cons
                  {:tag :td :content ""}
                  (map render-match-matrix-head-cell categories)))})

(defn- render-match-matrix-row
  [matrix max-score category-ids {:keys [id name] :as video}]
  {:tag :tr
   :content
   (vec (cons {:tag :th
               :content [{:tag :a
                          :attrs {:href (str "videos/" id "/index.html")
                                  :title name}
                          :content name}]}
              (map
               (fn [category-id]
                 (let [match (get-in matrix [category-id id])
                       score (:score match)
                       video-max-score (:max-score video)
                       normalized-score (if (> max-score 0) (/ score max-score) 0)
                       video-normalized-score (if (> video-max-score 0) (/ score video-max-score) 0)]
                   (if match
                     {:tag :td
                      :attrs {:data-video-id id
                              :data-category-id category-id
                              :style (str "background-color: rgba(234,30,106," video-normalized-score ")")}
                      :content [(link (str "categories/" category-id "/index.html?match=" id)
                                      (format "%.1f%%" (* 100 normalized-score)))]}
                     {:tag :td :content []})))
               category-ids)))})

(defn render-match-matrix
  [{:keys [videos categories max-score] :as args}]
  (let [matrix (->> categories
                    (map
                     (fn [category]
                       [(:id category)
                        (->> videos
                             (map
                              (fn [video]
                                [(:id video)
                                 (get (:matches video) (:id category))]))
                             (map
                              (fn [[video-id match]]
                                [video-id match]))
                             (apply concat)
                             (apply hash-map))]))
                    (apply concat)
                    (apply hash-map))
        category-ids (map :id categories)]
    {:tag :figure
     :content
     [{:tag :table
       :attrs {:class "match-matrix"}
       :content
       [{:tag :thead
         :content
         [(render-match-matrix-head-row categories)]}
        {:tag :tbody
         :content
         (vec (map (partial render-match-matrix-row matrix max-score category-ids) videos))}]}]}))
