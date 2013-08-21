(ns unittest.log-search.frame
    (:use testing.core
        log-search.frame
        log-search.searchparser 
    )
)

(def ^:private test-loglist1
    [
        {
            :timestamp 1375684054193
            :message 
            " 1970-01-01 08:00:00,001 INFO Class.func: hello world! "}
        {
            :timestamp 1375684065183
            :message 
            " 1970-01-01 08:00:00,002 INFO Class.func: hello world! "}
        {
            :timestamp 1375684074183
            :message 
            " 1970-01-01 08:00:00,003 INFO Class.func: hello world! "}
        {
            :timestamp 1375684081183
            :message 
            " 1970-01-01 08:00:00,004 INFO Class.func: hello world! "}
    ]
)

(suite "check with searchparser"
    (:fact searchparser-event-checkcount
        (->>
            (do-search (sparser "*003*" "300" 1375684054183 ) test-loglist1)
            :logtable
            :data
            count
        )
        :is
        1
    )
    (:fact searchparser-parse-checkkey1
        (let [psr (sparser "*1970* | parse \":00,* INFO\" as test-parse-1"
                "300" 1375684054183)]
            (->> 
                (do-search psr test-loglist1)
                :logtable
                :header
            )
        )
        :is
        (list :timestamp "test-parse-1" :message)        
    )
    (:fact searchparser-parse-checkkey2
        (let [psr (sparser  "*1970* | parse \":00,* INFO\" as parse-1
                | parse \"hello*\" as parse-2"
                "300" 1375684054183
                )]
            (->> 
                (do-search psr test-loglist1)
                :logtable
                :header
            )
        )
        :is
        (list :timestamp "parse-2" "parse-1"  :message)
    )
    (:fact searchparser-group-checkkey1
        (let [psr (sparser  "*1970-01-01 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count b by parse-1"
                "300" 1375684054183
                )]
            (->> 
                (do-search psr test-loglist1)
                :meta
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001"}
    )
    (:fact searchparser-group-checkkey2
        (let [psr (sparser  "*1970* | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count a by parse-1,parse-2"
                "300" 1375684054183)]
            (->> 
                (do-search psr test-loglist1)
                :meta
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001","parse-2" "world!"}
    )
    (:fact searchparser-count-check-1
        (let [psr (sparser  "1970* | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count parse-1 by parse-2"
                "300" 1375684054183)]
            (->> 
                psr
                (#(do-search % test-loglist1))
                :meta
                first
                (#(get % "count_parse-1"))
            )
        )
        :is
        4
    )
    (:fact searchparser-count-check-2
        (let [psr (sparser  "*1970* | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|sum parse-1 by parse-2"
                "300" 1375684054183)]
            (->> 
                (do-search psr test-loglist1)
                :meta
                first
                (#(get % "sum_parse-1"))
            )
        )
        :is
        10
    )
    (:fact searchparser-time-check-gkey1
        (let [psr (sparser  "*1970* | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|sum parse-1 by parse-2" 
                "300" 1375684054183)]
            (->> 
                (do-search psr test-loglist1)
                :grouptable
                first
            )
        )
        :is
        {:timestamp "2013-08-05 14:27:30", :events [{:gId 0, "sum_parse-1" 1}]}
    )
)

