(ns mastersign.wordcloud-test
  (:import [java.awt Color RenderingHints])
  (:require [mastersign.imaging :refer :all])
  (:require [mastersign.geom :refer :all])
  (:require [mastersign.drawing :refer :all])
  (:require [mastersign.wordcloud :refer :all]))

; ########################

(defn test-painter
  [g w h]
  (let [font (:font default-args)
        text "MjgqAIkqQ$§|"
        p1 (point 100 30)
        p2 (point 100 60)
        pc (point (/ (:width default-args) 2.0) (/ (:height default-args) 2.0))
        r1 (string-centered-bounds g font text p1)
        r2 (string-centered-bounds g font text p2)
        a1 (area r1)
        c1 (rect-center r1)
        c2 (rect-center r2)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
      (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)

      ;(draw-dots (test-ring 50 0.2) pc)
      ;(draw-dots (test-ring 10 0.2) pc)
      (draw-dots (#'mastersign.wordcloud/test-sequence default-args {:text "Alpha"}) pc)

      (draw-rect (rectangle 0 0 (dec w) (dec h)))
      (fill-rect r1)
      (draw-rect r1)
      ;(fill-rect r2)
      ;(draw-rect r2)
      (draw-string-centered text p1 :font font)
      (draw-string-centered text p2 :font font)
      ;(draw-dot c1)
      (draw-dot c2 :color Color/MAGENTA)

      (draw-dot pc)
      )))

(let [img (create-image (:width default-args) (:height default-args) test-painter)]
  (save-image img "D:/Temp/test.png"))

; ########################

(def words
  ["Alpha" "Beta" "Charlie" "Delta" "Echo" "Foxtrott"
   "Golf" "Hotel" "India" "Juliette" "Kilo" "Lima"
   "Mike" "November" "Oscar" "Papa" "Quebec" "Romeo"
   "Sierra" "Tango" "Uniform" "Victor" "Whiskey" "X-Ray"
   "Yankee" "Zulu"
   "Adventskalender" "Mittelpunktbestimmung" "Zentralfeuerwaffe"])

(def stats (vec (map-indexed (fn [id x] [id x (/ (rand-int 101) 100.0) (rand)]) words)))

(def test-infos (time (build-word-infos stats)))

(def test-cloud (time (generate-cloud test-infos)))

(save-image (paint-cloud test-cloud) "D:/Temp/cloud.png")
