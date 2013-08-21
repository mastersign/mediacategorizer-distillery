(ns distillery.view.cloud
  (:require [distillery.config :as cfg])
  (:require [distillery.view.html :refer :all]))

(defn build-cloud-word-data
  "Transforms the word meta-data from an index
  into the input format for the cloud generator."
  [index]
  (let [max-occurrence (double (dec (apply max (map #(count (:occurrences %)) (vals index)))))]
    (vec
     (map
      (fn [w]
        (let [occurrence (dec (count (:occurrences w)))
              confidence (/ (- (:mean-confidence w) cfg/min-confidence) (- 1 cfg/min-confidence))]
          [(:id w)
           (:lexical-form w)
           (/ occurrence max-occurrence)
           (* confidence confidence)]))
      (filter #(> (count (:occurrences %)) 1) (vals index))))))

(defn build-cloud-ui-data
  "Transforms the result data from the cloud generator
  into the format required to generate the UI on top of the cloud."
  [{:keys [word-infos]}]
  (str "["
       (->> word-infos
            (sort-by :v1)
            (map (fn
                   [{:keys [id rect]}]
                   (str "{id:' id "',r:{x:"(.x rect)",y:"(.y rect)",w:"(.width rect)",h:"(.height rect)"}},")))
            (apply str))
       "]"))

