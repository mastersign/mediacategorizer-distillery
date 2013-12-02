(ns mastersign.files
  (:import [java.net URL])
  (:import [java.nio.file Paths Path Files CopyOption StandardCopyOption LinkOption])
  (:import [java.nio.file.attribute FileAttribute]))

(defn get-path
  "Returs a `java.nio.file.Path` object for the given path.

  The path is specified by a base `path` and optional
  a number of additional `parts`.
  The base path can be a string or a `java.net.URL`."
  ^java.nio.file.Path
  [path & parts]
  (if (instance? URL path)
    (Paths/get (.toURI ^URL path))
    (Paths/get ^String path (if parts (into-array parts) (make-array String 0)))))

(defn combine-path
  "Returns a string with the specified path.

  Uses the same interface like `get-path`."
  ^String [path & parts]
  (.toString ^Path (apply get-path (cons path parts))))

(defn dir-name
  "Returns a string with the path of the parent of the specified file / directory.

  Uses the same interface like `get-path`."
  ^String [path & parts]
  (.toString ^Path (.getParent ^Path (apply get-path (cons path parts)))))

(defn file-name
  "Returns a string with the name of the specified file / directory.

  Uses the same interface like `get-path`."
  ^String [path & parts]
  (.toString ^Path (.getFileName ^Path (apply get-path (cons path parts)))))

(defn copy-file
  "Copies a file from a given `source` path to the `target` path.

  The source and target path need to be `java.nio.file.Path` objects."
  [^Path source ^Path target]
  (Files/copy source target (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))

(defn create-dir
  "Creates a directory including all non existing parent directories.

  Uses the same interface like `get-path`."
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/createDirectories path (make-array FileAttribute 0))))

(defn file-exists?
  "Checks if the specified file exists.

  Uses the same interface like `get-path`."
  [path & parts]
  (let [path (apply get-path (cons path parts))]
    (Files/exists path (make-array LinkOption 0))))









