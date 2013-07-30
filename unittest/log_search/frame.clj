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
        (count
            (do-search (sparser "003") test-loglist1)
        )
        :is
        1
    )
    (:fact searchparser-event-checkkey
        (let [psr (sparser "1970 | parse \":00,* INFO\" as test-parse-1")]
            (println psr)
            (->> 
                (do-search psr test-loglist1)
                first
                keys
            )
        )
        :is
        (list "test-parse-1" :message)        
    )
)


(suite "check event filter "
    (:fact parse-null
        (let [
                psr {:eventRules ()}
            ]
            (count 
                (do-search psr test-loglist1)
            )
        )
        :is
        4
    )
    (:fact filter-one
        (let [
                psr {:eventRules 
                        [
                            (fn [mes]
                                (->>
                                    (re-find #"003" mes)
                                    nil?
                                )
                            )
                        ]}
            ]
            (count 
                (do-search psr test-loglist1)
            )
        )
        :is
        3
    )
    (:fact filter-two
        (let [
                psr {:eventRules 
                        [
                            (fn [mes]
                                (->>
                                    (re-find #"003" mes)
                                    nil?
                                )
                            )
                            (fn [mes]
                                (->>
                                    (re-find #"001" mes)
                                    nil?
                                )
                            )
                        ]
                    }
            ]
            (count 
                (do-search psr test-loglist1)
            )
        )
        :is
        2
    )
    (:fact filter-all
        (let [
                psr {:eventRules 
                        [
                            (fn [mes]
                                (->>
                                    (re-find #"003" mes)
                                    nil?
                                )
                            ) 
                            (fn [mes]
                                (->>
                                    (re-find #"1970" mes)
                                    nil?
                                )
                            )
                        ]
                    }
            ]
            (count 
                (do-search psr test-loglist1)
            )
        )
        :is
        0
    )
)

(def ^:private test-parse-1 
    {:key "test-parse-1" :parser 
        (fn [mes]
            "pr-1"
        )
    }
)

(def ^:private test-parse-2 
    {:key "test-parse-2" :parser 
        (fn [mes]
            "pr-2"
        )
    }
)

(def ^:private test-parse-num
    {:key "num" :parser 
        (fn [mes]
            (re-find #"(?<=,)[0-9.]+(?= INFO)")
        )
    }
)

(def ^:private test-parse-nil 
    {:key "test-parse-nil" :parser 
        (fn [mes]
            nil
        )
    }
)

(suite "check parser"
    (:fact parse-one-keycheck
        (let [psr {:parseRules [test-parse-1]}]
            (->> 
                (do-search psr test-loglist1)
                first
                keys
            )
        )
        :is
        (list "test-parse-1" :message)
        
    )
    (:fact parse-one-valuecheck
        (let [psr {:parseRules [test-parse-1]}]
            (->> 
                (do-search psr test-loglist1)
                first
                (#(get % "test-parse-1"))
            )
        )
        :is
        "pr-1"
    )
    (:fact parse-two-keycheck
        (let [psr {:parseRules 
                [ 
                    test-parse-1
                    test-parse-2
                ]
            }]
            (->> 
                (do-search psr test-loglist1)
                first
                keys
            )
        )
        :is
        (list "test-parse-2" "test-parse-1" :message)

    )
    (:fact parse-two-valuecheck2
        (let [psr {:parseRules
                [ 
                    test-parse-1
                    test-parse-2
                ]
            }]
            (->> 
                (do-search psr test-loglist1)
                first
                (#(get % "test-parse-2"))
            )
        )
        :is
        "pr-2"
    )
    (:fact parse-two-valuecheck1
        (let [psr {:parseRules
                [ 
                    test-parse-1
                    test-parse-2
                ]
            }]
            (->> 
                (do-search psr test-loglist1)
                first
               (#(get % "test-parse-1"))
            )
        )
        :is
        "pr-1"
    )
)

(def ^:private testrule1
    {:eventRules  
        [
            (fn [mes]
                (->>
                    (re-find #"003" mes)
                    nil?
                    not
                )
            )
        ],
        :parseRules
        [test-parse-1]
    }
)

(def ^:private testrule-group
    {
        :parseRules
        [test-parse-1,test-parse-2],
        :groupKeys
        ["test-parse-1","test-parse-2"]
    }
)

(suite "check-goup"
    (:fact check-goup1-count
        (count (do-search testrule-group test-loglist1))
        :is
        1
    )
    (:fact check-goup1-key
        (->>
            (do-search testrule-group test-loglist1)
            first
            (#(get % :gKeys))
        )
        :is
        {"test-parse-1" "pr-1","test-parse-2" "pr-2"}
    )
    (:fact check-goup1-val-count
        (->>
            (do-search testrule-group test-loglist1)
            first
            (#(get % :gVal))
            count
        )
        :is
        4
    )    
)

(suite "check-whole-rule1"
    (:fact check-whole-rule1-count
        (->>
            (do-search testrule1 test-loglist1)
            count
        )
        :is
        1
    )
    (:fact check-whole-rule1-key
        (->>
            (do-search testrule1 test-loglist1)
            first
            keys
        )
        :is
        (list "test-parse-1" :message)
        
    )
    (:fact check-whole-rule1-value
        (->>
            (do-search testrule1 test-loglist1)
            first
            (#(get % "test-parse-1"))
        )
        :is
        "pr-1"
    )
)