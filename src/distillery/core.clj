(ns distillery.core
  (:require [clojure.string :as string])
  (:require [clojure.pprint :refer (pprint)])
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.data :refer :all])
  (:require [distillery.processing :refer :all]))

(def paths
  (map #(str "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\" % ".clj")
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

(def path (nth paths 3))

(def blacklist
  (set (take 3000 (load-list (resource "top10000de.txt")))))

(defn not-in-bl?
  [word]
  (not (contains? blacklist word)))

;; Load the recognition results
(def results (load-data path))

;; Group the words and compute stastics
(def word-groups (grouped-words results [not-in-bl? not-short?]))

;; Map of words with lexical form as key
(def index (apply hash-map (apply concat (map #(vector (:lexical-form %) %) word-groups))))

;; Words ordered by relevance
(def hitlist
  (->> word-groups
       (sort-by :squared-sum)
       (reverse)))

(def w (first hitlist))

;; Print head of hitlist
(println (string/join "\n" (map format-word-stat (take 10 hitlist))))
;; Print tail of hitlist
(println (string/join "\n" (map format-word-stat (take-last 10 hitlist))))

