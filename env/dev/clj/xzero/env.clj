(ns xzero.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [xzero.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[xzero started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[xzero has shut down successfully]=-"))
   :middleware wrap-dev})
