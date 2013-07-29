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

(defn- test-query [qStr log-atom query-atom]
    (reset! 
        query-atom
        (str (System/currentTimeMillis) "=" qStr "=" @log-atom) 
    )
    (Thread/sleep 2000)
    (recur qStr log-atom query-atom)
)

(defn- gen-query-id []
    (let [curtime (System/currentTimeMillis)
            randid (rand)
        ]
        (str curtime randid)
    )
)

(defn- get-query-result [query-id]
    (swap! 
        futurMap 
        #(update-in % [query-id :time] (fn [a] (System/currentTimeMillis)))
    )    
    @(get-in @futurMap [query-id :output])
)

(defn- delete-future [fMap qid]
    (future-cancel (get-in @fMap [qid :future]))
    (dissoc @fMap qid)
)

(defn- check-query [fMap]
    (println @fMap)
    (let [curtime (System/currentTimeMillis)
            lastTime (curtime - 30000)
            query-List (keys fMap)
        ]
        (->>
            (filter #(< lastTime (get-in @fMap [% :time])) query-List)
            (map  
                (partial delete-future @fMap)
            )
        )
    )
    (Thread/sleep 5000)
    (recur fMap)
)

(defn- create-query [qStr]
    (if  
        (> maxQueryCount (count (keys @futurMap)))
        
        (let [query-id (gen-query-id)
                output (atom [])
            ] 
            (swap! futurMap
                #(assoc % query-id 
                    {
                        :future (future (test-query qStr logdata output))
                        :time (System/currentTimeMillis)
                        :output output
                    }
                )
            )
            (str "{query-id:" query-id "}")
        )
        (str "the max query count is " maxQueryCount)
    )
)

(cp/defroutes app-routes
    (cp/GET "/" [uri params] (format "You requested %s with query %s" uri params))
    (cp/ANY "/query/create/:qStr" [qStr]
        (create-query qStr)
    )
    (cp/GET "/query/get/:query-id" [query-id] 
        (get-query-result query-id)
    )
    (route/resources "/")
    (route/not-found "Not Found")
)

(def ^:private app
    (handler/site app-routes)
)

(defn -main [& args]
    (rj/run-jetty #'app {:port 8085 :join? false})
    (check-query futurMap)
)

