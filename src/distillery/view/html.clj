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
  (assoc-in (paragraph content) [:attrs :class] css-class)))

(defn headline
  "Wrappes the given content into a headline. Headline levels from 1 to 8 are supported."
  ([level content]
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
  ([level css-class content]
   (assoc-in (headline level content) [:attrs :class] css-class)))

(defn preformatted
  "Wrappes the given text into a pre element."
  [text]
  {:tag :pre :content text})

(defn code
  "Wrappes the given text into a code and pre element."
  [text]
  {:tag :code :content [{:tag :pre :content text}]})

(defn emph
  "Wrappes the content into an emphasizing element."
  [content]
  {:tag :em :content (safe-content content)})

(defn strong
  "Wrappes the content int a strong emphasizing element."
  [content]
  {:tag :strong :content (safe-content content)})

(defn div
  "Creates a div container with a given CSS class and some content."
  ([content]
   {:tag :div :content (safe-content content)})
  ([css-class content]
   (assoc-in (div content) [:attrs :class] css-class)))

(defn span
  "Creates a span container with a given CSS class and some text."
  ([content]
   {:tag :span :content (safe-content content)})
  ([css-class content]
   (assoc-in (span content) [:attrs :class] css-class)))

(defn list-item
  "Creates a list item."
  ([content]
   {:tag :li :content (safe-content content)})
  ([css-class content]
   (assoc-in (list-item content) [:attrs :class] css-class)))

(defn ulist
  "Creates an unordered list with a given CSS class and a sequence of list items."
  ([items]
   {:tag :ul :content (vec items)})
  ([css-class items]
   (assoc-in (ulist items) [:attrs :class] css-class)))

(defn olist
  "Creates an ordered list with a given CSS class and a sequence of list items."
  ([items]
   {:tag :ol :content (vec items)})
  ([css-class items]
   (assoc-in (olist content) [:attrs :class] css-class)))

(defn link
  "Create a link tag with the given URL an optional target and some content."
  ([url content]
   {:tag :a :attrs {:href url} :content (safe-content content)})
  ([url target content]
   (assoc-in (link url content) [:attrs :target] target)))

(defn jscript
  "Creates a script tag for JavaScript code."
  [js]
  {:tag :script
   :attrs {:type "text/javascript"}
   :content [js]})

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
  (let [menu-item (fn [[label url]] (list-item (link url label)))]
    [(if title
       {:tag :div
        :attrs {:class "menu-title"}
        :content title}
       nil)
     {:tag :ul
      :content (map menu-item items)}]))

(defn innerpage
  "Builds the block structure for an inner page.
   Inner pages can be shown without loading content from the server."
  [id title active content]
  {:tag :article
   :attrs {:id id
           :class "innerpage"
           :style (str "display: " (if active "inherit" "none"))
           :data-start (str active)}
   :content [(headline 3 title)
             {:tag :div
              :attrs {:class "innerpage"}
              :content (safe-content content)}]})

(defn bar
  ([text color v]
   (bar text color v 1))
  ([text [r g b] v1 v2]
   (let [iv (int (* 100 v1))
         c (str "rgba(" (int (* 255 r)) "," (int (* 255 g)) "," (int (* 255 b)) "," (float v2) ")")]
     (div "bar"
          [(div "bar_text" text)
           (div "bar_client"
                {:tag :div
                 :attrs {:class "bar_beam"
                         :style (str "width:" iv "%; background-color: " c ";")}})]))))

(defn TODO
  [text]
  ;(println (format "TODO: %s" text))
  {:tag :div :attrs {:class "todo"} :content text})

