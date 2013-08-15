(ns distillery.view.html
  (:require [clojure.java.io :refer (resource)])
  (:require [net.cgrand.enlive-html :refer (html-resource transform content emit*)]))

(defn template
  "Loads a html template from the 'view' folder by its name without extension."
  [name]
  (let [path (str "distillery/view/" name ".html")]
    (html-resource path)))

(defn emit-view
  "Transforms the html model into a sequence of strings."
  [view]
  (apply str (emit* view)))

(defn save-page
  "Converts a HTML description map, converts it to a HTML string and writes it to the given file path."
  [path page]
  (spit path (emit-view page) :encoding "UTF-8"))

(defn replacer
  "Creates a function to replace the content of a specified html element."
  [selector & {:keys [data-transform]}]
  (fn [src data]
    (let [data (if data-transform (data-transform data) data)]
      (transform src selector (content data)))))

(defn attr-transformer
  "Creates a function to transform an attribute of a html element with a given function."
  [attr f]
  (fn [element]
    (if (contains? (:attrs element) attr)
      (update-in element [:attrs attr] f)
      element)))

(defn- safe-content
  [content]
  (if (or (string? content) (vector? content))
    content
    (if (and (map? content) (:tag content))
      [content]
      (if (coll? content)
        (vec (flatten content))
        (str content)))))

(defn paragraph
  "Wrappes the given content into a paragraph."
  ([content]
  {:tag :p :content (safe-content content)})
  ([css-class content]
  {:tag :p :attrs {:class css-class} :content (safe-content content)}))

(defn headline
  "Wrappes the given content into a headline. Headline levels from 1 to 8 are supported."
  [level content]
  (let [head-sym
        (cond
          (= level 1) :h1
          (= level 2) :h2
          (= level 3) :h3
          (= level 4) :h4
          (= level 5) :h5
          (= level 6) :h6
          (= level 7) :h7
          (= level 8) :h8)]
    {:tag head-sym :content (safe-content content)}))

(defn preformatted
  "Wrappes the given text into a pre element."
  [text]
  {:tag :pre :content text})

(defn code
  "Wrappes the given text into a code and pre element."
  [text]
  {:tag :code :content [{:tag :pre :content text}]})

(defn div
  "Creates a div container with a given CSS class and some content."
  [css-class content]
  {:tag :div :attrs {:class css-class} :content (safe-content content)})

(defn span
  "Creates a span container with a given CSS class and some text."
  [css-class content]
  {:tag :span :attrs {:class css-class} :content (safe-content content)})

(defn list-item
  "Creates a list item."
  [content]
  {:tag :li :content (safe-content content)})

(defn ulist
  "Creates an unordered list with a given CSS class and a sequence of list items."
  [css-class items]
  {:tag :ul :attrs {:class css-class} :content (vec items)})

(defn olist
  "Creates an ordered list with a given CSS class and a sequence of list items."
  [css-class items]
  {:tag :ol :attrs {:class css-class} :content (vec items)})

(defn jshref
  "Creates a URI referencing a JavaScript call."
  [js]
  (str "javascript:" js ))

(defn jslink
  "Creates a link tag with the given javascript command and some content.
   The javascript code must not contain double quotes."
  [js content]
  {:tag :a :attrs {:href (jshref js)} :content (safe-content content)})

(defn menu
  "Builds a menu structure from a menu title and a sequence of label/url pairs."
  [items & {:keys [title]}]
  (defn menu-item
    [[label url]]
    {:tag :li
     :content [{:tag :a
               :attrs {:href url}
               :content label}]})
  [(if title
     {:tag :div
      :attrs {:class "menu-title"}
      :content title}
     nil)
   {:tag :ul
    :content (map menu-item items)}])

(defn innerpage
  "Builds the block structure for an inner page.
   Inner pages can be shown without loading content from the server."
  [id title active content]
  {:tag :article
   :attrs {:id id :class "innerpage" :style (str "display: " (if active "inherit" "none"))}
   :content [(headline 3 title)
             {:tag :div
              :attrs {:class "innerpage"}
              :content (safe-content content)}]})

(defn TODO
  [text]
  (println (format "TODO: %s" text))
  {:tag :div :attrs {:class "todo"} :content text})