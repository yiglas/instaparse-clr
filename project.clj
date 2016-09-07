(defproject instaparse "1.4.2"
  :description "Instaparse-CLr: No grammar left behind"
  :url "https://github.com/yiglas/instaparse-clr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :profiles {:dev {:dependencies 
                   [[org.clojure/tools.trace "0.7.5"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7" "test"]}
  :test-paths ["test"]
  :target-path "target"
  :scm {:name "git"
        :url "https://github.com/yiglas/instaparse-clr"})
