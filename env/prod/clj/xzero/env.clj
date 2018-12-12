(ns xzero.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[xzero started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[xzero has shut down successfully]=-"))
   :middleware identity})
