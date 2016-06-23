(ns ^:figwheel-always all.test-runner
  (:require [kierros.async-test]
            [kierros.core-test]
            [kierros.model-test]
            [kierros.util-test]

            [lab.async-lab]

            [plongeur.model-test]
            [plongeur.sigma-test]
            [plongeur.view-test]

            [cljs.test :refer-macros [run-tests]]))

; Run both builds simultaneously!
; rlwrap lein figwheel dev test

(run-tests
           'kierros.core-test
           'kierros.async-test
           'kierros.model-test
           'kierros.util-test

           'lab.async-lab

           'plongeur.model-test
           ;'plongeur.sigma-test TODO fix me
           'plongeur.view-test
           )