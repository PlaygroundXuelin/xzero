(ns xzero.fmt
  (:require [clojure.string])
  )

(defn- to-csv-str [ss0 check-quotes]
  (if (nil? ss0) ""
                 (let [ss (str ss0)
                       has-quotes (clojure.string/includes? ss "\"")]
                   (if (and check-quotes (not has-quotes))
                     ss
                     (str "\"" (clojure.string/replace ss "\"" "\"\"") "\"")
                     )
                   )
                 )
  )

(defn to-csv [lst vals-fn headers]
  (let [headers-row (clojure.string/join "," (map #(to-csv-str % true) headers))
        rows
        (map
          (fn [item]
            (clojure.string/join "," (map #(to-csv-str % true) (vals-fn item)))
            )
          lst
          )
        ]
    (str headers-row "\n" (clojure.string/join "\n" rows))
    )
  )

(def text-sep "\n\n== ==\n\n")

(defn to-text [lst]
  (clojure.string/join text-sep lst)
  )

(defn from-text [txt]
  (clojure.string/split txt text-sep)
  )
