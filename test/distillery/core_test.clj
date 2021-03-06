(ns distillery.core-test
  (:require [clojure.test :refer :all])
  (:require [clojure.string :as string])
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :refer :all])
  (:require [distillery.trace :refer :all])
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

(def videos [
             ["11-01" "11.01 Theoretische, technische, praktische, angewandte Informatik"]
             ["12-01-1" "12.01.1 Datenstrukturen, Array, Queue, Stack"]
             ["12-01-2" "12.01.2 Baum als Datenstruktur"]
             ["Algo1" "Algorithmen und Datenstrukturen 001"]
             ["Algo2" "Algorithmen und Datenstrukturen 002"]
             ["Algo3" "Algorithmen und Datenstrukturen 003"]
             ["Algo4" "Algorithmen und Datenstrukturen 004"]
             ["Bernoulli" "Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720)"]
             ["Hypothesentest" "Einseitiger(rechtsseitiger) Hypothesentest_mit Ablesen aus der Tabelle, Stochastik, Nachhilfe online (720)"]
             ["Parabeln" "Parabeln_Quadratische Funktion_en Übersicht (Scheitelpunkt,Stauchung,Streckung,etc.) (720)"]
             ["Gauss" "Gauß-Algorithmus_Lineares Gleichungssystem lösen (einfach_schnell erklärt), Nachhilfe online (720)"]
             ["Lambda" "Der Lambda-Kalkül (720)"]
             ["IS-Kurve" "IS-Kurve im Vier-Quadrantenschema Die Herleitung (720)"]
             ["Makrooekonomie" "Makroökonomie online lernen - VWL Tutorium (360)"]
             ["Bankgeheimnis" "Bernd Senf 3. Bankgeheimnis Geldschöpfung - Monetative als Lösung (720)"]
             ])

(def job-descr
  {:job-name "Testlauf"
   :job-description "Ein Testprojekt für Testzwecke mit Testvideos und Testkategorien. Wird zum Testen verwendet."
   :output-dir (str root "Output")
   :result-file ""
   :configuration {:parallel-proc: true
                   :visualize-result: false
                   :skip-media-copy false
                   :skip-wordclouds false
                   :skip-word-includes false
                   :skip-match-includes false
                   :main-cloud {:precision :medium
                                :width 540
                                :height 350}
                   :video-cloud {:precision :medium}
                   :category-cloud {:precision :medium}}
   :videos (->> videos
                (map (fn
                       [[id name]]
                       {:id id
                        :name name
                        :video-file (str root "Media\\Video\\" name ".mp4")
                        :audio-file (str root "Media\\Audio\\de-DE\\" name ".wav")
                        :results-file (str root "Media\\Audio\\de-DE\\transcript\\" name ".srr")}))
                (vec))
   :categories [ {:id "Info"
                  :name "Informatik"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Informatik"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Theoretische_Informatik"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Praktische_Informatik"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Technische_Informatik"}]}
                 {:id "Queue"
                  :name "Datenstrukturen - Warteschlange"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Warteschlange_%28Datenstruktur%29"}]}
                 {:id "Stack"
                  :name "Datenstrukturen - Stapel"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Stapelspeicher"}]}
                 {:id "Tree"
                  :name "Datenstrukturen - Baum"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Baum_%28Graphentheorie%29"}]}

                 {:id "Math"
                  :name "Mathematik"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Mathematik"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Rechnen"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Zahl"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Arithmetik"}]}
                 {:id "Oeko"
                  :name "Wirtschaft"
                  :resources [{:type :wikipedia, :url "http://de.wikipedia.org/wiki/Wirtschaft"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Wirtschaftswissenschaft"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Bank"}
                              {:type :wikipedia, :url "http://de.wikipedia.org/wiki/Geld"}]}]})

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

(defn test-categories []
  (-> job-descr
      dt/load-categories
      dt/analyze-categories))

(defn test-resources []
  (dt/prepare-output-dir job-descr))

(defn test-analyze []
  (-> job-descr
      dt/load-speech-recognition-results
      dt/analyze-speech-recognition-results
      dt/load-categories
      dt/analyze-categories
      dt/match-videos
      dt/lookup-categories-matches
      dt/matching-stats))

(defn test-index []
  (let [job (-> job-descr
                dt/load-speech-recognition-results
                dt/analyze-speech-recognition-results
                dt/load-categories
                dt/analyze-categories
                dt/match-videos
                dt/lookup-categories-matches
                dt/matching-stats)]
    (trace-block
     "Index run"
     (dt/prepare-output-dir job)
     (doall ((dt/map-fn) #(% job)
             [dt/create-index-page
              dt/create-categories-page
              dt/create-videos-page]))
     (dt/show-main-page job)))
  nil)

(defn test-complete []
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
  nil)

(defn test-show-main-page []
  (dt/show-main-page job-descr))
