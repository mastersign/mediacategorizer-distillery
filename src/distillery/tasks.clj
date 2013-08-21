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
  (:require [distillery.view.cloud :refer (build-cloud-word-data build-cloud-ui-data)])
  (:require [distillery.view.index :as v-index])
  (:require [distillery.view.video :as v-video])
  (:require [distillery.view.word :as v-word])
  (:require [mastersign.wordcloud :as mwc])
  (:require [mastersign.drawing :as mdr]))

(defn print-progress
  [& msg]
  (println (str "# " (apply str msg))))


(defmacro long-task
  [msg & body]
  `(do
     (print-progress (str "BEGIN " ~msg "..."))
       (let [result# (time (do ~@body))]
         (print-progress (str "END   " ~msg))
         result#)))


(defn- load-speech-recognition-result
  "Loads the speech recognition results for a video."
  [{:keys [id results-file] :as video}]
  (print-progress "Loading results of " id)
  (let [results (-> results-file
                    load-data
                    proc/strip-alternates
                    proc/reverse-index-results)]
    (assoc video :results results)))


(defn load-speech-recognition-results
  "Loads the speech recognition results for the videos."
  [job]
  (long-task "Loading speech recognition results"
    (update-in job [:videos] #(vec (map load-speech-recognition-result %)))))


(defn- build-video-index
  "Analyzes the speech recognition results of a single video and builds the video word index."
  [video]
  (print-progress "Building index for " (:id video))
  (let [filters [proc/not-short? proc/noun? proc/min-confidence?]
        predicate (partial multi-filter filters)
        index (proc/video-word-index video :predicate predicate)]
    (assoc video :index index)))


(defn- build-video-statistics
  [{:keys [id results] :as video}]
  (print-progress "Building statistics for " id)
  (let [last-result (last results)
        duration (+ (:start last-result) (:duration last-result))]
    (assoc video
      :phrase-count (count results)
      :word-count (count (proc/words results))
      :confidence (mean (map :confidence results))
      :duration duration)))


(defn analyze-speech-recognition-results
  "Analyzes the speech recognition results an generates the index structures."
  [job]
  (long-task "Analyzing videos"
    (update-in job [:videos]
      #(vec (map
              (comp
                build-video-statistics
                build-video-index)
              %)))))


(defn prepare-output-dir
  "Prepares the output directory by creating a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (print-progress "Preparing output directory " output-dir)
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


(defn- create-include
  [include-name include-f {:keys [output-dir] :as args}]
  (let [target-file (combine-path output-dir include-name)]
    (->> args
         include-f
         (save-page target-file))))


(defn create-index-page
  "Creates the main page for the site."
  [job]
  (print-progress "Creating index page")
  (create-page "index.html" v-index/render-main-page job))


(defn create-categories-page
  "Creates the overview page for all categories."
  [job]
  (print-progress "Creating categories overview page")
  (create-page "categories.html" v-index/render-categories-page job))


(defn create-videos-page
  "Creates the overview page for all videos."
  [job]
  (print-progress "Creating videos overview page")
  (create-page "videos.html" v-index/render-videos-page job))


(defn create-glossary-page
  "Creates the overview page for all words."
  [job]
  (print-progress "Creating global glossary page")
  (create-page "glossary.html" v-index/render-glossary-page job))


(defn create-video-word-include
  "Creates the include file for a word in the context of a video."
  [{:keys [output-dir] :as job} video {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-video-word-include
    (assoc job :video video :word word)))


(defn create-video-word-includes
  "Create includes for all words of a video."
  [{:keys [output-dir] :as job} {:keys [id index path] :as video}]
  (long-task
   (str "Creating video word includes for " id)
   (let [words-path (combine-path path "words")]
     (create-dir (combine-path output-dir words-path))
     (doseq [word (vals index)]
       (let [word-path (combine-path words-path (:id word))]
         (create-video-word-include job video (assoc word :path word-path)))))))


(defn- create-video-cloud
  "Creates the word cloud for a video."
  [{:keys [output-dir cloud-precision] :as job} {:keys [id index path] :as video}]
  (long-task
   (str "Creating word cloud for " id)
   (let [target-path (combine-path output-dir path "cloud.png")
         word-data (build-cloud-word-data index)
         precision (case cloud-precision :low 0.25 :medium 0.45 :high 0.65 0.35)
         cloud-info (mwc/create-cloud word-data
                                      :target-file target-path
                                      :width 540
                                      :height 200
                                      :precision precision
                                      :order-priority 0.5
                                      :font (mdr/font "Segoe UI" 20 :bold)
                                      :min-font-size 13
                                      :max-font-size 70
                                      :color-fn #(mdr/color 0 0.3 0.8 (+ 0.25 (* % 0.75))))]
                                      ;:color-fn #(mdr/color (- 0.75 (* % 0.75)) (- 0.6 (* % 0.2)) (+ 0.5 (* % 0.5)) 1))]
     (build-cloud-ui-data cloud-info))))


(defn create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir] :as job} {:keys [id index video-file] :as video}]
  (print-progress "Creating video page for " id)
  (let [video-path (combine-path "videos" id)
        video* (assoc video :path video-path)
        cloud (create-video-cloud job video*)
        video* (assoc video* :cloud cloud)
        pindex (proc/partition-index index)
        video* (assoc video* :pindex pindex)
        args (assoc job :video video*)]

    (create-dir (combine-path output-dir video-path))

    (let [video-target-file (combine-path output-dir video-path (str id ".mp4"))]
      (when (not (file-exists? video-target-file))
        (copy-file (get-path video-file) (get-path video-target-file)))      )

    (create-page
     (combine-path video-path "index.html")
      v-video/render-video-page
      args)

    (create-video-word-includes job video*)))


(defn create-video-pages
  "Creates the main pages for all videos."
  [job]
  (long-task "Creating video pages"
    (doseq [video (:videos job)]
      (create-video-page job video))))


(defn print-reverse-indexed-results
  [{:keys [video]}]
  (let [results (load-data (:results-file video))]
    (pp/pprint (proc/reverse-index-results [(first results)]))))
