(ns mastersign.drawing
  (:import [java.awt Color Font Graphics2D])
  (:import [java.awt.geom
            AffineTransform
            Point2D Point2D$Float
            Rectangle2D Rectangle2D$Float])
  (:require [mastersign.geom :refer :all]))

(def default-font-size 16)
(def default-font-style Font/PLAIN)
(def default-font (Font. Font/SANS_SERIF default-font-style default-font-size))

(defn color
  ([v] (Color. (float v) (float v) (float v) (float 1)))
  ([r g b] (Color. (float r) (float g) (float b) (float 1)))
  ([r g b a] (Color. (float r) (float g) (float b) (float a))))

(defn- font-family-name
  [family]
  (case family
    :dialog Font/DIALOG
    :dialog-input Font/DIALOG_INPUT
    :mono Font/MONOSPACED
    :serif Font/SERIF
    :sans-serif Font/SANS_SERIF
    family))

(def ^:private ^:const font-styles
  {:plain Font/PLAIN
   :bold Font/BOLD
   :italic Font/ITALIC})

(defn- font-style
  [styles]
  (reduce
   (fn [r s] (+ r (get font-styles s)))
   Font/PLAIN
   styles))

(defn font
  ([]
   (font :dialog))
  ([family]
   (font family (float 14)))
  ([family size]
   (font family size :plain))
  ([family size & styles]
   (Font. (font-family-name family) (font-style styles) (float size))))

(defn draw-dot
  ([^Graphics2D g ^Point2D$Float p & {color :color
           :or {color Color/RED}}]
  (doto g
    (.setColor color)
    (.fill (ellipse (- (.x p) 1) (- (.y p) 1) 2 2)))))

(defn draw-dots
  [^Graphics2D g ps o & {color :color
                 :or {color Color/RED}}]
  (doseq [p (map #(translate-point % o) ps)] (draw-dot g p :color color)))

(defn draw-rect
  [^Graphics2D g ^Rectangle2D$Float r & {color :color
          :or {color Color/BLUE}}]
  (doto g
    (.setColor color)
    (.draw r)))

(defn fill-rect
  [^Graphics2D g ^Rectangle2D$Float r & {color :color
          :or {color Color/GREEN}}]
  (doto g
    (.setColor color)
    (.fill r)))

(defn draw-shape
  [^Graphics2D g s & {color :color
          :or {color Color/ORANGE}}]
  (doto g
    (.setColor color)
    (.draw s)))

(defn fill-shape
  [^Graphics2D g s & {color :color
          :or {color Color/YELLOW}}]
  (doto g
    (.setColor color)
    (.fill s)))

(defn draw-string
  [^Graphics2D g ^Point2D$Float p ^String text & {font :font
               color :color
               :or {font default-font
                    color Color/BLACK}}]
  (doto g
    (.setColor color)
    (.setFont font)
    (.drawString text (.x p) (.y p))))

(defn string-centered-offset
  [^Graphics2D g ^Font font ^String text]
  (let [gv (.createGlyphVector font (.getFontRenderContext g) text)
        r (.getVisualBounds gv)
        ^Point2D$Float c (rect-center r)]
     (point (- (.x c)) (- (.y c)))))

(defn string-centered-bounds
  ([^Graphics2D g font text]
   (string-centered-bounds g font text 0 0))
  ([^Graphics2D g font text ^Point2D$Float p]
   (string-centered-bounds g font text (.x p) (.y p)))
  ([^Graphics2D g ^Font font ^String text x y]
   (let [gv (.createGlyphVector font (.getFontRenderContext g) text)
         ^Rectangle2D$Float r (.getVisualBounds gv)
         ^Point2D$Float c (rect-center r)]
     (rectangle (- (+ x (.x r)) (.x c)) (- (+ y (.y r)) (.y c)) (.width r) (.height r)))))

(defn string-centered-glyphbounds
  ([^Graphics2D g font text]
   (string-centered-glyphbounds g font text 0 0))
  ([^Graphics2D g font text ^Point2D$Float p]
   (string-centered-glyphbounds g font text (.x p) (.y p)))
  ([^Graphics2D g ^Font font ^String text x y]
  (let [frc (.getFontRenderContext g)
        gv (.createGlyphVector font frc text)
        r (.getVisualBounds gv)
        ^Point2D$Float c (rect-center r)
        glyphbounds (map
                     #(.getBounds2D (.getGlyphOutline gv % (- x (.x c)) (- y (.y c))))
                     (range (.getNumGlyphs gv)))]
    (vec glyphbounds))))

(defn string-centered-outline
  ([^Graphics2D g font text]
   (string-centered-outline g font text 0 0))
  ([^Graphics2D g font text ^Point2D$Float p]
   (string-centered-outline g font text (.x p) (.y p)))
  ([^Graphics2D g ^Font font ^String text x y]
   (let [frc (.getFontRenderContext g)
         gv (.createGlyphVector font frc text)
         r (.getVisualBounds gv)
         ^Point2D$Float c (rect-center r)]
     (.getOutline gv (- x (.x c)) (- y (.y c))))))

(defn draw-string-centered
  [^Graphics2D g ^String text ^Point2D$Float p & {font :font
               color :color
               rotation :rotation
               :or {font default-font,
                    color Color/BLACK
                    rotation 0}}]
  (let [^Point2D$Float offset (string-centered-offset g font text)
        x (.x p)
        y (.y p)
        transform (AffineTransform/getRotateInstance (* rotation (/ Math/PI 180)) x y)]
    (doto g
        (.setColor color)
        (.setFont font)
        (.setTransform transform)
        (.drawString text
                     (float (+ (.x p) (.x offset)))
                     (float (+ (.y p) (.y offset))))
        (.setTransform (AffineTransform.)))))

