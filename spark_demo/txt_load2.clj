(ns spark-demo.txt-load2
    (:import 
         spark.api.java.JavaSparkContext         
    )
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
        [clj-spark.spark.functions :as f]
        [spark-demo.spark-engine :as spe]
        [log-search.searchparser :as lsp]
        [clojure.data.json :as json]
        [clojure.string :as cs]
    )    
    (:gen-class)
)

(set! *warn-on-reflection* true)
;spark.streaming.api.java.JavaStreamingContext
;spark.streaming.Duration
(defn- get-test-rdd []
    (let [sc (k/spark-context 
                :master "spark://10.144.44.18:7077" :job-name "Simple Job" 
                :spark-home "/home/hadoop/spark/" 
                :jars ["./log_search.jar"]
                )
            input-rdd (.textFile sc "/home/hadoop/build/namenodelog_all"
                )
            t1 (println "get-test-rdd")
            bv (.broadcast sc (f/function1
                        (sfn/fn f [log]
                            log
                        )
                    )
                )            
            ]
        [sc (.map 
            input-rdd
            (.value bv)
        )]
    )
)

(defn- run-test [inStr tw st]
    (let [[sc testrdd] (get-test-rdd)
            ;tp (lsp/sparser inStr tw st)
            t1 (println "run-test")
            bv (.broadcast sc (f/function1
                        (sfn/fn [log] (keys log))
                    )
                ) 
        ]
    (->
        testrdd
        (.map (.value bv) )
        (.count )
        println
    )    
    )
)

(defn -main []
    (let [            
        efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case "open" )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        (run-test 
            "*hdfs_* | parse-re \"(?<=HDFS_)[a-zA-Z]*\" as type 
            | parse \"bytes: *,\" as size | last size ,min size,uc size ,max size by type " 
            "86400"
            1377018378063
        ) 
    )
)


    (comment do
        ;(run-test "*hdfs*")
        (run-test 
            "*hdfs_* | parse-re \"(?<=HDFS_)[a-zA-Z]*\" as type 
            | parse \"bytes: *,\" as size | last size ,min size,uc size ,max size by type " 
            "86400"
            1377018378063
        )                
    )