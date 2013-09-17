(ns smoketest.log-collector.log-line-parser
    (:use
        testing.core
        [log-collector.log-line-parser :only (read-logs)]
    )
    (:require
        [utilities.shutil :as sh]
    )
)

(defn tb [test]
    (let [rt (sh/tempdir)]
        (try
            (let [foo (sh/getPath rt "foo")]
                (sh/spitFile foo "1970-01-01 08:00:00,001 INFO func: xixi
1970-01-01 08:00:00,010 INFO func: hehe")
                (test foo)
            )
        (finally
            (sh/rmtree rt)
        ))
    )
)

(defn discard-host [logs]
    (for [l logs]
        (dissoc l :host)
    )
)

(suite "parse log lines"
    (:testbench tb)
    (:fact parse-log-lines:from-beginning
        (fn [foo]
            (->> (read-logs {} foo 0)
                (discard-host)
            )
        )
        :eq
        (fn [_]
            [
                {:timestamp 1, :level "INFO", :location "func", :message "xixi"}
                {:timestamp 10, :level "INFO", :location "func", :message "hehe"}
            ]
        )
    )
    (:fact parse-log-lines:from-middle
        (fn [foo]
            (->> (read-logs {} foo 40)
                (discard-host)
            )
        )
        :eq
        (fn [_]
            [
                {:timestamp 10, :level "INFO", :location "func", :message "hehe"}
            ]
        )
    )
)