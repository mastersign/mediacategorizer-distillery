(ns distillery.view.simple-transcript
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn render-video
  [{ :keys [job-name, video-file-name] :as args }]
  [ :title job-name
    :js-code
      "videojs.options.flash.swf = 'video-js.swf';"
    :secondary-menu { "Übersicht" "" "Statistiken" "" }
    :page
      { :tag :div
        :attrs { :class "video-box" }
        :content
          [{ :tag :video
            :attrs
              { :id "main_video"
                :class "video-js vjs-default-skin"
                :controls "controls"
                :preload "none"
                :width "540"
                :height "360" }
            :content
              [{ :tag :source
                 :attrs
                   { :src video-file-name
                     :type "video/mp4" }}]}]}])