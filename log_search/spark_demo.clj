(ns log-search.spark-demo
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
    )
    (:gen-class)
)

(defn -main []
    (let [sc (k/spark-context 
                :master "192.168.1.100:7077" :job-name "Simple Job" 
            )
            input-rdd (.textFile sc "/Users/zhangjun/Desktop/code/fs/search/
                log-collector.log.2013-08-06"
                )
        ]
        (-> input-rdd
            (k/count)
            println
        )
    )
    
)