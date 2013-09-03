(ns mastersign.imaging
  (:import [java.io File])
  (:import [java.awt Color Graphics Graphics2D Image])
  (:import [java.awt.image BufferedImage])
  (:import [javax.swing JFrame JPanel])
  (:import [javax.imageio ImageIO]))

(defn image
  [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))

(defn create-image
  [w h f]
  (let [^BufferedImage img (image w h)
        ^Graphics g (.createGraphics img)]
    (f g w h)
    (.dispose g)
    img))

(defn save-image
  [^BufferedImage img ^String path]
  (ImageIO/write img "png" (File. path)))

(defn show-image
  [^Image img]
  (let [w (.getWidth img)
        h (.getHeight img)
        fw (+ w 32)
        fh (+ h 56)
        frame (JFrame. "Image Display")
        panel (proxy [JPanel] []
                (paintComponent
                 [^Graphics2D g]
                 (doto g
                   (.setColor (Color. 240 245 255))
                   (.fillRect 0 0 fw fh)
                   (.setColor (Color/BLACK))
                   (.drawRect 7 7 (inc w) (inc h))
                   (.drawImage img 8 8 w h this))))]
    (doto frame
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.setSize fw fh)
      (.setContentPane panel)
      (.setVisible true))))




