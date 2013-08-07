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


    (comment filter 
        #(empty?
            (filter nil? (vals %))
        )
        loglist
    )

(defn- filter-parse [loglist]
    loglist
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

(defn- delet-gVal [loglist]
    ()
)

(defn do-search [searchrules loglist]
   (let [eventFilter (get searchrules :eventRules)
            logFilted (event-search eventFilter loglist)
            parseRules (get searchrules :parseRules)
            parseResult (filter-parse 
                    (apply-parse parseRules logFilted)
                )
            limitResult (take-last 100 parseResult)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys parseResult)
            timeRule (:timeRule searchrules)
            logGroupWithTime (do-group-with-time groupKeys parseResult timeRule)
            statRules (get searchrules :statRules)
            statResult (do-statistic statRules logGrouped)
            limitStatResult (map #(dissoc % :gVal) statResult)
            statWithTimeResult (do-statistic statRules logGroupWithTime)
            limitResultWithTime (map #(dissoc % :gVal) statWithTimeResult)
        ]
        {
            :logtable limitResult,
            :grouptable limitResultWithTime,
            :groupall limitStatResult
        }
    )
)

