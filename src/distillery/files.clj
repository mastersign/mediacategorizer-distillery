(ns distillery.files
  (:import [java.nio.file Paths Files StandardCopyOption LinkOption])
  (:import [java.nio.file.attribute FileAttribute]))

(defn get-path
  [path & parts]
  (if (instance? java.net.URL path)
    (Paths/get (.toURI path))
    (Paths/get path (if parts (into-array parts) (make-array String 0)))))

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

(defn create-dir
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/createDirectories path (make-array FileAttribute 0))))

(defn file-exists?
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/exists path (make-array LinkOption 0))))