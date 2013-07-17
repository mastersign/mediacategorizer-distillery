(ns distillery.playground
  (:require [clojure.string :as string])
  (:require [clojure.pprint :refer (pprint)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :refer :all]))

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

(def path (nth paths 14))

;; Load the recognition results
(def results (load-data path))

;; The appearance of the most frequent word at all
(def ma (-> results most-frequent-word (get 1)))

;; Filter predicates:
;; Words:        not-short? noun? not-in-blacklist? no-punctuation?
;; Words groups: min-confidence?

;; Group the words and compute stastics
(def relevant-words (grouped-words results [not-short? noun? no-punctuation?] [min-confidence?]))

;; Frequent words which may be recognized fasley
(def correction-list (correction-candidates relevant-words))

;; Map of words with lexical form as key
(def index (apply hash-map (apply concat (map #(vector (:lexical-form %) %) relevant-words))))

;; Words ordered by relevance
(def hitlist
  (->> relevant-words
       (sort-by :scs)
       (reverse)))

;; The most relevant word
(def w (first hitlist))

;; Print head of hitlist
(print-word-list (take 10 hitlist))

;; Print tail of hitlist
(print-word-list (take-last 10 hitlist))

;; Print correction list
(print-word-list correction-list)
