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
            (gen-json @logdata)
        )
        
    }
)

(defn -main []
    (run-jetty #'app {:port 8085 :join? false})
    (wait-and-run logdata reload-from-file)
)

