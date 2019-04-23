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

(defn pm-editable-row [item]
  [:div.row
   [:div.col-md-4
    [ui/text-input :pm-update-row [[nil (:id item) :name]] "text" (:name item) false nil]]
   [:div.col-md-8
    [ui/text-input :pm-update-row [[nil (:id item) :value]] "text" (:value item) false nil]]])

(defn pm-readonly-row [item]
  [:div.row
   [:div.col-md-4
    [:span {:on-click #(rf/dispatch [:update-value [:pm :data :editing-id] (:id item)])} (:name item)]]
   [:div.col-md-8
    [:span {:on-click #(rf/dispatch [:update-value [:pm :data :editing-id] (:id item)])} (:value item)]]])

(defn pm-row [item editing]
  (if editing
    [pm-editable-row item]
    [pm-readonly-row item]))


(defn lst-page []
  (let [
        user @(rf/subscribe [:user])
        ]
    (if (db/hasPermission user [:page :lst])

      [:div.section
       [:div.container
        (let [
              lst @(rf/subscribe [:lst])
              current-lst-id "accounts"

              new-row-name (get-in lst [:new-row :name])
              new-row-value (get-in lst [:new-row :value])
              lsts (:lsts lst)
              current-lst (get lsts current-lst-id)
              editing-id (:editing-id lst)
              output (if-let [loading? (:loading? lst)] "loading..." (str "What?"))
              set-item (fn [text] (rf/dispatch [:update-value [:lst :lsts current-lst-id editing-id] text]))

              add-row [:div.row
                       [:div.col-md-1 "Name: "]
                       [:div.col-md-5
                          [ui/text-input :update-value [[:lst :new-row :name]] "text" new-row-name false {:size 30}]
                        [:br]
                        [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:lst-add-item nil]) :type "button" } "Add New Item"]
                        [:br]
                        [:button.btn.btn-default.btn-sm
                         {:on-click #(new-window "" (to-csv current-lst [:id :name :value] name)) :type "button" } "Export"]

                        ]
                       [:div.col-md-1 "Value: "]
                       [:div.col-md-5 [ui/textarea-input :update-value [[:lst :new-row :value]] new-row-value {:rows 3 :cols 30}]]
                       ]
              filter-str (or (:filter lst) "")
              filter-row [:div.row>div.col-md-12 "Filter: "
                          [ui/text-input :update-value [[:lst :filter]] "text" filter-str true nil]]
              filtered-list (if (clojure.string/blank? filter-str)
                              current-lst
                              (filter (fn [nv]
                                        (some
                                          true?
                                          (map
                                            #(nat-int? (.indexOf (.toLowerCase %) (.toLowerCase (.trim filter-str))))
                                            [(:name nv) (:value nv)])))
                                      current-lst))
              rows (map
                     (fn [item]
                       [pm-row item (= (:id item) editing-id)])
                     (sort-by :name filtered-list))
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

