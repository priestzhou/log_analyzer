(ns spark-demo.spark-demo
    (:import 
         spark.api.java.JavaSparkContext
    )
    (:gen-class)
)


(defn -main []
    (->>
        (JavaSparkContext. 
            "spark://192.168.1.100:7077" 
            "test job" 
            "/home/admin/spark-0.7.3"
            "/Users/zhangjun/temp.hs"
        )
        println
    )
)

(comment defn -main []
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