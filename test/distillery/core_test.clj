(ns distillery.core-test
  (:require [clojure.test :refer :all])
  (:require [distillery.tasks :as dt])
  (:require [distillery.core :refer :all]))

(def root "D:\\Daten\\FH\\OLL\\") ;; FHB
;(def root "D:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; HOME
;(def root "C:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; NB

(def job-descr
  {:job-name "Testlauf"
   :job-description "Ein Testprojekt für Testzwecke mit Testvideos und Testkategorien. Wird zum Testen verwendet."
   :output-dir (str root "Output")
   :cloud-precision :low ; :low, :medium, :high
   :videos [{:id "12-01-1"
             :name "12.01.1 Datenstrukturen, Array, Queue, Stack"
             :video-file (str root "Media\\Video\\12.01.1 Datenstrukturen, Array, Queue, Stack.mp4")
             :audio-file (str root "Media\\Audio\\de-DE\\12.01.1 Datenstrukturen, Array, Queue, Stack.wav")
             :results-file (str root "Media\\Audio\\de-DE\\transcript\\12.01.1 Datenstrukturen, Array, Queue, Stack.srr")}
            {:id "Lambda"
             :name "Der Lambda-Kalkül (720)"
             :video-file (str root "Media\\Video\\Der Lambda-Kalkül (720).mp4")
             :audio-file (str root "Media\\Audio\\de-DE\\Der Lambda-Kalkül (720).wav")
             :results-file (str root "Media\\Audio\\de-DE\\transcript\\Der Lambda-Kalkül (720).srr")}]
   :categories []})

(deftest test-resources
  (dt/prepare-output-dir job-descr))

(deftest test-analyze
  (-> job-descr
      dt/load-speech-recognition-results
      dt/analyze-speech-recognition-results))

(deftest test-index
  (let [job (-> job-descr
                dt/load-speech-recognition-results
                dt/analyze-speech-recognition-results)
        tasks {:prep dt/prepare-output-dir
               :main-page dt/create-index-page
               :categories-page dt/create-categories-page
               :videos-page dt/create-videos-page}]
    (dt/long-task
     "Index run"
     (doseq
       [task (vals tasks)]
       (task job)))))

(deftest test-complete
  (let [job (-> job-descr
                dt/load-speech-recognition-results
                dt/analyze-speech-recognition-results)
        tasks {:prep dt/prepare-output-dir
               :main-page dt/create-index-page
               :categories-page dt/create-categories-page
               :categories dt/create-category-pages
               :videos-page dt/create-videos-page
               :videos dt/create-video-pages}]
    (dt/long-task
     "Complete run"
     (doseq
       [task (vals tasks)]
       (task job)))))

