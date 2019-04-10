(ns xzero.user
  (:require
    [re-frame.core :as rf]
    [xzero.ui :as ui]))

(defn user-page []
  (let [user @(rf/subscribe [:user])]
    [:div.container
     [:div.row [:div.col-md-2 "Email address: "]
      [:div.col-md-6 [ui/text-input :update-value [[:user :email]] "text" (:email user) false nil]]]

     [:div.row
      [:div.col-md-2 "Password: "]
      [:div.col-md-6 [ui/text-input :update-value [[:user :password]] "password" (:password user) false nil]]
      ]
     [:div.row
      [:div.col-md-2]
      [:div.col-md-6
       [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:login]) :type "button" } "Login"]]]
     [:div.row
      [:div.col-md-12
       [:img {:src "/img/warning_clojure.png"}]]]
     ]
    )
  )
