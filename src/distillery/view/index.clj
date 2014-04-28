(ns distillery.view.index
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.data :refer [key-comp]])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.config :as cfg])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.glossary :as glossary])
  (:require [distillery.view.cloud :as cloud])
  (:require [distillery.view.hitlist :as hitlist])
  (:require [distillery.view.match :as match]))

(defn- render-hitlist
  [{:keys [words configuration] :as args}]
  (hitlist/render-occurrence-hitlist
   (vals words)
   :occurrences
   configuration
   :main-cloud))

(defn- render-main-overview
  [{:keys [job-description media categories words] :as args}]
  (innerpage "overview" (txt :main-overview-h) true
             [(paragraph "explanation" (txt :main-overview-d))
              (headline 4 (txt :main-overview-description-h))
              (paragraph job-description)
              (headline 4 (txt :main-overview-content-h))
              (ulist "main_statistic"
                     [(list-item (str (txt :main-overview-media) (count media)))
                      (list-item (str (txt :main-overview-categories) (count categories)))
                      (list-item (str (txt :main-overview-words) (count words)))])
              (headline 4 (txt :main-overview-hitlist-h))
              (paragraph "explanation" (txt :main-overview-hitlist-d))
              (render-hitlist args)]))

(defn- render-main-cloud
  [{:keys [cloud] :as args}]
  (innerpage "cloud" (txt :main-wordcloud-h) false
             [(paragraph "explanation" (txt :main-wordcloud-d))
              (cloud/render-cloud "global" cloud)]))

(defn- render-main-word-frame
  "Create the HTML for the word frame.
   The word frame is an empty container to load a word page into."
  [args]
  (innerpage "word" (txt :main-word-h) false nil))

(defn- render-main-glossary
  [{:keys [pwords] :as args}]
  (innerpage "glossary" (txt :main-glossary-h) false
             [(paragraph "explanation" (txt :main-glossary-d))
              (glossary/render-glossary pwords)]))

(defn- render-main-matching-matrix
  [args]
   (innerpage "matrix" (txt :main-matching-matrix-h) false
              [(paragraph "explanation" (txt :main-matching-matrix-d))
               (match/render-match-matrix args)]))

(defn render-main-page
  "Renders the main page for the site."
  [{:keys [job-name configuration] :as args}]
  [:title (build-title args nil)
   :secondary-menu [[(txt :main-menu-overview) (jshref "innerpage('overview')")]
                    (when-not (cfg/value :skip-wordclouds configuration)
                      [(txt :main-menu-wordcloud) (jshref "innerpage('cloud')")])
                    [(txt :main-menu-matching-matrix) (jshref "innerpage('matrix')")]
                    [(txt :main-menu-glossary) (jshref "innerpage('glossary')")]]
   :page
     [(headline 2 "Projekt")
      (render-main-overview args)
      (render-main-cloud args)
      (render-main-matching-matrix args)
      (render-main-glossary args)
      (render-main-word-frame args)]])

(defn- render-categories-list-item
  [{:keys [id name] :as category}]
  (list-item (link (str "categories/" id "/index.html") name)))

(defn- render-categories-list
  [{:keys [categories] :as args}]
  (let [categories-list (->> categories
                             (into (sorted-set-by (key-comp :name)))
                             (map render-categories-list-item)
                             (ulist))]
    (innerpage "overview" (txt :categories-overview-h) true
               [(paragraph "explanation" (txt :categories-overview-d))
                categories-list])))

(defn render-categories-page
  "Renders the categories main page."
  [{:keys [job-name] :as args}]
  [:title (build-title args (txt :categories-title))
   :secondary-menu [[(txt :categories-menu-overview) (jshref "innerpage('overview')")]]
   :page
     [(headline 2 (txt :categories-h))
      (render-categories-list args)]])

(defn- render-media-list-item
  "Renders a medium link as list item."
  [{:keys [id name] :as medium}]
  (list-item (link (str "media/" id "/index.html") name)))

(defn- render-media-list
  [{:keys [media] :as args}]
  (let [media-list (->> media
                        (into (sorted-set-by (key-comp :name)))
                        (map render-media-list-item)
                        (ulist))]
    (innerpage "overview" (txt :media-overview-h) true
               [(paragraph "explanation" (txt :media-overview-d))
                media-list])))

(defn render-media-page
  "Renders the media main page."
  [{:keys [job-name] :as args}]
  [:title (build-title args (txt :media-title))
   :secondary-menu [[(txt :media-menu-overview) (jshref "innerpage('overview')")]]
   :page
     [(headline 2 (txt :media-h))
      (render-media-list args)]])
