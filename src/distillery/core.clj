(ns distillery.core
  (:require [distillery.jobs :as jobs]))

(def root "D:\\Daten\\FH\\OLL\\") ;; FHB
;(def root "D:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; HOME
;(def root "C:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; NB

(def job-descr
  {:job-name "Testlauf"
   :job-description "Ein Testprojekt für Testzwecke mit Testvideos und Testkategorien. Wird zum Testen verwendet."
   :output-dir (str root "Output")
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


(def base-jobs [jobs/prepare-output-dir
                jobs/create-index-page
                jobs/create-categories-page
                jobs/create-videos-page
                jobs/create-glossary-page
                jobs/create-video-pages])

(doseq [job base-jobs] (job job-descr))
