(defproject cse341 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [com.cemerick/friend "0.2.0"]
                 [congomongo "0.4.1"]
                 [enlive "1.1.5"]
                 [markdown-clj "0.9.41"]
                 [slugger "1.0.1"]
                 [formative "0.8.8"]
                 [me.raynes/fs "1.4.4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler cse341.handler/app :nrepl {:start? true}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
