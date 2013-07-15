(ns distillery.jobs
  (:require [clojure.string :as string])
  (:require [distillery.config :as cfg])
  (:require [distillery.data :refer :all])
  (:require [distillery.files :refer :all])
  (:require [distillery.blacklist :refer :all])
  (:require [distillery.processing :refer :all])
  (:require [distillery.view.html :refer (save-page)])
  (:require [distillery.view.dependencies :refer (save-dependencies)])
  (:require [distillery.view.base :refer (render)])
  (:require [distillery.view.simple-transcript :as v-simple-t]))

(defn transcript-page
  [{:keys [output-dir job-name video-file results-file] :as args}]
  (let [results (load-data results-file)
        phrases (map best-alternate-phrase results)
        target-file (combine-path output-dir (str job-name ".transcript.htm"))
        video-file-name (str job-name ".mp4")
        args (assoc args :video-file-name video-file-name)]
    (save-dependencies output-dir)
    (copy-file (get-path video-file) (get-path output-dir video-file-name))
    (->> args
        v-simple-t/render-video
        (apply render)
        (save-page target-file))))

(def job-descr
  { :job-name "Binomialverteilung"
    :video-file "D:\\Daten\\FH\\OLL\\Media\\Video\\Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720).mp4"
    :audio-file "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720).wav"
    :results-file "D:\\Daten\\FH\\OLL\\Media\\Audio\\de-DE\\transcript\\Binomialverteilung_Formel von Bernoulli, Stochastik, Nachhilfe online, Hilfe in Mathe (720).clj"
    :output-dir "S:\\Temp\\distillery_out"
  })

(transcript-page job-descr)
