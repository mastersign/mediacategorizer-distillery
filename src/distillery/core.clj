(ns distillery.core
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.data :refer :all])
  (:require [distillery.trace :refer :all])
  (:require [distillery.tasks :as dt])
  (:gen-class))

(defn run-complete
  [job-descr]
  (let [job (dt/process-pipeline
             "Preparation"
             job-descr
             dt/load-speech-recognition-results
             dt/analyze-speech-recognition-results
             dt/load-categories
             dt/analyze-categories
             dt/match-videos
             dt/lookup-categories-matches
             dt/matching-stats)]
    (trace-block
     "Output generation"
     (dt/prepare-output-dir job)
     (dt/process-task-group
      "Output" job #(% job)
      [dt/create-index-page
       dt/create-categories-page
       dt/create-category-pages
       dt/create-videos-page
       dt/create-video-pages])))
  nil)

(defn -main
  [job-file]
  (-> job-file
      load-data
      run-complete)
  (System/exit 0))

