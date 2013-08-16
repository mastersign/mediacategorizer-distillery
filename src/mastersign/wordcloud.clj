(ns mastersign.wordcloud
  (:import [java.awt Graphics Graphics2D Color RenderingHints Font])
  (:import [java.awt.image BufferedImage])
  (:import [java.awt.font GlyphVector GlyphMetrics])
  (:import [java.awt.geom Area Rectangle2D Rectangle2D$Float Point2D Point2D$Float])
  (:import [javax.imageio ImageIO])
  (:import [java.io File]))

(def text-antialias-mode RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
(def default-color Color/BLACK)
(def font-family "Calibri")
(def font-style Font/BOLD)

(def base-font (Font. font-family font-style (float 100)))

(defn image
  [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))

(defn create-image
  [w h f]
  (let [img (image w h)
        g (.createGraphics img)]
    (f g w h)
    (.dispose g)
    img))

(defn save-image
  [img path]
  (ImageIO/write img "png" (File. path)))

(defn text-rect
  ([g font text]
   (let [gv (.createGlyphVector font (.getFontRenderContext g) text)]
     (.getVisualBounds gv)))
  ([g font text x y]
   (let [r (text-rect g font text)]
     (Rectangle2D$Float. (+ x (.x r)) (+ y (.y r)) (.width r) (.height r)))))

(defn text-area
  [g font text x y]
  (Area. (text-rect g font text x y)))

(defn rect-center
  [r]
  (Point2D$Float. (+ (.x r) (/ (.width r) 2.0)) (+ (.y r) (/ (.height r) 2.0))))

(defn center-to-offset
  [p]
  (Point2D$Float. (* -1 (.x p)) (* -1 (.y p))))


; ########################


(defn calc-font-size
  [[_ v]]
  (float (+ 14 (* 40 v))))

(defn get-font
  [x]
  (.deriveFont base-font (calc-font-size (second x))))

(defn get-rect
  [g font x]
  (text-rect g font (first x)))

(defn build-word-info
  [g x]
  (let [text (first x)
        font (get-font x)
        rect (get-rect g font x)
        offset (center-to-offset (rect-center rect))
        rect* (Rectangle2D$Float. (+ (.x rect) (.x offset)) (+ (.y rect) (.y offset)) (.width rect) (.height rect))]
      {:text text :font font :rect rect :offset offset :rect* rect*}))

; ##########################

(def words
  ["Alpha" "Beta" "Charlie" "Delta" "Echo" "Foxtrott"
   "Golf" "Hotel" "India" "Juliette" "Kilo" "Lima"
   "Mike" "November" "Oscar" "Papa" "Quebec" "Romeo"
   "Sierra" "Tango" "Uniform" "Victor" "Whiskey" "X-Ray"
   "Yankee" "Zulu"])

(def stats (vec (map (fn [x] [x (/ (rand-int 101) 100.0)]) words)))
(def sorted-stats (reverse (sort-by second stats)))
(println sorted-stats)

(defn test-painter
  [g w h]
  (let [font (.deriveFont base-font (float 30.0))
        text "MjgqAIkqQ$§|"
        r1 (text-rect g font text 0 30)
        r2 (text-rect g font text 0 60)
        a1 (Area. r1)
        c2 (rect-center r2)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING text-antialias-mode)
      (.setFont font)
      (.setColor default-color)
      (.drawRect 0 0 (- w 1) (- h 1))
      (.setColor Color/GREEN)
      (.fill a1)
      (.setColor default-color)
      (.draw a1)
      (.drawString text 0 30)
      (.drawString text 0 60)
      (.setColor Color/RED)
      (.fillRect (- (.x c2) 2) (- (.y c2) 2) 4 4)
      )))

(let [img (create-image 400 200 test-painter)]
  (save-image img "D:/Temp/test.png"))

