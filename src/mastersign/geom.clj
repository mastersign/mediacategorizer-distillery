(ns mastersign.geom
  (:import [java.awt.geom
            AffineTransform
            Area
            Rectangle2D Rectangle2D$Float
            Point2D Point2D$Float
            Ellipse2D Ellipse2D$Float]))

(defn point
  ([] (point 0 0))
  ([x y] (Point2D$Float. (float x) (float y))))

(defn polar-point
  [r a]
  (point (* r (Math/cos a)) (- (* r (Math/sin a)))))

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

(defn grow-rect
  [r d]
  (rectangle (- (.x r) d) (- (.y r) d) (+ (.width r) d d) (+ (.height r) d d)))

(defn translate-point
  ([p o]
   (translate-point p (.x o) (.y o)))
  ([p x y]
   (point (+ (.x p) x) (+ (.y p) y))))

(defn translate-rect
  ([r o]
   (translate-rect r (.x o) (.y o)))
  ([r x y]
   (rectangle (+ (.x r) x) (+ (.y r) y) (.width r) (.height r))))

(defn translate-ellipse
  ([e o]
   (translate-ellipse e (.x o) (.y o)))
  ([e x y]
   (ellipse (+ (.x e) x) (+ (.y e) y) (.width e) (.height e))))

(defn area
  ([] (Area.))
  ([s] (Area. s))
  ([s & shapes]
   (let [*a* (Area. s)]
     (doseq [s shapes]
       (.add *a* (Area. s)))
     *a*)))

(defn translate-area
  ([area o]
   (translate-area area (.x o) (.y o)))
  ([area x y]
   (.createTransformedArea
    area
    (AffineTransform/getTranslateInstance (double x) (double y)))))
