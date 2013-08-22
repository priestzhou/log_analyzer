(ns spark-demo.spark-demo
    (:import 
         spark.api.java.JavaSparkContext
    )
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
        [spark-demo.spark-engine :as spe]
        [log-search.searchparser :as lsp]
        [clojure.data.json :as json]
    )    
    (:gen-class)
)

(defn- get-test-rdd []
    (let [sc (k/spark-context 
                :master "spark://192.168.1.100:7077" :job-name "Simple Job" 
                :spark-home "/home/admin/spark-0.7.3" 
                :jars ["./spark_demo.jar"]
                )
            input-rdd (.textFile sc "/logfile"
                )
            ]
        (k/map 
            input-rdd
            (sfn/fn f [log]
                (json/read-str log :key-fn keyword)
            )
        )
    )
)

(defn- run-test [inStr]
    (let [testrdd (get-test-rdd)
            tp (lsp/parse-all inStr)
        ]
    (->>
        testrdd
        (spe/do-search tp)
        println
    )        
    )
)

(defn -main []
    (do
        ;(run-test "*hdfs*")
        (run-test "*hdfs_* | parse \"HDFS_*\" as type | count type by type ")
    )
)
