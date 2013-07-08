(ns distillery.core
  (:import [java.lang.Character])
  (:import [java.io.PushbackReader])
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.edn :as edn])
  (:require [clojure.pprint :refer (pprint)]))

;----- Helper Functions

(defn load-data
  "Loads the content of a file as EDN formatted data structure."
  [path]
  (with-open
    [r (java.io.PushbackReader. (reader path))]
    (edn/read r)))

(defn map-group-items
  "Applies a function to the items of the group collection."
  [f [k xs]] [k (map f xs)])

(defn map-pair-value
  "Applies a function to the value of a key-value-pair."
  [f [k v]] [k (f v)])

(defn multi-filter
  "Applies a number of predicates to a value and returns true if all predicates are true."
  [predicates x]
  (if (empty? predicates)
    true
    (every? true? ((apply juxt predicates) x))))

(defn sum
  "Computes the sum of a sequence."
  [xs] (apply + xs))

(defn squared-sum
  "Computes the sum of the squared items of a sequence."
  [xs] (apply + (map #(* % %) xs)))

;----- Domain Logic

(defn best-alternate
  "Return the best alternate phrase of a recgnition result."
  [result]
  (apply max-key :confidence (:alternates result)))

(defn noun?
  "Checks wheter the given word is a noun.
  The given word can be the word map from a recognition result with key :text
  or can be a plain string."
  [word]
  (let [word (if (map? word) (:text word) word)]
    (if (or (nil? word) (< (count word) 3))
      false
      (Character/isUpperCase (first word)))))

(defn words
  "Returns words from a result sequence."
  [results & {:keys [filters best-phrases]}]
  (let [predicate    (partial multi-filter (vec filters))
        phrase-src   (if best-phrases
                       (partial map best-alternate)
                       (partial mapcat :alternates))]
    (->> results
         (phrase-src)
         (mapcat :words)
         (filter predicate))))

(defn grouped-words
  "Returns ordered statistics over a sequence of phrases."
  [results filters columns]
  (let
    [source (words results :best-phrases true :filters filters)
     groups (group-by (apply juxt columns) source) ;; Group words by text
     confidences (map (partial map-group-items :confidence) groups) ;; Reject additional text per confidence
     squaresums (map (partial map-pair-value squared-sum) confidences)] ;; Build square sums of confidence per word
    (reverse (sort-by #(get % 1) squaresums)))) ;; Sort the words by sum of squared confidence

;----- Interactive Demo

(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\11.01 Theoretische, technische, praktische, angewandte Informatik.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\12.01.1 Datenstrukturen, Array, Queue, Stack.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\12.01.2 Baum als Datenstruktur.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Algorithmen und Datenstrukturen 001.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Algorithmen und Datenstrukturen 002.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Algorithmen und Datenstrukturen 003.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Algorithmen und Datenstrukturen 004.clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Bernd Senf 3. Bankgeheimnis Geldschöpfung - Monetative als Lösung (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Der Lambda-Kalkül (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Einseitiger(rechtsseitiger) Hypothesentest_mit Ablesen aus der Tabelle, Stochastik, Nachhilfe online (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\IS-Kurve im Vier-Quadrantenschema Die Herleitung (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Gauß-Algorithmus_Lineares Gleichungssystem lösen (einfach_schnell erklärt), Nachhilfe online (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Makroökonomie online lernen - VWL Tutorium (360).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Parabeln_Quadratische Funktion_en Übersicht (Scheitelpunkt,Stauchung,Streckung,etc.) (720).clj")

;; Load the recgnition results
(def results (load-data path))
;; group the words and compute stastics
(def grouped-nouns (grouped-words results [noun?] [:lexical-form :pronunciation]))
;; Print the head of the resulting list
(pprint (take 20 grouped-nouns))
