(ns xzero.lst
  (:require
    [re-frame.core :as rf]
    [xzero.ui :as ui]
    [clojure.string :as str]
    [xzero.db :as db]
    [cljs.pprint :as pprint]
    [xzero.utils :refer [new-window to-csv]]
    )
  )

(def account-lst-name "account")

(defn pm-editable-row [idx item]
  [:div.row
   [:div.col-md-8
    [ui/text-input :update-item [[account-lst-name idx] ] "text" item false nil]
    ]])

(defn pm-readonly-row [idx item]
  [:div.row
   [:div.col-md-8
    [:span {:on-click #(rf/dispatch [:update-value [:lst :editing-id] idx])} item]]])

(defn pm-row [idx item editing]
  (if editing
    [pm-editable-row idx item]
    [pm-readonly-row idx item]))


(defn to-name-val [nv]
  (if (clojure.string/blank? nv)
    ["" ""]
    (let [index (.indexOf nv ":")]
      (if (neg? index)
        [nv ""]
        [(.substring nv 0 index) (.substring nv (inc index))]
        )
      )
    )
  )

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

              new-row-value (:new-row lst)
              lsts (:lsts lst)
              current-lst (get lsts current-lst-name)
              items (:items current-lst)
              editing-id (:editing-id lst)
              output (if-let [loading? (:loading? lst)] "loading..." (str "What?"))
              set-item (fn [text] (rf/dispatch [:update-value [:lst :lsts current-lst-name editing-id] text]))

              add-row [:div.row
                       [:div.col-md-2
                        [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:lst-add-item account-lst-name]) :type "button" } "Add New Item"]
                        [:br]
                        [:button.btn.btn-default.btn-sm
                         {:on-click #(new-window "" (to-csv current-lst [:id :name :value] name)) :type "button" } "Export"]
                        ]

                       [:div.col-md-8 [ui/textarea-input :update-value [[:lst :new-row]] new-row-value {:rows 3 :cols 30}]]
                       ]
              filter-str (or (:filter lst) "")
              filter-row [:div.row>div.col-md-12 "Filter: "
                          [ui/text-input :update-value [[:lst :filter]] "text" filter-str true nil]]
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
                     (sort-by (fn [x y] (compare (second x) (second y))) filtered-items)
                     )
              ]
                    (into [] (concat [:div.container add-row [:hr] filter-row] rows))
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

