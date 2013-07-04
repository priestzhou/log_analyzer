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
        (format "You requested %s" 
            (count
                (parse-log parse-rules @logcahe)
            )
        )

    }
)

(defn -main []
    (println logcahe)
    (run-jetty #'app {:port 8085 :join? false})
    (Thread/sleep 20000)
    (println "sleep over")
    (reset! logcahe 
        (.split
            (slurp "/Users/zhangjun/Desktop/code/fs/log_analyzer/log_consumer/test.log") 
         "\n"
        )
    )
    (println (count @logcahe))
)

