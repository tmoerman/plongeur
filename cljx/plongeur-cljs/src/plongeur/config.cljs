(ns plongeur.config)

(def default-config
  "Default configuration map. Configuration is a part of the application state."

  {:sigma    {:settings {:verbose true ;; gets turned into json and passed to the sigma constructor.
                         }

              }

   :defaults {:plot     :tda ; [:tda :t-sne],

              :tda      {:force-layout :force-atlas2 ; [:force-atlas2, :fruchterman-reingold, ...]
                         }

              :t-sne    {:perplexity 1 ;; defaults for TSNA visualizations
                         }
              }

   :ui       {:theme    :dark  ; [:dark, :light]
              }

   })


(defn validate-config
  [state]
  ; TODO implement tasteful validation logic for the config
  )