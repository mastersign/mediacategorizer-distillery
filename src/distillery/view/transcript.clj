(ns distillery.view.transcript
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.processing :refer [word-identifier]])
  (:require [distillery.view.html :refer :all]))

(defn- format-time
  "Creates a pretty time string from total seconds."
  [seconds]
  (let [h (int (/ seconds (* 60 60)))
        m (int (mod (/ seconds 60) 60))
        s (int (mod seconds 60))]
    (if (> h 0)
      (format "%d:%02d:%02d" h m s)
      (format "%02d:%02d" m s))))

(defn- confidence-color
  "Creates a CSS compatible color definition string for a given confidence value."
  [confidence]
  (let [v (int (* (- 1 confidence) 192))]
    (format "#%02X%02X%02X" v v v)))

(defn- render-word
  "Creates the HTML for a recognized word."
  [{:keys [text lexical-form confidence pronunciation] :as word} & {:keys [index pivot]}]
  (let [highlight (= pivot lexical-form)
        color (if highlight "#FF0000" (confidence-color (* confidence confidence)))
        html {:tag :span
              :attrs {:style (str "color:" color)
                      :title pronunciation}
              :content (str text " ")}]
    (if (contains? index lexical-form)
      (jslink (str "word('" (word-identifier word) "')") html)
      html)))

(defn- render-phrase
  "Creates the HTML for the words of a recognized phrase."
  [{:keys [words]} & {:keys [index pivot]}]
  (map #(render-word % :index index :pivot pivot) words))

(defn- render-result
  "Creates the HTML for a single phrase."
  [result & {:keys [index pivot]}]
  (div "phrase" [(span "tc" (jslink (format "video_jump(%f)" (double (:start result))) (format-time (:start result))))
                 (span "pt" (render-phrase result :index index :pivot pivot))]))

(defn render-result-list
  "Creates the HTML for a sequence of phrases."
  [results & {:keys [index pivot]}]
  (div "transcript" (map #(render-result % :index index :pivot pivot) results)))