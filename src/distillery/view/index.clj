(ns distillery.view.index
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.data :refer [key-comp]])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.glossary :as glossary])
  (:require [distillery.view.cloud :as cloud]))

(defn- render-main-overview
  [{:keys [job-description videos categories words] :as args}]
  (innerpage "overview" "Übersicht" true
             [(paragraph job-description)
              (headline 3 "Inhalt")
              (ulist "main_statistic"
                     [(list-item (str "Videos: " (count videos)))
                      (list-item (str "Kategorien: " (count categories)))
                      (list-item (str "Wörter: " (count words)))])
              (render-hitlist args)]))

(defn- render-main-cloud
  [{:keys [cloud] :as args}]
  (innerpage "cloud" "Globale Wortwolke" false
             (cloud/render-cloud "global" cloud)))

(defn- render-main-word-frame
  "Create the HTML for the word frame.
   The word frame is an empty container to load a word page into."
  [args]
  (innerpage "word" "Wort" false nil))

(defn- render-main-glossary
  [{:keys [pwords] :as args}]
  (innerpage "glossary" "Globaler Glossar" false
             (glossary/render-glossary pwords)))

(defn render-main-page
  "Renders the main page for the site."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Übersicht" (jshref "innerpage('overview')")
                    "Wortwolke" (jshref "innerpage('cloud')")
                    "Glossar" (jshref "innerpage('glossary')")}
   :page
     [(headline 2 "Projekt")
      (render-main-overview args)
      (render-main-cloud args)
      (render-main-glossary args)
      (render-main-word-frame args)]])

(defn- render-categories-list
  [{:keys [categories] :as args}]
  (innerpage "overview" "Übersicht" true
             [(TODO "Kategorieliste")]))

(defn render-categories-page
  "Renders the categories main page."
  [{:keys [job-name] :as args}]
  [:title (str job-name " - " "Kategorien")
   :secondary-menu {"Übersicht" (jshref "innerpage('overview')")}
   :page
     [(headline 2 "Kategorien")
      (render-categories-list args)]])

(defn- render-video-list-item
  "Renders a video link as list item."
  [{:keys [id name] :as video}]
  (list-item (link (str "videos/" id "/index.html") name)))

(defn- render-videos-list
  [{:keys [videos] :as args}]
  (let [video-list (into (sorted-set-by (key-comp :name)) videos)]
    (innerpage "overview" "Übersicht" true
               (ulist (map render-video-list-item video-list)))))

(defn render-videos-page
  "Renders the videos main page."
  [{:keys [job-name] :as args}]
  [:title (str job-name " - " "Videos")
   :secondary-menu [["Übersicht" (jshref "innerpage('overview')")]]
   :page
     [(headline 2 "Videos")
      (render-videos-list args)]])

