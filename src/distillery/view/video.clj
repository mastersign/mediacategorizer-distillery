(ns distillery.view.video
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.config :as cfg])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.cloud :as cloud])
  (:require [distillery.view.transcript :as transcript])
  (:require [distillery.view.glossary :as glossary])
  (:require [distillery.view.hitlist :as hitlist]))

(defn- render-headline
  "Creates the headline for the video page."
  [{:keys [video]}]
  (headline 2 (:name video)))

(defn- render-video
  "Creates the HTML for the video display box."
  [{:keys [video]}]
  {:tag :figure
   :attrs {:class "video-box"}
   :content
   [{:tag :video
     :attrs {:id "main_video"
             :class "video-js vjs-default-skin"
             :controls "controls"
             :preload "auto"
             :width "540"
             :height "360" }
     :content [{:tag :source
                :attrs
                {:src (str (:id video) ".mp4")
                 :type "video/mp4" }}]}]})

(defn- render-hitlist
  [{:keys [configuration] :as job} {:keys [id index] :as video}]
  (hitlist/render-occurrence-hitlist
   (vals index)
   (fn [w] (filter #(= id (:video-id %)) (:occurrences w)))
   configuration
   :video-cloud))

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [video] :as job}]
  (innerpage "overview" (txt :video-overview-h) true
             [(ulist "video_statistic"
                     [(list-item (str (txt :video-overview-duration) (transcript/format-time (:duration video))))
                      (list-item (str (txt :video-overview-phrase-count) (:phrase-count video)))
                      (list-item (str (txt :video-overview-word-count) (:word-count video)))
                      (list-item (str (txt :video-overview-index-size) (count (:index video))))
                      (list-item (str (txt :video-overview-mean-confidence) (format "%1.1f%%" (* 100 (:confidence video)))))])
              (headline 4 (txt :video-overview-hitlist-h))
              (paragraph "explanation" (txt :video-overview-hitlist-d))
              (render-hitlist job video)]))

(defn- render-video-transcript
  "Creates the HTML for the transcript with all phrases."
  [{{:keys [results index] :as video} :video :as args}]
  (innerpage "transcript" (txt :video-transcript-h) false
             [(paragraph "explanation" (txt :video-transcript-d))
              (transcript/render-transcript results :index index)]))

(defn- render-video-glossary
  "Create the HTML for the video glossary."
  [{{:keys [pindex] :as video} :video :as args}]
  (innerpage "glossary" (txt :video-glossary-h) false
             [(paragraph "explanation" (txt :video-glossary-d))
              (glossary/render-glossary pindex)]))

(defn- render-cloud
  "Create the HTML for the video word cloud."
  [{{:keys [id cloud] :as video} :video :as args}]
  (innerpage "cloud" (txt :video-wordcloud-h) false
             [(paragraph "explanation" (txt :video-wordcloud-d))
              (cloud/render-cloud id cloud)]))

(defn- render-categories
  "Create the HTML for the video categories."
  [{:keys [configuration categories video max-score] :as args}]
  (let [category-fn (fn [cid] (first (filter #(= cid (:id %)) categories)))]
    (innerpage "categories" (txt :video-categories-h) false
               [(paragraph "explanation" (txt :video-categories-d))
                (hitlist/render-category-matchlist
                 video
                 categories
                 configuration)])))

(defn- render-video-word-frame
  "Create the HTML for the word frame.
  The word frame is an empty container to load a video word page into."
  [args]
  (innerpage "word" (txt :video-word-h) false nil))

(defn render-video-page
  "Renders the main page for a video."
  [{:keys [job-name video categories configuration] :as args}]
  [:base-path "../../"
   :title (build-title args (txt :video-title))
   ;   :js-code "videojs.options.flash.swf = 'video-js.swf';"
   :secondary-menu [[(txt :video-menu-overview) (jshref "innerpage('overview')")]
                    (when-not (cfg/value :skip-wordclouds configuration)
                      [(txt :video-menu-wordcloud) (jshref "innerpage('cloud')")])
                    [(txt :video-menu-transcript) (jshref "innerpage('transcript')")]
                    (when (seq categories)
                      [(txt :video-menu-categories) (jshref "innerpage('categories')")])
                    [(txt :video-menu-glossary) (jshref "innerpage('glossary')")]]
   :page
   [(render-headline args)
    (render-video args)
    (render-overview args)
    (render-video-transcript args)
    (render-video-glossary args)
    (render-cloud args)
    (render-categories args)
    (render-video-word-frame args)]])
