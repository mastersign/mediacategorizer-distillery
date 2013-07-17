(ns distillery.view.index
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.html :refer :all]))

(defn render-overview
  [{:keys [job-description] :as args}]
  [(paragraph job-description)
   (paragraph "TODO: Überblick über die Site.")])

(defn render-main-page
  "Renders the main page for the site."
  [{:keys [job-name] :as args}]
  [:title job-name
   :secondary-menu {"Projekt" ""
                    "Statistiken" ""}
   :page
     [(render-overview args)]])
