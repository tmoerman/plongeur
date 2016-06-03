(ns plongeur.config)

(def default-config
  "Default configuration map. Configuration is a part of the application state."

  {:sigma {:settings {:verbose                            true
                      :immutable                          true
                      :defaultLabelColor                  "#999"
                      :defaultNodeColor                   "#FF9"

                      :nodeHoverBorderSize                2
                      :defaultLabelActiveColor            "#CCC"
                      :defaultNodeHoverBorderColor        "#fff"
                      :nodeActiveBorderSize               2
                      :nodeActiveOuterBorderSize          3
                      :defaultNodeActiveBorderColor       "#000"
                      :defaultNodeActiveOuterBorderColor  "#f33",
                      :edgeHoverExtremities               true
                      :nodeHaloColor                      "#99F"
                      :nodeHaloSize                       5}

           :props    {:force-layout        :force-atlas2
                      :force-layout-active true
                      :lasso-tool-active   false
                      :sync-interval-ms    30000 }}})


(defn validate-config
  [state]
  ; TODO implement tasteful validation logic for the config ~ core.spec
  )

#_{:nodeHoverBorderSize                2
:defaultNodeHoverBorderColor        "#fff"
:nodeActiveBorderSize               2
:nodeActiveOuterBorderSize          3
:defaultNodeActiveBorderColor       "#fff"
:defaultNodeActiveOuterBorderColor  "rgb(236, 81, 72)",
:enableEdgeHovering                 true
:edgeHoverExtremities               true}