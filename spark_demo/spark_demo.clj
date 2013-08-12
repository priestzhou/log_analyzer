(ns spark-demo.spark-demo
    (:import 
         spark.api.java.JavaSparkContext
    )
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
    )    
    (:gen-class)
)

(comment defn -main []
    (->>
        (JavaSparkContext. 
            "spark://192.168.1.100:7077" 
            "test job" 
            "/home/admin/spark-0.7.3"
            "/Users/zhangjun/temp.hs"
        )
        (#(.textFile % "/etc/hosts"))
        (#(.map % 
            (sfn/fn [log]
                ({:message log})
            )
        ))
        (#(.collect %))
        (into [] )
        println
        ;(#(map println %))
        ;dorun
    )
)

(defn -main []
    (let [sc (k/spark-context 
                :master "spark://192.168.1.100:7077" :job-name "Simple Job" 
                :spark-home "/home/admin/spark-0.7.3" 
                :jars "/Users/zhangjun/temp.hs"
            )
            input-rdd (.textFile sc "/etc/hosts"
                )
        ]
        (println (k/count input-rdd))            
    )
)