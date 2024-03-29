(ns log-search.webserver
    (:use 
        [ring.middleware.params :only (wrap-params)]
        [logging.core :only [defloggers]]
    )
    (:require
        [ring.adapter.jetty :as rj]
        [compojure.core :as cp]        
        [compojure.handler :as handler]
        [compojure.route :as route]
        [log-search.consumer :as lc]
        [log-search.frame :as fr]
        [log-search.searchparser :as sp]
        [argparser.core :as arg]
        [utilities.core :as util]
        [clojure.data.json :as js]
    )
    (:gen-class)
)

(defloggers debug info warn error)

(def ^:private futurMap 
    (atom {})
)

(def ^:private maxQueryCount 10)

(def ^:private logdata 
    (atom [])
)

(defn- run-query [psr log-atom query-atom]
    (info "query run " :inputlogcount (count @log-atom))
    (reset! 
        query-atom
        (assoc (fr/do-search psr @log-atom) 
            :query-time (str (System/currentTimeMillis) )
        )
    )
    (Thread/sleep 5000)
    (info " next query running")
    (recur psr log-atom query-atom)
)

(defn- gen-query-id []
    (let [curtime (System/currentTimeMillis)
            randid (rand)
        ]
        (str curtime randid)
    )
)

(defn- get-query-result [query-id]
    (if (nil? (get @futurMap query-id))
        (do
            (debug "the query is invalid " :query-id query-id)
            "the query is invalid"
        )
        (do
            (swap! 
                futurMap 
                #(update-in % [query-id :time] 
                    (fn [a] (System/currentTimeMillis))
                )
            )
            {:status 202
                :headers {
                    "Access-Control-Allow-Origin" "*"
                    "content-type" "application/json"
                }
                :body 
                (js/write-str @(get-in @futurMap [query-id :output]) )
            }
            
        )
    )
)

(defn- delete-future [fMap qid]
    (let [ft (get-in @fMap [qid :future])]
        (swap! fMap #(dissoc % qid))
        (future-cancel ft)
    )   
)

(defn- check-query [fMap]
    (let [curtime (System/currentTimeMillis)
            lastTime (- curtime  30000)
            query-List (keys @fMap)
        ]
        (->>
            (filter #(> lastTime (get-in @fMap [% :time])) query-List)
            (#(map  
                (partial delete-future fMap)
                %
            ))
            doall
        )
    )
    (Thread/sleep 5000)
    (recur fMap)
)

(defn- create-query-t [qStr timewindow]
    (debug "create-query-t " :string qStr  :timewindow timewindow)
    (if  (> maxQueryCount (count (keys @futurMap)))
        (let [query-id (gen-query-id)
                output (atom [])
            ]
            (debug "create-query-t into let")
            (swap! futurMap
                #(assoc % query-id 
                    {
                        :future 
                            (future 
                                (run-query 
                                    (sp/sparser qStr timewindow) 
                                    logdata 
                                    output
                                )
                            )
                        :time (System/currentTimeMillis)
                        :output output
                    }
                )
            )
            (str "{\"query-id\":\"" query-id "\"}")
        )
        (str "the max query count is " maxQueryCount)
    )
)

(defn- get-log-example []
    (map #(str % "\n") (take 10 @logdata))
)

(cp/defroutes app-routes
    (cp/GET "/test" {params :params} 
        (format "You requested with query %s" params)
    )
    (cp/POST "/query/create" {params :params}
        (do
            (debug "get a query create post" (:query params))
            {:status 202
                :headers {
                    "Access-Control-Allow-Origin" "*"
                    "content-type" "application/json"
                }
                :body (create-query-t (:query params ) (:timewindow params))
            }
            
        )
    )
    (cp/GET "/query/get" {params :params} 
        (do
            (get-query-result (:query-id params))    
        )       
    )
    (cp/GET "/testlog/first" []
        (get-log-example)
    )
    (route/files "/" {:root "public"})
    (route/not-found "Not Found")
)

(def ^:private app
    (handler/site app-routes)
)

(def ^:pravite max-log 500)
(def ^:pravite new-log 200)

(defn- max-log-watch [watch-key log-atom old-data new-data]
    (let [logcount (count new-data)]
        (if (> max-log logcount)
            (debug "logcount don't over the threshold" 
                :threshold max-log :log-count logcount
            )
            (do
                (info "the logcount over the threshold"
                    :threshold max-log :log-count logcount
                )
                (swap! log-atom #(take-last new-log %))
            )
        )
    )
)

(defn -main [& args]
    (let [arg-spec 
            {
                :usage "Usage: zookeeper [topic] [group] exejar"
                :args [
                    (arg/opt :help
                        "-h|--help" "show this help")
                    (arg/opt :zkp 
                        "-zkp <ip:port>" "the zookeeper ip & port")
                    (arg/opt :topic
                        "-topic <topic>" "the kafka topic")
                    (arg/opt :group
                        "-group <group>" "the consumer group")
                    (arg/opt :webport
                        "-webport <webport>" "jetty web port")
                    ]
            }
            opts (arg/transform->map (arg/parse arg-spec args))
            default-args 
                {
                    :topic ["hdfs.data-node"]
                    :group ["log_search"]
                    :webport ["8086"]
                }
            opts-with-default (merge default-args opts)
        ]
        (when (:help opts-with-default)
            (println (arg/default-doc arg-spec))
            (System/exit 0)            
        )
        (util/throw-if-not (:zkp opts-with-default)
            IllegalArgumentException. 
            "the zookeeper info is needed"
        )  
        (rj/run-jetty #'app 
            {
                :port 
                (read-string (first (:webport opts-with-default))) 
                :join? false
            }
        )
        (add-watch logdata :max-log-watch max-log-watch)
        (future (check-query futurMap))
        (lc/consumer-from-kfk 
            (first (:zkp opts-with-default))
            (first (:topic opts-with-default))
            (first (:group opts-with-default))
            logdata
        )
    )
)

