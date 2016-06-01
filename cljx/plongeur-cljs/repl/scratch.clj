(ns scratch)

{:seq 4,
 :plots {2 {:type :tda,
            :props {:force-layout :force-atlas2,
                    :force-layout-active true},
            :data nil},

         3 {:type :tda,
            :props {:force-layout :force-atlas2,
                    :force-layout-active true},
            :data nil}},

 :config {:sigma {:settings {:verbose true,
                             :immutable false,
                             :defaultLabelColor "#FFF",
                             :labelColor "default"}},

          :defaults {:plot :tda,
                     :tda {:force-layout :force-atlas2, :force-layout-active true},
                     :t-sne {:perplexity 1}},

          :ui {:theme :dark}},

 :transient {:launched #inst "2016-05-31T15:10:56.676-00:00"}}


