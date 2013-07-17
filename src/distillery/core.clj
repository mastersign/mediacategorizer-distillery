(ns distillery.core
  (:require [distillery.jobs :as jobs]))

(def root "D:\\Daten\\FH\\OLL\\") ;; FHB
(def root "D:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; HOME

(def base-job-descr
  {:job-name "Testlauf"
   :output-dir (str root "Output")})

(def job-descr (assoc base-job-descr
   :video {:id "12-01-1"
           :name "12.01.1 Datenstrukturen, Array, Queue, Stack"
           :video-file (str root "Media\\Video\\12.01.1 Datenstrukturen, Array, Queue, Stack.mp4")
           :audio-file (str root "Media\\Audio\\de-DE\\12.01.1 Datenstrukturen, Array, Queue, Stack.wav")
           :results-file (str root "Media\\Audio\\de-DE\\transcript\\12.01.1 Datenstrukturen, Array, Queue, Stack.srr")}))

(jobs/prepare-output-dir job-descr)
(jobs/create-video-page job-descr)
