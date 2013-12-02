(ns distillery.view.category
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.view.cloud :as cloud])
  (:require [distillery.view.glossary :as glossary])
  (:require [distillery.view.hitlist :as hitlist])
  (:require [distillery.view.html :refer :all]))

(defn- render-headline
  "Creates the headline for the video page."
  [{:keys [category]}]
  (headline 2 (:name category)))

(defn- render-hitlist
  [{:keys [configuration] :as job} {:keys [id index] :as category}]
  (hitlist/render-occurrence-hitlist
   (vals index)
   :occurrences
   configuration
   :category-cloud))

(defn- render-overview
  "Creates the HTML for the overview page."
  [{:keys [category] :as job}]
  (innerpage "overview" "Übersicht" true
             [(ulist "category_statistic"
                    [(list-item (str "Quellen: " (count (:resources category))))
                     (list-item (str "Gesamtanzahl Worte: " (count (:words category))))
                     (list-item (str "Worte im Index: " (count (:index category))))])
              (render-hitlist job category)]))

(defn- render-glossary
  "Creates the HTML for the category glossary page."
  [{{:keys [pindex] :as category} :category :as args}]
  (innerpage "glossary" "Glossar" false
             (glossary/render-glossary pindex)))

(defn- render-cloud
  "Creates the HTML for the overview page."
  [{{:keys [id cloud] :as category} :category :as args}]
  (innerpage "cloud" "Wolke" false
             (cloud/render-cloud id cloud)))

(defn- render-videos
  "Creates the HTML for the overview page."
  [{:keys [videos category max-score configuration] :as args}]
  (let [video-fn (fn [mid] (first (filter #(= mid (:id %)) videos)))]
    (innerpage "videos" "Videos" false
              (hitlist/render-video-matchlist
               category
               videos
               configuration))))

(defn- render-category-word-frame
  "Create the HTML for the word frame.
   The word frame is an empty container to load a category word page into."
  [args]
  (innerpage "word" "Wort" false nil))

(defn- render-category-match-frame
  "Create the HTML for the match frame.
   The word frame is an empty container to load a category match page into."
  [args]
  (innerpage "match" "Übereinstimmung" false nil))

(defn render-category-page
  "Renders the main page for a category."
  [{:keys [job-name category] :as args}]
  [:base-path "../../"
   :title (str job-name " - " "Kategorie")
   :secondary-menu [["Übersicht" (jshref "innerpage('overview')")]
                    ["Wortwolke" (jshref "innerpage('cloud')")]
                    ["Videos" (jshref "innerpage('videos')")]
                    ["Glossar" (jshref "innerpage('glossary')")]]
   :page
     [(render-headline args)
      (render-overview args)
      (render-glossary args)
      (render-cloud args)
      (render-videos args)
      (render-category-word-frame args)
      (render-category-match-frame args)]])



