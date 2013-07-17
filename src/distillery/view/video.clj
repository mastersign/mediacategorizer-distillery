(ns distillery.view.video
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn render-headline
  "Creates the headline for the video page."
  [{:keys [video]}]
  (headline 2 (:name video)))

(defn render-video
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
          :preload "none"
          :width "540"
          :height "360" }
       :content
         [{:tag :source
           :attrs
             {:src (str (:id video) ".mp4")
              :type "video/mp4" }}]}]})

(defn format-result
  "Creates the HTML for a single phrase."
  [result]
  (div "phrase" [(span "tc" (:start result)) (span "pt" (:text result))]))

(defn render-transcript
  "Creates the HTML for the transcript with all phrases."
  [{:keys [results] :as args}]
  [(headline 3 "Transkript")
   (div "transcript" (map format-result results))])

(defn render-video-page
  "Renders the main page for a video."
  [{:keys [job-name video] :as args}]
  [:base-path "../../"
   :title job-name
   :js-code
     "videojs.options.flash.swf = 'video-js.swf';"
   :secondary-menu {"Übersicht" ""
                    "Transcript" ""
                    "Glossar" ""
                    "Cloud" ""
                    "Kategorien" ""}
   :page
     [(render-headline args)
      (render-video args)
      (render-transcript args)]])

