(ns distillery.view.match
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [distillery.data :refer [key-comp any?]])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all]))

(defn- render-category-match-word-list
  [{:keys [medium match max-score max-word-score] :as args}]
  (let [index (:index medium)
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
  [{:keys [media category match max-score] :as args}]
  (let [{:keys [medium-id]} match
        medium (first (filter #(= (:id %) medium-id) media))]
    [(paragraph [(str (txt :medium) ": ") (strong (:name medium)) {:tag :br}
                 (str (txt :category) ": ") (strong (:name category))])
     (paragraph (str (txt :match-normalized) (format "%.4f" (/ (:score match) max-score))))
     (paragraph "explanation" (txt :match-d))
     (render-category-match-word-list (assoc args :medium medium))]))

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
  [configuration matrix max-score category-ids {:keys [id name] :as medium}]
  (let [[r g b a] (get-in configuration [:matrix :color])
        cell-color (fn [v] (java.lang.String/format nil
                                                    "rgba(%d, %d, %d, %.3f)"
                                                    (to-array [(int (* 255 r))
                                                               (int (* 255 g))
                                                               (int (* 255 b))
                                                               v])))]
    {:tag :tr
     :content
     (vec (cons {:tag :th
                 :content [{:tag :a
                            :attrs {:href (str "media/" id "/index.html")
                                    :title name}
                            :content name}]}
                (map
                 (fn [category-id]
                   (let [match (get-in matrix [category-id id])
                         score (:score match)
                         medium-max-score (:max-score medium)
                         normalized-score (if (> max-score 0) (/ score max-score) 0)
                         medium-normalized-score (if (> medium-max-score 0) (/ score medium-max-score) 0)]
                     (if match
                       {:tag :td
                        :attrs {:data-medium-id id
                                :data-category-id category-id
                                :style (str "background-color: " (cell-color medium-normalized-score))}
                        :content [(link (str "categories/" category-id "/index.html?match=" id)
                                        (format "%.1f%%" (* 100 normalized-score)))]}
                       {:tag :td :content []})))
                 category-ids)))}))

(defn render-match-matrix
  [{:keys [media categories max-score configuration] :as args}]
  (let [matrix (->> categories
                    (map
                     (fn [category]
                       [(:id category)
                        (->> media
                             (map
                              (fn [medium]
                                [(:id medium)
                                 (get (:matches medium) (:id category))]))
                             (map
                              (fn [[medium-id match]]
                                [medium-id match]))
                             (apply concat)
                             (apply hash-map))]))
                    (apply concat)
                    (apply hash-map))
        category-ids (map :id categories)]
    {:tag :figure
     :content
     [{:tag :table
       :attrs {:class "match_matrix"}
       :content
       [{:tag :thead
         :content
         [(render-match-matrix-head-row categories)]}
        {:tag :tbody
         :content
         (vec (map (partial render-match-matrix-row configuration matrix max-score category-ids) media))}]}]}))
