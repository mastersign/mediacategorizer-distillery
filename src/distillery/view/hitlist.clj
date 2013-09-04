(ns distillery.view.hitlist
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn render-hitlist
  [words occ-fn config cloud-key]
  (let [hitlist (take 10 (reverse (sort-by #(count (occ-fn %)) words)))
        max-occ (count (occ-fn (first hitlist)))
        color (or (cfg/value [cloud-key :color] config) [0 0 0])
        conf-fn (fn [cnf]
                  (let [minc (cfg/value :min-confidence config)
                        cnf* (/ (- cnf minc) (- 1 minc))]
                    (* cnf* cnf*)))
        stats-str-fn (if (= 1.0 (mean (map :mean-confidence hitlist)))
                       (fn [n c] (str n))
                       (fn [n c] (str n " | " (format "%2.1f%%" (* 100 c)))))
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
         [(headline 3 "Häufige Worte")
          (olist (map item-gen hitlist))])))



