(ns notepad
  (:require [clojure.test :refer :all])
  (:require [clojure.pprint :refer [pprint]])
  ;(:require [net.cgrand.enlive-html :as eh])
  ;(:require [mastersign.wordcloud-test])
  ;(:require [distillery.data :refer :all])
  ;(:require [distillery.tasks :refer :all])
  (:require [distillery.core-test])
  )

;(run-tests 'mastersign.wordcloud-test)

;(distillery.core-test/test-playground)

;(distillery.core-test/test-resources)

;(distillery.core-test/test-analyze)

;(distillery.core-test/test-categories)

;(distillery.core-test/test-index)

;(distillery.core-test/test-complete)

;(distillery.core-test/test-show-main-page)

; -----------------

;(let [dldir "C:\\Temp\\wiki\\"] (download-resources dldir) (adjust-job-for-offline dldir))
;(adjust-job-for-offline "C:\\Temp\\wiki\\")

(defn download-resources
  "Downloads all resources into dldir."
  [dldir]
  (let [ress (mapcat (fn [c] (map #(vector (:type %) (:url %))
                                  (:resources c)))
                     (:categories distillery.core-test/job-descr))]
    (doseq [[typ url] ress]
      (let [url* (java.net.URL. url)
            p (.getPath url*)]
        (when-not (= (.getProtocol url*) "file")
          (let [n (.substring p (inc (.lastIndexOf p "/")))
                f (str dldir n ".offline")]
            (spit f (slurp (if (= :wikipedia typ)
                             (str url "?action=render")
                             url)))))))))

(defn adjust-job-for-offline
  [dldir]
  (intern 'distillery.core-test 'job-descr
    (update-in
     distillery.core-test/job-descr [:categories]
     (fn [cats]
       (vec (map
             (fn [cat]
               (update-in
                cat [:resources]
                (fn [ress]
                  (vec (map
                        (fn [r]
                          (let [url (:url r)
                                p (.getPath (java.net.URL. url))
                                n (.substring p (inc (.lastIndexOf p "/")))
                                f (str dldir n ".offline")]
                            (assoc r :url (str "file:///" f))))
                        ress)))))
             cats))))))

