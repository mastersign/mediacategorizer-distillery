(ns mastersign.drawing
  (:import [java.awt Color Font])
  (:require [mastersign.geom :refer :all]))

(def default-font-size 16)
(def default-font-style Font/PLAIN)
(def default-font (Font. Font/SANS_SERIF default-font-style default-font-size))

(defn draw-dot
  ([g p & {color :color
           :or {color Color/RED}}]
  (doto g
    (.setColor color)
    (.fill (ellipse (- (.x p) 2) (- (.y p) 2) 4 4)))))

(defn draw-dots
  [g ps o & {color :color
                 :or {color Color/RED}}]
  (doseq [p (map #(translate-point % o) ps)] (draw-dot g p :color color)))

(defn draw-rect
  [g r & {color :color
          :or {color Color/BLUE}}]
  (doto g
    (.setColor color)
    (.draw r)))

(defn fill-rect
  [g r & {color :color
          :or {color Color/GREEN}}]
  (doto g
    (.setColor color)
    (.fill r)))

(defn draw-shape
  [g s & {color :color
          :or {color Color/ORANGE}}]
  (doto g
    (.setColor color)
    (.draw s)))

(defn fill-shape
  [g s & {color :color
          :or {color Color/YELLOW}}]
  (doto g
    (.setColor color)
    (.fill s)))

(defn draw-string
  [g p text & {font :font
               color :color
               :or {font default-font
                    color Color/BLACK}}]
  (doto g
    (.setColor color)
    (.setFont font)
    (.drawString text (.x p) (.y p))))

(defn string-centered-offset
  [g font text]
  (let [gv (.createGlyphVector font (.getFontRenderContext g) text)
        r (.getVisualBounds gv)
        c (rect-center r)]
     (point (- (.x c)) (- (.y c)))))

(defn string-centered-bounds
  ([g font text]
   (string-centered-bounds g font text 0 0))
  ([g font text p]
   (string-centered-bounds g font text (.x p) (.y p)))
  ([g font text x y]
   (let [gv (.createGlyphVector font (.getFontRenderContext g) text)
         r (.getVisualBounds gv)
         c (rect-center r)]
     (rectangle (- (+ x (.x r)) (.x c)) (- (+ y (.y r)) (.y c)) (.width r) (.height r)))))

(defn string-centered-glyphbounds
  ([g font text]
   (string-centered-glyphbounds g font text 0 0))
  ([g font text p]
   (string-centered-glyphbounds g font text (.x p) (.y p)))
  ([g font text x y]
  (let [frc (.getFontRenderContext g)
        gv (.createGlyphVector font frc text)
        r (.getVisualBounds gv)
        c (rect-center r)
        glyphbounds (map
                     #(.getBounds2D (.getGlyphOutline gv % (- x (.x c)) (- y (.y c))))
                     (range (.getNumGlyphs gv)))]
    (vec glyphbounds))))

(defn string-centered-outline
  ([g font text]
   (string-centered-outline g font text 0 0))
  ([g font text p]
   (string-centered-outline g font text (.x p) (.y p)))
  ([g font text x y]
   (let [frc (.getFontRenderContext g)
         gv (.createGlyphVector font frc text)
         r (.getVisualBounds gv)
         c (rect-center r)]
     (.getOutline gv (- x (.x c)) (- y (.y c))))))

(defn draw-string-centered
  [g text p & {font :font,
               color :color,
               :or {font default-font,
                    color Color/BLACK}}]
  (let [offset (string-centered-offset g font text)]
    (doto g
        (.setColor color)
        (.setFont font)
        (.drawString text
                     (float (+ (.x p) (.x offset)))
                     (float (+ (.y p) (.y offset)))))))
