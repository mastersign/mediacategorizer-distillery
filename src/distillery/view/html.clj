(ns distillery.view.html
  (:require [clojure.pprint :refer (pprint)])
  (:require [clojure.java.io :refer (resource)])
  (:require [net.cgrand.enlive-html :refer (html-resource transform content emit*)])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.trace :refer :all])
  (:require [distillery.text :refer [txt]]))

(defn template
  "Loads a html template from the 'view' folder by its name without extension."
  [name]
  (let [path (str "distillery/view/" name ".html")]
    (html-resource path)))

(defn build-title
  "Generates the title for a page."
  [{:keys [job-name] :as args} title]
  (if title
    (str job-name " - " title)
    job-name))

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
      :content (->> items
                    (filter #(not (nil? %)))
                    (map menu-item))}]))

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
             (div "innerpage" content)]})

(defn bar
  "Builds a bar as part of a diagram."
  ([text color v]
   (bar text color v (+ 0.5 (* 0.5 v))))
  ([text [r g b] v1 v2]
   (let [iv (int (* 100 v1))
         c (str "rgba(" (int (* 255 r)) "," (int (* 255 g)) "," (int (* 255 b)) "," (float v2) ")")]
     (div "bar"
          [(div "bar_text" text)
           (div "bar_client"
                {:tag :div
                 :attrs {:class "bar_beam"
                         :style (str "width:" iv "%; background-color: " c ";")}})]))))

(defn DEBUG
  "Builds an HTML debug message element with a pretty printed version of the given object.
  Writes a debug message to the trace as a side effect."
  [x]
  (let [w (java.io.StringWriter.)]
    (pprint x w)
    (let [txt (.toString w)]
      (trace-message "DEBUG:\n" txt)
      {:tag :pre
       :content [{:tag :strong :content [(txt :DEBUG) "\n"] }
                 {:tag :code
                  :content [txt]}]})))

(defn TODO
  "Builds a highly visible HTML todo message.
  Writes the todo message to the trace as a side effect."
  [text]
  (trace-message "TODO: " text)
  {:tag :div :attrs {:class "todo"} :content text})
