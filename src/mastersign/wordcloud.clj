(ns mastersign.wordcloud
  (:import [java.lang Math])
  (:import [java.awt Graphics Graphics2D Color RenderingHints Font])
  (:import [java.awt.geom Area
            Rectangle2D Rectangle2D$Float
            Point2D Point2D$Float
            Ellipse2D Ellipse2D$Float])
  (:require [clojure.pprint :refer (pprint)])
  (:require [mastersign.geom :refer :all])
  (:require [mastersign.drawing :refer :all])
  (:require [mastersign.imaging :refer :all]))

(def antialias-mode RenderingHints/VALUE_ANTIALIAS_ON)
(def text-antialias-mode RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
(def default-color Color/BLACK)
(def font-family "Calibri")
(def font-style Font/BOLD)
(def base-font (Font. font-family font-style (float 100)))

(def default-args
  {:width 400
   :height 200
   :precision 0.66
   :font base-font
   :min-font-size 14
   :max-font-size 60
   :max-test-radius 250
   :padding 4
   })

(defn- get-rect
  [g font x]
  (string-centered-rect g font (first x)))

(defn- calc-font-size
  [{:keys [min-font-size max-font-size]} v]
  (float (+ min-font-size (* (- max-font-size min-font-size) v v))))

(defn- get-font
  [{:keys [font min-font-size max-font-size] :as args} x]
  (.deriveFont font (calc-font-size args (second x))))

(defn- build-word-info
  [g args w]
  (let [text (first w)
        font* (get-font args w)
        rect (get-rect g font* w)]
      {:text text :font font* :rect rect}))

(defn- build-word-infos
  [word-stats & args]
  (let [args (merge default-args args)
        img (image 1 1)
        graphics (.createGraphics img)
        word-stats (reverse (sort-by second word-stats))
        word-infos (doall (map (partial build-word-info graphics args) word-stats))]
    (.dispose graphics)
    word-infos))

(defn- setup
  [g]
  (doto g
    (.setRenderingHint RenderingHints/KEY_ANTIALIASING antialias-mode)
    (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING text-antialias-mode)))

(defn- lazy-numbers
   ([] (lazy-numbers 0))
   ([n] (lazy-seq (cons n (lazy-numbers (inc n))))))

(defn- test-ring
  [r precision]
  (let [n (int (+ 4 (* precision precision (- (* Math/PI r) 4))))
        step (/ (* 2 Math/PI) n)]
    (map (partial polar-point r) (range 0.0 (* 2 Math/PI) step))))

(defn- test-sequence
  [{:keys [precision max-font-size max-test-radius] :as args}]
  (let [imprecision (* (- 1.0 precision) (- 1.0 precision))
        radii (take-while
                  #(< % max-test-radius)
                  (map #(* (+ 1 (* imprecision max-font-size)) %) (lazy-numbers 1)))
        rings (map #(test-ring % precision) radii)]
    (cons (point) (apply concat rings))))

(defn- check-position
  [test-area boundaries {:keys [rect]} p]
  (let [rect* (translate-rect rect p)]
  (and
   (.contains boundaries rect*)
   (not (.intersects test-area rect*)))))

(defn- find-position
  [test-area boundaries point-sequence {:keys [rect] :as word-info}]
  (first (filter #(check-position test-area boundaries word-info %)
              point-sequence)))

(defn generate-cloud
  [word-infos & args]
  (let [args (merge default-args args)
        {:keys [width height padding]} args
        boundaries (area (rectangle (- (/ width 2)) (- (/ height 2)) width height))
        *test-area* (area)
        point-sequence (test-sequence args)
        finder (fn [{:keys [text rect] :as word-info}]
                 ;(println (str "Placing " text " ..."))
                 (let [hit (find-position *test-area* boundaries point-sequence word-info)]
                   (if (nil? hit)
                     nil
                     (do
                       (.add *test-area* (area (grow-rect (translate-rect rect hit) padding)))
                       ;(println (str "Found place at " (.x hit) ", " (.y hit)))
                       (assoc word-info :position hit)))))]
    (doall (map finder word-infos))))

(defn- cloud-painter
  [word-infos g w h]
  (doto g
    setup)

  (doseq [{:keys [text font position]} (filter #(not (nil? %)) word-infos)]
    (draw-string-centered g text
                          (+ (/ w 2.0) (.x position))
                          (+ (/ h 2.0) (.y position)) :font font)))

(defn paint-cloud
  [word-infos & args]
  (let [args (merge default-args)]
    (create-image (:width args) (:height args)
                  (partial cloud-painter word-infos))))

; ########################

(defn test-painter
  [g w h]
  (let [font (.deriveFont base-font (float 30.0))
        text "MjgqAIkqQ$§|"
        r1 (string-centered-rect g font text 100 30 )
        r2 (string-centered-rect g font text 100 60)
        a1 (Area. r1)
        c2 (rect-center r2)]
    (doto g
      setup
      ;(draw-rect (rectangle 0 0 (dec w) (dec h)))
      ;(fill-rect r1)
      ;(draw-rect r1)
      ;(draw-string-centered text 100 30 :font font)
      ;(draw-string-centered text 100 60 :font font)
      ;(draw-dot c2)

      ;(draw-dot (point 200 100))
      ;(draw-dot-seq 200 100 (test-ring 50 0.2))
      ;(draw-dot-seq 200 100 (test-ring 10 0.2))

      (draw-dot-seq 200 100 (test-sequence default-args))
      )))

(let [img (create-image 400 200 test-painter)]
  (save-image img "D:/Temp/test.png"))

; ########################

(def words
  ["Alpha" "Beta" "Charlie" "Delta" "Echo" "Foxtrott"
   "Golf" "Hotel" "India" "Juliette" "Kilo" "Lima"
   "Mike" "November" "Oscar" "Papa" "Quebec" "Romeo"
   "Sierra" "Tango" "Uniform" "Victor" "Whiskey" "X-Ray"
   "Yankee" "Zulu"])

(def stats (vec (map (fn [x] [x (/ (rand-int 101) 100.0)]) words)))

(def test-infos (build-word-infos stats))

(def test-cloud (generate-cloud test-infos))

(save-image (paint-cloud test-cloud) "D:/Temp/cloud.png")
