(ns user
  (:require [xzero.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [xzero.figwheel :refer [start-fw stop-fw cljs]]
            [xzero.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'xzero.core/repl-server))

(defn stop []
  (mount/stop-except #'xzero.core/repl-server))

(defn restart []
  (stop)
  (start))


