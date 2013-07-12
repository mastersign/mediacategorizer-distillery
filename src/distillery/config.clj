(ns distillery.config
  (:require [clojure.java.io :refer (resource)]))


(def best-phrases-only true)

(def blacklist-resource (resource "top10000de.txt"))
(def blacklist-max-size 3000)

(def min-confidence 0.400)
(def good-confidence 0.700)

(def min-relative-appearance (/ 1.0 4.0))