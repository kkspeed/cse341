(ns cse341.utils
  (:import [java.util Date]
           [java.text SimpleDateFormat])
  (:require [slugger.core :as slugger]
            [net.cgrand.enlive-html :as h]
            [markdown.core :as md]
            [clojure.string :as string]
            [me.raynes.fs :as fs]))

(defn current-date-time []
  (-> (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
    (.format (Date.))
    (str)))

(defn datetime-reformat-iso [d]
  (->> d
       (string/upper-case)
       (.parse (SimpleDateFormat. "MM/dd/yyyy h:m a"))
       (.format (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))
       (str)))

(defn mk-slugger [s]
  (slugger/->slug s))

(defn md->snippet [s]
  (-> s
      (md/md-to-html-string)
      (h/html-snippet)))

(defn first-n-lines [s n]
  (->> s
       (string/split-lines)
       (take n)
       (string/join "\n")))

(defn ensure-directory-exists [d]
  (fs/mkdir d))

(defn space->underscore [s]
  (string/replace s #" " "_"))

(defn image? [m]
  (#{"image/png" "image/jpg" "image/jpeg" "image/gif"} m))
