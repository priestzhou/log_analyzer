(ns log-consumer.main
    (:use clojure.java.io)
    (:use log-consumer.logparser)
    (:gen-class)
)

(def teststr 
    (.split
        (slurp "/Users/zhangjun/Desktop/code/fs/log_analyzer/log_consumer/test.log") 
         "\n"
    )
)




(defn -main [& arg]
    (println (parse-log app teststr))
)