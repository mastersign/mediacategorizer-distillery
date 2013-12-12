(ns distillery.view.defaults
  (:require [distillery.text :refer [txt]]))

(def main-menu-title (txt :frame-top-menu-title))

(def secondary-menu-title (txt :frame-menu-title))

(def main-menu
  [[(txt :frame-top-menu-project) "index.html"]
   [(txt :frame-top-menu-categories) "categories.html"]
   [(txt :frame-top-menu-videos) "videos.html"]])

(def copyright
  {:tag :p
   :content
    ["Copyright \u00A9 "
     (.get (java.util.GregorianCalendar.) java.util.Calendar/YEAR)
    " "
    {:tag :a
     :attrs {:href (txt :copyright-href) :target "_blank"}
     :content [(txt :copyright-holder)]}
    "."]})
