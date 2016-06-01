(ns plongeur.config)

(def default-config
  "Default configuration map. Configuration is a part of the application state."

  {:sigma {:settings {:verbose           true
                      :immutable         false
                      :defaultLabelColor "#FFF"
                      :labelColor        "default"}

           :props    {:force-layout        :force-atlas2
                      :force-layout-active true
                      :lasso-tool-active   false
                      :sync-interval-ms    30000 }}})


(defn validate-config
  [state]
  ; TODO implement tasteful validation logic for the config ~ core.spec
  )