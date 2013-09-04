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
        [clojure.string :as cs]
        [clj-json.core :as clj ]
    )    
    (:gen-class)
)
;spark.streaming.api.java.JavaStreamingContext
;spark.streaming.Duration
(comment defn- get-test-rdd []
    (let [sc (k/spark-context 
                :master "spark://192.168.1.100:7077" :job-name "Simple Job" 
                :spark-home "/home/admin/spark-0.7.3" 
                :jars ["./log_search.jar"]
                )
            input-rdd (.textFile sc "hdfs://192.168.1.100/namenode/*/*"
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

(comment defn- get-streaming-rdd []
    (let [sc (JavaStreamingContext. 
                "spark://192.168.1.100:7077" "Simple Job" 
                (Duration. 50000)
                "/home/admin/spark-0.7.3" 
                "./log_search.jar"
                )
            input-rdd (.kafkaStream sc "192.168.1.100:2181" "test1"
             (java.util.HashMap. {"hdfs.data-node" (Integer. 3)})
                )
            ]
        (->
            input-rdd
            ;(k/map (sfn/fn [log] [(count log) log]))
            (.print )
        )
        (Thread/sleep 5000)
        (.start sc)
    )
)

(comment defn- run-test [inStr tw st]
    (let [testrdd (get-streaming-rdd)
            ;tp (lsp/sparser inStr tw st)
        ]
    (->
        testrdd
        ;(k/map (sfn/fn [log] [(count log) log]))
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
        file (.split (slurp "/home/admin/zhangjun/hdfs-test") "\n")
        ]
        (println (System/currentTimeMillis))
        (->>
            file
            (map #(json/read-str % :key-fn keyword))
            ;(pmap #(get % "message"))
            ;(filter efunc)
            count
            println
        )
        (println (System/currentTimeMillis))
        (->>
            file
            (map #(json/read-str % ))
            ;(pmap #(get % "message"))
            ;(filter efunc)
            count
            println
        )
        (println (System/currentTimeMillis))        
        (->>
            file
            (map #(clj/parse-string % ))
            ;(pmap #(get % "message"))
            ;(filter efunc)
            count
            println
        )        
        (println (System/currentTimeMillis))     
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