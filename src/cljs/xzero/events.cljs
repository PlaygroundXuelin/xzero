(ns xzero.events
  (:require [re-frame.core :as rf]
            [xzero.db :as db]
            [xzero.crypt :as crypt]
            [ajax.core :as ajax]))

;;dispatchers

(rf/reg-event-db
  :navigate
  (fn [db [_ route]]
    (assoc db :route route)))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :headers (db/authBearer db)
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

;;subscriptions

(rf/reg-sub
  :route
  (fn [db _]
    (-> db :route)))

(rf/reg-sub
  :page
  :<- [:route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))

(rf/reg-sub
  :cmd
  (fn [db _]
    (:cmd db)))

(rf/reg-sub
  :lst
  (fn [db _]
    (:lst db)))

(rf/reg-sub
  :user
  (fn [db _]
    (:user db)))

;
;
;

(rf/reg-event-fx
  :process-user-check-response
  []
  (fn [{:keys [db]} [_ response]]
    (let [bearer (:data response)]
      (if (clojure.string/blank? bearer)
        {:db (assoc db :user {:name name :bearer nil})}
        {:db (assoc db :user {:name name :bearer bearer})}
        ))))

(rf/reg-event-fx
  :initialize-user
  []
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :headers (db/authBearer db)
                  :uri             "/xzeros/user/check"
                  :params          {}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:process-user-check-response]
                  :on-failure      [:process-user-check-response]}
     }))

(rf/reg-event-fx
  :initialize-db
  []
  (fn [{:keys [db]} [_]]
    {:db db/default-db
     :dispatch [:initialize-user nil]}))

(rf/reg-event-db
 :update-value
 []
 (fn [db [_ value-path val]]
   (assoc-in db value-path val)))

(rf/reg-event-db
 :update-values
 []
 (fn [db [_ path-values]]
   (reduce (fn [db [path val]] (assoc-in db path val)) db (partition 2 path-values))))


(defn url-encode [ss] (js/encodeURIComponent ss))
(defn url-encode-map [mm]
  (let [nvs (into [] mm)
        encoded-nvs (map #(into [] (map url-encode %))
                         nvs)]
    (into {} (into [] encoded-nvs))))

(defn clj->json [ds] (.stringify js/JSON (clj->js ds)))
(defn clj->url-encoded-json [ds] (url-encode (clj->json ds)))

(defn process-response [db [_ path response]]
  (-> db
      (assoc-in (conj path :loading?) false)
      (assoc-in (conj path :result) response)))

(rf/reg-event-db
  :process-cmd-response
  []
  (fn [db [_ response]] (process-response db [_ [:cmd] response])))

(rf/reg-event-fx
 :execute-cmd
 []
 (fn [{:keys [db]} [_]]
   (println "before execute-cmd db is: " db)
   (let
     [cmd (:cmd db)
      cmd-type (or (:cmd-type cmd) "bash")
      script (or (get (:script cmd) cmd-type) "")
      params {"cmd-type" cmd-type "script" script}]
     {:http-xhrio {:method          :post
                   :headers (db/authBearer db)
                   :uri             "/xzeros/cmd/execute"
                   :format          (ajax/json-request-format)
                   :params          params
                   :response-format (ajax/json-response-format {:keywords? false})
                   :on-success      [:process-cmd-response]
                   :on-failure      [:process-cmd-response]}
      :db  (assoc-in db [:cmd :loading?] true)})))

(defn hash-auth [user nonce]
  (let [pw (:password user)
        ;        _ (assert (= 32 (count nonce)))
        salted-pw (str nonce pw)
        ]
    (-> salted-pw
        (crypt/str-to-byte-array true)
        (crypt/byte-array-to-hash256 true)
        (crypt/byte-array-to-hash256 true)
        (crypt/byte-array-to-hex true))
    )
  )

(defn clear-init [db] (assoc db :init {}))

(rf/reg-event-fx
  :process-login-response
  []
  (fn [{:keys [db]} [_ response]]
    (println "response is: " response)
    (let [bearer (:data response)
          _ (println "bearer is " bearer)
          db-error-login (assoc-in db [:user] (merge (:user db) {:error "Email address or password doesn't match"
                                                                          :bearer nil}))
          db-login (assoc-in db [:user] (merge (:user db) {:error "" :password ""  :bearer bearer}))]

      (if bearer
        {:db db-login
         ;         :dispatch [:user-get-data nil]
         }
        {:db db-error-login}))))

(rf/reg-event-fx
  :process-nonce-response
  []
  (fn [{:keys [db]} [_ response]]
    (let
      [nonce (:data response)
       user (get-in db [:user])
       ]
      {:http-xhrio {:method          :get
                    :uri             "/xzeros/user/login"
                    :params          {:name (:name user) :password (hash-auth user nonce)}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-login-response]
                    :on-failure      [:process-login-response]}
       }
      )))

(rf/reg-event-fx
  :login
  []
  (fn [{:keys [db]} [_]]
    (let
      [user (get-in db [:user])]
      {:http-xhrio {:method          :get
                    :uri             "/xzeros/user/nonce"
                    :params          {:name (:name user)}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-nonce-response]
                    :on-failure      [:process-nonce-response]}
       })))

(rf/reg-event-fx
  :user-logout
  []
  (fn [{:keys [db]} [_ auth-header]]
    (let
      [user (get-in db [:user])]
      {:http-xhrio {:method          :get
                    :headers auth-header
                    :uri             "/xzeros/user/logout"
                    :params          {:name (:name user)}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      []
                    :on-failure      []}
       })))

(rf/reg-event-fx
  :logout
  []
  (fn [{:keys [db]} [_]]
    (let
      [user (get-in db [:user])]
      {:db (assoc db :user (merge user {:name ""
                                                            :password ""
                                                            :error ""
                                                            :bearer nil}))
       :dispatch [:user-logout (db/authBearer db)]}
)))


(rf/reg-event-fx
  :process-load-lst-response
  []
  (fn [{:keys [db]} [_ lst-name]]
    (let
      [lst-data (:data response)
       error (:error response)
       ]
      (if error
        {:db (assoc-in db [:lst :error] error)}
        {:db (assoc-in (assoc db :error nil) [:lst :lsts lst-name] lst-data)}
        )
      )))

(rf/reg-event-fx
  :load-lst
  []
  (fn [{:keys [db]} [_ lst-name]]
    (let
      [lst (:lst db)
       lsts (:lsts lst)
       the-lst (get lsts lst-name)]
      (if (nil? the-lst)
        {:http-xhrio {:method          :get
                      :uri             "/xzeros/lst/getOrNew"
                      :params          {:name lst-name}
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [:process-load-lst-response]
                      :on-failure      [:process-load-lst-response]}
         }

        {:db db :dispatch [:get-lst lst-name]}
        )
      )))
