(ns xzero.db)

(def default-db
  {:page :home
   :user {:bearer nil}
   :cmd {:cmd-type "bash"
         :script {
                  "bash"
                  "
echo 'Bash version ${BASH_VERSION}...'
for i in {0..10}
do
    echo \"Welcome $i times\"
done
"
                  "clojure"
                  "
                  (let [pi (Math/PI)
                      r  1959
                      area (* 4 pi r r)]
                    (str \"Earth area is \" (int area) \" square miles\"))"
                  }}})

(defn authBearer [db]
  (let [bearer  (get-in db [:user :bearer])]
    (if bearer
      [:Authorization (str "Bearer " bearer)]
      nil
      )
    )
  )

(defn hasPermission [user permission]
  (cond
    (= permission [:page :cmd]) (and (= (:name user) "xuelin.wang@gmail.com") (:bearer user))
    (= permission [:page :lst]) (and (= (:name user) "xuelin.wang@gmail.com") (:bearer user))
    :else true)
  )

