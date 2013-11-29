(ns mastersign.geom
  (:import [java.awt.geom
            AffineTransform
            Area
            Rectangle2D Rectangle2D$Float
            Point2D Point2D$Float
            Ellipse2D Ellipse2D$Float]))

(defn point
  ([] (point 0 0))
  (^Point2D$Float
   [x y] (Point2D$Float. (float x) (float y))))

(defn polar-point
  [r a]
  (point (* r (Math/cos a)) (- (* r (Math/sin a)))))

(defn rectangle
  ([] (rectangle 0 0 0 0))
  (^Rectangle2D$Float
   [x y w h] (Rectangle2D$Float. (float x) (float y) (float w) (float h)))
  ([^Point2D$Float p w h] (rectangle (.x p) (.y p) w h)))

(defn ellipse
  ([] (ellipse 0 0 0 0))
  (^Ellipse2D$Float
   [x y w h] (Ellipse2D$Float. (float x) (float y) (float w) (float h)))
  ([^Point2D$Float p w h] (ellipse (.x p) (.y p) w h)))

(defn rect-center
  [^Rectangle2D$Float r]
  (point (+ (.x r) (/ (.width r) 2.0))
         (+ (.y r) (/ (.height r) 2.0))))

(defn grow-rect
  [^Rectangle2D$Float r d]
  (rectangle (- (.x r) d) (- (.y r) d) (+ (.width r) d d) (+ (.height r) d d)))

(defn translate-point
  ([^Point2D$Float p ^Point2D$Float o]
   (translate-point p (.x o) (.y o)))
  ([^Point2D$Float p x y]
   (point (+ (.x p) x) (+ (.y p) y))))

(defn translate-rect
  ([^Rectangle2D$Float r ^Point2D$Float o]
   (translate-rect r (.x o) (.y o)))
  ([^Rectangle2D$Float r x y]
   (rectangle (+ (.x r) x) (+ (.y r) y) (.width r) (.height r))))

(defn translate-ellipse
  ([^Ellipse2D$Float e ^Point2D$Float o]
   (translate-ellipse e (.x o) (.y o)))
  ([^Ellipse2D$Float e x y]
   (ellipse (+ (.x e) x) (+ (.y e) y) (.width e) (.height e))))

(defn area
  (^Area
   [] (Area.))
  (^Area
   [s] (Area. s))
  (^Area
   [s & shapes]
   (let [*a* (Area. s)]
     (doseq [s shapes]
       (.add *a* (Area. s)))
     *a*)))

(defn translate-area
  ([area ^Point2D$Float o]
   (translate-area area (.x o) (.y o)))
  (^Area
   [^Area area x y]
   (.createTransformedArea
    area
    (AffineTransform/getTranslateInstance (double x) (double y)))))

