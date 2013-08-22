(ns distillery.core
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.tasks :as dt]))

;(def root "D:\\Daten\\FH\\OLL\\") ;; FHB
;(def root "D:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; HOME
(def root "C:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; NB

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
             :results-file (str root "Media\\Audio\\de-DE\\transcript\\Der Lambda-Kalkül (720).srr")}]})

(def job (-> job-descr
             dt/load-speech-recognition-results
             dt/analyze-speech-recognition-results))

(def tasks {:prep dt/prepare-output-dir
            :main-page dt/create-index-page
            :categories-page dt/create-categories-page
            :videos-page dt/create-videos-page
            :glossary-page dt/create-glossary-page
            :videos dt/create-video-pages})

((:prep tasks) job)

(dt/long-task "Complete run"
  (doseq
    [task (rest (vals tasks))]
    (task job)))
