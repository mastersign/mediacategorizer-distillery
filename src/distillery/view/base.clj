(ns distillery.view.base
  (:require [net.cgrand.enlive-html :as eh])
  (:require [mastersign.html :refer :all])
  (:require [mastersign.files :refer :all])
  (:require [distillery.view.defaults :as defaults])
  (:require [distillery.view.html :refer :all]))

(defn fix-url
  [base-path url]
  (let [uri (java.net.URI. url)]
    (if (or (= (.getScheme uri) "javascript") (.isAbsolute uri))
      url
      (str base-path url))))

(defn fix-urls
  "Fixes all relative URLs in the template by suffixing with the given base path."
  [base-path template]
  (let [fix-url (partial fix-url base-path)
        fix-href (attr-transformer :href fix-url)
        fix-src (attr-transformer :src fix-url)]
    (eh/at template
           [:head :link] fix-href
           [:script] fix-src
           [:a] fix-href
           [:img] fix-href)))

(defn fix-menu-urls
  "Fixes all relative URLs in the menu by suffixing with the given base path."
  [base-path menu]
  (let [map-map (fn [f m] (into {} (for [[k v] m] [k (f v)])))]
    (map-map (partial fix-url base-path) menu)))

(defn render
  "Renders the layout template."
  [& {:keys [base-path
             title
             js-code
             main-menu
             main-menu-title
             secondary-menu
             secondary-menu-title
             head
             page
             foot]}]

  (let [title (if title (str "distillery - " title) "distillery")
        js-code (if js-code (str "$(function () { " js-code " });") nil)
        head (or head (headline 1 title))
        foot (or foot defaults/copyright)
        main-menu (menu
                    (or main-menu (fix-menu-urls base-path defaults/main-menu))
                    :title (or main-menu-title defaults/main-menu-title))
        secondary-menu (if secondary-menu
                         (menu
                           secondary-menu
                           :title (or secondary-menu-title defaults/secondary-menu-title))
                         nil)]

    (eh/at (fix-urls base-path (template "base"))
	    [:head :title] (eh/content title)
	    [:head :script#js_code] (eh/content js-code)
	    [:nav#main-menu] (eh/content main-menu)
	    [:nav#secondary-menu] (eh/content secondary-menu)
	    [:header] (eh/content head)
	    [:#page] (eh/content page)
	    [:footer] (eh/content foot))))



