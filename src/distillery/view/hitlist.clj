(ns distillery.view.hitlist
  (:require [mastersign.html :refer :all])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all]))

(defn render-occurrence-hitlist
  [words occ-fn config cloud-key]
  (let [hitlist (take 10 (reverse (sort-by #(count (occ-fn %)) words)))
        max-occ (count (occ-fn (first hitlist)))
        color (or (cfg/value [cloud-key :color] config) [0 0 0])
        conf-fn (fn [cnf]
                  (let [minc (cfg/value :min-confidence config)
                        cnf* (/ (- cnf minc) (- 1 minc))]
                    (* cnf* cnf*)))
        stats-str-fn (if (== 1 (mean (map :mean-confidence hitlist)))
                       (fn [n c] (str n))
                       (fn [n c] (str n " | " (format "%2.1f%%" (* 100.0 c)))))
        item-gen (fn [{:keys [id lexical-form pronunciation mean-confidence] :as w}]
                   (let [num-occ (count (occ-fn w))]
                     (list-item
                      (bar
                       [(span "hitlist_text"
                              (jslink
                               (str "word('" id "');")
                               {:tag :span
                                :attrs {:title pronunciation}
                                :content lexical-form}))
                        (span "hitlist_stats"
                              (strong (stats-str-fn num-occ mean-confidence)))]
                       color
                       (/ num-occ max-occ)
                       (conf-fn mean-confidence)))))]
    (div "hitlist"
         [(olist (map item-gen hitlist))])))

(defn render-category-matchlist
  [{:keys [matches max-score] :as video} categories config]
  (let [matchlist (->> matches
                       (filter #(>= (:score (second %)) (cfg/value [:min-match-score] config)))
                       (sort-by #(:score (second %)))
                       reverse)
        color (or (cfg/value [:category-cloud :color] config) [0 0 0])
        stats-str-fn (fn [score rel-score]
                       (format "%1.3f | %05.1f%%" score (* rel-score 100)))
        item-gen (fn [[category-id {:keys [score] :as m}]]
                   (let [category (first (filter #(= (:id %) category-id) categories))
                         rel-score (/ score max-score)]
                     (list-item
                      (bar
                       [(span "hitlist_text"
                              (link
                               (str "../../categories/" category-id "/index.html?match=" (:id video))
                               (:name category)))
                        (span "hitlist_stats"
                              (strong (stats-str-fn score rel-score)))]
                       color
                       rel-score))))]
    (div "hitlist"
         [(olist (map item-gen matchlist))])))

(defn render-video-matchlist
  [{:keys [matches] :as category} videos config]
  (let [rel-score-fn (fn [match] (let [video (first (filter #(= (:id %) (:video-id match)) videos))
                                       max-score (:max-score video)]
                                   (/ (:score match) max-score)))
        matchlist (->> matches
                       (filter #(>= (:score (second %)) (cfg/value [:min-match-score] config)))
                       (sort-by #(rel-score-fn (second %)))
                       reverse)
        color (or (cfg/value [:video-cloud :color] config) [0 0 0])
        stats-str-fn (fn [score rel-score]
                       (format "%1.3f | %05.1f%%" score (* rel-score 100)))
        item-gen (fn [[video-id {:keys [score] :as m}]]
                   (let [video (first (filter #(= (:id %) video-id) videos))
                         rel-score (rel-score-fn m)]
                     (list-item
                      (bar
                       [(span "hitlist_text"
                              (link
                               (str "../../videos/" video-id "/index.html?match=" (:id category))
                               {:tag :span
                                :attrs {:title (:name video)}
                                :content (:name video)}))
                        (span "hitlist_stats"
                              (strong (stats-str-fn score rel-score)))]
                       color
                       rel-score))))]
    (div "hitlist"
         [(olist (map item-gen matchlist))])))
