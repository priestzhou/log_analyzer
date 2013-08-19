(ns log-search.frame
)

(defn- get-message [log]
    (get log :message)
)

(defn- event-search [fitlers loglist startTime endTime]
    (filter  
        (fn [log]
            (and (reduce 
                    #(and %1 %2)
                    true
                    (map
                        #(% (get-message log))
                        fitlers
                    )
                )
                (<
                    startTime
                    (:timestamp log)
                    endTime
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
        (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
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
            header
            :data 
            (map 
                (fn [log]
                    (println "log in map" log)
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

(defn- get-matchart [gTimeList loglist timeRule]
    (let [tf (:tf timeRule)
            ll (map #(tf (:timestamp %)) loglist)
        ]
        {
            :time-series 
            gTimeList
            :search-count 
            (map  
                #(count (filter (partial = %) ll))
                gTimeList
            )
        }
    )
)

(defn do-search [searchrules loglist]
    (let [eventFilter (:eventRules searchrules)            
            timeRule (:timeRule searchrules)
            startTime (eval (:startTime timeRule))
            endTime (+ startTime (:tw timeRule))
            logFilted (event-search eventFilter loglist startTime endTime)
            parseRules (:parseRules searchrules)
            parseResult (filter-parse 
                (apply-parse parseRules logFilted)
            )
            limitResult (showlog parseResult)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys parseResult)
            gTimeList (get-time-list timeRule startTime)
            matchchart (get-matchart gTimeList parseResult timeRule)
            logGroupWithTime (do-group-with-time groupKeys parseResult timeRule)
            statRules (:statRules searchrules)
            statResult (do-statistic statRules logGrouped)
            limitStatResult (map #(dissoc % :gVal) statResult)
            statWithTimeResult (do-statistic statRules logGroupWithTime)
            limitResultWithTime (showLimitResult 
                statWithTimeResult 
                timeRule 
                limitStatResult
                gTimeList
            )
        ]
        {
            :matchchart matchchart
            :logtable limitResult,
            :grouptable limitResultWithTime,
            :meta limitStatResult
        }
    )
)

