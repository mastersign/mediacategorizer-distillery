(ns distillery.view.video
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn- render-headline
  "Creates the headline for the video page."
  [{:keys [video]}]
  (headline 2 (:name video)))

(defn- render-video
  "Creates the HTML for the video display box."
  [{:keys [video]}]
  {:tag :figure
   :attrs { :class "video-box" }
   :content
     [{:tag :video
       :attrs
         {:id "main_video"
          :class "video-js vjs-default-skin"
          :controls "controls"
          :preload "auto"
          :width "540"
          :height "360" }
       :content
         [{:tag :source
           :attrs
             {:src (str (:id video) ".mp4")
              :type "video/mp4" }}]}]})

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [video]}]
  (innerpage "overview" "Übersicht" true
             (TODO "Videoübersicht")))

(defn- format-time
  [seconds]
  (let [h (int (/ seconds (* 60 60)))
        m (int (mod (/ seconds 60) 60))
        s (int (mod seconds 60))]
    (if (> h 0)
      (format "%d:%02d:%02d" h m s)
      (format "%02d:%02d" m s))))

(defn- confidence-color
  [confidence]
  (let [v (int (* (- 1 confidence) 192))]
    (format "#%02X%02X%02X" v v v)))

(defn- format-word
  [{:keys [text confidence pronunciation]}]
  {:tag :span
   :attrs {:style (str "color:" (confidence-color (* confidence confidence)))
          :title pronunciation}
   :content (str text " ")})

(defn- format-phrase
  [{:keys [words]}]
  (map format-word words))

(defn- format-result
  "Creates the HTML for a single phrase."
  [result]
  (div "phrase" [(span "tc" (jslink (format "video_jump(%f)" (double (:start result))) (format-time (:start result))))
                 (span "pt" (format-phrase result))]))

(defn- render-transcript
  "Creates the HTML for the transcript with all phrases."
  [{{:keys [results] :as video} :video :as args}]
  (innerpage "transcript" "Transkript" false
              (div "transcript" (map format-result results))))

(defn- render-glossary-word
  "Creates the HTML for a glossary entry."
  [[lexical {:keys [occurrences mean-confidence]}]]
  (list-item (format "%s (%d, %f)" lexical (count occurrences) mean-confidence)))

(defn- glossary-partition-id
  "Creates an identifier for a glossary partition by its letter."
  [letter]
  (if (= \? letter) "SYM" (str letter)))

(defn- render-glossary-navigation
  "Creates the HTML for the navigation bar of a glossary."
  [pindex]
  (let [letters (map char (concat (range 65 91) [63]))]
    (div "glossary-nav"
      (map (fn [l]
             (if (contains? pindex l)
               (jslink (str "glossary('" (glossary-partition-id l) "')") (str l "  "))
               (str l "  ")))
           letters))))

(defn- render-glossary-partition
  "Creates the HTML for a partion of a glossary."
  [[letter index-part]]
  {:tag :div
   :attrs {:id (str "glossary-part-" (glossary-partition-id letter)) :class "glossary-part"}
   :content [(ulist "glossary" (map render-glossary-word index-part))]})

(defn- render-glossary
  "Create the HTML for the video glossary."
  [{{:keys [pindex] :as video} :video :as args}]
  (innerpage "glossary" "Glossar" false
    (cons (render-glossary-navigation pindex)
          (map render-glossary-partition pindex))))

(defn- render-cloud
  "Create the HTML for the video word cloud."
  [{{:keys [index] :as video} :video :as args}]
  (innerpage "cloud" "Wolke" false
             (TODO "Videowortwolke")))

(defn- render-categories
  "Create the HTML for the video categories."
  [args]
  (innerpage "categories" "Kategorien" false
             (TODO "Videokategorien")))

(defn render-video-page
  "Renders the main page for a video."
  [{:keys [job-name video] :as args}]
  [:base-path "../../"
   :title job-name
;   :js-code "videojs.options.flash.swf = 'video-js.swf';"
   :secondary-menu {"Übersicht" (jshref "innerpage('overview')")
                    "Transcript" (jshref "innerpage('transcript')")
                    "Glossar" (jshref "innerpage('glossary')")
                    "Cloud" (jshref "innerpage('cloud')")
                    "Kategorien" (jshref "innerpage('categories')")}
   :page
     [(render-headline args)
      (render-video args)
      (render-overview args)
      (render-transcript args)
      (render-glossary args)
      (render-cloud args)
      (render-categories args)]])

