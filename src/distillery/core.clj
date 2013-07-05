(ns distillery.core
  (:import [java.lang.Character])
  (:import [java.io.PushbackReader])
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.edn :as edn])
  (:require [clojure.pprint :refer (pprint)]))


(defn load-data
  "Loads the content of a file as EDN formatted data structure."
  [path]
  (with-open
    [r (java.io.PushbackReader. (reader path))]
    (edn/read r)))

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

(defn map-group-items [f [k xs]] [k (map f xs)])

(defn map-pair-value [f [k v]] [k (f v)])

(defn sum [xs] (apply + xs))

(defn squared-sum [xs] (apply + (map #(* % %) xs)))


;----------------------------

;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\11.01 Theoretische, technische, praktische, angewandte Informatik.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\12.01.1 Datenstrukturen, Array, Queue, Stack.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\12.01.2 Baum als Datenstruktur.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Algorithmen und Datenstrukturen 001.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Algorithmen und Datenstrukturen 002.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Algorithmen und Datenstrukturen 003.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Algorithmen und Datenstrukturen 004.clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Bernd Senf 3. Bankgeheimnis Geldschöpfung - Monetative als Lösung (720).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720).clj")
(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Der Lambda-Kalkül (720).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Einseitiger(rechtsseitiger) Hypothesentest_mit Ablesen aus der Tabelle, Stochastik, Nachhilfe online (720).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\IS-Kurve im Vier-Quadrantenschema Die Herleitung (720).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Gauß-Algorithmus_Lineares Gleichungssystem lösen (einfach_schnell erklärt), Nachhilfe online (720).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Makroökonomie online lernen - VWL Tutorium (360).clj")
;(def path "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Parabeln_Quadratische Funktion_en Übersicht (Scheitelpunkt,Stauchung,Streckung,etc.) (720).clj")

(def results (load-data path)) ;; Load the recgnition results
(def phrases (map best-alternate results)) ;; Extract the best phrases from the alternates
;(def phrases (mapcat :alternates results)) ;; Take all phrases from the results
(def words (mapcat :words phrases)) ;; Extract the words (text, confidence)
(def nouns (filter noun? words)) ;; Only take words with first character upper case

(def grouped-nouns
  (let
    [groups (group-by :text nouns) ;; Group words by text
     confidences (map (partial map-group-items :confidence) groups) ;; Reject additional text per confidence
     squaresums (map (partial map-pair-value squared-sum) confidences) ;; Build square sums of confidence per word
     ordered (reverse (sort-by #(get % 1) squaresums))] ;; Sort the words by sum of squared confidence
    ordered))

(pprint (take 20 grouped-nouns)) ;; Print the head of the resulting list
