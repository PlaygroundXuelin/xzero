(ns xzero.core
  (:require [baking-soda.core :as b]
            [day8.re-frame.http-fx]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [xzero.ajax :as ajax]
            [xzero.events]
            [xzero.dashboard]
            [xzero.cmd :as cmd]
            [xzero.lst :as lst]
            [xzero.cloud :as cloud]
            [xzero.db :as db]
            [xzero.async]
            [xzero.user :as user]
            [reitit.core :as reitit]
            [clojure.string :as string])
  (:import goog.History))

; the navbar components are implemented via baking-soda [1]
; library that provides a ClojureScript interface for Reactstrap [2]
; Bootstrap 4 components.
; [1] https://github.com/gadfly361/baking-soda
; [2] http://reactstrap.github.io/

(defn nav-link [uri title page]
  [b/NavItem
   [b/NavLink
    {:href   uri
     :active (when (= page @(rf/subscribe [:page])) "active")}
    title]])

(defn user-link [login?]
  [nav-link "#/user" "User" :user]
  )

(defn navbar []
  (let [user @(rf/subscribe [:user])
        login? (:bearer user)]
    (r/with-let [expanded? (r/atom true)]
                [b/Navbar {:light true
                           :class-name "navbar-dark bg-primary"
                           :expand "md"}
                 ;                                  [b/NavbarBrand {:href "/"} "xzero"]
                 [b/NavbarToggler {:on-click #(swap! expanded? not)}]
                 [b/Collapse {:is-open @expanded? :navbar true}
                  [b/Nav {:class-name "mr-auto" :navbar true}
                   (if (db/hasPermission user [:page :cmd])
                     [nav-link "#/cmd" "Command" :cmd]
                     )
                   (if (db/hasPermission user [:page :lst])
                     [nav-link "#/lst" "List" :lst]
                     )
                   (if (db/hasPermission user [:page :cloud])
                     [nav-link "#/cloud" "Cloud" :cloud]
                     )
                   [nav-link "#/dashboard" "Dashboard" :dashboard]
                   ;                   [nav-link "#/home" "Docs" :home]
                    [user-link login?]
                   ]]])
    )
  )

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(def pages
  {:home #'home-page
   :cmd #'cmd/cmd-page
   :lst #'lst/lst-page
   :cloud #'cloud/cloud-page
   :dashboard #'xzero.dashboard/dashboard-page
   :user #'user/user-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]
     ["/cmd" :cmd]
     ["/lst" :lst]
     ["/cloud" :cloud]
     ["/dashboard" :dashboard]
     ["/user" :user]]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
          (rf/dispatch
            [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])

  (ajax/load-interceptors!)

  (xzero.async/bg-task 300000 300000
                       (fn [cnt ms] ms)
                       (fn [] (rf/dispatch [:check-user nil])))

  (rf/dispatch [:fetch-docs])
  (hook-browser-navigation!)
  (mount-components))
