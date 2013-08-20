(ns mastersign.wordcloud
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

(defn default-color-fn
  [value]
  (let [n (float 0.4)
        v1 (float value)
        v2 (float (- 1.0 value))]
  (Color. v1 n v2)))

(def default-args
  {:width 600
   :height 300
   :precision 0.7
   :font base-font
   :min-font-size 14
   :max-font-size 60
   :max-test-radius 350
   :order-priority 0.7
   :padding 3
   :shape-mode :word-box ; :word-box, :glyph-box
   :allow-rotation true
   :order-mode :value1 ; :id, :text, :value1, :value2
   :color-fn #'default-color-fn
   })

(defn- calc-font-size
  [{:keys [min-font-size max-font-size]} v]
  (float (+ min-font-size (* (- max-font-size min-font-size) v v))))

(defn- get-font
  [{:keys [font] :as args} v]
  (.deriveFont font (calc-font-size args v)))

(defn- get-color
  [{:keys [color-fn]} v]
  (color-fn v))

(defn- build-word-info
  [g args [id text value1 value2]]
  (let [font* (get-font args value1)
        color (get-color args value2)]
    {:text text
     :font font*
     :color color
     :word-bounds (string-centered-bounds g font* text)
     :glyph-bounds (string-centered-glyphbounds g font* text)}))

(defn- build-word-infos
  [word-stats & args]
  (let [args (merge default-args args)
        img (image 1 1)
        graphics (.createGraphics img)
        word-stats (case (:order-mode args)
                       :id (sort-by first word-stats)
                       :text (sort-by second word-stats)
                       :value2 (reverse (sort-by #(nth % 3) word-stats))
                       (reverse (sort-by #(nth % 2) word-stats)))
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

(defn- priority-angle
  [text]
  (let [clean-DE (fn [c] (case c \Ä \A \Ö \O \Ü \U \ß \S c))
        c (clean-DE (first (.toUpperCase text)))
        n (int c)
        s (- (if (or (< n 64) (> n 90)) 64 n) 65)
        a (* (/ Math/PI 13) s)]
    (if (<= a Math/PI) (- Math/PI a) a)))

(defn- ring-step
  [r prec]
  (/ 2.0 (+ 4 (* prec prec (* Math/PI r)))))

(ring-step 5 1.0)

(defn- scale-radius
  [priority r a]
  (let [base (* r (- 1.0 priority))
        a* (* Math/PI (Math/sqrt a))]
    (+ base (* (- r base) (+ (* (Math/cos a*) 0.5) 0.5)))))

(defn- ring-angles
  [precision priority r a]
  (lazy-seq (cons
             a
             (ring-angles precision priority r
                          (+ a (ring-step (scale-radius priority r a) precision))))))

(defn- test-ring
  [r precision pa priority]
  (cons
   (polar-point r pa)
   (apply concat (map
             (fn [a]
               (let [sr (scale-radius priority r a)
                     a* (* Math/PI a)]
                 [(polar-point sr (+ pa a*))
                  (polar-point sr (- pa a*))]))
             (take-while #(< % 1.0) (ring-angles precision priority r 0))))))

(defn- test-sequence
  [{:keys [width height precision max-font-size max-test-radius order-priority] :as args} {:keys [text] :as word-info}]
  (let [imprecision (* (- 1.0 precision) (- 1.0 precision))
        radii (take-while
                  #(< (scale-radius order-priority % 1) max-test-radius)
                  (map #(* (+ 1 (* imprecision max-font-size 0.5)) %) (lazy-numbers 1)))
        pa (if (> order-priority 0.0)
             (priority-angle text)
             (rand (* Math/PI 2)))
        rings (map #(test-ring % precision pa order-priority) radii)
        boundaries (rectangle (- (/ width 2.0)) (- (/ height 2.0)) width height)
        reject-pred (fn [p] (.contains boundaries (.x p) (.y p)))]
    (filter reject-pred (cons (point) (apply concat rings)))))

(defn- rotate-rect
  [r c a]
  (if (= a 0)
    r
    (let [x (.x r)
          y (.y r)
          w (.width r)
          h (.height r)
          cx (.x c)
          cy (.y c)
          dx (- x cx)
          dy (- y cy)]
      (case a
        90 (rectangle (- cx dy h) (+ cy dx) h w)
        180 (rectangle (- cx dx w) (- cy dy h) w h)
        270 (rectangle (+ cx dy) (- cy dx w) h w)))))

(defn- get-word-rects
  [{:keys [shape-mode padding]} word-info pos rotation use-padding]
  (let [rects (case shape-mode
                :word-box [(translate-rect (:word-bounds word-info) pos)]
                :glyph-box (map #(translate-rect % pos) (:glyph-bounds word-info)))
        rects (if use-padding
                (map #(grow-rect % padding) rects)
                rects)
        rects (map #(rotate-rect % pos rotation) rects)]
    rects))

(defn- check-position
  [args test-area boundaries word-info pos rotation]
  (let [rects (get-word-rects args word-info pos rotation false)]
    (if (and
         (every? #(.contains boundaries %) rects)
         (every? #(not (.intersects test-area %)) rects))
      [pos rotation]
      nil)))

(defn- find-position
  [{:keys [allow-rotation] :as args} test-area boundaries point-sequence {:keys [word-bounds] :as word-info}]
  (let [cp (partial check-position args test-area boundaries word-info)
        cp (if allow-rotation
               (fn [pos] (or (cp pos 0) (cp pos 90) (cp pos 270)))
               (fn [pos] (cp pos 0)))]
    (first (filter #(not (nil? %)) (map cp point-sequence)))))

(defn- add-word-to-area!
  [args *area* word-info pos rotation]
  (let [rects (get-word-rects args word-info pos rotation true)]
    (.add *area* (apply area rects))))

(defn generate-cloud
  [word-infos & args]
  (let [args (merge default-args args)
        {:keys [width height padding]} args
        boundaries (area (rectangle (- (/ width 2)) (- (/ height 2)) width height))
        *test-area* (area)
        finder (fn [{:keys [text word-bounds] :as word-info}]
                 ;(println (str "Placing " text " ..."))
                 (let [point-sequence (test-sequence args word-info)
                       [hit rotation] (find-position args *test-area* boundaries point-sequence word-info)]
                   (if (nil? hit)
                     nil
                     (do
                       (add-word-to-area! args *test-area* word-info hit rotation)
                       ;(println (str "Found place at " (.x hit) ", " (.y hit)))
                       (assoc word-info :position hit :rotation rotation)))))]
    {:word-infos (doall (map finder word-infos))
     :test-area *test-area*}))

(defn- cloud-painter
  [{:keys [word-infos test-area]} g w h]
  (let [c (point (/ w 2.0) (/ h 2.0))
        bg (translate-area test-area c)]
    (doto g
      setup
      ;(draw-rect (rectangle 0 0 (dec w) (dec h)))
      ;(fill-shape bg)
      ;(draw-shape bg)
      )
    (doseq [{:keys [text font color position rotation]} (filter #(not (nil? %)) word-infos)]
      (draw-string-centered g text (translate-point c position) :font font :color color :rotation rotation))))

(defn paint-cloud
  [cloud & args]
  (let [args (merge default-args)]
    (create-image (:width args) (:height args)
                  (partial cloud-painter cloud))))

; ########################

(defn test-painter
  [g w h]
  (let [font (.deriveFont base-font (float 30.0))
        text "MjgqAIkqQ$§|"
        p1 (point 100 30)
        p2 (point 100 60)
        pc (point (/ (:width default-args) 2.0) (/ (:height default-args) 2.0))
        r1 (string-centered-bounds g font text p1)
        r2 (string-centered-bounds g font text p2)
        a1 (Area. r1)
        c1 (rect-center r1)
        c2 (rect-center r2)]
    (doto g
      setup
      ;(draw-dots (test-ring 50 0.2) pc)
      ;(draw-dots (test-ring 10 0.2) pc)
      (draw-dots (test-sequence default-args {:text "Alpha"}) pc)
      (draw-rect (rectangle 0 0 (dec w) (dec h)))
      (fill-rect r1)
      (draw-rect r1)
      ;(fill-rect r2)
      ;(draw-rect r2)
      (draw-string-centered text p1 :font font)
      (draw-string-centered text p2 :font font)
      ;(draw-dot c1)
      (draw-dot c2 :color Color/GREEN)

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

(def test-infos (build-word-infos stats))

(def test-cloud (generate-cloud test-infos))

(save-image (paint-cloud test-cloud) "D:/Temp/cloud.png")
