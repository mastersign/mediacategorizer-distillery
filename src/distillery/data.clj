(ns distillery.data
  (:require [clojure.java.io :refer (reader)])
  (:require [clojure.string :as string])
  (:require [clojure.edn :as edn])
  (:require [net.cgrand.enlive-html :as eh]))

;; ## Data Input
;; Functions to read data from URLs or files.

;; ### Private Functions

(defn- build-reader
  "Creates an java.io.Reader from an URL or file path.
  The sequence opts can be [:encoding \"<encoding>\"].
  The default encoding is UTF-8"
  [x opts]
  (apply reader (cons x opts)))

(defn- read-text
  "Reads all text from the given URL or file path.
  For opts see build-reader."
  [x opts]
  (apply slurp (cons x opts)))

;; ### Public Functions

;; All `load-*` functions do have the same interface.
;; The first argument is the URL or file path to the resource.
;; The later arguments can be flags to control the way
;; the resource is read.
;; For now, only the `:encoding` flag is supported.
;;
;; To read all text from a resource use
;; `(load-text "myfile.txt" :encoding "ISO-LATIN-1")`.

(defn load-data
  "Loads the content of an URL or file as EDN formatted data structure."
  [x & opts]
  (with-open
    [r (java.io.PushbackReader. (build-reader x opts))]
    (edn/read r)))

(defn load-text
  "Loads the content from an URL or file as plain text."
  [x & opts]
  (read-text x opts))

(defn load-list
  "Loads the content of an URL or file as word list.
  Whitespace and commas are word separators."
  [x & opts]
  (let [text (read-text x opts)]
    (string/split text #"[\s,]+")))

(defn load-text-from-html
  "Loads the textual content from an HTML page.
  The optional `:selector` argument gives an enlive selector for the HTML nodes to use.
  Example: `(load-text-from-html \"http://my-site.com\" :selector [[:section (attr= \"main\")]])`"
  [x & opts]
  (let [opts-map (apply hash-map opts)
        selector (or (:selector opts-map) #{[:head :title] [:body]})
        nodes (-> (build-reader x opts)
                  (eh/html-resource)
                  (eh/select selector)
                  (eh/transform [:script] nil)
                  (eh/select [eh/text-node]))]
    (->> nodes
         (map eh/text)
         (filter #(not (string/blank? %)))
         (map string/trim)
         (string/join " "))))

(defn load-text-from-wikipedia
  "Loads the textual content from an Wikipedia page."
  [^String url & opts]
  (let [url (if (.startsWith url "file://")
              url
              (str url "?action=render"))
        nodes (-> (build-reader url opts)
                  (eh/html-resource)
                  (eh/transform #{[:script]
                                  [:.noprint]
                                  [:.mw-editsection]
                                  [:.mw-magiclink-isbn]
                                  [:.sisterproject]
                                  [:#Siehe_auch]
                                  [:#Literatur]
                                  [:#Einzelnachweise]
                                  [:#Weblinks]
                                  [:#normdaten]} nil)
                  (eh/select [eh/text-node]))]
    (->> nodes
         (map eh/text)
         (filter #(not (string/blank? %)))
         (map string/trim)
         (string/join " "))))

(defn words-from-text
  "Creates a sequence of words from a text."
  [text]
  (->> (string/split text #"\b")
       (filter #(not (string/blank? %)))
       (map string/trim)))

;; ## Collection Processing
;; Basic helper functions to process maps and collections.

;; ### General

(defn any?
  "Returns true if the predicate returns true for at least one value in coll; otherwise false."
  [pred coll]
  (not (empty? (filter pred coll))))

(defmacro key-comp
  "Creates a comparator, defined by a function which takes an element and returns the key."
  [f-key]
  `(fn [a# b#] (compare (~f-key a#) (~f-key b#))))

(defn multi-filter
  "Applies a number of predicates to a value and returns true if all predicates are true."
  [predicates x]
  (if (empty? predicates)
    true
    (every? true? ((apply juxt predicates) x))))

(defn reduce-by
  "Groups a collection by a key, computed by key-fn, and reduces the values of each group with f."
  [key-fn f init coll]
  (reduce (fn [summaries x]
            (let [k (key-fn x)]
              (assoc summaries k (f (summaries k init) x))))
          {} coll))

(defn reduce-by-sorted
  "Groups a collection by a key, computed by key-fn, and reduces the values of each group with f.
   Returns a sorted map."
  [key-fn f init coll]
  (reduce (fn [summaries x]
            (let [k (key-fn x)]
              (assoc summaries k (f (summaries k init) x))))
          (sorted-map) coll))

;; ### Maps

(defn map-group-items
  "Applies a function to the items of a group collection."
  [f [k xs]] [k (map f xs)])

(defn map-pair-value
  "Applies a function to the value of a key-value-pair."
  [f [k v]] [k (f v)])

(defn map-values
  "Applies a function to the values of a map and returns a map with associating the original keys with the transformed values."
  [f m]
  (apply (if (sorted? m) sorted-map hash-map) (apply concat (map #(map-pair-value f %) m))))

;; ## Collection Math
;; A few helper functions to simplify some mathematical tasks on collections.

(defn sum
  "Computes the sum of a numeric sequence."
  [xs] (apply + xs))

(defn mean
  "Computes the mean value of a numeric sequence."
  [xs]
  (let [cnt (count xs)]
    (if (> cnt 0) (/ (apply + xs) cnt) 0)))

(defn squared-sum
  "Computes the sum of the squared items of a sequence."
  [xs] (apply + (map #(* % %) xs)))


