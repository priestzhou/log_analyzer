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
        (#(.textFile % "/etc/hosts"))
        (#(.count %))
        println
    )
)

(comment defn -main []
    (let [sc (k/spark-context 
                :master "spark://192.168.1.100:7077" :job-name "Simple Job" 
            )
            input-rdd (.textFile sc "/etc/hosts"
                )
        ]
        (println (k/count input-rdd))            
    )   
)