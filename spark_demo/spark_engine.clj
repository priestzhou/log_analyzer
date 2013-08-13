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

(defn- filter-parse [loglist]
;    ( filter 
;        #(empty?
;            (filter nil? (vals %))
;        )
        loglist
;    )
)

(defn do-search [searchrules rdd]
   (let [eventFilter (:eventRules searchrules)
            logFilted (event-search eventFilter rdd)
            parseRules (:parseRules searchrules)
            parseResult (filter-parse 
                    (apply-parse parseRules logFilted)
                )
;            limitResult (showlog parseResult)
;            groupKeys (get searchrules :groupKeys)
;            logGrouped (do-group groupKeys parseResult)
;            timeRule (:timeRule searchrules)
;            logGroupWithTime (do-group-with-time groupKeys parseResult timeRule)
;            statRules (:statRules searchrules)
;            statResult (do-statistic statRules logGrouped)
;            limitStatResult (map #(dissoc % :gVal) statResult)
;            statWithTimeResult (do-statistic statRules logGroupWithTime)
;            limitResultWithTime (showLimitResult statWithTimeResult)
        ]
        parseResult
        ;{
         ;   :logtable logFilted,
;            :grouptable limitResultWithTime,
;            :groupall limitStatResult
        ;}
    )
)