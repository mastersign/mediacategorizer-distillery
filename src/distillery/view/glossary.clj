(ns distillery.view.glossary
  (:require [distillery.view.html :refer :all]))

(defn- render-glossary-word
  "Creates the HTML for a glossary entry."
  [[lexical {:keys [id occurrences] :as word}]]
  (list-item (jslink (str "word('" id "')") lexical)))

(defn- glossary-partition-id
  "Creates an identifier for a glossary partition by its letter."
  [letter]
  (if (= \? letter) "SYM" (str letter)))

(defn- render-glossary-navigation
  "Creates the HTML for the navigation bar of a glossary."
  [pindex]
  (let [letters (map char (concat (range 65 91) [63]))]
    (div "glossary-nav"
      (map (fn [l]
             (if (contains? pindex l)
               (jslink (str "glossary('" (glossary-partition-id l) "')") (str l "  "))
               (str l "  ")))
           letters))))

(defn- render-glossary-partition
  "Creates the HTML for a partion of a glossary."
  [[letter index-part]]
  {:tag :div
   :attrs {:id (str "glossary-part-" (glossary-partition-id letter)) :class "glossary-part"}
   :content [(ulist "glossary" (map render-glossary-word index-part))]})

(defn render-glossary
  "Create the HTML for a glossary."
  [pindex]
  (vec (cons
        (render-glossary-navigation pindex)
        (map render-glossary-partition pindex))))

