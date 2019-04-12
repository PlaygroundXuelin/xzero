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
            [xzero.db :as db]
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
        login? (:login user)]
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
                   [nav-link "#/dashboard" "Dashboard" :dashboard]
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
  (rf/dispatch [:fetch-docs])
  (hook-browser-navigation!)
  (mount-components))
