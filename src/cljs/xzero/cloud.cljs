(ns xzero.cloud
  (:require
    [re-frame.core :as rf]
    [xzero.ui :as ui]
    [clojure.string :as str]
    [xzero.db :as db]
    [cljs.pprint :as pprint]
    )
  )

(defn cloud-page []
  (let [
        user @(rf/subscribe [:user])
        ]
    (if (db/hasPermission user [:page :cloud])

      [:div.section
       [:div.container
        (let [
              lst @(rf/subscribe [:cloud])
              ]
          [:div.container
           [:div "cloud details goes here"]
           ]
          )
        ]
       ]

      [:div.section
       [:div.container
        [:div "Permission denied"]
        ]
       ]
      )
    )
  )

