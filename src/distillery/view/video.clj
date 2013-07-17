(ns distillery.view.video
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

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

(defn render-video-page
  "Renders the main page for a video."
  [{:keys [video] :as args}]
  [:base-path "../../"
   :title (:name video)
   :js-code
     "videojs.options.flash.swf = 'video-js.swf';"
   :secondary-menu { "Übersicht" "" "Statistiken" "" }
   :page
     [(render-video args)]])

