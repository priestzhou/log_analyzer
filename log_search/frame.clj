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

(defn- do-group [groupKeys loglist]
 
    (if (nil? groupKeys)
        loglist
        (group-by
            (fn [log]
                (reduce
                    #(assoc %1 %2 (get log %2))
                    {}
                    groupKeys
                )
            )
            loglist
        )
    )
)

(defn do-search [searchrules loglist]
   (let [eventFilter (get searchrules :eventRules)
            logFilted (event-search eventFilter loglist)
            parseRules (get searchrules :parseRules)
            parseResult (apply-parse parseRules logFilted)
            groupKeys (get searchrules :groupKeys)
            logGrouped (do-group groupKeys parseResult)
        ]
        logGrouped
    )
)




