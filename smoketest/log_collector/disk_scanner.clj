(ns smoketest.disk-scanner
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
        (fn [base]
            (dsks/scan sort base #"haha[.]log.*")
        )
        :eq
        (fn [_] 
            (map sh/getPath 
                ["st_dsks/haha.log" "st_dsks/haha.log.2014-07-01"]
            )
        )
    )
)
