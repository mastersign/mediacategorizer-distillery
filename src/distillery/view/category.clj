(ns distillery.view.category
  (:require [clojure.string :as string])
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.config :as cfg])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.view.cloud :as cloud])
  (:require [distillery.view.glossary :as glossary])
  (:require [distillery.view.hitlist :as hitlist])
  (:require [distillery.view.html :refer :all]))

(defn- render-headline
  "Creates the headline for the medium page."
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
  (innerpage "overview" (txt :category-overview-h) true
             [(ulist "category_statistic"
                    [(list-item (str (txt :category-overview-resource-count) (count (:resources category))))
                     (list-item (str (txt :category-overview-word-count) (count (:words category))))
                     (list-item (str (txt :category-overview-index-size) (count (:index category))))])
              (headline 4 (txt :category-overview-hitlist-h))
              (paragraph "explanation" (txt :category-overview-hitlist-d))
              (render-hitlist job category)]))

(defn- render-glossary
  "Creates the HTML for the category glossary page."
  [{{:keys [pindex] :as category} :category :as args}]
  (innerpage "glossary" (txt :category-glossary-h) false
             [(paragraph "explanation" (txt :category-glossary-d))
              (glossary/render-glossary pindex)]))

(defn- render-cloud
  "Creates the HTML for the overview page."
  [{{:keys [id cloud] :as category} :category :as args}]
  (innerpage "cloud" (txt :category-wordcloud-h) false
             [(paragraph "explanation" (txt :category-wordcloud-d))
              (cloud/render-cloud id cloud)]))

(defn- render-media
  "Creates the HTML for the overview page."
  [{:keys [media category max-score configuration] :as args}]
  (let [medium-fn (fn [mid] (first (filter #(= mid (:id %)) media)))]
    (innerpage "media" (txt :category-media-h) false
               [(paragraph "explanation" (txt :category-media-d))
                (hitlist/render-medium-matchlist
                 category
                 media
                 configuration)])))

(defn- render-category-word-frame
  "Create the HTML for the word frame.
   The word frame is an empty container to load a category word page into."
  [args]
  (innerpage "word" (txt :category-word-h) false nil))

(defn- render-category-match-frame
  "Create the HTML for the match frame.
   The word frame is an empty container to load a category match page into."
  [args]
  (innerpage "match" (txt :category-match-h) false nil))

(defn render-category-page
  "Renders the main page for a category."
  [{:keys [job-name category configuration] :as args}]
  [:base-path "../../"
   :title (build-title args (txt :category-title))
   :secondary-menu [[(txt :category-menu-overview) (jshref "innerpage('overview')")]
                    (when-not (cfg/value :skip-wordclouds configuration)
                      [(txt :category-menu-wordcloud) (jshref "innerpage('cloud')")])
                    [(txt :category-menu-media) (jshref "innerpage('media')")]
                    [(txt :category-menu-glossary) (jshref "innerpage('glossary')")]]
   :page
     [(render-headline args)
      (render-overview args)
      (render-glossary args)
      (render-cloud args)
      (render-media args)
      (render-category-word-frame args)
      (render-category-match-frame args)]])
