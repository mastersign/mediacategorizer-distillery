(ns mastersign.geom
  (:import [java.awt.geom
            Area
            Rectangle2D Rectangle2D$Float
            Point2D Point2D$Float
            Ellipse2D Ellipse2D$Float]))

(defn point
  ([] (point 0 0))
  ([x y] (Point2D$Float. (float x) (float y))))

(defn rectangle
  ([] (rectangle 0 0 0 0))
  ([x y w h] (Rectangle2D$Float. (float x) (float y) (float w) (float h)))
  ([p w h] (rectangle (.x p) (.y p) w h)))

(defn ellipse
  ([] (ellipse 0 0 0 0))
  ([x y w h] (Ellipse2D$Float. (float x) (float y) (float w) (float h)))
  ([p w h] (ellipse (.x p) (.y p) w h)))

(defn rect-center
  [r]
  (point (+ (.x r) (/ (.width r) 2.0))
         (+ (.y r) (/ (.height r) 2.0))))

(defn area
  ([] (Area.))
  ([s] (Area. s))
  ([s & shapes]
  (let [a (Area. s)]
    (doseq [s shapes]
      (.add a (Area. s))
    a))))

(defn translate-point
  [p x y]
  (point (+ (.x p) x) (+ (.y p) y)))

(defn translate-rect
  [r x y]
  (rectangle (+ (.x r) x) (+ (.y r) y) (.width r) (.height r)))

(defn translate-ellipse
  [e x y]
  (ellipse (+ (.x e) x) (+ (.y e) y) (.width e) (.height e)))