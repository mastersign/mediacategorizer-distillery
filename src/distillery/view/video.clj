(ns distillery.view.video
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.config :as cfg])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.cloud :as cloud])
  (:require [distillery.view.transcript :as transcript])
  (:require [distillery.view.glossary :as glossary]))

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
  [video]
  (let [video-id (:id video)
        words (vals (:index video))
        occ (fn [w] (filter #(= video-id (:video-id %)) (:occurrences w)))
        hitlist (take 10 (reverse (sort-by #(count (occ %)) words)))
        max-occ (count (occ (first hitlist)))
        conf-fn (fn [cnf]
                  (let [minc cfg/min-confidence
                        cnf* (/ (- cnf minc) (- 1 minc))]
                    (* cnf* cnf*)))
        item-gen (fn [{:keys [id lexical-form pronunciation mean-confidence] :as w}]
                   (let [num-occ (count (occ w))]
                     (list-item
                      (bar
                       [(span "hitlist_text"
                              (jslink
                               (str "word('" id "');")
                               {:tag :span
                                :attrs {:title pronunciation}
                                :content lexical-form}))
                        (span "hitlist_stats"
                              ;(strong (str num-occ))
                              (strong (str num-occ " | " (format "%2.1f%%" (* 100 mean-confidence)))))]
                       (/ num-occ max-occ)
                       (conf-fn mean-confidence)))))]
    (div "hitlist"
         [(headline 3 "H??ufige Worte")
          (olist (map item-gen hitlist))])))

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [video]}]
  (innerpage "overview" "Übersicht" true
             [(ulist "video_statistic"
                     [(list-item (str "Länge: " (transcript/format-time (:duration video))))
                      (list-item (str "Erkannte Phrasen: " (:phrase-count video)))
                      (list-item (str "Erkannte Worte: " (:word-count video)))
                      (list-item (str "Worte im Glossar: " (count (:index video))))
                      (list-item (format "Mittlere Erkennungssicherheit: %1.1f%%" (* 100 (:confidence video))))])
              (render-hitlist video)]))

(defn- render-video-transcript
  "Creates the HTML for the transcript with all phrases."
  [{{:keys [results index] :as video} :video :as args}]
  (innerpage "transcript" "Transkript" false
              (transcript/render-transcript results :index index)))

(defn- render-video-glossary
  "Create the HTML for the video glossary."
  [{{:keys [pindex] :as video} :video :as args}]
  (innerpage "glossary" "Glossar" false
    (glossary/render-glossary pindex)))

(defn- render-cloud
  "Create the HTML for the video word cloud."
  [{{:keys [id index cloud] :as video} :video :as args}]
  (let [];code (map #(div (str (first %) ": " (let [r (second %)] (str (.x r) ", " (.y r))))) cloud)]
    (innerpage "cloud" "Wortwolke" false
               (cloud/render-cloud id cloud))))

(defn- render-categories
  "Create the HTML for the video categories."
  [args]
  (innerpage "categories" "Kategorien" false
             (TODO "Videokategorien")))

(defn- render-video-word-frame
  "Create the HTML for the word frame.
   The word frame is an empty container to load a video word page into."
  [args]
  (innerpage "word" "Wort" false nil))

(defn render-video-page
  "Renders the main page for a video."
  [{:keys [job-name video] :as args}]
  [:base-path "../../"
   :title (str job-name " - " "Video")
;   :js-code "videojs.options.flash.swf = 'video-js.swf';"
   :secondary-menu [["Übersicht" (jshref "innerpage('overview')")]
                    ["Wortwolke" (jshref "innerpage('cloud')")]
                    ["Transkript" (jshref "innerpage('transcript')")]
                    ["Kategorien" (jshref "innerpage('categories')")]
                    ["Glossar" (jshref "innerpage('glossary')")]]
   :page
     [(render-headline args)
      (render-video args)
      (render-overview args)
      (render-video-transcript args)
      (render-video-glossary args)
      (render-cloud args)
      (render-categories args)
      (render-video-word-frame args)]])


