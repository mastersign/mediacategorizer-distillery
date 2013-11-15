(ns distillery.core
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.data :refer :all])
  (:require [distillery.trace :refer :all])
  (:require [distillery.tasks :as dt])
  (:gen-class))

(defn run-complete
  [job-descr]
  (let [job (-> job-descr
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
     (doall ((dt/map-fn) #(% job)
             [dt/create-index-page
              dt/create-categories-page
              dt/create-category-pages
              dt/create-videos-page
              dt/create-video-pages]))))
  (System/exit 0)
  nil)

(defn -main
  [job-file]
  (-> job-file
      load-data
      run-complete))

