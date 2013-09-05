(ns smoketest.log-collector.disk-scanner
    (:require
        [utilities.core :as util]
        [utilities.shutil :as sh]
        [log-collector.disk-scanner :as dsks]
        [log-collector.log-line-parser :as llp]
    )
    (:use 
        testing.core
    )
    (:import
        [java.nio.file Files]
    )
)

(defn tb1 [test]
    (let [rt (sh/tempdir)]
        (try
            (test rt)
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

(suite "find log files"
    (:testbench tb1)
    (:fact scan-files:daily-rolling
        (fn [rt]
            (sh/spitFile (sh/getPath rt "haha.log.2014-07-02") "include")
            (sh/spitFile (sh/getPath rt "haha.log.2014-07-01") "include")
            (sh/spitFile (sh/getPath rt "haha.log") "include")
            (sh/spitFile (sh/getPath rt "others/haha.log") "exclude")
            (->>
                (dsks/scan {
                    :topic {
                        :base (str (.toAbsolutePath rt))
                        :pattern #"haha[.]log.*"
                    }
                })
                (map #(get % 1))
            )
        )
        :eq
        (fn [rt] 
            (->>
                ["haha.log.2014-07-01" "haha.log.2014-07-02" "haha.log"]
                (map #(sh/getPath rt %))
            )
        )
    )
    (:fact scan-files:numeric
        (fn [rt]
            (sh/spitFile (sh/getPath rt "haha.log.2") "include")
            (sh/spitFile (sh/getPath rt "haha.log.1") "include")
            (sh/spitFile (sh/getPath rt "haha.log") "include")
            (->>
                (dsks/scan {
                    :topic {
                        :base (str (.toAbsolutePath rt))
                        :pattern #"haha[.]log.*"
                        :sorter :numeric
                    }
                })
                (map #(get % 1))
            )
        )
        :eq
        (fn [rt] 
            (->>
                ["haha.log.2" "haha.log.1" "haha.log"]
                (map #(sh/getPath rt %))
            )
        )
    )
    (:fact filter-files:new-file
        (fn [rt]
            (let [foo (sh/getPath rt "foo")]
                (sh/spitFile foo "bar")
                (let [[fis fs] (dsks/filter-files {} [[:opt foo]])]
                    [
                        (into {}
                            (for [[fl [opt f]] fis]
                                [fl [opt f]]
                            )
                        )
                        fs
                    ]
                )
            )
        )
        :eq
        (fn [rt]
            [
                {"bar" [:opt (sh/getPath rt "foo")]}
                [[:opt (sh/getPath rt "foo") 0]]
            ]
        )
    )
    (:fact filter-files:new-empty-file
        (fn [rt]
            (let [foo (sh/getPath rt "foo")]
                (sh/spitFile foo "")
                (dsks/filter-files {} [[:topic foo]])
            )
        )
        :eq
        (fn [rt]
            [{} []]
        )
    )
    (:fact filter-files:no-new-lines
        (fn [rt]
            (let [foo (sh/getPath rt "foo")
                _ (sh/spitFile foo "bar")
                [fis _] (dsks/filter-files {} [[:topic foo]])
                [_ new-files] (dsks/filter-files fis [[:topic foo]])
                ]
                new-files
            )
        )
        :eq
        (fn [rt]
            []
        )
    )
    (:fact filter-files:new-lines
        (fn [rt]
            (let [foo (sh/getPath rt "foo")
                _ (sh/spitFile foo "bar\n")
                [fis _] (dsks/filter-files {} [[:topic foo]])
                _ (sh/spitFile foo "bar\nBAR\n")
                [_ new-files] (dsks/filter-files fis [[:topic foo]])
                ]
                new-files
            )
        )
        :eq
        (fn [rt]
            [[:topic (sh/getPath rt "foo") 4]]
        )
    )
    (:fact filter-files:rename-files
        (fn [rt]
            (let [foo1 (sh/getPath rt "foo1")
                _ (sh/spitFile foo1 "bar\n")
                [fis _] (dsks/filter-files {} [[:topic foo1]])
                foo2 (sh/getPath rt "foo2")
                _ (sh/spitFile foo1 "BAR\n")
                _ (sh/spitFile foo2 "bar\n")
                [_ new-files] (dsks/filter-files fis [[:topic foo1] [:topic foo2]])
                ]
                new-files
            )
        )
        :eq
        (fn [rt]
            (let [foo1 (sh/getPath rt "foo1")]
                [[:topic foo1 0]]
            )
        )
    )
    (:fact read-lines:updated
        (fn [rt]
            (let [foo (sh/getPath rt "foo")
                _ (sh/spitFile foo "1970-01-01 08:00:00,001 INFO func: xixi\n")
                [fis [[_ f offset]]] (dsks/filter-files {} [[:topic foo]])
                lines1 (doall (llp/read-logs {} f offset))
                _ (sh/spitFile foo "1970-01-01 08:00:00,001 INFO func: xixi
1970-01-01 08:00:00,010 INFO func: hehe")
                [_ [[_ f offset]]] (dsks/filter-files fis [[:topic foo]])
                lines2 (doall (llp/read-logs {} f offset))
                ]
                [(discard-host lines1) (discard-host lines2)]
            )
        )
        :eq
        (fn [rt]
            [
                [{:timestamp 1, :level "INFO", :location "func", :message "xixi"}]
                [{:timestamp 10, :level "INFO", :location "func", :message "hehe"}]
            ]
        )
    )
)
