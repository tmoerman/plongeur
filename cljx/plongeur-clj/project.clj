(defproject plongeur-clj "0.1.0-SNAPSHOT"

  :description "Plongeur backend - System/Cycle architecture."

  :url "https://github.com/tmoerman/plongeur"

  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}

  :repositories {"local" "file:/Users/tmo/.m2"}

  :source-paths ["src"]
  :test-paths   ["test"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.netflix.rxjava/rxjava-clojure "0.20.7"]
                 [com.rpl/specter "0.10.0"]

                 [ring "1.4.0"]
                 [compojure "1.5.0"]
                 [clojurewerkz/route-one "1.2.0"]
                 [http-kit "2.1.18"]
                 [com.taoensso/sente "1.8.1"]
                 [com.taoensso/timbre "4.3.1"]

                 [environ "1.0.2"]

                 ;[t6/from-scala "0.2.1"]
                 ;[org.scala-lang/scala-library "2.11.6"]
                 ;[org.tmoerman/plongeur-spark_2.10 "0.2.1"]

                 ]

  :plugins [[lein-environ "1.0.2"]]

  :profiles {:uberjar {:aot :all}

             :dev     {:source-paths ["dev"]
                       :repl-options {:port 8666}
                       :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      [org.clojure/java.classpath "0.2.3"]
                                      [org.clojure/tools.nrepl "0.2.11"]
                                      [midje "1.8.3"]
                                      [aprint "0.1.3"]
                                      [org.clojure/tools.trace "0.7.9"]]
                       :plugins [[lein-midje "3.2"]
                                 [lein-kibit "0.1.2"]]
                       :main user }}



  )