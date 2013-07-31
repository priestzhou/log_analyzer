(ns unittest.log-search.frame
    (:use testing.core
        log-search.frame
        log-search.searchparser 
    )
)

(def ^:private test-loglist1
    [
        {:message 
            "1970-01-01 08:00:00,001 INFO Class.func: hello world!"}
        {:message 
            "1970-01-01 08:00:00,002 INFO Class.func: hello world!"}
        {:message 
            "1970-01-01 08:00:00,003 INFO Class.func: hello world!"}
        {:message 
            "1970-01-01 08:00:00,004 INFO Class.func: hello world!"}
    ]
)

(suite "check with searchparser"
    (:fact searchparser-event-checkcount
        (->>
            (do-search (sparser "003") test-loglist1)
            :logtable
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
        (list "test-parse-1" :message)        
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
        (list "parse-2" "parse-1" :message)
    )
    (:fact searchparser-group-checkkey
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count by parse-1")]
            (->> 
                (do-search psr test-loglist1)
                :grouptable
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001"}
    )
    (:fact searchparser-group-checkkey2
        (let [psr (sparser "1970 | parse \":00,* INFO\" as parse-1
                | parse \"hello *\" as parse-2|count by parse-1,parse-2")]
            (->> 
                (do-search psr test-loglist1)
                :grouptable
                first
                :gKeys
            )
        )
        :is
        {"parse-1" "001","parse-2" "world!"}
    )
)

