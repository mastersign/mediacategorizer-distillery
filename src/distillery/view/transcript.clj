(ns distillery.view.transcript
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
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
  [{:keys [text confidence pronunciation]}]
  {:tag :span
   :attrs {:style (str "color:" (confidence-color (* confidence confidence)))
          :title pronunciation}
   :content (str text " ")})

(defn- render-phrase
  "Creates the HTML for the words of a recognized phrase."
  [{:keys [words]}]
  (map render-word words))

(defn- render-result
  "Creates the HTML for a single phrase."
  [result]
  (div "phrase" [(span "tc" (jslink (format "video_jump(%f)" (double (:start result))) (format-time (:start result))))
                 (span "pt" (render-phrase result))]))

(defn render-result-list
  "Creates the HTML for a sequence of phrases."
  [results]
  (div "transcript" (map render-result results)))