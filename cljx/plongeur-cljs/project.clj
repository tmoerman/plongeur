(defproject plongeur-cljs "0.1.0-SNAPSHOT"

  :description "Plongeur frontend: Kierros/Cycle architecture."
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [com.taoensso/sente "1.8.1"]
                 [quiescent "0.3.1"]
                 [sablono "0.6.3"]
                 [cljsjs/material "1.1.3-1"]
                 [com.rpl/specter "0.11.2"]
                 [prismatic/dommy "1.1.0"]]
  
  :plugins [[lein-figwheel "0.5.4-4"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]
            [lein-npm "0.6.2"]]

  :hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false} [;"resources/public/js/compiled"
                                    ;"resources/public/js/node_modules"
                                    ;"resources/private/js/compiled"
                                    "target"]

  :cljsbuild {:builds {:dev {:source-paths ["src" "test"]
                             :figwheel {:on-jsload "plongeur.system/on-js-reload"}
                             :compiler {:main plongeur.core
                                        :asset-path "js/compiled/out"
                                        :output-to "resources/public/js/compiled/plongeur.js"
                                        :output-dir "resources/public/js/compiled/out"
                                        :source-map-timestamp true
                                        :foreign-libs [{:file     "resources/public/js/node_modules/linkurious/dist/sigma.js"
                                                        :file-min "resources/public/js/node_modules/linkurious/dist/sigma.min.js"
                                                        :provides ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.helpers.graph.min.js"
                                                        :provides ["foreign.graph"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.fullScreen.min.js"
                                                        :provides ["foreign.fullscreen"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.keyboard.min.js"
                                                        :provides ["foreign.keyboard"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/patch/linkurious/plugins/sigma.plugins.activeState/sigma.plugins.customActiveState.js"
                                                        :provides ["foreign.activestate"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.dragNodes.min.js"
                                                        :provides ["foreign.dragnodes"]
                                                        :requires ["foreign.activestate"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.select.min.js"
                                                        :provides ["foreign.select"]
                                                        :requires ["foreign.activestate" "foreign.graph"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.lasso.min.js"
                                                        :provides ["foreign.lasso"]
                                                        :requires ["foreign.activestate"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.layouts.forceAtlas2.min.js"
                                                        :provides ["foreign.forceatlas2"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.renderers.halo.min.js"
                                                        :provides ["foreign.halo"]
                                                        :requires ["foreign.sigma"]}

                                                       {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.renderers.linkurious.min.js"
                                                        :provides ["foreign.linkurious"]
                                                        :requires ["foreign.sigma"]}]

                                        }}

                       :test {:source-paths ["src" "test"]
                              :compiler {:output-to "resources/private/js/compiled/unit-test.js"
                                         :pretty-print true
                                         :foreign-libs [{:file     "resources/public/js/node_modules/linkurious/dist/sigma.js"
                                                         :file-min "resources/public/js/node_modules/linkurious/dist/sigma.min.js"
                                                         :provides ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.helpers.graph.min.js"
                                                         :provides ["foreign.graph"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.fullScreen.min.js"
                                                         :provides ["foreign.fullscreen"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.keyboard.min.js"
                                                         :provides ["foreign.keyboard"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/patch/linkurious/plugins/sigma.plugins.activeState/sigma.plugins.customActiveState.js"
                                                         :provides ["foreign.activestate"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.dragNodes.min.js"
                                                         :provides ["foreign.dragnodes"]
                                                         :requires ["foreign.activestate"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.select.min.js"
                                                         :provides ["foreign.select"]
                                                         :requires ["foreign.activestate" "foreign.graph"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.plugins.lasso.min.js"
                                                         :provides ["foreign.lasso"]
                                                         :requires ["foreign.activestate"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.layouts.forceAtlas2.min.js"
                                                         :provides ["foreign.forceatlas2"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.renderers.halo.min.js"
                                                         :provides ["foreign.halo"]
                                                         :requires ["foreign.sigma"]}

                                                        {:file     "resources/public/js/node_modules/linkurious/dist/plugins/sigma.renderers.linkurious.min.js"
                                                         :provides ["foreign.linkurious"]
                                                         :requires ["foreign.sigma"]}]

                                         }}

                       ;:prod {:source-paths ["src"]
                       ;       :compiler {:output-to "main.js"
                       ;                  :source-map "main.js.map"
                       ;                  :optimizations :advanced
                       ;                  :pretty-print true}}

                       }

              :test-commands {"unit" ["phantomjs"
                                      "resources/private/js/compiled/unit-test.js"]}}

  ;; lein clean
  ;; lein npm install
  :npm {:dependencies [[linkurious "1.5.1"]]
        :root "resources/public/js"}

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [figwheel-sidecar        "0.5.1"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :figwheel {:css-dirs ["resources/public/css"] ;; watch and update CSS
             :nrepl-port 7888}

  )