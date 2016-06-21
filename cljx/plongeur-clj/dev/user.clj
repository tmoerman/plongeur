(defn dev
  "Usage:

  1. project folder terminal: `lein dive`
  2. connect nREPL to port 8666 (see project.clj)
  3. evaluate this namespace in the repl
  4. evaluate: `(dev)` in the repl"
  []
  (require 'dev)
  (in-ns 'dev))