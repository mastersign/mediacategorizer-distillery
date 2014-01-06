;; # The Namespace for Tasks
;; A task is a high-level processing step in this application.
;; Every task deals with a well delimited portion of the processing.
;; The tasks are used by [distillery.core](#distillery.core).

(ns distillery.tasks
  (:require [clojure.string :as string])
  (:require [clojure.pprint :as pp])
  (:require [clojure.java.browse :refer (browse-url)])
  (:require [mastersign.html :refer (save-page)])
  (:require [mastersign.trace :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.config :as cfg])
  (:require [distillery.text :refer [txt]])
  (:require [distillery.data :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :as proc])
  (:require [distillery.blacklist :as bl])
  (:require [distillery.xmlresult :as xr])
  (:require [distillery.txtresult :as tr])
  (:require [distillery.view.dependencies :refer (save-dependencies)])
  (:require [distillery.view.base :refer (render)])
  (:require [distillery.view.cloud :refer (build-cloud-word-data build-cloud-ui-data create-cloud)])
  (:require [distillery.view.index :as v-index])
  (:require [distillery.view.category :as v-category])
  (:require [distillery.view.video :as v-video])
  (:require [distillery.view.word :as v-word])
  (:require [distillery.view.match :as v-match]))

;; ## Helper

(defn map-fn
  "This HOF returns the map function that should be
  used for time consuming transformations.
  The configuration value `:parallel-proc`
  controls whether `pmap` or `map` is returned."
  [job]
  (if (cfg/value [:configuration :parallel-proc] job)
    #(doall (pmap %1 %2))
    #(doall (map %1 %2))))

(defmacro process-pipeline
  "This macro allows the definition of a pipeline
  similar to `->`, but with the automatic generation
  of tracing messages which allow the monitoring of
  the pipeline during execution.

  The first argument `pipe-name` is the name of the pipeline,
  the second `x` is the initial argument for the first function
  in the pipeline,
  and the all following arguments `fs` are functions,
  defining the process steps."
  [pipe-name x & fs]
  (let [trc (gensym "trc")]
  `(let [~trc (fn [x# no#] (trace-message ~(str "PIPELINE_STEP " pipe-name " ") (inc no#)) x#)]
     (trace-message ~(str "PIPELINE " pipe-name " [" (count fs) "]"))
     ~(cons `->
            (cons x
                  (apply concat
                         (map-indexed
                          (fn [i f]
                            [f `(~trc ~(inc i))])
                          fs)))))))

(defmacro process-task-group
  "This macro allows a group of potentially parallel tasks
  be executed, and monitored by automatically generated
  trace messages.

  The first argument `group-name` is a name for the process group,
  the second argument `job` is a job description and contains the configuration
  for the parallelization,
  the third argument `f` is a function which is called for every task in the group,
  and all following arguments are process tasks."
  [group-name job f xs]
  `(let [f# (fn [x#] (let [res# (~f x#)] (trace-message ~(str "TASK_END " group-name)) res#))]
     (trace-message ~(str "TASKGROUP " group-name " [" (count xs) "]"))
     (let [resv# ((map-fn ~job) f# ~xs)]
     (trace-message ~(str "TASKGROUP_END " group-name))
       resv#)))

;; The private var `filter-map` holds a look-up-table
;; mapping the filter keywords from the configuration to
;; actual predicates.
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
;; The following public functions represent all process tasks used by the
;; core tasks in [distillery.core](#distillery.core).
;; The task functions are accompanied by some private functions,
;; helping to organize the code of the tasks.
;;
;; The task functions are grouped by the following goals:
;;
;;  * Dependencies
;;  * Preprocessing and Analysis
;;      + Categories
;;      + Speech Recognition Results
;;      + Similarity Matching
;;  * XML Result Generation
;;  * Website Generation
;;      + Main Pages
;;      + Category Pages
;;      + Video Pages


;; ### Dependencies

(defn prepare-output-dir
  "Prepares the output directory by creating
  a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (trace-message "Preparing output directory " output-dir)
  (create-dir output-dir)
  (create-dir output-dir "categories")
  (create-dir output-dir "videos")
  (create-dir output-dir "words")
  (save-dependencies output-dir))


;; ### Preprocessing and Analysis

;; #### Categories

(defn- load-category-resource
  "Loads a single resource for a category.
  The resource is defined by an URL and can have on the following formats:

  * `:plain` A plain text file, which can be simply tokenized to extract the words
  * `:html` A HTML file, where the text is extracted by taking the page body
    and concatenating all textual content from the markup.
  * `:wikipedia` A Wikipedia page, which is preprocessed
    to remove the navigation elements and some common headlines
    which occur in every Wikipedia page.

  The preprocessing functions to extract the words from the resources
  reside in [distillery.data](#distillery.data)."
  [{:keys [id]} {:keys [type url] :as resource}]
  (trace-message "Loading category resource " url)
  (assoc resource :words
    (-> (case type
          :plain (load-text url)
          :html (load-text-from-html url)
          :wikipedia (load-text-from-wikipedia url))
        words-from-text)))


(defn- load-category-resources
  "Loads all resources for one category."
  [job {:keys [id resources] :as category}]
  (trace-message "Loading category resources for '" id "'")
  (let [resources* ((map-fn job) #(load-category-resource category %) resources)
        words (apply concat (map :words resources*))]
    (assoc category
      :resources resources*
      :words words)))


(defn load-categories
  "**TASK** - Loads all resources for all categories."
  [{:keys [categories] :as job}]
  (trace-block
   "Loading category resources"
   (assoc job :categories
     (vec ((map-fn job) #(load-category-resources job %) categories)))))


(defn- build-category-words
  "Extends a [Category Description](data-structures.html#CategoryDescription)
  by adding the slot `:words` with a collection of
  [Descriptive Words](data-structures.html#DescriptiveWord)."
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
  "Extends a [Category Description](data-structures.html#CategoryDescription)
  by adding the slot `:index` with a map pointing to
  [Category Words](data-structures.html#CategoryWords) and
  the slot `:index-stats` with the [Index Statistics](data-structures.html#IndexStatistics)."
  [job category]
  (trace-message "Building index for category '" (:id category) "'")
  (proc/add-category-word-index category :predicate (word-predicate job)))


(defn analyze-categories
  "**TASK** - Analyzes the categories and generates the index structures."
  [job]
  (trace-block
   "Analyzing categories"
   (let [job* (update-in
               job [:categories]
               (fn [categories]
                 (vec ((map-fn job)
                       (fn [category]
                         (build-category-index
                          job
                          (build-category-words category)))
                       categories))))]
     job*)))


;; #### Speech Recognition Results


(defn- load-speech-recognition-result
  "Loads the [Speech Recognition Result](data-structures.html#SpeechRecognitionResult)
  for a video and extends the [Video Description](data-structures.html#VideoDescription)
  by the slot `:results`."
  [{:keys [id results-file] :as video}]
  (trace-message "Loading results of " id)
  (let [results (-> results-file
                    load-data
                    proc/strip-alternates
                    proc/reverse-index-results)]
    (assoc video :results results)))


(defn load-speech-recognition-results
  "**TASK** - Loads the speech recognition results for all videos."
  [job]
  (trace-block
   "Loading speech recognition results"
   (update-in job [:videos]
              #(vec ((map-fn job) load-speech-recognition-result %)))))


(defn- build-video-statistics
  "Extends a [Video Description](data-structures.html#VideoDescription)
  by the slots `:phrase-count`, `:word-count`, and `:confidence`."
  [{:keys [id results] :as video}]
  (trace-message "Building statistics for video '" id "'")
  (assoc video
    :phrase-count (count results)
    :word-count (count (proc/words results))
    :confidence (mean (map :confidence results))))


(defn- build-video-index
  "Extends a [Video Description](data-structures.html#VideoDescription)
  by the slot `:index`, which is a map, pointing to [Video Words](data-structures.html#VideoWord),
  and by the slot `:index-stats`, which holds the [Index Statistics](data-structures.html#IndexStatistics)."
  [job video]
  (trace-message "Building index for video '" (:id video) "'")
  (proc/add-video-word-index video :predicate (word-predicate job)))


(defn- build-global-index
  "Merges the video indexes into one global [Word Index](data-structures.html#WordIndex)."
  [{:keys [videos]}]
  (trace-block
   "Merging indexes"
   (apply (partial merge-with proc/merge-index-entries) (map :index videos))))


(defn analyze-speech-recognition-results
  "**TASK** - Analyzes the speech recognition results and generates the index structures including
  the [Video Words](data-structures.html#VideoWord) and the [Word Index](data-structures.html#WordIndex)."
  [job]
  (trace-block
   "Analyzing videos"
   (let [job* (update-in
               job [:videos]
               (fn [videos]
                 (vec ((map-fn job)
                       (fn [video] (build-video-index job (build-video-statistics video)))
                       videos))))
         job* (assoc job*
                :words (build-global-index job*))]
     job*)))


;; #### Similarity Matching


(defn match-videos
  "**TASK** - Matches the [Video Words](data-structures.html#VideoWord) of all videos against
  the [Category Words](data-structures.html#CategoryWords) of all categories
  and completes the given [Video Results](data-structures.html#VideoResult)
  with the slots `:matches` and `:max-score`."
  [{:keys [videos] :as job}]
  (trace-block
   "Matching videos against categories"
   (assoc job
     :videos (vec ((map-fn job)
                   (partial proc/match-video job)
                   videos)))))


(defn lookup-categories-matches
  "**TASK** - Looks up the matching scores from the [Video Results](data-structures.html#VideoResult)
  and adds them to the [Category Results](data-structures.html#CategoryResult)."
  [{:keys [categories] :as job}]
  (trace-block
   "Looking up category matching scores against videos"
   (assoc job
     :categories (vec (map
                       (partial proc/lookup-category-match job)
                       categories)))))


(defn matching-stats
  "**TASK** - Builds some statistic values over the matching scores for the whole job.

  Completes the given [Analysis Results](data-structures.html#AnalysisResults)
  by adding the slots `:max-score` and `:max-word-score`."
  [job]
  (let [matches (mapcat #(vals (:matches %)) (:videos job))
        scores (map :score matches)
        word-scores (mapcat #(vals (:word-scores %)) matches)]
        (assoc job
          :max-score (safe-max scores)
          :max-word-score (safe-max word-scores))))


;; ### XML Result Generation


(defn save-result-as-xml
  "**TASK** - Writes the essential analysis and matching results to a XML file,
  specified by the [Job Description](data-structures.html#JobDescription)."
  [{:keys [output-dir result-file] :as job}]
  (let [path (combine-path output-dir result-file)]
    (xr/save-result path job))
  nil)


;; ### Text Result Generation


(defn save-result-as-txt
  "**TASK** - Writes a text file with all recognized phrases for each video."
  [{:keys [output-dir] :as job}]
  (doall (map
          (fn [video]
            (tr/save-result video (.toString (get-path output-dir "videos" (:id video) "transcript.txt"))))
          (:videos job)))
  nil)


;; ### Website Generation


(defn- create-main-menu
  "Builds the main menu structure."
  [{:keys [categories] :as args}]
  [[(txt :frame-top-menu-project) "index.html"]
   (when (seq categories)
     [(txt :frame-top-menu-categories) "categories.html"])
   [(txt :frame-top-menu-videos) "videos.html"]])


(defn- create-page
  "Generates a HTML page by calling the page function `page-f`
  with the [Analysis Results](data-structures.html#AnalysisResults) `args`.
  Saves the generated page in the `output-dir` of the job.
  The filename of the page is specified by the `page-name`."
  [page-name page-f {:keys [output-dir] :as args}]
  (let [target-file (combine-path output-dir page-name)
        page-def (-> args
                     page-f
                     (concat [:main-menu (create-main-menu args)]))
        page (apply render page-def)]
    (save-page target-file page)))


(defn- create-include
  "Generates a HTML include file by calling the include function `include-f`
  with the [Analysis Results](data-structures.html#AnalysisResults) `args`.
  Saves the generated include in the `output-dir` of the job.
  The filename of the include is specified by the `include-name`."
  [include-name include-f {:keys [output-dir] :as args}]
  (let [target-file (combine-path output-dir include-name)]
    (->> args
         include-f
         (save-page target-file))))


;; #### Main Pages


(defn- create-word-include
  "Creates the include file for a word in the global context."
  [{:keys [output-dir] :as job} {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-word-include
    (assoc job :word word)))


(defn- create-word-includes
  "Create the include files for all words of the project."
  [{:keys [output-dir configuration words] :as job}]
  (if (cfg/value :skip-word-includes configuration)
    (trace-message "Skipping global word includes")
    (trace-block
     "Creating global word includes"
     (let [words-path "words"]
       (create-dir (combine-path output-dir words-path))
       ((map-fn job)
        (fn [word]
          (let [word-path (combine-path words-path (:id word))]
            (create-word-include job (assoc word :path word-path))))
        (vals words))))))


(defn- create-main-cloud
  "Creates the word cloud for the global context."
  [{:keys [output-dir configuration words] :as job}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping global wordcloud") [])
    (trace-block
     "Creating global wordcloud"
     (build-cloud-ui-data (create-cloud (build-cloud-word-data words configuration :main-cloud)
                                        (combine-path output-dir "cloud.png")
                                        configuration
                                        :main-cloud)))))


(defn create-index-page
  "**TASK** - Creates the main page for the website including the
  global word cloud and the include files for all words."
  [job]
  (trace-message "Creating index page")
  (let [cloud (create-main-cloud job)
        job* (assoc job :cloud cloud)
        pwords (proc/partition-index (:words job*))
        job* (assoc job* :pwords pwords)]
    (create-page "index.html" v-index/render-main-page job*)
    (create-word-includes job*)))


(defn create-categories-page
  "**TASK** - Creates the overview page for all categories."
  [job]
  (trace-message "Creating categories overview page")
  (create-page "categories.html" v-index/render-categories-page job))


(defn create-videos-page
  "**TASK** - Creates the overview page for all videos."
  [job]
  (trace-message "Creating videos overview page")
  (create-page "videos.html" v-index/render-videos-page job))


;; #### Category Pages

(defn- create-category-word-include
  "Creates the include file for a word in the context of a category."
  [{:keys [output-dir] :as job} category {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-category-word-include
    (assoc job :category category :word word)))


(defn- create-category-word-includes
  "Create includes for all words of a category."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as category}]
  (if (cfg/value :skip-word-includes configuration)
    (trace-message "Skipping word includes for category '" id "'")
    (trace-block
     (str "Creating word includes for category '" id "'")
     (let [words-path (combine-path path "words")]
       (create-dir (combine-path output-dir words-path))
       ((map-fn job)
        (fn [word]
          (let [word-path (combine-path words-path (:id word))]
            (create-category-word-include job category (assoc word :path word-path))))
        (vals index))))))


(defn- create-category-match-include
   "Creates the include file for a match in the context of a category."
  [{:keys [output-dir] :as job} category {:keys [path] :as match}]
  (create-include
    (str path ".inc.html")
    v-match/render-category-match-include
    (assoc job :category category :match match)))


(defn- create-category-match-includes
  "Create includes for all video matches of a category."
  [{:keys [output-dir configuration] :as job} {:keys [id matches path] :as category}]
  (if (cfg/value :skip-match-includes configuration)
    (trace-message "Skipping match includes for category '" id "'")
    (trace-block
     (str "Creating match includes for category '" id "'")
     (let [matches-path (combine-path path "matches")]
       (create-dir (combine-path output-dir matches-path))
       ((map-fn  job)
        (fn [match]
          (let [match-path (combine-path matches-path (:video-id match))]
            (create-category-match-include job category (assoc match :path match-path))))
        (vals matches))))))


(defn- create-category-cloud
  "Creates the word cloud for a category."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as category}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping wordcloud for category '" id "'") [])
    (trace-block
     (str "Creating wordcloud for category '" id "'")
     (build-cloud-ui-data (create-cloud (build-cloud-word-data index configuration :category-cloud)
                                        (combine-path output-dir path "cloud.png")
                                        configuration
                                        :category-cloud)))))


(defn- create-category-page
  "Create the main page for a certain category."
  [{:keys [output-dir] :as job} {:keys [id index] :as category}]
  (trace-message "Creating category page for '" id "'")
  (let [category-path (combine-path "categories" id)]

    (create-dir (combine-path output-dir category-path))

    (let [category* (assoc category :path category-path)
          cloud (create-category-cloud job category*)
          category* (assoc category* :cloud cloud)
          pindex (proc/partition-index index)
          category* (assoc category* :pindex pindex)
          args (assoc job :category category*)]

      (create-page
       (combine-path category-path "index.html")
       v-category/render-category-page
       args)

      (create-category-word-includes job category*)
      (create-category-match-includes job category*))))


(defn create-category-pages
  "**TASK** - Creates one page for every category
  including the word clouds and the word include files."
  [job]
  (trace-block
   "Creating category pages"
   (doall
    ((map-fn job)
     #(create-category-page job %)
     (:categories job))))
  nil)


;; #### Video Pages

(defn- create-video-word-include
  "Creates the include file for a word in the context of a video."
  [{:keys [output-dir] :as job} video {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-video-word-include
    (assoc job :video video :word word)))


(defn- create-video-word-includes
  "Create includes for all words of a video."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as video}]
  (if (cfg/value :skip-word-includes configuration)
    (trace-message "Skipping word includes for video '" id "'")
    (trace-block
     (str "Creating word includes for video '" id "'")
     (let [words-path (combine-path path "words")]
       (create-dir (combine-path output-dir words-path))
       ((map-fn job)
        (fn [word]
          (let [word-path (combine-path words-path (:id word))]
            (create-video-word-include job video (assoc word :path word-path))))
        (vals index))))))


(defn- create-video-cloud
  "Creates the word cloud for a video."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as video}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping wordcloud for video '" id "'") [])
    (trace-block
     (str "Creating wordcloud for " id)
     (build-cloud-ui-data (create-cloud (build-cloud-word-data index configuration :video-cloud)
                                        (combine-path output-dir path "cloud.png")
                                        configuration
                                        :video-cloud)))))


(defn- create-video-page
  "Create the main page for a certain video."
  [{:keys [output-dir configuration] :as job} {:keys [id index video-file] :as video}]
  (trace-message "Creating video page for '" id "'")
  (let [video-path (combine-path "videos" id)]

    (create-dir (combine-path output-dir video-path))

    (let [video* (assoc video :path video-path)
          cloud (create-video-cloud job video*)
          video* (assoc video* :cloud cloud)
          pindex (proc/partition-index index)
          video* (assoc video* :pindex pindex)
          args (assoc job :video video*)]

      (if (cfg/value :skip-media-copy configuration)
        (trace-message "Skipping copy mediafile for video '" id "'")
        (let [video-target-file (combine-path output-dir video-path (str id ".mp4"))]
          (trace-message "Copy mediafile for video '" id "'")
          (when (not (file-exists? video-target-file))
            (copy-file (get-path video-file) (get-path video-target-file)))))

      (create-page
       (combine-path video-path "index.html")
       v-video/render-video-page
       args)

      (create-video-word-includes job video*))))


(defn create-video-pages
  "**TASK** - Creates one page for every video
  including the word clouds and the word include files."
  [job]
  (trace-block
   "Creating video pages"
   (doall
    ((map-fn job)
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
