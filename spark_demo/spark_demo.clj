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
                :jars ["./clj_spark_rebuild.jar" "./spark_demo.jar"
                    "./log_search.jar"] ;
                )
            input-rdd (.textFile sc "/logfile"
                )
            testp (lsp/parse-all "*hdfs*")
        ]
        (->
            input-rdd
            (k/map 
                (sfn/fn f [log]
                    (json/read-str log :key-fn keyword)
                )
            )
            (#(spe/do-search testp %))
            (#(k/first %))
            println
        )
    )
)

            (comment k/map 
                (sfn/fn f1 [log]
                    [(get log "pcid") log]
                )
            k/group-by-key
            (k/map second)
            )
