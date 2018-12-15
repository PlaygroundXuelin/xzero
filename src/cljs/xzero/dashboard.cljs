(ns xzero.dashboard
  (:require [cljsjs.d3]
            [rid3.core :as rid3])
  )

(defn viz [ratom]
  [rid3/viz
   {:id    "some-id"
    :ratom ratom
    :svg   {:did-mount (fn [node _]
                         (-> node
                             (.attr "width" 200)
                             (.attr "height" 200)
                             (.style "border" "solid 1px grey")))}
    :pieces
           [{:kind      :elem
             :class     "backround"
             :tag       "circle"
             :did-mount (fn [node _]
                          (-> node
                              (.attr "cx" 100)
                              (.attr "cy" 100)
                              (.attr "r" 50)
                              (.attr "fill" "grey")))}
            ;; ATTENTION \/
            {:kind      :elem
             :class     "foreground"
             :tag       "text"
             :did-mount (fn [node _]
                          (-> node
                              (.attr "x" 100)
                              (.attr "y" 100)
                              (.attr "text-anchor" "middle")
                              (.attr "alignment-baseline" "middle")
                              (.attr "fill" "green")
                              (.attr "font-size" "24px")
                              (.attr "font-family" "sans-serif")
                              (.text "RID3")))}]
    }]
  )

(defn dashboard-page []
  ;  (js/console.log (renderToString (createElement "div" nil "Hello World!")))
  [:div.tile.is-ancestor
   [:div.tile.is-vertical.is-8
    [:div.tile
     [:div.tile.is-parent.is-vertical
      [:article.tile.is-child.notification.is-primary
       [:p.title "Vertical..."]
       [:p.subtitle "Top tile"]
       ]

      [:article.tile.is-child.notification.is-warning
       [:p.title "...tiles"]
       [:p.subtitle "Bottom tile"]
       ]
      ]
     [:div.tile.is-parent
      [:article.tile.is-child.notification.is-info
       [:p.title "Middle tile"]
       [:p.subtitle "With an image"]
       [:figure.image.is-4by3
        [:img {:src "https://bulma.io/images/placeholders/640x480.png"}]
        ]]
      ]
     ]
    [:div.tile.is-parent
     [:article.tile.is-child.notification.is-danger
      [:p.title "Wide tile"]
      [:p.subtitle "Aligned with the right tile"]
      [:div.content

       ]
      ]
     ]
    ]
   [:div.tile.is-parent
    [:article.tile.is-child.notification.is-success
     [:div.content
      [:p.title "Tall tile"]
      [:p.subtitle "With even more content"]
      [viz (atom [])]
      ]
     ]
    ]
   ]
  )
