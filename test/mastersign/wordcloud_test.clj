(ns mastersign.wordcloud-test
  (:import [java.awt Color RenderingHints])
  (:require [clojure.test :refer (deftest is)])
  (:require [mastersign.imaging :refer :all])
  (:require [mastersign.geom :refer :all])
  (:require [mastersign.drawing :refer :all])
  (:require [mastersign.wordcloud :as cloud]))

(deftest test-paint-test-sequence
  (let [args cloud/default-args
        painter (fn [g w h]
                  (let [pc (point (/ (:width args) 2.0) (/ (:height args) 2.0))]
                    (doto g
                      (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
                      (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
                      (draw-dots (#'mastersign.wordcloud/test-sequence args {:text "Alpha"}) pc))))
        img (create-image (:width args) (:height args) painter)]
    (show-image img)))

(deftest test-create-cloud
  (let [words ["Alpha" "Beta" "Charlie" "Delta" "Echo" "Foxtrott"
               "Golf" "Hotel" "India" "Juliette" "Kilo" "Lima"
               "Mike" "November" "Oscar" "Papa" "Quebec" "Romeo"
               "Sierra" "Tango" "Uniform" "Victor" "Whiskey" "X-Ray"
               "Yankee" "Zulu"
               "Adventskalender" "Mittelpunktbestimmung" "Zentralfeuerwaffe"]
        stats (vec (map-indexed (fn [id x] [id x (/ (rand-int 101) 100.0) (rand)]) words))
        cloud (time (cloud/create-cloud stats))]
    (show-image (:image cloud))))
