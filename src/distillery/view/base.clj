(ns distillery.view.base
  (:require [net.cgrand.enlive-html :as eh])
  (:require [distillery.files :refer :all])
  (:require [distillery.view.defaults :as defaults])
  (:require [distillery.view.html :refer :all])
  (:require [distillery.view.dependencies :refer (save-dependencies)]))

(defn render
  "Renders the layout template."
  [& {:keys [title js-code main-menu main-menu-title secondary-menu secondary-menu-title head page foot]}]
  (let [title (if title (str "distillery - " title) "distillery")
        js-code (if js-code (str "$(function () { " js-code " });") nil)
        head (or head (headline 1 title))
        foot (or foot defaults/copyright)
        main-menu (menu (or main-menu defaults/main-menu) :title (or main-menu-title defaults/main-menu-title))
        secondary-menu (if secondary-menu (menu secondary-menu :title (or secondary-menu-title defaults/secondary-menu-title)) nil)]
    (eh/at (template "base")
	    [:head :title] (eh/content title)
	    [:head :script#js_code] (eh/content js-code)
	    [:nav#main-menu] (eh/content main-menu)
	    [:nav#secondary-menu] (eh/content secondary-menu)
	    [:header] (eh/content head)
	    [:#page] (eh/content page)
	    [:footer] (eh/content foot))))
