(ns distillery.jobs
  (:require [clojure.string :as string])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.files :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :refer :all])
  (:require [distillery.view.html :refer (save-page)])
  (:require [distillery.view.dependencies :refer (save-dependencies)])
  (:require [distillery.view.base :refer (render)])
  (:require [distillery.view.video :as v-video]))

(defn prepare-output-dir
  "Prepares the output directory by creating a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (create-dir output-dir)
  (create-dir output-dir "categories")
  (create-dir output-dir "videos")
  (create-dir output-dir "words")
  (save-dependencies output-dir))

(defn create-index-page
  "Creates the main page for the site."
  [{:keys [output-dir job-name job-description]
    :as args}]

  )

(defn create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir video]
    :as args}]

  (let [results (load-data (:results-file video))
        video-id (:id video)
        phrases (map best-alternate-phrase results)
        target-file (combine-path output-dir "videos" video-id "index.html")
        video-target-file (combine-path output-dir "videos" video-id (str video-id ".mp4"))]

    (create-dir output-dir "videos" video-id)
    (when (not (file-exists? video-target-file))
      (copy-file (get-path (:video-file video)) (get-path video-target-file)))

    (->> args
         v-video/render-video-page
         (apply render)
         (save-page target-file))))
