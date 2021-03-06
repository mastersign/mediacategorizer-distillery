(ns distillery.view.cloud
  (:require [clojure.string :as string])
  (:require [mastersign.drawing :as mdr])
  (:require [mastersign.wordcloud :as mwc])
  (:require [mastersign.html :refer :all])
  (:require [distillery.config :as cfg])
  (:require [distillery.view.html :refer :all]))

(defn build-cloud-word-data
  "Transforms the word meta-data from an index
  into the input format for the cloud generator."
  [index config cloud-key]
  (let [max-occurrence (double (dec (apply max (cons 0 (map #(count (:occurrences %)) (vals index))))))]
    (vec
     (map
      (fn [w]
        (let [occurrence (dec (count (:occurrences w)))
              confidence (/ (- (:mean-confidence w) (cfg/value :min-confidence config))
                            (- 1 (cfg/value :min-confidence config)))]
          [(:id w)
           (:lexical-form w)
           (if (> max-occurrence 0) (/ occurrence max-occurrence) 0.5)
           (* confidence confidence)]))
      (filter #(>= (count (:occurrences %)) (cfg/value [cloud-key :min-occurrence] config)) (vals index))))))

(defn build-cloud-ui-data
  "Transforms the result data from the cloud generator
  into the format required to generate the UI on top of the cloud."
  [{:keys [word-infos]}]
  (str "["
       (->> word-infos
            (sort-by :v1)
            (map (fn
                   [{:keys [id ^java.awt.geom.Rectangle2D$Float rect]}]
                   (str "{id:'" id "',r:{x:"(.x rect)",y:"(.y rect)",w:"(.width rect)",h:"(.height rect)"}}")))
            (string/join ","))
       "]"))

(defn render-cloud
  [id, cloud]
  {:tag :figure
   :attrs {:class "wordcloud"
           :data-cloud-id id}
   :content [{:tag :img
              :attrs {:class "wordcloud"
                      :src "cloud.png"}}
             (jscript (str "register_cloud_data('" id "'," cloud ");"))]})


(defn create-cloud
  [word-data target-path config cloud-key]
  (let [ccv (fn [k] (cfg/value [cloud-key k] config))]
    (mwc/create-cloud word-data
                      :target-file target-path
                      :width (ccv :width)
                      :height (ccv :height)
                      :precision (case (ccv :precision) :low 0.2 :medium 0.4 :high 0.6 0.4)
                      :order-priority (ccv :order-priority)
                      :font (apply mdr/font (concat [(ccv :font-family) 20] (ccv :font-style)))
                      :min-font-size (ccv :min-font-size)
                      :max-font-size (ccv :max-font-size)
                      :color-fn #(apply mdr/color (concat (take 3 (ccv :color)) [(+ 0.25 (* % 0.75))]))
                      :background-color (apply mdr/color (ccv :background-color)))))
