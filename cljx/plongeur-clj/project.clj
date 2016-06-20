(defproject plongeur-clj "0.1.0-SNAPSHOT"

  :description "Plongeur backend - System/Cycle architecture."

  :url "https://github.com/tmoerman/plongeur"

  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}

  :repositories {"local" "file:/Users/tmo/.m2"}

  :source-paths ["src"]
  :test-paths   ["test"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha5"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.netflix.rxjava/rxjava-clojure "0.20.7"]
                 [com.rpl/specter "0.11.2"]

                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0" :exclusions [[javax.servlet/*]]]
                 [compojure "1.5.0"]
                 [clojurewerkz/route-one "1.2.0"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [com.taoensso/sente "1.8.1"]
                 [com.taoensso/timbre "4.3.1"]

                 [environ "1.0.2"]

                 [gorillalabs/sparkling "1.2.5"]

                 [t6/from-scala "0.3.0"]
                 [org.tmoerman/plongeur-spark_2.10 "0.2.2"]

                 ]

  :plugins [[lein-environ "1.0.2"]]

  :profiles {:uberjar {:aot :all}

             :provided {:dependencies [[org.apache.spark/spark-core_2.10 "1.6.1"]]}

             :dev     {:source-paths ["dev"]
                       :repl-options {:port 8666}
                       :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      [org.clojure/java.classpath "0.2.3"]
                                      [org.clojure/tools.nrepl "0.2.11"]
                                      [midje "1.9.0-alpha2"]
                                      [aprint "0.1.3"]
                                      [org.clojure/tools.trace "0.7.9"]]
                       :plugins [[lein-midje "3.2"]
                                 [lein-kibit "0.1.2"]]
                       :main user }}

  :aliases {"dive" ["with-profile" "+dev" "repl"]}

  )