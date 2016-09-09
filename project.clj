(defproject instaparse "1.4.2"
  :description "Instaparse-CLr: No grammar left behind"
  :url "https://github.com/yiglas/instaparse-clr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :target-framework "net45"
  :dependencies [[Clojure "1.8.0"]]
  :profiles {:1.5 {:dependencies [[Clojure "1.5.0"]]}
             :1.6 {:dependencies [[Clojure "1.6.0"]]}
             :1.7 {:dependencies [[Clojure "1.7.0"]]}
             :1.8 {:dependencies [[Clojure "1.8.0"]]}}
  :aliases {"test-all" ["with-profile" "+1.5:+1.6:+1.7:+1.8" "test"]}
  :test-paths ["test"]
  :target-path "target"
  :scm {:name "git"
        :url "https://github.com/yiglas/instaparse-clr"}
        )
