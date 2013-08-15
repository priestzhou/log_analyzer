(ns log-search.frame
)

(defn- get-message [log]
    (get log :message)
)

(defn- event-search [fitlers loglist]
    (filter  
        (fn [log]
            (reduce 
                #(and %1 %2)
                true
                (map
                    #(% (get-message log))
                    fitlers
                )
            )
        )
        loglist
    )
)

(defn- do-parse [parseRule log]
    (let [pkey (get parseRule :key)
            tparser (get parseRule :parser)
        ]
        {pkey (tparser log)}
    )
)


(defn- apply-parse [parseRules loglist]
    (map
        (fn [log]
            (let [psr (map 
                            #(do-parse % (get-message log)) 
                            parseRules
                        )
                ]
                (reduce merge log psr)
            )
        )
        loglist
    )
)

(defn- filter-parse [loglist]
    ( filter 
        #(empty?
            (filter nil? (vals %))
        )
        loglist
    )
)

(defn- do-group [groupKeys loglist]
    (if (nil? groupKeys)
        []
        (let [groupMap (group-by
                    (fn [log]
                        (reduce
                            #(assoc %1 %2 (get log %2))
                            {}
                            groupKeys
                        )
                    )
                    loglist
                )
                gKeys (keys groupMap)
            ]
            (map
                (fn [k] {:gKeys k,:gVal (get groupMap k)})
                gKeys
            )
        )
    )
)

(defn- do-group-with-time [groupKeys loglist timeRule]
    (if (or (nil? groupKeys) (nil? timeRule))
        []
        (let [groupMap (group-by
                    (fn [log]
                        (reduce
                            #(assoc %1 %2 (get log %2))
                            ;;{:gTime (getime log)}
                            {:groupTime 
                                ((:tf timeRule)
                                     (:timestamp log)
                                )
                            }
                            groupKeys
                        )
                    )
                    loglist
                )
                gKeys (keys groupMap)
            ]
            (map
                (fn [k] {:gKeys k,:gVal (get groupMap k)})
                gKeys
            )
        )
    )
)

(defn- static-fun [stRule log]
    (let [logVal (:gVal log)
            inKey (:statInKey stRule)
            statFun (:statFun stRule)
        ]
        (->>
            logVal
            (map #(get % inKey) )
            statFun
        )
    )
)

(defn- do-statistic [statRules loglist]
    (if (nil? statRules)
        loglist
        (map
            (fn [log]
                (reduce
                    #(assoc %1 
                        (:statOutKey %2)
                        (static-fun %2 log)
                    )
                    log
                    statRules
                )
            )
            loglist
        )
    )
)

(defn- dateformat [t]
    (.format 
        (java.text.SimpleDateFormat. "MM/dd/yyyy HH:mm:ss") 
        t
    )    
)

(defn- change-time [log]
    (update-in log [:timestamp] dateformat)
)

(defn- getime [log]
    (:timestamp log)
)

(defn- get-header [logkeys]
    (let [userkeys (filter string? logkeys)
            syskeys (filter keyword? logkeys)
            usedkeys (remove #(or (= % :message) (= % :timestamp) ) syskeys)
        ]
        (concat 
            [:timestamp]
            usedkeys
            userkeys
            [:message]
        )
    )
)

(defn- showlog [loglist]
    (let [limitLog (->>
                loglist
                (#(map change-time %))
                (#(sort-by getime %))
                (take-last 100)
                reverse
            )
            logkeys (keys (first limitLog))
            header (get-header logkeys)
        ]
        
        {:header 
            logkeys
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

(defn- showLimitResult [loglist]
    (let [ll (map #(dissoc % :gVal) loglist)
        ]
        (sort-by #(get-in % [:gKeys :groupTime]) ll)
    )
)

(defn do-search [searchrules loglist]
   (let [eventFilter (:eventRules searchrules)
            logFilted (event-search eventFilter loglist)
            parseRules (:parseRules searchrules)
            parseResult (filter-parse 
                    (apply-parse parseRules logFilted)
                )
            limitResult (showlog parseResult)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys parseResult)
            timeRule (:timeRule searchrules)
            logGroupWithTime (do-group-with-time groupKeys parseResult timeRule)
            statRules (:statRules searchrules)
            statResult (do-statistic statRules logGrouped)
            limitStatResult (map #(dissoc % :gVal) statResult)
            statWithTimeResult (do-statistic statRules logGroupWithTime)
            limitResultWithTime (showLimitResult statWithTimeResult)
        ]
        {
            :logtable limitResult,
            :grouptable limitResultWithTime,
            :meta limitStatResult
        }
    )
)

