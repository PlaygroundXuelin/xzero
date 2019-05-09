(ns xzero.async
  (:use-macros [cljs.core.async.macros :only [go]])
  (:require [cljs.core.async :refer [<! timeout]])
  )

(defn bg-task [delay-ms freq-ms stop-fn f & args]
  (let [cnt (atom 0)]
    (go
      (loop [timeout-ms delay-ms]
        (if (pos? timeout-ms)
          (do
            (<! (timeout timeout-ms))
            (swap! cnt inc)
            (apply f args)
            (let [next-timeout-ms (stop-fn @cnt freq-ms)]
              (if (pos? next-timeout-ms) (recur next-timeout-ms))
              )
            )
          )
        )
      )
    )
  )
