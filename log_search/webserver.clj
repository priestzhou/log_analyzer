(ns log-search.webserver
    (:import 
        spark.SparkContext
        com.flyingsand.spark.Spark_init
    )    
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
        [log-search.search-driver :as sd]
        [log-search.searchparser :as sp]
        [argparser.core :as arg]
        [utilities.core :as util]
        [clojure.data.json :as js]
        [clj-spark.api :as k]
        [serializable.fn :as sfn]
        [clojure.data.json :as json]
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

(def ^:private rddData 
    (atom [])
)

(defn- get-test-rdd [master jobname sparkhome input]
    (info "get-test-rdd1")
    (let
        [
            si (Spark_init. master jobname sparkhome input)
            rdd (.getInitRdd si)
        ]
        rdd
    ) 
)

(defn- run-query [psr rdd log-atom  group-atom]
    (println "query run " )
    (try
        (sd/do-search psr rdd log-atom group-atom)

        (println " query done")
        (catch Exception error
            (println error)
            (println (.printStackTrace error))
        )
    )
)

(defn- gen-query-id []
    (let [curtime (System/currentTimeMillis)
            randid (rand)
        ]
        (str curtime randid)
    )
)

(defn- wait-result [query-id kw]
    (Thread/sleep 1000)
    (let [flag (get-in @futurMap [query-id kw])]
        (if (= @flag [])
            (recur query-id kw)
            (debug "result ready")
        )
    )
)

(defn- get-query-result [query-id kw]
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
            (wait-result query-id kw)
            {:status 202
                :headers {
                    "Access-Control-Allow-Origin" "*"
                    "content-type" "application/json"
                }
                :body 
                (js/write-str @(get-in @futurMap [query-id kw]) )
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
            lastTime (- curtime  300000)
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

(defn- create-query-t [qStr start end]
    (info "create-query-t " :string qStr  :start start :end end)
    (if  (> maxQueryCount (count (keys @futurMap)))
        (let [query-id (gen-query-id)
                logout (atom [])
                groupout (atom [])
                srule  (sp/sparser qStr "14400" 1376813000267) 
                t1rule (.put srule "startTime" start)
                t2rule (.put srule "endTime" end)    
            ]
            (info "create-query-t into let")
            (println "srule - " srule)
            (swap! futurMap
                #(assoc % query-id 
                    {
                        :future 
                            (future 
                                (do (println "future in ")
                                (run-query 
                                    srule
                                    @rddData
                                    logout
                                    groupout
                                )
                                )
                            )
                        :time (System/currentTimeMillis)
                        :logout logout
                        :groupout groupout
                    }
                )
            )
            (info "create-query-t swap end")
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
            (info "get a query create post" (:query params))
            {:status 202
                :headers {
                    "Access-Control-Allow-Origin" "*"
                    "content-type" "application/json"
                }
                :body (create-query-t (:query params ) (:start params) (:end params))
            }
            
        )
    )
    (cp/GET "/query/log" {params :params} 
        (do
            (get-query-result (:query-id params) :logout)    
        )       
    )
    (cp/GET "/query/result" {params :params} 
        (do
            (get-query-result (:query-id params) :groupout)    
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
                :usage "Usage: master sparkhome input [jobname] exejar"
                :args [
                    (arg/opt :help
                        "-h|--help" "show this help")
                    (arg/opt :webport
                        "-webport <webport>" "jetty web port")
                    (arg/opt :master
                        "-master <master>" "spark master")
                    (arg/opt :jobname
                        "-jobname <jobname>" "spark jobname")
                    (arg/opt :sparkhome
                        "-sparkhome <sparkhome>" "spark home path")
                    (arg/opt :input
                        "-input <input>" "spark input file path")
                    ]
            }
            opts (arg/transform->map (arg/parse arg-spec args))
            default-args 
                {
                    :jobname ["log search"]
                    :webport ["8086"]
                }
            opts-with-default (merge default-args opts)
            rdd (get-test-rdd 
                    (first (:master opts-with-default)) 
                    (first (:jobname opts-with-default))
                    (first (:sparkhome opts-with-default))
                    (first (:input opts-with-default))
                )
        ]
        (when (:help opts-with-default)
            (println (arg/default-doc arg-spec))
            (System/exit 0)            
        )
        (util/throw-if-not (:master opts-with-default)
            IllegalArgumentException. 
            "the master info is needed"
        )
        (util/throw-if-not (:sparkhome opts-with-default)
            IllegalArgumentException. 
            "the sparkhome info is needed"
        )
        (util/throw-if-not (:input opts-with-default)
            IllegalArgumentException. 
            "the input info is needed"
        )        
        (println (k/count rdd))
        (reset! rddData rdd)          
        (rj/run-jetty #'app 
            {
                :port 
                (read-string (first (:webport opts-with-default))) 
                :join? false
            }
        )
        (add-watch logdata :max-log-watch max-log-watch)
        (future (check-query futurMap))
    )
)

