(ns spark-demo.spark-engine
    (:import 
         spark.api.java.JavaSparkContext
    )    
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
    )
)

(defn- event-search [fitlers rdd]
    (k/filter rdd
        (sfn/fn f [log]
            (reduce 
                (sfn/fn f1 [a b]
                    (and a b)
                )
                true
                (map
                    (sfn/fn f1 [a](a(:message log)))
                    fitlers
                )
            )
        )        
    )
)

(defn- where-filter [fitlers rdd]
    (k/filter rdd
        (sfn/fn f [log]
            (reduce 
                (sfn/fn f1 [a b]
                    (and a b)
                )
                true
                (map
                    (sfn/fn f1 [a](a log))
                    fitlers
                )
            )
        ) 
    )
)

(defn- do-parse [parseRule log]
    (let [pkey (get parseRule :key)
            tparser (get parseRule :parser)
        ]
        {pkey (tparser log)}
    )
)


(defn- apply-parse [parseRules rdd]
    (k/map rdd
        (sfn/fn [log]
            (let [psr (map 
                            #(do-parse % (:message log)) 
                            parseRules
                        )
                ]
                (reduce merge log psr)
            )
        )
    )
)

(defn- filter-parse [rdd]
;    ( filter 
;        #(empty?
;            (filter nil? (vals %))
;        )
        rdd
;    )
)

(defn- dateformat [t]
    (.format 
        (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
        t
    )    
)

(defn- change-time [log]
    (update-in log [:timestamp] dateformat)
)

(defn- get-newest-log [rdd]
    (->
        rdd
        (k/map
            (sfn/fn [log]
                [(:timestamp log)  (change-time log)]
            )
        )
        (k/sort-by-key compare false)
        (k/take 10)
        (#(map last %))
    )
)

(defn- get-key-func [groupKeys timeRule]
    (let [startmap (if (nil? timeRule)
                (sfn/fn ft1 [log] {})
                (sfn/fn ft2 [log]
                    {:groupTime
                        ((:tf timeRule)
                            (:timestamp log)
                        )
                    }
                )
            )
        ]
        (sfn/fn gk [log]
            (reduce
                #(assoc %1 %2 (get log %2))
                (startmap log)                
                groupKeys
            )
        )
    )
)

(defn- do-group [groupKeys rdd timeRule]
    (let [keyFunc (get-key-func groupKeys timeRule)]
        
        (-> rdd
            (k/map  
                (sfn/fn gen-key [log]
                    [(keyFunc log) log]
                )
            )
            (k/group-by-key)
        )
    )
)

(defn- static-fun [stRule log]
    (let [logVal (second log)
            inKey (:statInKey stRule)
            statFun (:statFun stRule)
        ]
        (->>
            logVal
            (map 
                (sfn/fn f [log](get log inKey) )
            )
            statFun
        )
    )
)

(defn- do-statistic [statRules rdd]
    (if (nil? statRules)
        rdd
        (k/map rdd
            (sfn/fn fs [log]
                [(reduce
                    (sfn/fn f [a b]
                        (assoc a 
                        (:statOutKey b)
                        (static-fun b log))
                    )
                    (first log)
                    statRules
                ) (second log)]
            )
        )
    )
)

(defn do-search [searchrules rdd]
   (let [eventFilter (:eventRules searchrules)
            logFilted (event-search eventFilter rdd)
            whereRules (:whereRules searchrules)
            
            parseRules (:parseRules searchrules)
            parseResult (filter-parse 
                    (apply-parse parseRules logFilted)
                )
            limitResult (get-newest-log parseResult)
            whereResult (where-filter whereRules parseResult)
            timeRule (:timeRule searchrules)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys whereResult timeRule)

;            logGroupWithTime (do-group-with-time groupKeys parseResult timeRule)
            statRules (:statRules searchrules)
            statResult (do-statistic statRules logGrouped)
;            limitStatResult (map #(dissoc % :gVal) statResult)
;            statWithTimeResult (do-statistic statRules logGroupWithTime)
;            limitResultWithTime (showLimitResult statWithTimeResult)
        ]
        (println (first whereRules))
        
        {
            :logtable limitResult
            ;:grouptable limitResultWithTime,
            ;:meta (k/collect statResult)
        }
    )
)