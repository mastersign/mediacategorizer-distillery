(ns distillery.view.index
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.data :refer [key-comp]])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))


(defn- render-main-overview
  [{:keys [job-description] :as args}]
  (innerpage "overview" "Projekt" true
             [(paragraph job-description)]))

(defn- render-main-statistics
  [{:keys [videos categories words] :as args}]
  (innerpage "statistics" "Statistiken" false
             [(TODO "Hauptstatistik der Site.")]))

(defn render-main-page
  "Renders the main page for the site."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Projekt" (jshref "innerpage('overview')")
                    "Statistiken" (jshref "innerpage('statistics')")}
   :page
     [(headline 2 "Start")
      (render-main-overview args)
      (render-main-statistics args)]])


(defn- render-categories-list
  [{:keys [categories] :as args}]
  (innerpage "list" "Liste" true
             [(TODO "Kategorieliste")]))

(defn render-categories-page
  "Renders the categories main page."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Liste" (jshref "innerpage('list')")}
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
    (innerpage "list" "Liste" true
               (ulist (map render-video-list-item video-list)))))

(defn render-videos-page
  "Renders the videos main page."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Liste" (jshref "innerpage('list')")}
   :page
     [(headline 2 "Videos")
      (render-videos-list args)]])


(defn- render-glossary-list
  [{:keys [videos] :as args}]
  (innerpage "list" "Liste" true
             [(TODO "Wortliste")]))

(defn- render-glossary-cloud
  [{:keys [videos] :as args}]
  (innerpage "cloud" "Wolke" false
             [(TODO "Wortwolke")]))

(defn render-glossary-page
  "Renders the main glossary page."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Liste" (jshref "innerpage('list')")
                    "Wolke" (jshref "innerpage('cloud')")}
   :page
     [(headline 2 "Glossar")
      (render-glossary-list args)
      (render-glossary-cloud args)]])
