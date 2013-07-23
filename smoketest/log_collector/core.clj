(ns smoketest.log-collector.core
    (:use 
        testing.core
    )
    (:require
        [clojure.data.json :as json]
        [utilities.shutil :as sh]
        [zktools.core :as zk]
        [kfktools.core :as kfk]
        [log-collector.core :as lc]
    )
    (:import
        java.nio.charset.Charset
        [java.nio.file Path]
    )
)

(suite "main entry"
    (:testbench
        (fn [test]
            (let [rt (sh/tempdir "log_collector_st")
                    zkdir (sh/getPath rt "zk")
                    kfkdir (sh/getPath rt "kfk")
                    kfkprp (sh/getPath rt "kafka.properties")
                    lccfg (sh/getPath rt "yy.cfg")
                ]
                (try
                    (sh/mkdir zkdir)
                    (sh/mkdir kfkdir)
                    (sh/spitFile (sh/getPath rt "xx.log")
"2013-06-01 22:48:59,466 INFO Client: msg1
2013-06-01 22:48:60,466 DEBUG Client: msg2
"
                    )
                    (sh/spitFile lccfg (format 
"{
    :hdfs.data-node {
        :base \"%s\"
        :pattern \"xx[.]log$\" 
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str rt))
                    )
                    (with-open [z (zk/start 10240 zkdir)
                            k (kfk/start 
                                :zookeeper.connect "localhost:10240"
                                :broker.id 0
                                :log.dirs (.toAbsolutePath kfkdir)
                            )
                        ]
                        (kfk/createTopic "localhost:10240" "hdfs.data-node")
                        (test lccfg)
                    )
                (finally
                    (sh/rmtree rt)
                    (sh/rmtree kfkprp)
                ))
            )
        )
    )
    (:fact main 
        (fn [lccfg]
            (with-open [c (kfk/newConsumer 
                        :zookeeper.connect "localhost:10240" 
                        :group.id "alone" 
                        :auto.offset.reset "smallest"
                    )
                    lc (sh/newCloseableProcess 
                        (sh/popen 
                            ["java" "-cp" ".:build/log_collector.jar" 
                                "log_collector.main" (str lccfg)
                            ]
                        )
                    )
                ]
                (Thread/sleep 5000)
                (let [cseq (take 2 (kfk/listenTo c "hdfs.data-node"))]
                    (for [{message :message} (doall cseq)]
                        (-> message
                            (String. (Charset/forName "UTF-8"))
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_]
            [
                {
                    :timestamp 1370098139466
                    :level "INFO"
                    :location "Client"
                    :message "msg1"
                }
                {
                    :timestamp 1370098140466
                    :level "DEBUG"
                    :location "Client"
                    :message "msg2"
                }
            ]
        )
    )
)
