(ns distillery.view.medium
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
  "Creates the headline for the medium page."
  [{:keys [medium]}]
  (headline 2 (:name medium)))

(defn- render-media-source
  "Creates the source element for an audio or video player."
  [{:keys [id] :as medium} {:keys [ext mime-type] :as emf}]
  {:tag :source
   :attrs {:src (str (:id medium) ext)
           :type mime-type}})

(defn- render-video
  "Creates the HTML for the video display box."
  [{:keys [medium]}]
  (let [sources (map
                 (partial render-media-source medium)
                 (:encoded-media-files medium))]
    {:tag :figure
     :attrs {:class "video-box"}
     :content
     [{:tag :video
       :attrs {:id "main_video"
               :controls "controls"
               :preload "auto"
               :width "540"
               :height "360" }
       :content (vec sources)}]}))

(defn- render-audio
  "Creates the HTML for the audio player."
  [{:keys [medium]}]
  (let [sources (map
                 (partial render-media-source medium)
                 (:encoded-media-files medium))]
    {:tag :figure
     :attrs {:class "audio_box"}
     :content
     [{:tag :audio
       :attrs {:id "main_audio"
               :controls "controls"
               :preload "auto"
               :style (str "width: " 540 "px;")}
       :content (vec sources)}]}))

(defn- render-hitlist
  [{:keys [configuration] :as job} {:keys [id index] :as medium}]
  (hitlist/render-occurrence-hitlist
   (vals index)
   (fn [w] (filter #(= id (:medium-id %)) (:occurrences w)))
   configuration
   :medium-cloud))

(defn- render-waveform
  "Creates the HTML for the waveform."
  [{:keys [output-dir configuration] :as job} {:keys [id] :as medium}]
  (copy-file
   (get-path (:waveform-file medium))
   (get-path output-dir "media" id "waveform.png"))
  (copy-file
   (get-path (:waveform-file-bg medium))
   (get-path output-dir "media" id "waveform2.png"))
  {:tag :figure
   :attrs {:class "waveform_box"}
   :content [{:tag :div
              :attrs {:id "main_waveform"
                      :class "waveform"
                      :style (str
                              "width: "
                              (cfg/value [:waveform :width] configuration)
                              "px; height: "
                              (cfg/value [:waveform :height] configuration)
                              "px;")
                      :data-duration (:duration medium)}
              :content [{:tag :div
                         :attrs {:class "waveform_bg"
                                 :style (str "background-image: url('waveform2.png'); width: "
                                             (cfg/value [:waveform :width] configuration)
                                             "px; height: "
                                             (cfg/value [:waveform :height] configuration)
                                             "px;")}}
                        {:tag :div
                         :attrs {:class "waveform_img"
                                 :style (str "background-image: url('waveform.png'); width: "
                                             0
                                             "px; height: "
                                             (cfg/value [:waveform :height] configuration)
                                             "px;") }}]}]})

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [medium] :as job}]
  (innerpage "overview" (txt :medium-overview-h) true
             [(ulist "medium_statistic"
                     [(list-item (str (txt :medium-overview-duration) (transcript/format-time (:duration medium))))
                      (list-item (str (txt :medium-overview-phrase-count) (:phrase-count medium)))
                      (list-item (str (txt :medium-overview-word-count) (:word-count medium)))
                      (list-item (str (txt :medium-overview-index-size) (count (:index medium))))
                      (list-item (str (txt :medium-overview-mean-confidence) (format "%1.1f%%" (* 100 (:confidence medium)))))])
              (headline 4 (txt :medium-overview-waveform-h))
              (paragraph "explanation" (txt :medium-overview-waveform-d))
              (render-waveform job medium)
              (headline 4 (txt :medium-overview-hitlist-h))
              (paragraph "explanation" (txt :medium-overview-hitlist-d))
              (render-hitlist job medium)]))

(defn- render-medium-transcript
  "Creates the HTML for the transcript with all phrases."
  [{{:keys [results index] :as medium} :medium :as args}]
  (innerpage "transcript" (txt :medium-transcript-h) false
             [(paragraph "explanation" (txt :medium-transcript-d))
              (transcript/render-transcript results :index index)]))

(defn- render-medium-glossary
  "Create the HTML for the medium glossary."
  [{{:keys [pindex] :as medium} :medium :as args}]
  (innerpage "glossary" (txt :medium-glossary-h) false
             [(paragraph "explanation" (txt :medium-glossary-d))
              (glossary/render-glossary pindex)]))

(defn- render-cloud
  "Create the HTML for the medium word cloud."
  [{{:keys [id cloud] :as medium} :medium :as args}]
  (innerpage "cloud" (txt :medium-wordcloud-h) false
             [(paragraph "explanation" (txt :medium-wordcloud-d))
              (cloud/render-cloud id cloud)]))

(defn- render-categories
  "Create the HTML for the media categories."
  [{:keys [configuration categories medium max-score] :as args}]
  (let [category-fn (fn [cid] (first (filter #(= cid (:id %)) categories)))]
    (innerpage "categories" (txt :medium-categories-h) false
               [(paragraph "explanation" (txt :medium-categories-d))
                (hitlist/render-category-matchlist
                 medium
                 categories
                 configuration)])))

(defn- render-medium-word-frame
  "Create the HTML for the word frame.
  The word frame is an empty container to load a medium word page into."
  [args]
  (innerpage "word" (txt :medium-word-h) false nil))

(defn render-medium-page
  "Renders the main page for a medium."
  [{:keys [job-name medium categories configuration] :as args}]
  [:base-path "../../"
   :title (build-title args (txt :medium-title))
   :secondary-menu [[(txt :medium-menu-overview) (jshref "innerpage('overview')")]
                    (when-not (cfg/value :skip-wordclouds configuration)
                      [(txt :medium-menu-wordcloud) (jshref "innerpage('cloud')")])
                    [(txt :medium-menu-transcript) (jshref "innerpage('transcript')")]
                    (when (seq categories)
                      [(txt :medium-menu-categories) (jshref "innerpage('categories')")])
                    [(txt :medium-menu-glossary) (jshref "innerpage('glossary')")]]
   :page
   [(render-headline args)
    (when (not (cfg/value :skip-media-copy configuration))
      (case (:medium-type medium)
        :video (render-video args)
        :audio (render-audio args)
        nil))
    (render-overview args)
    (render-medium-transcript args)
    (render-medium-glossary args)
    (render-cloud args)
    (render-categories args)
    (render-medium-word-frame args)]])
