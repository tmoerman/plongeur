(defproject plongeur-cljs "0.1.0-SNAPSHOT"

  :description "Plongeur frontend: Kierros/Cycle architecture."
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/sente "1.8.1"]
                 [quiescent "0.3.1"]
                 [sablono "0.6.3"]]
  
  :plugins [[lein-figwheel "0.5.2"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]
            [lein-npm "0.6.2"]]

  :hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/private/js/compiled"
                                    "target"]

  :cljsbuild {:builds {:dev {:source-paths ["src" "test"]
                             :figwheel {:on-jsload "plongeur-cljs.core/on-js-reload"}
                             :compiler {:main plongeur-cljs.core
                                        :asset-path "js/compiled/out"
                                        :output-to "resources/public/js/compiled/plongeur_cljs.js"
                                        :output-dir "resources/public/js/compiled/out"
                                        :source-map-timestamp true}}

                       ;:prod {:source-paths ["src"]
                       ;       :compiler {:output-to "main.js"
                       ;                  :source-map "main.js.map"
                       ;                  :optimizations :advanced
                       ;                  :pretty-print true}}

                       :test {:source-paths ["src" "test"]
                              :compiler {:output-to "resources/private/js/compiled/unit-test.js"
                                         :optimizations :whitespace
                                         :pretty-print true}}}

              :test-commands {"unit" ["phantomjs"
                                      "resources/private/js/compiled/unit-test.js"]}}

  :npm {:dependencies [[sigma "1.1.0"]] ; installed with npm
        }

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [figwheel-sidecar        "0.5.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :figwheel {:css-dirs ["resources/public/css"] ;; watch and update CSS
             :nrepl-port 7888})
