(ns web.main
    (:use ring.adapter.jetty)
    (:use clojure.java.io)
    (:use log-consumer.logparser)
    (:use log-consumer.logconsumer)
    (:require
        [argparser.core :as arg]
        [utilities.core :as util]
    )  
    (:gen-class)
)

(def logdata 
    (atom [])
)

(defn- get-json []
    (swap! logdata filter-by-time )
    (gen-json @logdata)
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
                    :group ["data-node-consumer"]
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
        (consumer-from-kfk 
            (first (:zkp opts-with-default))
            (first (:topic opts-with-default))
            (first (:group opts-with-default))
            logdata
        )
    )
)
