(ns smoketest.log-collector.disk-scanner
    (:require
        [utilities.shutil :as sh]
        [log-collector.disk-scanner :as dsks]
    )
    (:use 
        testing.core
    )
    (:import
        [java.nio.file Files]
    )
)

(defn tb1 [test]
    (let [path "st_dsks"]
        (sh/rmtree path)
        (try
            (sh/spitFile (sh/getPath path "haha.log.2014-07-02") "include")
            (sh/spitFile (sh/getPath path "haha.log.2014-07-01") "include")
            (sh/spitFile (sh/getPath path "haha.log") "include")
            (sh/spitFile (sh/getPath path "others/haha.log") "exclude")
            (test path)
        (finally
            (sh/rmtree path)
        ))
    )
)

(suite "find log files"
    (:testbench tb1)
    (:fact disk-scanner-1 
        (fn [rt]
            (dsks/scan dsks/sort-daily-rolling rt #"haha[.]log.*")
        )
        :eq
        (fn [rt] 
            (map #(sh/getPath rt %)
                ["haha.log" "haha.log.2014-07-02" "haha.log.2014-07-01"]
            )
        )
    )
)
