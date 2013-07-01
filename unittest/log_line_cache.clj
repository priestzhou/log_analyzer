(ns unittest.log-line-cache
    (:use testing.core
        log-collector.log-line-cache
    )
)

(suite "log line cache: return uncached log line"
    (:testbench
        (fn [test]
            (cache-log-line {:timestamp 1, :message "xixi"})
            (test)
        )
    )
    (:fact log-line-cache 
        (map cache-log-line 
            [
                {:timestamp 1, :message "xixi"}
                {:timestamp 2, :message "hehe"}
            ]
        )
        :is
        [nil {:timestamp 2, :message "hehe"}]
    )
)
