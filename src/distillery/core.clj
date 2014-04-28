(ns distillery.core
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.data :refer :all])
  (:require [distillery.trace :refer :all])
  (:require [distillery.tasks :as dt])
  (:gen-class))

(defn- run-preparation
  [job-descr]
  (dt/process-pipeline
   "Preparation"
   job-descr
   dt/load-speech-recognition-results
   dt/analyze-speech-recognition-results
   dt/load-categories
   dt/analyze-categories
   dt/match-videos
   dt/lookup-categories-matches
   dt/matching-stats))

(defn- run-output-generation
  [job]
  (trace-block
   "Result Output"
   (dt/save-result-as-xml job))
  (when (get-in job [:configuration :visualize-results])
    (trace-block
     "Result Visualization"
     (dt/prepare-output-dir job)
     (dt/process-task-group
      "Output" job #(% job)
      [dt/create-index-page
       dt/create-categories-page
       dt/create-category-pages
       dt/create-videos-page
       dt/create-video-pages]))))

(defn run-complete
  [job-descr]
  (-> job-descr
      run-preparation
      run-output-generation)
  nil)

(defn -main
  [job-file]
  (-> job-file
      load-data
      run-complete)
  (System/exit 0))


;;(def job-descr (load-data "S:\\Temp\\MediaCategorizerOutput\\_tmp_\\job.edn"))
;;(def job (run-preparation job-descr))
;;(run-output-generation job)
;;(run-complete job-descr)

