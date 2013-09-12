(ns log-search.search-driver
    (:import 
        com.flyingsand.spark.Spark_engine
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


(defn- dateformat [t]
    (.format 
        (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
        t
    )    
)

(defn- change-time [log]
    (update-in log ["timestamp"] dateformat)
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
 
(defn- showlog [inlog]
    (info "showlog" )
    (let [
            limitLog (->> 
                    inlog
                    (map #(into {} %) )
                    (map change-time)
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



(defn do-search [searchrules rdd output]
    (info "do-search run")

    (let [se (Spark_engine. searchrules rdd)
            fr (.getFilterResult se)
            llog (.takeSample fr false 100 9)
            loglist (showlog llog)
            logtable {:logtable loglist}
            p1 (reset! output logtable)
            sr (.getGroupResult se)
            statResult (.collect sr)
            p2Result (assoc logtable :meta statResult)
            t2 (println statResult )
            p2 (reset! output p2Result)
        ]
    )
   (comment let [eventFilter (:eventRules searchrules)
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