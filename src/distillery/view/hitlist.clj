(ns distillery.view.hitlist
  (:require [distillery.config :as cfg])
  (:require [distillery.view.html :refer :all]))

(defn render-hitlist
  [words occ-fn]
  (let [hitlist (take 10 (reverse (sort-by #(count (occ-fn %)) words)))
        max-occ (count (occ-fn (first hitlist)))
        conf-fn (fn [cnf]
                  (let [minc cfg/min-confidence
                        cnf* (/ (- cnf minc) (- 1 minc))]
                    (* cnf* cnf*)))
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
                              ;(strong (str num-occ))
                              (strong (str num-occ " | " (format "%2.1f%%" (* 100 mean-confidence)))))]
                       (/ num-occ max-occ)
                       (conf-fn mean-confidence)))))]
    (div "hitlist"
         [(headline 3 "Häufige Worte")
          (olist (map item-gen hitlist))])))

