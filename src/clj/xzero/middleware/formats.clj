(ns xzero.middleware.formats
  (:require
            [muuntaja.core :as m])
  (:import [com.fasterxml.jackson.datatype.jdk8 Jdk8Module]
           ))

(def instance
  (m/create
    (-> m/default-options
        (assoc-in
          [:formats "application/json" :opts :modules]
          [(Jdk8Module.)])
        )))
