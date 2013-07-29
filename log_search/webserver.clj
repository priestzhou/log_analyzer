(ns log-search.webserver
    (:use 
        [ring.middleware.params :only (wrap-params)]
    )
    (:require
        [ring.adapter.jetty :as rj]
        [compojure.core :as cp]        
        [compojure.handler :as handler]
        [compojure.route :as route]
    )
    (:gen-class)
)

(def ^:private futurMap 
    (atom {})
)

(def ^:private maxQueryCount 10)

(def ^:private logdata 
    (atom [])
)

(defn- test-query []
)

(defn- gen-query-id []
    (let [curtime (System/currentTimeMillis)
            randid (rand)
        ]
        (str curtime randid)
    )
)

(defn- create-query [qStr]
    (if (< maxQueryCount (count (keys futurMap)))
        (str "the max query count is " maxQueryCount)
        (let [query-id (gen-query-id)
            ] 
            (swap! futurMap
                #(assoc % query-id 
                    {
                        :futur (futur (demo-fun qStr))
                        :time (System/currentTimeMillis)
                    }
                )
            )
            (str "{query-id:" query-id "}")
        )
    )
)

(cp/defroutes app-routes
    (cp/GET "/" [uri params] (format "You requested %s with query %s" uri params))
    (cp/ANY "/user/:qStr" [qStr]
        (create-query qStr)
    )
    (route/resources "/")
    (route/not-found "Not Found")
)

(def ^:private app
    (handler/site app-routes)
)

(defn -main [& args]
    (rj/run-jetty #'app {:port 8085 :join? false})
)

