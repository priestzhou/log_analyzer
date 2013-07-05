(ns web.main
    (:use ring.adapter.jetty)
    (:use clojure.java.io)
    (:use log-consumer.logparser)
    (:gen-class)
)

(def  logcahe (atom ["12","23"])
)

(defn app
    [{:keys [uri]}]
    {:body 
        (->>  
            (parse-log parse-rules @logcahe)
            (filter 
                #(<  1 (count %))
            )
            concat
            first
            gen-json
        )

    }
)

(defn -main []
    (run-jetty #'app {:port 8085 :join? false})
    (Thread/sleep 2000)
    (reset! logcahe 
        (.split
            (slurp "/Users/zhangjun/Desktop/code/fs/log_analyzer/log_consumer/test.log") 
         "\n"
        )
    )
    (->> 
        (parse-log parse-rules @logcahe)
        (filter 
                #(<  1 (count %))
        )
            concat
            first
            gen-json
            println
    )
)

