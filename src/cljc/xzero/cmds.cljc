(ns xzero.cmds)

(def cmds
  {"bash" ["/bin/bash" "-c"]
   "clojure" nil
   })

(def cmd-types (keys cmds))