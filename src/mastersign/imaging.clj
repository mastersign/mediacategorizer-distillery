(ns mastersign.imaging
  (:import [java.io File])
  (:import [java.awt.image BufferedImage])
  (:import [javax.imageio ImageIO]))

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