;; # Blacklist
;; This namespace contains functions for using the blacklist
;; to filter words.

(ns distillery.blacklist
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.processing :refer (word-text)]))


;; This private var holds a reference to a set with all words of the blacklist.
;; The words are loaded automatically into the var during application startup.
;; The resource file with the words is specified by the application configuration.
(def ^:private blacklist
  (->> (cfg/value :blacklist-resource)
       resource
       load-list
       (take (cfg/value :blacklist-max-size))
       (map #(.toLowerCase ^String %))
       set))

(defn not-in-blacklist?
  "This function is a predicate, checking if `word` is not in the blacklist.
  The word can be a string or a map with a the key `:lexical-form.`"
  [word]
  (let [text (.toLowerCase ^String (word-text word))]
    (not (contains? blacklist text))))



