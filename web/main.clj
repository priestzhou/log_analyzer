(ns web.main
    (:use 
        [ring.adapter.jetty]
        [clojure.java.io]
        [logging.core :only [defloggers]]
    )
    (:require
        [argparser.core :as arg]
        [utilities.core :as util]
        [log-consumer.logparser :as lp]
        [log-consumer.logconsumer :as lc]
    )  
    (:gen-class)
)

(defloggers debug info warn error)

(def logdata 
    (atom [])
)

(defn- get-json []
    (swap! logdata lp/filter-by-time )
    (info "time filter output log " :count (count @logdata))
    (lp/gen-json @logdata)
)

(def ^:pravite max-log 1000)
(def ^:pravite new-log 500)

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

(defn app
    [{:keys [uri]}]
    {:body
        (if (= uri "/d3")
            (slurp 
                (
                    .getResourceAsStream (.getClass System) 
                    "/web/d3_experiments1.html"
                )
            )
            (get-json)            
        )
        
    }
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
                    ]
            }
            opts (arg/transform->map (arg/parse arg-spec args))
            default-args 
                {
                    :topic ["hdfs.data-node"]
                    :group ["data.node-consumer"]
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
        (run-jetty #'app {:port 8085 :join? false})
        (add-watch logdata :max-log-watch max-log-watch)
        (lc/consumer-from-kfk 
            (first (:zkp opts-with-default))
            (first (:topic opts-with-default))
            (first (:group opts-with-default))
            logdata
        )
    )
)
