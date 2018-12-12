(ns xzero.routes.home
  (:require [xzero.layout :as layout]
            [clojure.java.io :as io]
            [xzero.middleware :as middleware]
            [ring.util.http-response :as response]))

(defn home-page [_]
  (layout/render "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

