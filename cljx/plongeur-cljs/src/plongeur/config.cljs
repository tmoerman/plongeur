(ns plongeur.config)

(def default-config
  "Default configuration map. Configuration is a part of the application state."

  {:sigma    {:lib      ;; specifies which sigma variant is used.

              :sigma ; [:sigma, :linkurious]

              :settings ;; gets turned into json and passed to the sigma constructor.

              {:verbose true}

              }

   :defaults {:viz      :tda ; [:tda :t-sne],

              :tda      ;; defaults for TDA visualizations

                        {:force-layout :force-atlas2 ; [:force-atlas2, :fruchterman-reingold, ...]

                         }
              :t-sne    ;; defaults for TSNA visualizations
                        {:perplexity 1

                         }
              }

   :ui       {:theme    ;; specifies the dark or light theme

              :dark  ; [:dark, :light]
              }
   }

  )


(defn validate-config
  [state]
  ; TODO implement tasteful validation logic for the config
  )