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

(defn default-color-fn
  [value]
  (let [n (float 0.4)
        v1 (float value)
        v2 (float (- 1.0 value))]
  (Color. v1 n v2)))

(def default-args
  {:width 600
   :height 300
   :precision 0.4
   :font (Font. "Calibri" Font/BOLD (float 30))
   :min-font-size 14
   :max-font-size 60
   :max-test-radius 350
   :order-priority 0.7
   :padding 4
   :debug false
   :shape-mode :glyph-box ; :word-box, :glyph-box
   :allow-rotation true
   :final-refine true
   :order-mode :value1 ; :id, :text, :value1, :value2
   :background-color (Color. 0 0 0 0)
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

(defn build-word-infos
  [word-data args]
  (let [img (image 1 1)
        graphics (.createGraphics img)
        word-data* (case (:order-mode args)
                       :id (sort-by first word-data)
                       :text (sort-by second word-data)
                       :value2 (reverse (sort-by #(nth % 3) word-data))
                       (reverse (sort-by #(nth % 2) word-data)))
        word-infos (doall (map (partial build-word-info graphics args) word-data*))]
    (.dispose graphics)
    word-infos))

(defn- setup
  [g]
  (doto g
    (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
    (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)))

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

(defn- recur-seq
  [f args]
  (let [args* (apply f args)]
    (if (nil? args*)
      [args]
      (cons
       args
       (recur-seq f args*)))))

(defn- approach
  [check-fn ref-pos pos rotation]
  (let [sign #(if (< % 0) -1 1)
        rx (int (.x ref-pos))
        ry (int (.y ref-pos))
        dx (- rx (int (.x pos)))
        dy (- ry (int (.y pos)))
        sx (sign dx)
        sy (sign dy)
        step-fn (fn [p r]
                  (let [px (int (.x p))
                        py (int (.y p))
                        x (+ px sx)
                        y (+ py sy)]
                    (or
                     (when (or (and (== sx -1) (> x rx)) (and (== sx 1) (< x rx))) (check-fn (point (+ (.x p) sx) (.y p)) r))
                     (when (or (and (== sy -1) (> y ry)) (and (== sy 1) (< y ry))) (check-fn (point (.x p) (+ (.y p) sy)) r)))))]
    (if (and (== 0 dx) (== 0 dy))
      [pos rotation]
      (last (recur-seq step-fn [pos rotation])))))

(defn- find-position
  [{:keys [allow-rotation final-refine] :as args} test-area boundaries point-sequence {:keys [word-bounds] :as word-info}]
  (let [check (partial check-position args test-area boundaries word-info)
        check* (if allow-rotation
               (fn [pos] (or (check pos 0) (check pos 90) (check pos 270)))
               (fn [pos] (check pos 0)))
        [p a] (first (filter #(not (nil? %)) (map check* point-sequence)))]
    (if (and final-refine (not (nil? p)))
      (approach check (point) p a)
      [p a])))

(defn- add-word-to-area!
  [args *area* word-info pos rotation]
  (let [rects (get-word-rects args word-info pos rotation true)]
    (.add *area* (apply area rects))))

(defn build-cloud-info
  [word-infos args]
  (let [{:keys [width height padding]} args
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
    {:args args
     :word-infos (doall (map finder word-infos))
     :test-area *test-area*}))

(defn- cloud-painter
  [{:keys [args word-infos test-area]} g w h]
  (let [c (point (/ w 2.0) (/ h 2.0))
        bg (translate-area test-area c)]
    (doto g
      setup
      (fill-rect (rectangle 0 0 w h) :color (:background-color args)))
    (when (:debug args)
      (doto g
        (draw-rect (rectangle 0 0 (dec w) (dec h)))
        (fill-shape bg)
        (draw-shape bg)))
    (doseq [{:keys [text font color position rotation]} (filter #(not (nil? %)) word-infos)]
      (draw-string-centered g text (translate-point c position) :font font :color color :rotation rotation))))

(defn paint-cloud
  [cloud args]
  (create-image (:width args) (:height args)
                (partial cloud-painter cloud)))

(defn create-cloud
  [word-data & {:as args}]
  (let [args (merge default-args args)
        word-infos (build-word-infos word-data args)
        cloud-info (build-cloud-info word-infos args)
        img (paint-cloud cloud-info args)
        target-file (:target-file args)]
    (when target-file (save-image img target-file))
    (assoc cloud-info :image img)))
