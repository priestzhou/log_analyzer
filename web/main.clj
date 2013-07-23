(ns web.main
    (:use ring.adapter.jetty)
    (:use clojure.java.io)
    (:use log-consumer.logparser)
    (:use log-consumer.logconsumer)  
    (:gen-class)
)
  
(def  logcahe (atom ["12","23"])
)
(def logdata 
    (atom [])
)

(defn- get-json []
    (reset! logdata (filter-by-time @logdata))
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

(defn -main []
    (run-jetty #'app {:port 8085 :join? false})
    (consumer-from-kfk 
        "115.28.40.198:2181" 
        "hdfs.data-node" 
        "data-consumer24"
        logdata
    )
)

