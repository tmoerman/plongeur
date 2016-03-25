(defproject plongeur-vdom "0.1.0-SNAPSHOT"

  :description "Plongeur reactive frontend."

  :min-lein-version "2.5.3"
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]

                 [com.2tothe8th/dominator "0.4.0"]
                 [jamesmacaulay/zelkova   "0.4.0"]
                 [stch-library/html       "0.1.2"]
                 [org.clojure/core.match  "0.2.2"]

                 ;[cljsjs/virtual-dom      "2.1.1-0"]
                 ]

  :plugins [[lein-figwheel "0.5.1"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src" "script"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/private/js/compiled"
                                    "target"]

  :cljsbuild {:builds {:dev {:source-paths ["src"]

                             ;; If no code is to be run, set :figwheel true for continued automagical reloading
                             :figwheel {:on-jsload "plongeur-vdom.core/on-js-reload"}

                             :compiler {:main plongeur-vdom.core
                                        :externs ["externs/dominator.js"]
                                        :asset-path "js/compiled/out"
                                        :output-to "resources/public/js/compiled/plongeur_vdom.js"
                                        :output-dir "resources/public/js/compiled/out"
                                        :source-map-timestamp true}}

                       ;; compressed minified build for production: `lein cljsbuild once min`
                       :min {:source-paths ["src"]
                             :compiler {:output-to "resources/public/js/compiled/plongeur_vdom.js"
                                        :main plongeur-vdom.core
                                        :externs ["externs/dominator.js"]
                                        :optimizations :advanced
                                        :pretty-print false}}

                       :test {:source-paths ["src" "test"]
                              :compiler {;:externs ["externs/dominator.js"]
                                         :output-to "resources/private/js/compiled/unit-test.js"
                                         :optimizations :whitespace
                                         :preamble ["public/js/vdom.js"]
                                         :pretty-print true}}}

              :test-commands {"unit" ["phantomjs"
                                      "resources/private/js/compiled/unit-test.js"]}}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [figwheel-sidecar        "0.5.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :figwheel {:css-dirs ["resources/public/css"] ;; watch and update CSS
             :nrepl-port 7888} ;; Start an nREPL server into the running figwheel process
  )
