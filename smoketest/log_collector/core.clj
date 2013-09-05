(ns smoketest.log-collector.core
    (:use 
        testing.core
    )
    (:require
        [clojure.data.json :as json]
        [utilities.shutil :as sh]
        [utilities.net :as net]
        [zktools.core :as zk]
        [kfktools.core :as kfk]
        [log-collector.core :as lc]
        [kafka-hdfs.core :as kfk->hdfs]
    )
    (:import
        [java.nio.charset StandardCharsets]
        [java.nio.file Path]
        [java.util.concurrent BlockingQueue TimeUnit ArrayBlockingQueue]
    )
)

(defn- tb1 [test]
    (let [rt (sh/tempdir "log_collector_st")
            zkdir (sh/getPath rt "zk")
            kfkdir (sh/getPath rt "kfk")
            kfkprp (sh/getPath rt "kafka.properties")
            lccfg (sh/getPath rt "yy.cfg")
        ]
        (try
            (sh/mkdir zkdir)
            (sh/mkdir kfkdir)
            (sh/spitFile (sh/getPath rt "xx.log.2013-06-01")
"2013-06-01 00:00:00,000 INFO Client: msg1
"
            )
            (sh/spitFile (sh/getPath rt "xx.log.2013-06-02")
"2013-06-02 00:00:00,000 INFO Client: msg2
"
            )
            (sh/spitFile (sh/getPath rt "xx.log")
"2013-06-03 00:00:00,000 INFO Client: msg3
"
            )
            (with-open [z (zk/start 10240 zkdir)
                k (kfk/start 
                    :zookeeper.connect "localhost:10240"
                    :broker.id 0
                    :log.dirs (.toAbsolutePath kfkdir)
                )
                ]
                (kfk/createTopic "localhost:10240" "hdfs.data-node")
                (test rt lccfg)
            )
        (finally
            (sh/rmtree rt)
            (sh/rmtree kfkprp)
        ))
    )
)

(suite "main entry"
    (:testbench tb1)
    (:fact main:without-cpt
        (fn [rt lccfg]
            (sh/spitFile lccfg (format 
"{
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str rt))
            )
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
                (let [cseq (take 3 (kfk/listenTo c "hdfs.data-node"))]
                    (for [{message :message} (doall cseq)]
                        (-> message
                            (String. (StandardCharsets/UTF_8))
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_ _]
            [
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370016000000
                    :level "INFO"
                    :location "Client"
                    :message "msg1"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370102400000
                    :level "INFO"
                    :location "Client"
                    :message "msg2"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370188800000
                    :level "INFO"
                    :location "Client"
                    :message "msg3"
                }
            ]
        )
    )
    (:fact main:with-cpt
        (fn [rt lccfg]
            (sh/spitFile lccfg (format 
"{
    :myself {
        :checkpoint \"%s\"
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
    }
}
" (str (sh/getPath rt "log_collector.cpt")) (str rt)))
            (sh/spitFile (sh/getPath rt "log_collector.cpt")
"{
    \"2013-06-01 00:00:00,000 INFO Client: msg1\" [\"xx.log.2013-06-01\" 42]
}
")
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
                            (String. (StandardCharsets/UTF_8))
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_ _]
            [
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370102400000
                    :level "INFO"
                    :location "Client"
                    :message "msg2"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370188800000
                    :level "INFO"
                    :location "Client"
                    :message "msg3"
                }
            ]
        )
    )

    (:fact customized-sorter
        (fn [rt lccfg]
            (sh/spitFile lccfg (format 
"
(defn my-sorter [files] (sort files))

{
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
        :sorter my-sorter
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str rt))
            )
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
                (let [cseq (take 3 (kfk/listenTo c "hdfs.data-node"))]
                    (for [{message :message} (doall cseq)]
                        (-> message
                            (String. (StandardCharsets/UTF_8))
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_ _]
            [
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370188800000
                    :level "INFO"
                    :location "Client"
                    :message "msg3"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370016000000
                    :level "INFO"
                    :location "Client"
                    :message "msg1"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370102400000
                    :level "INFO"
                    :location "Client"
                    :message "msg2"
                }
            ]
        )
    )

    (:fact customized-parser
        (fn [rt lccfg]
            (sh/spitFile lccfg (format 
"
(defn my-parser [ln]
    (case ln
        \"2013-06-01 00:00:00,000 INFO Client: msg1\" [1 \"A\" \"AA\" \"AAA\"]
        \"2013-06-02 00:00:00,000 INFO Client: msg2\" [2 \"B\" \"BB\" \"BBB\"]
        \"2013-06-03 00:00:00,000 INFO Client: msg3\" [3 \"C\" \"CC\" \"CCC\"]
        nil
    )
)

{
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
        :parser my-parser
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str rt))
            )
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
                (let [cseq (take 3 (kfk/listenTo c "hdfs.data-node"))]
                    (for [{message :message} (doall cseq)]
                        (-> message
                            (String. (StandardCharsets/UTF_8))
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_ _]
            [
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1
                    :level "A"
                    :location "AA"
                    :message "AAA"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 2
                    :level "B"
                    :location "BB"
                    :message "BBB"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 3
                    :level "C"
                    :location "CC"
                    :message "CCC"
                }
            ]
        )
    )
)

(defn tb2 [test]
    (let [rt (sh/tempdir "log_collector_st")
            zkdir (sh/getPath rt "zk")
            kfkdir (sh/getPath rt "kfk")
            kfkprp (sh/getPath rt "kafka.properties")
            lccfg (sh/getPath rt "yy.cfg")
        ]
        (try
            (sh/mkdir zkdir)
            (sh/mkdir kfkdir)
            (sh/spitFile lccfg (format 
"
{
    :myself {
        :checkpoint \"%s\"
    }
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str (sh/getPath rt "log_collector.cpt")) (str rt))
            )
            (with-open [z (zk/start 10240 zkdir)
                k (kfk/start 
                    :zookeeper.connect "localhost:10240"
                    :broker.id 0
                    :log.dirs (.toAbsolutePath kfkdir)
                )
                ]
                (kfk/createTopic "localhost:10240" "hdfs.data-node")
                (test rt lccfg)
            )
        (finally
            (sh/rmtree rt)
            (sh/rmtree kfkprp)
        ))
    )
)

(defn read-msg [q]
    (if-let [msg (kfk->hdfs/poll! q 5000)]
        (let [{:keys [message]} msg]
            (-> message
                (String. (StandardCharsets/UTF_8))
                (json/read-str :key-fn keyword)
            )
        )
    )
)

(defn new-consumer []
    (kfk/newConsumer 
        :zookeeper.connect "localhost:10240" 
        :group.id "alone" 
        :auto.offset.reset "smallest"
    )
)

(defn start-log-collector [lccfg]
    (sh/newCloseableProcess 
        (sh/popen 
            ["java" "-cp" ".:build/log_collector.jar" 
                "log_collector.main" (str lccfg)
            ]
        )
    )
)

(defn read-until-nothing' [q result]
    (if-let [x (read-msg q)]
        (recur q (conj result x))
        (conj result nil)
    )
)

(defn read-until-nothing [lccfg q]
    (with-open [
        lc (start-log-collector lccfg)
        c (new-consumer)
        ]
        (let [stubs (kfk->hdfs/assign-consumer-to-queue! c "hdfs.data-node" q)]
            (Thread/sleep 1000)
            (.close c)
            (Thread/sleep 1000)
            (read-until-nothing' q [])
        )
    )
)

(suite "checkpoint: which logs have been sent"
    (:testbench tb2)
    (:fact main:cpt-effect
        (fn [rt lccfg]
            (let [q (ArrayBlockingQueue. 16)
                _ (sh/spitFile (sh/getPath rt "xx.log.2013-06-01")
                    "2013-06-01 00:00:00,000 INFO Client: msg1\n"
                )
                _ (sh/spitFile (sh/getPath rt "xx.log.2013-06-02")
                    "2013-06-02 00:00:00,000 INFO Client: msg2\n"
                )
                ms1 (read-until-nothing lccfg q)
                _ (sh/spitFile (sh/getPath rt "xx.log.2013-06-02")
                    "2013-06-02 00:00:00,000 INFO Client: msg2\n2013-06-03 00:00:00,000 INFO Client: msg3\n"
                )
                ms2 (read-until-nothing lccfg q)
                ]
                (shutdown-agents)
                (concat ms1 ms2)
            )
        )
        :eq
        (fn [rt lccfg]
            [
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370016000000
                    :level "INFO"
                    :location "Client"
                    :message "msg1"
                }
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370102400000
                    :level "INFO"
                    :location "Client"
                    :message "msg2"
                }
                nil
                {
                    :host (-> (net/localhost) (first) (.getHostAddress))
                    :timestamp 1370188800000
                    :level "INFO"
                    :location "Client"
                    :message "msg3"
                }
                nil
            ]
        )
    )
)
