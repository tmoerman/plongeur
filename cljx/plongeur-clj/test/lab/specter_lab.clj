(ns lab.specter-lab
  (:use midje.sweet)
  (:use com.rpl.specter)
  (:use com.rpl.specter.macros))

(facts
  "about Specter"

  (facts
    "about setval"

    (setval [:bla] 2 {:bla 1}) => {:bla 2}

    (setval [:foo :gee] 2 {:foo {:gee 1}}) => {:foo {:gee 2}}



    )

  )
