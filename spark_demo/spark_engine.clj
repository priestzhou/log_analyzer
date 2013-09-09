(ns spark-demo.spark-engine
    (:import 
         spark.api.java.JavaSparkContext
         spark.storage.StorageLevel
    )    
    (:require 
        [serializable.fn :as sfn]
        [clj-spark.api :as k]
    )
    (:use
        [logging.core :only [defloggers]]
    )
)

(defloggers debug info warn error)

(defn- event-search [fitlers rdd startTime endTime]
    (println "event-search")
    (k/filter rdd
        (sfn/fn f [log]
            (and
                (<
                    startTime
                    (get log "timestamp")
                    endTime
                )
                ((first fitlers) (get log "message"))
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
    (println "do-parse")
    (let [pkey (get parseRule :key)
            tparser (get parseRule :parser)
        ]
        {pkey (tparser log)}
    )
)


(defn- apply-parse [parseRules rdd]
    (println "apply-parse")
    (k/map rdd
        (sfn/fn [log]
            (let [psr (map 
                            #(do-parse % (get log "message")) 
                            parseRules
                        )
                ]
                (reduce merge log psr)
            )
        )
    )
)

(defn- filter-parse [rdd]
    (println "filter-parse")
    (->
        rdd
        (k/filter 
            (sfn/fn [l]
                (empty?
                    (filter nil? (vals l))
                )
            )
        )
        (k/persist (StorageLevel/MEMORY_AND_DISK))
        ;k/cache
    )
)

(defn- dateformat [t]
    (.format 
        (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
        t
    )    
)

(defn- change-time [log]
    (update-in log ["timestamp"] dateformat)
)

(defn- get-newest-log [rdd]
    (println "get-newest-log")
    (->
        rdd
        (k/map
            (sfn/fn [log]
                (change-time log)
            )
        )
        ;(k/sort-by-key compare false)
        (k/takeSample 100)
        doall
    )
)

(defn- get-header [logkeys]
    (let [  userkeys (filter string? logkeys)
            syskeys (filter keyword? logkeys)
            usedkeys (remove #{"message" "timestamp"} userkeys)
        ]
        (concat 
            ["timestamp"]
            syskeys
            usedkeys
            ["message"]
        )
    )
)
 
(defn- showlog [rdd]
    (info "showlog" )
    (let [limitLog (get-newest-log
                rdd
            )
            logkeys (keys (first limitLog))
            header (get-header logkeys)
        ]
        {:header 
            header
            :data 
            (map 
                (fn [log]
                    (map
                        #(get log %)
                        header
                    )
                )
                limitLog
            )
        }
    )
)

(defn- get-event-item [t i v loglist]
    (let [filterlist (filter
                #(and 
                    (= t (get-in % [:gKeys :groupTime]))
                    (= v (dissoc (:gKeys %) :groupTime))
                )
                loglist
            )
        ]
        (if (empty? filterlist)
            nil
            (assoc 
                (dissoc (first filterlist) :gKeys :gVal)
                :gId i
            )
        )
    )
)

(defn- showLimitResult [loglist timeRule metaData gTimeList]
    (println "showLimitResult")
    (let [ll (map #(dissoc % :gVal) loglist)
            sortlist (sort-by #(get-in % [:gKeys :groupTime]) ll)
            metaValue (map #(:gKeys %) metaData)
        ]
        (map 
            (fn [t]
                {:timestamp t,
                    :events
                    (remove
                        nil?
                        (map-indexed
                            (fn [i k]
                                (get-event-item t i k loglist)
                            )
                            metaValue
                        )                        
                    )
                }
            )
            gTimeList
        )
    )
)

(defn- get-time-list [timeRule startTime]
    (let [timelist1 (iterate #(+ 5000 %) startTime)
            step (/ (:tw timeRule) 5000)
            timelist (take 
                step
                timelist1
            )
            gTimeList (distinct (map (:tf timeRule) timelist))
        ]
        gTimeList
    )
)

(defn- get-matchart [gTimeList rdd timeRule]
    (println "get-matchart")
    (let [ll (k/map 
                rdd 
                (sfn/fn [log] 
                    [((:tf timeRule) (get log "timestamp")) 1]
                ) 
            )
        ]
        {
            :time-series 
            gTimeList
            :search-count 
            (->
                ll
                (k/reduce-by-key +)
                k/collect
                ((sfn/fn [cl]
                    (map  
                        (sfn/fn [gt]
                            (last(last (filter #(= gt (first %)) cl)))
                        )  
                        gTimeList
                    )
                )  )
                doall
            )
        }
    )
)

(defn- get-key-func [groupKeys timeRule]
    (let [startmap (if (nil? timeRule)
                (sfn/fn ft1 [log] {})
                (sfn/fn ft2 [log]
                    (println log)
                    (println timeRule)
                    {:groupTime
                        ((:tf timeRule)
                            (get log "timestamp")
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
    (println "do-group")
    (let [keyFunc (get-key-func groupKeys timeRule)]        
        (-> rdd
            (k/map  
                (sfn/fn gen-key [log]
                    [{:gKeys (keyFunc log)} log]
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
                (sfn/fn f [l](get l inKey) )
            )
            statFun
        )
    )
)

(defn- do-statistic [statRules rdd]
    (println "do-statistic")
    (if (nil? statRules)
        []
        (->
            rdd
            (k/map 
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
            (k/map first)
            (k/collect)
            doall
        )
    )
)

(defn do-search [searchrules rdd]
    (info "do-search run")
   (let [eventFilter (:eventRules searchrules)
            timeRule (:timeRule searchrules)
            startTime (eval (:startTime timeRule))
            endTime (+ startTime (:tw timeRule))
            gTimeList (get-time-list timeRule startTime)            
            logFilted (event-search eventFilter rdd startTime endTime)

            whereRules (:whereRules searchrules)
            parseRules (:parseRules searchrules)
            parseResult (filter-parse 
                    (apply-parse parseRules logFilted)
                )
            limitResult (showlog parseResult)
            matchchart (get-matchart gTimeList parseResult timeRule)
            whereResult (where-filter whereRules parseResult)
            timeRule (:timeRule searchrules)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys whereResult nil)
            logGroupWithTime (do-group groupKeys parseResult timeRule)
            statRules (:statRules searchrules)
            statResult (do-statistic statRules logGrouped)
            statWithTimeResult (do-statistic statRules logGroupWithTime)
            limitResultWithTime (showLimitResult 
               statWithTimeResult
                timeRule 
                statResult
                gTimeList
            )
        ]
        (info "do-search sucessful")
        {
            :logtable limitResult
            :grouptable limitResultWithTime
            :meta statResult
            :matchchart matchchart
        }
    )
)