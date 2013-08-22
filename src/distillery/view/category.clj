(ns distillery.view.category
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn- render-headline
  "Creates the headline for the video page."
  [{:keys [category]}]
  (headline 2 (:name category)))

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [category]}]
  (innerpage "overview" "Übersicht" true
             (TODO "Kategorieübersicht")))

(defn- render-glossary
  "Creates the HTML for the category glossary page."
  [{:keys [category]}]
  (innerpage "glossary" "Glossar" true
             (TODO "Glossar der Kategorie")))

(defn- render-cloud
  "Creates the HTML for the overview page."
  [{:keys [category]}]
  (innerpage "cloud" "Wolke" true
             (TODO "Wortwolke der Kategorie")))

(defn- render-videos
  "Creates the HTML for the overview page."
  [{:keys [category]}]
  (innerpage "videos" "Videos" true
             (TODO "Videoliste der Kategories")))

(defn render-category-page
  "Renders the main page for a category."
  [{:keys [job-name category] :as args}]
  [:base-path "../../"
   :title job-name
   :secondary-menu [["Übersicht" (jshref "innerpage('overview')")]
                    ["Videos" (jshref "innerpage('videos')")]
                    ["Cloud" (jshref "innerpage('cloud')")]
                    ["Glossar" (jshref "innerpage('glossary')")]]
   :page
     [(render-headline args)
      (render-overview args)
      (render-glossary args)
      (render-cloud args)
      (render-videos args)]])