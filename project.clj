(defproject distillery "0.5.0"
  :description "A Clojure application to filter the most relevant words from speech recognition results."
  :url "http://mastersign.github.io/mediacategorizer/"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [enlive "1.1.1"]]
  :global-vars {*warn-on-reflection* false}
  :profiles {:uberjar {:main distillery.core
                       :aot [distillery.core]}})
