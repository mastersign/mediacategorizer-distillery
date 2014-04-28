(defproject distillery "0.1.0"
  :description "A Clojure application to filter the most relevant words from speech recognition results."
  :url "http://informatik.fh-brandenburg.de/~kiertsch/"
  :license {:name "None"
            :url ""}
  :plugins [[lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.xml "0.0.7"]
                 [enlive "1.1.1"]]
  :global-vars {*warn-on-reflection* true}
  :aot [distillery.core]
  :main distillery.core)

