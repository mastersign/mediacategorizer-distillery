(ns distillery.tasks
  (:require [clojure.string :as string])
  (:require [clojure.pprint :as pp])
  (:require [clojure.java.browse :refer (browse-url)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.files :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :as proc])
  (:require [distillery.blacklist :as bl])
  (:require [distillery.view.html :refer (save-page)])
  (:require [distillery.view.dependencies :refer (save-dependencies)])
  (:require [distillery.view.base :refer (render)])
  (:require [distillery.view.cloud :refer (build-cloud-word-data build-cloud-ui-data create-cloud)])
  (:require [distillery.view.index :as v-index])
  (:require [distillery.view.video :as v-video])
  (:require [distillery.view.word :as v-word]))

;; ## Task Tracing

(def trace-agent (agent nil))

(defn trace-message
  [& msg]
  (let [text (apply str msg)]
  (send-off trace-agent
        (fn [state] (println (str "# " text))))
  text))

(defmacro trace-block
  [msg & body]
  `(do
     (trace-message (str "BEGIN " ~msg "..."))
     (let [result# (time (do ~@body))]
       (trace-message (str "END   " ~msg))
       result#)))


;; ## Helper

(defn map-fn
  "This HOF returns the map function that should be
  used for time consuming transformations.
  The configuration var `distillery.configuration/parallel-proc`
  controls whether `pmap` or `map` is returned."
  []
  (if (cfg/value :parallel-proc)
    #(doall (pmap %1 %2))
    #(doall (map %1 %2))))

(def ^:private filter-map
  {:not-short proc/not-short?
   :noun proc/noun?
   :min-confidence proc/min-confidence?
   :good-confidence proc/good-confidence?
   :no-punctuation proc/no-punctuation?
   :not-in-blacklist bl/not-in-blacklist?})

(defn- word-predicate
  "Create the word predicate for the video index."
  [{:keys [configuration] :as job}]
  (let [filters (map #(% filter-map) (cfg/value :index-filter configuration))]
    (partial multi-filter (filter #(not (nil? %)) filters))))


;; ## Task Functions

;; ### Dependencies

(defn prepare-output-dir
  "Prepares the output directory by creating a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (trace-message "Preparing output directory " output-dir)
  (create-dir output-dir)
  (create-dir output-dir "categories")
  (create-dir output-dir "videos")
  (create-dir output-dir "words")
  (save-dependencies output-dir))


;; ### Preprocessing, Analysis

;; #### Categories

(defn- load-category-resource
  "Loads a single resource for a category."
  [{:keys [id]} {:keys [type url] :as resource}]
  (trace-message "Loading category resource " url)
  (assoc resource :words
    (case type
      :plain (load-words url)
      :html (load-words-from-html url))))


(defn- load-category-resources
  "Loads the resources for a category."
  [{:keys [id resources] :as category}]
  (trace-message "Loading category resources for '" id "'")
  (let [resources* ((map-fn) #(load-category-resource category %) resources)
        words (apply concat (map :words resources*))]
    (assoc category
      :resources resources*
      :words words)))


(defn load-categories
  "Loads the resources of the categories."
  [{:keys [categories] :as job}]
  (trace-block
   "Loading category resources"
   (assoc job :categories
     (doall (map #(load-category-resources %) categories)))))


(defn- build-category-words
  [{:keys [id words] :as category}]
  (trace-message "Building words for category '" id "'")
  (assoc category :words
    (map-indexed
     (fn [i w] {:no i
                :text w
                :lexical-form w
                :confidence 1})
     words)))


(defn- build-category-index
  [job category]
  (trace-message "Building index for category '" (:id category) "'")
  (let [index (proc/category-index category :predicate (word-predicate job))]
    (assoc category :index index)))


(defn analyze-categories
  "Analyzes the categories and generates the index structures."
  [job]
  (trace-block
   "Analyzing categories"
   (let [job* (update-in
               job [:categories]
               (fn [categories]
                 (vec ((map-fn)
                       (fn [category]
                         (build-category-index
                          job
                          (build-category-words category)))
                       categories))))]
     job*)))


;; #### Speech Recognition Results


(defn- load-speech-recognition-result
  "Loads the speech recognition results for a video."
  [{:keys [id results-file] :as video}]
  (trace-message "Loading results of " id)
  (let [results (-> results-file
                    load-data
                    proc/strip-alternates
                    proc/reverse-index-results)]
    (assoc video :results results)))


(defn load-speech-recognition-results
  "Loads the speech recognition results for the videos."
  [job]
  (trace-block
   "Loading speech recognition results"
   (update-in job [:videos]
              #(vec ((map-fn) load-speech-recognition-result %)))))


(defn- build-video-statistics
  [{:keys [id results] :as video}]
  (trace-message "Building statistics for video '" id "'")
  (let [last-result (last results)
        duration (+ (:start last-result) (:duration last-result))]
    (assoc video
      :phrase-count (count results)
      :word-count (count (proc/words results))
      :confidence (mean (map :confidence results))
      :duration duration)))


(defn- build-video-index
  "Analyzes the speech recognition results of a single video and builds the video word index."
  [job video]
  (trace-message "Building index for video '" (:id video) "'")
  (let [index (proc/video-word-index video :predicate (word-predicate job))]
    (assoc video :index index)))


(defn- build-global-index
  "Merges the video indexes into one global word index."
  [{:keys [videos]}]
  (trace-block
   "Merging indexes"
   (apply (partial merge-with proc/merge-index-entries) (map :index videos))))


(defn analyze-speech-recognition-results
  "Analyzes the speech recognition results and generates the index structures."
  [job]
  (trace-block
   "Analyzing videos"
   (let [job* (update-in
               job [:videos]
               (fn [videos]
                 (vec ((map-fn)
                       (fn [video] (build-video-index job (build-video-statistics video)))
                       videos))))
         job* (assoc job*
                :words (build-global-index job*))]
     job*)))


;; ### Output Generation

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


(defn create-word-include
  "Creates the include file for a word in the global context."
  [{:keys [output-dir] :as job} {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-word-include
    (assoc job :word word)))


(defn create-word-includes
  "Create includes for all words of the project."
  [{:keys [output-dir words] :as job}]
  (trace-block
   "Creating global word includes"
   (let [words-path "words"]
     (create-dir (combine-path output-dir words-path))
     (map
      (fn [word]
        (let [word-path (combine-path words-path (:id word))]
          (create-word-include job (assoc word :path word-path))))
      (vals words)))))


(defn- create-main-cloud
  "Creates the word cloud the global context."
  [{:keys [output-dir configuration words] :as job}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping global wordcloud") [])
    (trace-block
     "Creating global wordcloud"
     (build-cloud-ui-data (create-cloud (build-cloud-word-data words)
                                        (combine-path output-dir "cloud.png")
                                        configuration
                                        :main-cloud)))))


(defn create-index-page
  "Creates the main page for the site."
  [job]
  (trace-message "Creating index page")
  (let [cloud (create-main-cloud job)
        job* (assoc job :cloud cloud)
        pwords (proc/partition-index (:words job*))
        job* (assoc job* :pwords pwords)]
    (create-page "index.html" v-index/render-main-page job*)
    (create-word-includes job*)))


(defn create-categories-page
  "Creates the overview page for all categories."
  [job]
  (trace-message "Creating categories overview page")
  (create-page "categories.html" v-index/render-categories-page job))


(defn create-category-pages
  "Creates one page for every category."
  [job]
  (trace-block "Creating category pages"
             (println "TODO: category pages")))


(defn create-videos-page
  "Creates the overview page for all videos."
  [job]
  (trace-message "Creating videos overview page")
  (create-page "videos.html" v-index/render-videos-page job))


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
  (trace-block
   (str "Creating video word includes for " id)
   (let [words-path (combine-path path "words")]
     (create-dir (combine-path output-dir words-path))
     ((map-fn)
      (fn [word]
       (let [word-path (combine-path words-path (:id word))]
         (create-video-word-include job video (assoc word :path word-path))))
      (vals index)))))


(defn- create-video-cloud
  "Creates the word cloud for a video."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as video}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping wordcloud for " id) [])
    (trace-block
     (str "Creating wordcloud for " id)
     (build-cloud-ui-data (create-cloud (build-cloud-word-data index)
                                        (combine-path output-dir path "cloud.png")
                                        configuration
                                        :video-cloud)))))


(defn create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir] :as job} {:keys [id index video-file] :as video}]
  (trace-message "Creating video page for " id)
  (let [video-path (combine-path "videos" id)]

    (create-dir (combine-path output-dir video-path))

    (let [video* (assoc video :path video-path)
          cloud (create-video-cloud job video*)
          video* (assoc video* :cloud cloud)
          pindex (proc/partition-index index)
          video* (assoc video* :pindex pindex)
          args (assoc job :video video*)]

      (let [video-target-file (combine-path output-dir video-path (str id ".mp4"))]
        (when (not (file-exists? video-target-file))
          (copy-file (get-path video-file) (get-path video-target-file)))      )

      (create-page
       (combine-path video-path "index.html")
       v-video/render-video-page
       args)

      (create-video-word-includes job video*))))


(defn create-video-pages
  "Creates one page for every video."
  [job]
  (trace-block
   "Creating video pages"
   (doall
    ((map-fn)
     #(create-video-page job %)
     (:videos job))))
  nil)


;; ## Debug Tasks


(defn print-reverse-indexed-results
  [{:keys [video]}]
  (let [results (load-data (:results-file video))]
    (pp/pprint (proc/reverse-index-results [(first results)]))))


(defn show-main-page
  [{:keys [output-dir]}]
  (-> (combine-path output-dir "/index.html")
      get-path
      .toUri
      .toString
      browse-url))



