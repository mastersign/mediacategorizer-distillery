(ns distillery.txtresult
  (:import [java.util Locale])
  (:require [clojure.string :as str])
  (:require [clojure.java.io :refer (writer)])
  (:require [mastersign.files :refer :all]))

(defn save-result
  [medium file]
  (with-open [w (writer file)]
    (doseq [result (:results medium)]
        (.write w (str/join " " (map :text (:words result))))
        (.write w (format "%n")))))
