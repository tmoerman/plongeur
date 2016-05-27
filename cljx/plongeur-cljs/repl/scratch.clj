(ns scratch)


(def state-example
  {:config {:sigma    {:settings {:verbose   true
                                  :immutable true}},
            :defaults {:t-sne {:perplexity 1},
                       :viz   :tda,
                       :tda   {:force-layout :force-atlas2}},
            :ui       {:theme :dark}},

   :seq    20,

   :plots  {16 {:tda  {:force-layout :force-atlas2},
                :data nil},

            17 {:tda  {:force-layout :force-atlas2},
                :data nil},

            18 {:tda  {:force-layout :force-atlas2},
                :data nil},

            19 {:tda  {:force-layout :force-atlas2},
                :data nil}}})


(not (= {:seq 2,

         :plots {1 {nil {:plot :tda,
                         :tda {:force-layout :force-atlas2},
                         :t-sne {:perplexity 1}},
                    :data nil}},

         :config {:sigma {:settings {:verbose true,
                                     :immutable true}},
                  :defaults {:plot :tda,
                             :tda {:force-layout :force-atlas2},
                             :t-sne {:perplexity 1}},
                  :ui {:theme :dark}}}

        {:seq 2,

         :plots {1 {:force-layout :force-atlas2}},

         :config {:sigma {:settings {:verbose true,
                                     :immutable true}},
                  :defaults {:plot :tda,
                             :tda {:force-layout :force-atlas2},
                             :t-sne {:perplexity 1}},
                  :ui {:theme :dark}}}))