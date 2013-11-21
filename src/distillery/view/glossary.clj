(ns distillery.view.glossary
  (:require [distillery.view.html :refer :all]))

(defn- render-glossary-word
  "Creates the HTML for a glossary entry."
  [{:keys [id lexical-form occurrences] :as word}]
  (list-item (jslink (str "word('" id "')") lexical-form)))

(defn- glossary-partition-id
  "Creates an identifier for a glossary partition by its letter."
  [letter]
  (if (= \? letter) "SYM" (str letter)))

(defn- letter-list
  []
  (map char (concat (range 65 91) [63])))

(defn- render-glossary-navigation
  "Creates the HTML for the navigation bar of a glossary."
  [pindex]
  (let [letters (letter-list)]
    (div "glossary-nav"
      (map (fn [l]
             (if (contains? pindex l)
               (jslink (str "glossary('" (glossary-partition-id l) "')") (str l "  "))
               (str l "  ")))
           letters))))

(defn- first-letter
  [pindex]
  (let [index-letters (map first pindex)]
    (->> (letter-list)
         (map (fn [l] (map #(== % l) index-letters)))
         first)))

(defn- render-glossary-partition
  "Creates the HTML for a partion of a glossary."
  [first-letter [letter index-part]]
  (let [is-first-letter (== letter first-letter)]
    {:tag :div
     :attrs {:id (str "glossary-part-" (glossary-partition-id letter))
             :class "glossary-part"
             :display (if is-first-letter "inherit" "none")}
     :content [(ulist "glossary" (map render-glossary-word (vals index-part)))]}))

(defn render-glossary
  "Create the HTML for a glossary."
  [pindex]
  (let [fl (first-letter pindex)]
    (vec (cons
          (render-glossary-navigation pindex)
          (map (partial render-glossary-partition fl) pindex)))))

