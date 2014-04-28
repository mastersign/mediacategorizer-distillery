(ns distillery.blacklist
  (:require [clojure.java.io :refer (resource)])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.processing :refer (word-text)]))

(def ^:private blacklist
  (->> (cfg/value :blacklist-resource)
       resource
       load-list
       (take (cfg/value :blacklist-max-size))
       (map #(.toLowerCase ^String %))
       set))

(defn not-in-blacklist?
  [word]
  (let [text (.toLowerCase ^String (word-text word))]
    (not (contains? blacklist text))))


