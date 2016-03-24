(defproject plongeur-serve "0.1.0-SNAPSHOT"

  :description "Plongeur server subsystem"

  ;; see Sente example project https://github.com/ptaoussanis/sente/blob/master/example-project/project.clj

  :dependencies [[org.clojure/clojure     "1.8.0"]
                 [org.clojure/core.async  "0.2.374"]
                 [org.clojure/tools.nrepl "0.2.12"]

                 [com.taoensso/sente      "1.8.1"]
                 [com.taoensso/timbre     "4.3.1"]

                 [http-kit                "2.2.0-alpha1"]

                 [ring                    "1.4.0"]
                 [ring/ring-defaults      "0.2.0"]
                 [compojure               "1.5.0"]

                 ;; transit for speed


                 ]


  :plugins [[lein-pprint         "1.1.2"]
            [lein-ancient        "0.6.8"]
            [com.cemerick/austin "0.1.6"]
            [lein-cljsbuild      "1.1.3"]
            [cider/cider-nrepl   "0.11.0"] ; Optional, for use with Emacs
            ]

                 ])
