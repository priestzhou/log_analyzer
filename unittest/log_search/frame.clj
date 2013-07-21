(ns unittest.log-search.frame
    (:use testing.core
        log-search.frame 
    )
)

(defn- get-test-loglist1 []
    (list
        {:message 
            "1970-01-01 08:00:00,001 INFO Class.func: hello world!"}
            {:message 
                "1970-01-01 08:00:00,002 INFO Class.func: hello world!"}
            {:message 
                "1970-01-01 08:00:00,003 INFO Class.func: hello world!"}
            {:message 
                "1970-01-01 08:00:00,004 INFO Class.func: hello world!"}
            )
)

(suite "check event filter "
    (:fact parse-null
        (let [
                psr ({:eventRules ()})
            ]
            (count 
                (do-search psr get-test-loglist1)
            )
        )
        :is
        4
    )
    (:fact filter-one
        (let [
                psr ({:eventRules 
                        (fn [mes]
                            (->>
                                (re-find #"003" mes)
                                nil?
                                not
                            )
                        )})
            ]
            (count 
                (do-search psr get-test-loglist1)
            )
        )
        :is
        3
    )
    (:fact filter-tow
        (let [
                psr ({:eventRules 
                        (list
                            (fn [mes]
                                (->>
                                    (re-find #"003" mes)
                                    nil?
                                    not
                                )
                            )
                            (fn [mes]
                                (->>
                                    (re-find #"001" mes)
                                    nil?
                                    not
                                )
                            )
                        )
                    })
            ]
            (count 
                (do-search psr get-test-loglist1)
            )
        )
        :is
        2
    )
    (:fact filter-all
        (let [
                psr ({:eventRules 
                        (list
                            (fn [mes]
                                (->>
                                    (re-find #"003" mes)
                                    nil?
                                    not
                                )
                            )
                            (fn [mes]
                                (->>
                                    (re-find #"1970" mes)
                                    nil?
                                    not
                                )
                            )
                        )
                    })
            ]
            (count 
                (do-search psr get-test-loglist1)
            )
        )
        :is
        0
    )
)

(defn- test-parse-1 []
    {:key "test-parse-1" :parser 
        (fn [mes]
            "pr-1"
        )
    }
)

(defn- test-parse-2 []
    {:key "test-parse-2" :parser 
        (fn [mes]
            "pr-2"
        )
    }
)

(defn- test-parse-nil []
    {:key "test-parse-nil" :parser 
        (fn [mes]
            nil
        )
    }
)

(suite "check parser"
    (:fact parse-one-keycheck
        (let [psr ({:parseRules test-parse-1})]
            (->> 
                (do-search psr get-test-loglist1)
                keys
                sort
            )
        )
        :is
        (->>
            (list :message "test-parse-1")
            sort
        )
    )
    (:fact parse-one-valuecheck
        (let [psr ({:parseRules test-parse-1})]
            (->> 
                (do-search psr get-test-loglist1)
                (#(get % "test-parse-1"))
            )
        )
        :is
        "pr-1"
    )
    (:fact parse-two-keycheck
        (let [psr ({:parseRules 
                (list 
                    test-parse-1
                    test-parse-2
                )
            })]
            (->> 
                (do-search psr get-test-loglist1)
                keys
                sort
            )
        )
        :is
        (->>
            (list :message "test-parse-1" "test-parse-2")
            sort
        )
    )
    (:fact parse-two-valuecheck2
        (let [psr ({:parseRules
                (list 
                    test-parse-1
                    test-parse-2
                )
            })]
            (->> 
                (do-search psr get-test-loglist1)
                (#(get % "test-parse-2"))
            )
        )
        :is
        "pr-2"
    )
    (:fact parse-two-valuecheck1
        (let [psr ({:parseRules
                (list 
                    test-parse-1
                    test-parse-2
                )
            })]
            (->> 
                (do-search psr get-test-loglist1)
               (#(get % "test-parse-1"))
            )
        )
        :is
        "pr-1"
    )
)

(comment suite "check-event-and-parse"
    (:fact 
    )
)