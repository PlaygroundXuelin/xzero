(ns xzero.lst
  (:require
    [re-frame.core :as rf]
    [xzero.ui :as ui]
    [xzero.crypt :as crypt]
    [clojure.string :as str]
    [xzero.db :as db]
    [cljs.pprint :as pprint]
    [xzero.fmt :refer [to-csv to-text]]
    )
  )

(def account-lst-name "account")

(defn pm-editable-row [idx item]
  [:div.row
   [:div.col-md-8
    [ui/textarea-input :update-item [[account-lst-name idx] ] item false {:rows 3 :cols 80}]
    ]])

(defn pm-readonly-row [idx item]
  [:div.row
   [:div.col-md-8
    [:pre {:on-click #(rf/dispatch [:update-value [:lst :editing-id] idx])} item]]
   ])

(defn pm-row [idx item editing]
  (if editing
    [pm-editable-row idx item]
    [pm-readonly-row idx item]))

(defn lst-page []
  (let [
        user @(rf/subscribe [:user])
        ]
    (if (db/hasPermission user [:page :lst])

      [:div.section
       [:div.container
        (let [
              lst @(rf/subscribe [:lst])
              current-lst-name account-lst-name

              new-item-value (:new-item lst)
              lsts (:lsts lst)
              current-lst (get lsts current-lst-name)
              items (:items current-lst)
              editing-id (:editing-id lst)
              output (if-let [loading? (:loading? lst)] "loading..." (str "What?"))

              buttons-row [:div.row
                           [:div.col-md-2
                            [:button.btn.btn-default.btn-sm
                             {:on-click #(ui/new-window "" (to-csv items (fn [item] [item]) ["account"])) :type "button" } "Export to CSV"]
                            [:button.btn.btn-default.btn-sm
                            {:on-click #(ui/new-window "" (to-text items)) :type "button" } "Export as text"]
                            [:button.btn.btn-default.btn-sm
                             {:on-click  #(rf/dispatch [:lst-import-text account-lst-name])   :type "button" } "Import from text"]
                            ]
                           ]
              add-row [:div.row
                       [:div.col-md-2
                        [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:lst-add-item account-lst-name]) :type "button" } "Add New Item"]
                        ]

                       [:div.col-md-8 [ui/textarea-input :update-value [[:lst :new-item]] new-item-value false {:rows 3 :cols 80}]]
                       ]
              filter-str (or (:filter lst) "")
              filter-row [:div.row>div.col-md-12 "Filter: "
                          [ui/text-input :update-value [[:lst :filter]] "text" filter-str true nil]
                          [:button.btn.btn-default.btn-sm
                            {:disabled @(rf/subscribe [:lst-not-editing?]) :on-click #(rf/dispatch [:lst-delete-item account-lst-name]) :type "button" } "Delete"]
                          ]
              index-items                                 (map-indexed
                                                          (fn [idx item] [idx item])
                                                          items
                                                          )

              filtered-items (if (clojure.string/blank? filter-str)
                               index-items
                              (filter
                                (fn [[idx item]]
                                  (nat-int? (.indexOf (.toLowerCase item) (.toLowerCase (.trim filter-str))))
                                  )
                                index-items
                                ))
              rows (map
                     (fn [[idx item]]
                       [pm-row idx item (= idx editing-id)])
                     (sort-by second compare filtered-items)
                     )
              ]
                    (into [] (concat [:div.container buttons-row add-row [:hr] filter-row] rows))
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

