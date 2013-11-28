(ns distillery.files
  (:import [java.net URL])
  (:import [java.nio.file Paths Path Files StandardCopyOption LinkOption])
  (:import [java.nio.file.attribute FileAttribute]))

(defn get-path
  [path & parts]
  (if (instance? URL path)
    (Paths/get (.toURI ^URL path))
    (Paths/get ^String path (if parts (into-array parts) (make-array String 0)))))

(defn combine-path
  [path & parts]
  (.toString ^Path (apply get-path (cons path parts))))

(defn dir-name
  [path & parts]
  (.toString ^Path (.getParent ^Path (apply get-path (cons path parts)))))

(defn file-name
  [path & parts]
  (.toString ^Path (.getFileName ^Path (apply get-path (cons path parts)))))

(defn copy-file
  [^Path src ^Path trg]
  (Files/copy src trg (into-array [StandardCopyOption/REPLACE_EXISTING])))

(defn create-dir
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/createDirectories path (make-array FileAttribute 0))))

(defn file-exists?
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/exists path (make-array LinkOption 0))))

