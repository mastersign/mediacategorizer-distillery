(ns distillery.blacklist
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.processing :refer (word-text)]))

(def ^:private blacklist
  (->> cfg/blacklist-resource
       load-list
       (take cfg/blacklist-max-size)
       set))

(defn not-in-blacklist?
  [word]
  (let [text (word-text word)]
    (not (contains? blacklist text))))

