(ns distillery.files
  (:import [java.net URL])
  (:import [java.nio.file Paths Path Files CopyOption StandardCopyOption LinkOption])
  (:import [java.nio.file.attribute FileAttribute]))

(defn get-path
  ^java.nio.file.Path [path & parts]
  (if (instance? URL path)
    (Paths/get (.toURI ^URL path))
    (Paths/get ^String path (if parts (into-array parts) (make-array String 0)))))

(defn combine-path
  ^String [path & parts]
  (.toString ^Path (apply get-path (cons path parts))))

(defn dir-name
  ^String [path & parts]
  (.toString ^Path (.getParent ^Path (apply get-path (cons path parts)))))

(defn file-name
  ^String [path & parts]
  (.toString ^Path (.getFileName ^Path (apply get-path (cons path parts)))))

(defn copy-file
  [^Path src ^Path trg]
  (Files/copy src trg (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn create-dir
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/createDirectories path (make-array FileAttribute 0))))

(defn file-exists?
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/exists path (make-array LinkOption 0))))





