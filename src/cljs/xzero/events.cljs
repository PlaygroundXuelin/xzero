(ns xzero.events
  (:require [re-frame.core :as rf]
            [xzero.db :as db]
            [xzero.fmt :as utils]
            [xzero.crypt :as crypt]
            [ajax.core :as ajax]))

(defn encrypt [vals k]
  (let [aes (crypt/new-aes k)]
    (mapv #(crypt/byte-array-to-hex (crypt/aes-encrypt-str aes % true) true) vals)))

(defn encrypt-item [val k]
  (first (encrypt [val] k))
  )

(defn decrypt [vals k]
  (let [aes (crypt/new-aes k)]
    (mapv
      #(.trim (crypt/byte-array-to-str (crypt/aes-decrypt-bytes aes (crypt/hex-to-byte-array % true) true) true))
      vals)))

(defn decrypt-item [val k]
  (first (decrypt [val] k))
  )

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
  :lst-not-editing?
  (fn [db _]
    (nil? (get-in db [:lst :editing-id]))))

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
        {:db (assoc db :user {:name name :bearer nil} :lst {})}
        {:db (assoc db :user {:name name :bearer bearer})}
        ))))

(rf/reg-event-fx
  :check-user
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
    {:db db/default-db
     :dispatch [:check-user nil]}))

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

(defn hash-auth-local [user nonce]
  (let [pw (:password user)
        ;        _ (assert (= 32 (count nonce)))
        salted-pw (str pw nonce)
        ]
    (-> salted-pw
        (crypt/str-to-byte-array true)
        (crypt/byte-array-to-hash256 true)
        (crypt/byte-array-to-hash256 true)
        (crypt/byte-array-to-hex true))
    )
  )

(defn encrypt-client [vals user]
  (let [k (:local-hash user)]
    (encrypt vals k)
    )
  )

(defn encrypt-item-client [val user]
  (first (encrypt-client [val] user))
  )

(defn decrypt-client [vals user]
  (let [k (:local-hash user)]
    (decrypt vals k)
    )
  )

(defn decrypt-item-client [val user]
  (first (decrypt-client [val] user))
  )

(defn clear-init [db] (assoc db :init {}))

(rf/reg-event-fx
  :process-login-response
  []
  (fn [{:keys [db]} [_ nonce response]]
    (let [bearer (:data response)
          user (get-in db [:user])
          db-error-login (assoc-in db [:user] (merge (:user db) {:error "Email address or password doesn't match"
                                                                          :bearer nil}))
          db-login (assoc-in db [:user] (merge (:user db) {:error "" :password ""  :local-hash (hash-auth-local user nonce) :bearer bearer}))]

      (if bearer
        {:db db-login
          :dispatch [:load-user-data nil]
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
                    :on-success      [:process-login-response nonce]
                    :on-failure      [:process-login-response nonce]}
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
  (fn [{:keys [db]} [_ lst-name response]]
    (let
      [{:keys [lst-id items] :as lst-data} (:data response)
       error (:error response)
       ]
      (if error
        {:db (assoc-in db [:lst :error] error)}
        {:db (assoc-in (assoc db :error nil) [:lst :lsts lst-name] {:list-id lst-id :items (decrypt-client items (:user db))})}
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
      {:http-xhrio {:method          :post
                    :uri             "/xzeros/lst/getOrNew"
                    :headers         (db/authBearer db)
                    :format          (ajax/json-request-format)
                    :params          {:name lst-name}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-load-lst-response lst-name]
                    :on-failure      [:process-load-lst-response lst-name]}
       }
      )))

(rf/reg-event-fx
  :load-user-data
  []
  (fn [{:keys [db]} [_]]
    {:db db :dispatch [:load-lst "account"]}
    ))

(rf/reg-event-fx
  :process-lst-add-item-response
  []
  (fn [{:keys [db]} [_ lst-name response]]
    (let
      [lst-id (:data response)
       error (:error response)
       ]
      (if error
        {:db (assoc-in db [:lst :error] error)}
        (let [curr-items (get-in db [:lst :lsts lst-name :items])
              new-item (get-in db [:lst :new-item])]
          {:db (assoc-in (assoc db :error nil) [:lst :lsts lst-name :items] (into curr-items [new-item]))}
          )
        )
      )))

(rf/reg-event-fx
  :lst-add-item
  []
  (fn [{:keys [db]} [_ lst-name]]
    (let [new-item (encrypt-item-client (get-in db [:lst :new-item]) (:user db))]
      {:http-xhrio {:method          :post
                    :headers         (db/authBearer db)
                    :uri             "/xzeros/lst/addItems"
                    :format          (ajax/json-request-format)
                    :params          {:name lst-name :items [new-item]}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-lst-add-item-response lst-name]
                    :on-failure      [:process-lst-add-item-response lst-name]}
       }
      )
    )
  )

(rf/reg-event-fx
  :process-lst-delete-item-response
  []
  (fn [{:keys [db]} [_ lst-name idx response]]
    (let
      [lst-id (:data response)
       error (:error response)
       ]
      (if error
        {:db (assoc-in db [:lst :error] error)}
        (let [curr-items (get-in db [:lst :lsts lst-name :items])
              new-items (into (subvec curr-items 0 idx) (subvec curr-items (inc idx)))
              ]
          {:db
           (
             -> db
                (assoc-in [:lst :error] nil)
                (assoc-in [:lst :editing-id] nil)
                (assoc-in [:lst :lsts lst-name :items] new-items)
             )
           }
          )
        )
      )))

(rf/reg-event-fx
  :lst-delete-item
  []
  (fn [{:keys [db]} [_ lst-name]]
    (let [idx (get-in db [:lst :editing-id])]
      {:http-xhrio {:method          :post
                    :headers         (db/authBearer db)
                    :uri             "/xzeros/lst/deleteItem"
                    :format          (ajax/json-request-format)
                    :params          {:name lst-name :index idx}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-lst-delete-item-response lst-name idx]
                    :on-failure      [:process-lst-delete-item-response lst-name idx]}
       }
      )
    )
  )

(rf/reg-event-fx
  :process-lst-update-item-response
  []
  (fn [{:keys [db]} [_ lst-name response]]
    (let
      [lst-id (:data response)
       error (:error response)
       ]
      {:db db}
      )))

(rf/reg-event-fx
  :update-items
  []
  (fn [{:keys [db]} [_ [lst-name idx] val]]
    (let [new-items (assoc (get-in db [:lst :lsts lst-name :items]) idx val)]
      {:http-xhrio {:method          :post
                    :headers         (db/authBearer db)
                    :uri             "/xzeros/lst/updateItems"
                    :format          (ajax/json-request-format)
                    :params          {:name lst-name :items [val] :index idx}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-lst-update-item-response lst-name]
                    :on-failure      [:process-lst-update-item-response lst-name]}
       }
      )
    )
  )

(rf/reg-event-fx
  :update-item
  []
  (fn [{:keys [db]} [_ [lst-name idx] val]]
    (let [new-items (assoc (get-in db [:lst :lsts lst-name :items]) idx val)]
      {
       :db (assoc-in db [:lst :lsts lst-name :items] new-items)
       :dispatch
           [:update-items [lst-name idx] (encrypt-item-client val (:user db))]
       }
      )
    )
  )

(rf/reg-event-fx
  :process-lst-import-text-response
  []
  (fn [{:keys [db]} [_ lst-name response]]
    (let
      [lst-id (:data response)
       error (:error response)
       ]
      (if error
        {:db (assoc-in db [:lst :error] error)}
        (let [curr-items (get-in db [:lst :lsts lst-name :items])
              new-plain-items (utils/from-text (get-in db [:lst :new-item]))]
          {:db (assoc-in (assoc db :error nil) [:lst :lsts lst-name :items] (into curr-items new-plain-items))}
          )
        )
      )))

(rf/reg-event-fx
  :lst-import-text
  []
  (fn [{:keys [db]} [_ lst-name]]
    (let [new-plain-items (utils/from-text (get-in db [:lst :new-item]))
          new-items (encrypt-client new-plain-items (:user db))]
      {:http-xhrio {:method          :post
                    :headers         (db/authBearer db)
                    :uri             "/xzeros/lst/addItems"
                    :format          (ajax/json-request-format)
                    :params          {:name lst-name :items new-items}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:process-lst-import-text-response lst-name]
                    :on-failure      [:process-lst-import-text-response lst-name]}
       }
      )
    )
  )
