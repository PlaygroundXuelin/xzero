(ns xzero.events
  (:require [re-frame.core :as rf]
            [xzero.db :as db]
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
  (fn [_ _]
    {:http-xhrio {:method          :get
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

;
;
;

(rf/reg-event-db
  :initialize-db
  (fn [db _]
    db/default-db))

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
                   :uri             "/xzeros/cmd/execute"
                   :format          (ajax/json-request-format)
                   :params          params
                   :response-format (ajax/json-response-format {:keywords? false})
                   :on-success      [:process-cmd-response]
                   :on-failure      [:process-cmd-response]}
      :db  (assoc-in db [:cmd :loading?] true)})))

