(ns distillery.tasks
  (:require [clojure.string :as string])
  (:require [clojure.pprint :as pp])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.files :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :as proc])
  (:require [distillery.view.html :refer (save-page)])
  (:require [distillery.view.dependencies :refer (save-dependencies)])
  (:require [distillery.view.base :refer (render)])
  (:require [distillery.view.index :as v-index])
  (:require [distillery.view.video :as v-video]))


(defn- load-speech-recognition-result
  "Loads the speech recognition results for a video."
  [{:keys [results-file] :as video}]
  (let [results (proc/strip-alternates (load-data results-file))]
    (assoc video :results results)))


(defn load-speech-recognition-results
  "Loads the speech recognition results for the videos."
  [job]
  (update-in job [:videos] #(vec (map load-speech-recognition-result %))))

(defn- analyze-speech-recognition-result
  "Analyzes the speech recognition results of a single video and builds the video word index."
  [{:keys [results] :as video}]
  (let [filters [proc/not-short? proc/noun? proc/min-confidence?]
        predicate (partial multi-filter filters)
        index (proc/video-word-index results :predicate predicate)]
    (assoc video :index index)))

(defn analyze-speech-recognition-results
  "Analyzes the speech recognition results an generates the index structures."
  [job]
  (-> job
      (update-in [:videos] #(vec (map analyze-speech-recognition-result %)))))

(defn prepare-output-dir
  "Prepares the output directory by creating a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (create-dir output-dir)
  (create-dir output-dir "categories")
  (create-dir output-dir "videos")
  (create-dir output-dir "words")
  (save-dependencies output-dir))


(defn- create-page
  [page-name page-f {:keys [output-dir] :as args}]
  (let [target-file (combine-path output-dir page-name)]
    (->> args
         page-f
         (apply render)
         (save-page target-file))))


(defn create-index-page
  "Creates the main page for the site."
  [args]
  (create-page "index.html" v-index/render-main-page args))

(defn create-categories-page
  "Creates the overview page for all categories."
  [args]
  (create-page "categories.html" v-index/render-categories-page args))

(defn create-videos-page
  "Creates the overview page for all videos."
  [args]
  (create-page "videos.html" v-index/render-videos-page args))

(defn create-glossary-page
  "Creates the overview page for all words."
  [args]
  (create-page "glossary.html" v-index/render-glossary-page args))

(defn create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir] :as job} {:keys [index] :as video}]
  (let [video-id (:id video)
        video-target-file (combine-path output-dir "videos" video-id (str video-id ".mp4"))
        pindex (proc/partition-index index)
        video (assoc video :pindex pindex)
        args (assoc job :video video)]

    (create-dir output-dir "videos" video-id)
    (when (not (file-exists? video-target-file))
      (copy-file (get-path (:video-file video)) (get-path video-target-file)))

    (create-page
      (combine-path "videos" video-id "index.html")
      v-video/render-video-page
      args)))

(defn create-video-pages
  "Creates the main pages for all videos."
  [job]
  (doseq [video (:videos job)]
    (create-video-page job video)))

(defn print-reverse-indexed-results
  [{:keys [video]}]
  (let [results (load-data (:results-file video))]
    (pp/pprint (proc/reverse-index-results [(first results)]))))
