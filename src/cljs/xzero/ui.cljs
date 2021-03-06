(ns xzero.ui
  (:require
    [reagent.core :as r]
    [re-frame.core :as rf]
    [clojure.string :as str]
    ))

(defn text-input [event-id event-params input-type init-val save-on-change? props]
  (let [val (r/atom init-val)
        stop #(reset! val init-val)
        save #(rf/dispatch (into [] (concat [event-id] event-params [(if (nil? %) @val %)])))
        save-on-change #(rf/dispatch (into [] (concat [event-id] event-params [@val])))]
    [:input
     (merge
       {
        :type input-type
        :default-value (or init-val "")
        :on-blur (partial save nil)
        :on-change #(let [new-val (-> % .-target .-value)]
                      (if save-on-change? (save new-val) (reset! val new-val)))
        :on-key-down #(case (.-which %)
                        13 (save nil)
                        27 (stop)
                        nil)}
       props)]))


(defn textarea-input [event-id event-params init-val save-on-change? props]
  (let [val (r/atom init-val)
        save #(rf/dispatch (into [] (concat [event-id] event-params [(if (nil? %) @val %)])))
        save-on-change #(rf/dispatch (into [] (concat [event-id] event-params [@val])))]
    [:textarea
     (merge
       {
        :default-value (or init-val "")
        :on-blur (partial save nil)
        :on-change #(let [new-val (-> % .-target .-value)]
                      (if save-on-change? (save new-val) (reset! val new-val)))
        }
       props)]))

;(defn textarea-input [event-id event-params val props]
;  (let [save #(rf/dispatch (into [] (concat [event-id] event-params [%])))]
;    [:textarea
;     (merge
;       {
;        :value val
;        :on-change #(save (-> % .-target .-value))
;        }
;       props)]))

(defn raw-textbox [props text]
  (when text
    [:pre (merge props
                 {:dangerouslySetInnerHTML
                  {:__html (str/replace (str text) "\\n" "\n")}}
                 ) ]
    )
  )

(defn items-list [options key-prefix click-callback]
  (let [items
        (vec
          (map-indexed
            (fn [index item]
              (let [comp-key (str key-prefix "-" index)]
                [:div
                 {
                  :on-click (fn [] (when (some? click-callback) (click-callback item)))
                  }
                 [raw-textbox {:class (if (even? index) "alternate-a" "alternate-b") :key comp-key} item]
                 ]
                )
              )
            options)
          )
        ]
    (into [:div.container] (interpose [:div [:hr]] items))
    )
  )

(defn new-window [url content]
  (let [win (.open js/window url "_blank")
        _ (-> win
              (.-document)
              (.open)
              (.write "<html><head/><body></body></html>"))
        ]
    (if (some? content)

      (let [
            init-fn (fn []
                      (let [doc (.-document win)
                            pre (.createElement doc "pre")
                            _ (set! (.-innerText pre) content)
                            body (.-body doc)
                            ]
                        (.appendChild body pre)
                        )
                      )
            ]
        (init-fn)
        ;        (set! (.-onload win) init-fn)
        win
        )
      )
    ;    (.close win)

    )
  )