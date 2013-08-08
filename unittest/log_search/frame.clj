(ns unittest.log-search.frame
    (:use testing.core
        log-search.frame
        log-search.searchparser 
    )
)

(def ^:private test-loglist1
    [
        {
            :timestamp 1375684054183
            :message 
            "1970-01-01 08:00:00,001 INFO Class.func: hello world!"}
        {
            :timestamp 1375684065183
            :message 
            "1970-01-01 08:00:00,002 INFO Class.func: hello world!"}
        {
            :timestamp 1375684074183
            :message 
            "1970-01-01 08:00:00,003 INFO Class.func: hello world!"}
        {
            :timestamp 1375684081183
            :message 
            "1970-01-01 08:00:00,004 INFO Class.func: hello world!"}
    ]
)

(suite "check with searchparser"
    (:fact searchparser-event-checkcount
        (->>
            (do-search (sparser "003") test-loglist1)
            :logtable
            (remove nil? )
            count
        )
        :is
        1
    )
    (:fact searchparser-parse-checkkey
        (let [psr (sparser "1970 | parse \":00,* INFO\" as test-parse-1")]
            (->> 
                (do-search psr test-loglist1)
                :logtable
                first
                keys
            )
        )
        :is
        (list "test-parse-1" :timestamp :message)        
    )
    (:fact searchparser-parse-checkkey2
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello*\" as parse-2")]
            (->> 
                (do-search psr test-loglist1)
                :logtable
                first
                keys
            )
        )
        :is
        (list "parse-2" "parse-1" :timestamp :message)
    )
    (:fact searchparser-group-checkkey
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count b by parse-1")]
            (->> 
                (do-search psr test-loglist1)
                :groupall
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001"}
    )
    (:fact searchparser-group-checkkey2
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count a by parse-1,parse-2")]
            (->> 
                (do-search psr test-loglist1)
                :groupall
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001","parse-2" "world!"}
    )
    (:fact searchparser-count-check-1
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count by parse-2")]
            (->> 
                (assoc psr :statRules 
                    [{:statOutKey "count-1",
                    :statFun (fn [l] 
                                (reduce #(+ %1 (read-string %2)) 0 l)
                            )                        
                        :statInKey "parse-1"
                    }]
                )
                (#(do-search % test-loglist1))
                :groupall
                first
                (#(get % "count-1"))
            )
        )
        :is
        10
    )
    (:fact searchparser-count-check-2
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|sum parse-1 by parse-2")]
            (->> 
                (do-search psr test-loglist1)
                :groupall
                first
                (#(get % "sum_parse-1"))
            )
        )
        :is
        10
    )
    (:fact searchparser-time-check-gkey1
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|sum parse-1 by parse-2" "60")]
            (->> 
                (do-search psr test-loglist1)
                :grouptable
                first
                :gKeys
            )
        )
        :is
        {"parse-2" "world!", :groupTime "08/05/2013 14:27:30"}
    )
)

