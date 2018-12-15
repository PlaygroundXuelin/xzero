(ns xzero.cmds)

(def cmds
  {"bash" ["/bin/bash" "-c"]
   "clojure" nil
   "python3" ["/Users/xuelinwang/anaconda3/bin/python3" "-c"]
   })

(def cmd-types (keys cmds))