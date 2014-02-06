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
  (:require [distillery.view.medium :as v-medium])
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
  "Create the word predicate for the medium index."
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
;;  * Text Result Generation
;;  * Website Generation
;;      + Main Pages
;;      + Category Pages
;;      + Medium Pages


;; ### Dependencies

(defn prepare-output-dir
  "Prepares the output directory by creating
  a number of sub directories and copying site dependencies."
  [{:keys [output-dir]}]
  (trace-message "Preparing output directory " output-dir)
  (create-dir output-dir)
  (create-dir output-dir "categories")
  (create-dir output-dir "media")
  (create-dir output-dir "words")
  (save-dependencies output-dir))


;; ### Preprocessing and Analysis

;; #### Categories

(defn- load-category-resource
  "Loads a single resource for a category.
  The resource is defined by an absolute path to a local file or an URL. If both are given, the local file takes precedence.
  The resource can have one of the following types:

  * `:plain` A plain text file, which can be simply tokenized to extract the words
  * `:html` A HTML file, where the text is extracted by taking the page body
    and concatenating all textual content from the markup.
  * `:wikipedia` A Wikipedia page, which is preprocessed
    to remove the navigation elements and some common headlines
    which occur in every Wikipedia page.

  The preprocessing functions to extract the words from the resources
  reside in [distillery.data](#distillery.data)."
  [{:keys [id]} {:keys [type url file] :as resource}]
  (trace-message "Loading category resource " file)
  (let [url (if file (str "file:///" file) url)]
    (assoc resource :words
      (-> (case type
            :plain (load-text url)
            :html (load-text-from-html url)
            :wikipedia (load-text-from-wikipedia url))
          words-from-text))))


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
  for a medium and extends the [Medium Description](data-structures.html#MediumDescription)
  by the slot `:results`."
  [{:keys [id results-file] :as medium}]
  (trace-message "Loading results of " id)
  (let [results (-> results-file
                    load-data
                    proc/strip-alternates
                    proc/reverse-index-results)]
    (assoc medium :results results)))


(defn load-speech-recognition-results
  "**TASK** - Loads the speech recognition results for all media."
  [job]
  (trace-block
   "Loading speech recognition results"
   (update-in job [:media]
              #(vec ((map-fn job) load-speech-recognition-result %)))))


(defn- build-medium-statistics
  "Extends a [Medium Description](data-structures.html#MediumDescription)
  by the slots `:phrase-count`, `:word-count`, and `:confidence`."
  [{:keys [id results] :as medium}]
  (trace-message "Building statistics for medium '" id "'")
  (assoc medium
    :phrase-count (count results)
    :word-count (count (proc/words results))
    :confidence (mean (map :confidence results))))


(defn- build-medium-index
  "Extends a [Medium Description](data-structures.html#MediumDescription)
  by the slot `:index`, which is a map, pointing to [Medium Words](data-structures.html#MediumWord),
  and by the slot `:index-stats`, which holds the [Index Statistics](data-structures.html#IndexStatistics)."
  [job medium]
  (trace-message "Building index for medium '" (:id medium) "'")
  (proc/add-medium-word-index medium :predicate (word-predicate job)))


(defn- build-global-index
  "Merges the medium indexes into one global [Word Index](data-structures.html#WordIndex)."
  [{:keys [media]}]
  (trace-block
   "Merging indexes"
   (apply (partial merge-with proc/merge-index-entries) (map :index media))))


(defn analyze-speech-recognition-results
  "**TASK** - Analyzes the speech recognition results and generates the index structures including
  the [Medium Words](data-structures.html#MediumWord) and the [Word Index](data-structures.html#WordIndex)."
  [job]
  (trace-block
   "Analyzing media"
   (let [job* (update-in
               job [:media]
               (fn [media]
                 (vec ((map-fn job)
                       (fn [medium] (build-medium-index job (build-medium-statistics medium)))
                       media))))
         job* (assoc job*
                :words (build-global-index job*))]
     job*)))


;; #### Similarity Matching


(defn match-media
  "**TASK** - Matches the [Medium Words](data-structures.html#MediumWord) of all media against
  the [Category Words](data-structures.html#CategoryWords) of all categories
  and completes the given [Medium Results](data-structures.html#MediumResult)
  with the slots `:matches` and `:max-score`."
  [{:keys [media] :as job}]
  (trace-block
   "Matching media against categories"
   (assoc job
     :media (vec ((map-fn job)
                   (partial proc/match-medium job)
                   media)))))


(defn lookup-categories-matches
  "**TASK** - Looks up the matching scores from the [Medium Results](data-structures.html#MediumResult)
  and adds them to the [Category Results](data-structures.html#CategoryResult)."
  [{:keys [categories] :as job}]
  (trace-block
   "Looking up category matching scores against media"
   (assoc job
     :categories (vec (map
                       (partial proc/lookup-category-match job)
                       categories)))))


(defn matching-stats
  "**TASK** - Builds some statistic values over the matching scores for the whole job.

  Completes the given [Analysis Results](data-structures.html#AnalysisResults)
  by adding the slots `:max-score` and `:max-word-score`."
  [job]
  (let [matches (mapcat #(vals (:matches %)) (:media job))
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
  "**TASK** - Writes a text file with all recognized phrases for each medium."
  [{:keys [output-dir] :as job}]
  (doseq [medium (:media job)]
    (let [dir (combine-path output-dir "media" (:id medium))
          path (combine-path dir "transcript.txt")]
      (when (not (file-exists? dir))
        (create-dir dir))
      (tr/save-result medium path)))
  nil)


;; ### Website Generation


(defn- create-main-menu
  "Builds the main menu structure."
  [{:keys [categories] :as args}]
  [[(txt :frame-top-menu-project) "index.html"]
   (when (seq categories)
     [(txt :frame-top-menu-categories) "categories.html"])
   [(txt :frame-top-menu-media) "media.html"]])


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


(defn create-media-page
  "**TASK** - Creates the overview page for all media."
  [job]
  (trace-message "Creating media overview page")
  (create-page "media.html" v-index/render-media-page job))


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
  "Create includes for all medium matches of a category."
  [{:keys [output-dir configuration] :as job} {:keys [id matches path] :as category}]
  (if (cfg/value :skip-match-includes configuration)
    (trace-message "Skipping match includes for category '" id "'")
    (trace-block
     (str "Creating match includes for category '" id "'")
     (let [matches-path (combine-path path "matches")]
       (create-dir (combine-path output-dir matches-path))
       ((map-fn  job)
        (fn [match]
          (let [match-path (combine-path matches-path (:medium-id match))]
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


;; #### Media Pages

(defn- create-medium-word-include
  "Creates the include file for a word in the context of a medium."
  [{:keys [output-dir] :as job} medium {:keys [path] :as word}]
  (create-include
    (str path ".inc.html")
    v-word/render-medium-word-include
    (assoc job :medium medium :word word)))


(defn- create-medium-word-includes
  "Create includes for all words of a medium."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as medium}]
  (if (cfg/value :skip-word-includes configuration)
    (trace-message "Skipping word includes for medium '" id "'")
    (trace-block
     (str "Creating word includes for medium '" id "'")
     (let [words-path (combine-path path "words")]
       (create-dir (combine-path output-dir words-path))
       ((map-fn job)
        (fn [word]
          (let [word-path (combine-path words-path (:id word))]
            (create-medium-word-include job medium (assoc word :path word-path))))
        (vals index))))))


(defn- create-medium-cloud
  "Creates the word cloud for a medium."
  [{:keys [output-dir configuration] :as job} {:keys [id index path] :as medium}]
  (if (cfg/value :skip-wordclouds configuration)
    (do (trace-message "Skipping wordcloud for medium '" id "'") [])
    (trace-block
     (str "Creating wordcloud for " id)
     (build-cloud-ui-data (create-cloud (build-cloud-word-data index configuration :medium-cloud)
                                        (combine-path output-dir path "cloud.png")
                                        configuration
                                        :medium-cloud)))))


(defn- create-medium-page
  "Create the main page for a certain medium."
  [{:keys [output-dir configuration] :as job} {:keys [id index encoded-media-files] :as medium}]
  (trace-message "Creating medium page for '" id "'")
  (let [medium-path (combine-path "media" id)]

    (create-dir (combine-path output-dir medium-path))

    (let [medium* (assoc medium :path medium-path)
          cloud (create-medium-cloud job medium*)
          medium* (assoc medium* :cloud cloud)
          pindex (proc/partition-index index)
          medium* (assoc medium* :pindex pindex)
          encoded-media-files* (map #(assoc % :ext (file-name-ext (:path %))) encoded-media-files)
          medium* (assoc medium* :encoded-media-files encoded-media-files*)
          args (assoc job :medium medium*)]

      (if (cfg/value :skip-media-copy configuration)
        (trace-message "Skipping copy mediafile for medium '" id "'")
        (do
          (trace-message "Copy media files for medium '" id "'")
          (doseq [{:keys [path ext]} encoded-media-files*]
            (let [medium-target-file (combine-path output-dir medium-path (str id ext))]
              (when (not (file-exists? medium-target-file))
                (copy-file (get-path path) (get-path medium-target-file)))))))

      (create-page
       (combine-path medium-path "index.html")
       v-medium/render-medium-page
       args)

      (create-medium-word-includes job medium*))))


(defn create-medium-pages
  "**TASK** - Creates one page for every medium
  including the word clouds and the word include files."
  [job]
  (trace-block
   "Creating medium pages"
   (doall
    ((map-fn job)
     #(create-medium-page job %)
     (:media job))))
  nil)


;; ## Debug Tasks


(defn print-reverse-indexed-results
  [{:keys [medium]}]
  (let [results (load-data (:results-file medium))]
    (pp/pprint (proc/reverse-index-results [(first results)]))))


(defn show-main-page
  [{:keys [output-dir]}]
  (-> (combine-path output-dir "/index.html")
      get-path
      .toUri
      .toString
      browse-url))
