(ns log-search.webserver
    (:require
        [ring.adapter.jetty :as rj]
        [compojure.core :as cp]        
        [compojure.handler :as handler]
        [compojure.route :as route]
    )
    (:gen-class)
)

(cp/defroutes app-routes
  (cp/GET "/" [] "Hello World")
  (route/resources "/")
  (route/not-found "Not Found")
)

(def app
  (handler/site app-routes)
)

(defn -main [& args]
    (rj/run-jetty #'app {:port 8085 :join? false})
)