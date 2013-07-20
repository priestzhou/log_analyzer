(ns unittest.log-search.frame
    (:use testing.core
        log-search.frame 
    )
)

(suite "check event filter "
    (:fact parse-null
        (let [loglist 
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
            ]
            (count 
                (do-search [] loglist)
            )
        )
        :is
        4
    )
)