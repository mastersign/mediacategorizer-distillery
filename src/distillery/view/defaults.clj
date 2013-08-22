(ns distillery.view.defaults)

(def main-menu-title "Menü")

(def secondary-menu-title "Views")

(def main-menu
  {"Projekt" "index.html"
   "Kategorien" "categories.html"
   "Videos" "videos.html"})

(def copyright
  {:tag :p
   :content
    ["Copyright \u00A9 "
     (.get (java.util.GregorianCalendar.) java.util.Calendar/YEAR)
    " "
    {:tag :a
     :attrs {:href "http://informatik.fh-brandenburg.de/~kiertsch/" :target "_blank"}
     :content ["Tobias Kiertscher"]}
    ", Brandenburg University of Applied Sciences."]})

