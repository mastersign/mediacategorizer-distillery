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
  (:require [distillery.view.video :as v-video])
  (:require [distillery.view.word :as v-word]))


(defn- load-speech-recognition-result
  "Loads the speech recognition results for a video."
  [{:keys [results-file] :as video}]
  (let [results (-> results-file
                    load-data
                    proc/strip-alternates
                    proc/reverse-index-results)]
    (assoc video :results results)))


(defn load-speech-recognition-results
  "Loads the speech recognition results for the videos."
  [job]
  (update-in job [:videos] #(vec (map load-speech-recognition-result %))))

(defn- analyze-speech-recognition-result
  "Analyzes the speech recognition results of a single video and builds the video word index."
  [video]
  (let [filters [proc/not-short? proc/noun? proc/min-confidence?]
        predicate (partial multi-filter filters)
        index (proc/video-word-index video :predicate predicate)]
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
    (println (str "Creating Page: " target-file))
    (->> args
         page-f
         (apply render)
         (save-page target-file))))

(defn- create-include
  [include-name include-f {:keys [output-dir] :as args}]
  (let [target-file (combine-path output-dir include-name)]
    (println (str "Creating Include: " target-file))
    (->> args
         include-f
         (save-page target-file))))


(defn create-index-page
  "Creates the main page for the site."
  [job]
  (create-page "index.html" v-index/render-main-page job))

(defn create-categories-page
  "Creates the overview page for all categories."
  [job]
  (create-page "categories.html" v-index/render-categories-page job))

(defn create-videos-page
  "Creates the overview page for all videos."
  [job]
  (create-page "videos.html" v-index/render-videos-page job))

(defn create-glossary-page
  "Creates the overview page for all words."
  [job]
  (create-page "glossary.html" v-index/render-glossary-page job))

(defn create-video-word-include
  [{:keys [output-dir] :as job} video {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-video-word-include
    (assoc job :video video :word word)))

(defn create-video-word-includes
  [{:keys [output-dir] :as job} {:keys [id index path] :as video}]
  (let [words-path (combine-path path "words")]
    (create-dir (combine-path output-dir words-path))
    (doseq [word (vals index)]
      (let [word-path (combine-path words-path (:id word))]
        (create-video-word-include job video (assoc word :path word-path))))))

(defn create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir] :as job} {:keys [index video-file] :as video}]
  (let [video-id (:id video)
        video-path (combine-path "videos" video-id)
        video-target-file (combine-path output-dir video-path (str video-id ".mp4"))
        pindex (proc/partition-index index)
        video* (assoc video :pindex pindex :path video-path)
        args (assoc job :video video*)]

    (create-dir (combine-path output-dir video-path))
    (when (not (file-exists? video-target-file))
      (copy-file (get-path video-file) (get-path video-target-file)))

    (create-page
      (combine-path video-path "index.html")
      v-video/render-video-page
      args)

    (create-video-word-includes job video*)))

(defn create-video-pages
  "Creates the main pages for all videos."
  [job]
  (doseq [video (:videos job)]
    (create-video-page job video)))

(defn print-reverse-indexed-results
  [{:keys [video]}]
  (let [results (load-data (:results-file video))]
    (pp/pprint (proc/reverse-index-results [(first results)]))))
