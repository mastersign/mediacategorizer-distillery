(ns distillery.core-test
  (:require [clojure.test :refer :all])
  (:require [clojure.string :as string])
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :refer :all])
  (:require [distillery.tasks :as dt])
  (:require [distillery.core :refer :all]))

(def root "D:\\Daten\\FH\\OLL\\") ;; FHB
;(def root "D:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; HOME
;(def root "C:\\Repository\\Projekte\\Arbeit\\FHB\\OLL\\") ;; NB

(def paths
  (map #(str "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\" % ".srr")
       ["11.01 Theoretische, technische, praktische, angewandte Informatik"
        "12.01.1 Datenstrukturen, Array, Queue, Stack"
        "12.01.2 Baum als Datenstruktur"
        "Algorithmen und Datenstrukturen 001"
        "Algorithmen und Datenstrukturen 002"
        "Algorithmen und Datenstrukturen 003"
        "Algorithmen und Datenstrukturen 004"
        "Bernd Senf 3. Bankgeheimnis Geldschöpfung - Monetative als Lösung (720)"
        "Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720)"
        "Der Lambda-Kalkül (720)"
        "Einseitiger(rechtsseitiger) Hypothesentest_mit Ablesen aus der Tabelle, Stochastik, Nachhilfe online (720)"
        "IS-Kurve im Vier-Quadrantenschema Die Herleitung (720)"
        "Gauß-Algorithmus_Lineares Gleichungssystem lösen (einfach_schnell erklärt), Nachhilfe online (720)"
        "Makroökonomie online lernen - VWL Tutorium (360)"
        "Parabeln_Quadratische Funktion_en Übersicht (Scheitelpunkt,Stauchung,Streckung,etc.) (720)"]))

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

(deftest test-playground
  (let [path (nth paths 14)

        ;; Load the recognition results
        results (load-data path)

        ;; The appearance of the most frequent word at all
        ma (-> results most-frequent-word (get 1))

        ;; Filter predicates:
        ;; Words:        not-short? noun? not-in-blacklist? no-punctuation?
        ;; Words groups: min-confidence?

        ;; Group the words and compute stastics
        relevant-words (grouped-words results [not-short? noun? no-punctuation?] [min-confidence?])

        ;; Frequent words which may be recognized fasley
        correction-list (correction-candidates relevant-words)

        ;; Map of words with lexical form as key
        index (apply hash-map (apply concat (map #(vector (:lexical-form %) %) relevant-words)))

        ;; Words ordered by relevance
        hitlist
        (->> relevant-words
             (sort-by :scs)
             (reverse))

        ;; The most relevant word
        w (first hitlist)]

    ;; Print head of hitlist
    (print-word-list (take 10 hitlist))

    ;; Print tail of hitlist
    (print-word-list (take-last 10 hitlist))

    ;; Print correction list
    (print-word-list correction-list)
    ))

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


