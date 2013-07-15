(ns distillery.files
  (:import [java.nio.file Paths Files StandardCopyOption]))

(defn get-path
  [path & parts]
  (if (instance? java.net.URL path)
    (Paths/get (.toURI path))
    (Paths/get path (into-array (or parts [""])))))

(defn combine-path
  [path & parts]
  (.toString (apply get-path (cons path parts))))

(defn dir-name
  [path & parts]
  (.toString (.getParent (apply get-path (cons path parts)))))

(defn file-name
  [path & parts]
  (.toString (.getFileName (apply get-path (cons path parts)))))

(defn copy-file
  [src trg]
  (Files/copy src trg (into-array [StandardCopyOption/REPLACE_EXISTING])))
