(ns smoketest.log-collector.core
    (:use 
        testing.core
    )
    (:require
        [clojure.data.json :as json]
        [utilities.core :as util]
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

(defn new-consumer []
    (kfk/newConsumer 
        :zookeeper.connect "localhost:10240" 
        :group.id "alone" 
        :auto.offset.reset "smallest"
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
                (with-open [c (new-consumer)]
                    (test rt lccfg (kfk/listenTo c "hdfs.data-node"))
                )
            )
        (finally
            (sh/rmtree rt)
        ))
    )
)

(suite "main entry"
    (:testbench tb1)
    (:fact main:without-cpt
        (fn [rt lccfg consumer]
            (sh/spitFile lccfg (format 
"{
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
            (with-open [
                lc (sh/newCloseableProcess 
                    (sh/popen 
                        ["java" "-cp" ".:build/log_collector.jar" 
                            "log_collector.main" (str lccfg)
                        ]
                    )
                )
                ]
                (Thread/sleep 5000)
                (let [cseq (doall (take 3 consumer))]
                    (for [{message :message} cseq]
                        (-> message
                            (util/bytes->str)
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [& _]
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
        (fn [rt lccfg consumer]
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
            (with-open [
                lc (sh/newCloseableProcess 
                    (sh/popen 
                        ["java" "-cp" ".:build/log_collector.jar" 
                            "log_collector.main" (str lccfg)
                        ]
                    )
                )
                ]
                (Thread/sleep 5000)
                (let [cseq (doall (take 2 consumer))]
                    (for [{message :message} cseq]
                        (-> message
                            (util/bytes->str)
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [& _]
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
        (fn [rt lccfg consumer]
            (sh/spitFile lccfg (format 
"
(defn my-sorter [files] (sort files))

{
    :myself {
        :checkpoint \"%s\"
    }
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
        :sorter my-sorter
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str (sh/getPath rt "cpt")) (str rt))
            )
            (with-open [
                lc (sh/newCloseableProcess 
                    (sh/popen 
                        ["java" "-cp" ".:build/log_collector.jar" 
                            "log_collector.main" (str lccfg)
                        ]
                    )
                )
                ]
                (Thread/sleep 5000)
                (let [cseq (doall (take 3 consumer))]
                    (for [{message :message} cseq]
                        (-> message
                            (util/bytes->str)
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [& _]
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
        (fn [rt lccfg consumer]
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
    :myself {
        :checkpoint \"%s\"
    }
    :hdfs.data-node {
        :base \"%s\"
        :pattern #\"^xx[.]log.*\" 
        :parser my-parser
    }
    :kafka {
        :metadata.broker.list \"localhost:6667\"
    }
}
" (str (sh/getPath rt "cpt")) (str rt))
            )
            (with-open [
                lc (sh/newCloseableProcess 
                    (sh/popen 
                        ["java" "-cp" ".:build/log_collector.jar" 
                            "log_collector.main" (str lccfg)
                        ]
                    )
                )
                ]
                (Thread/sleep 5000)
                (let [cseq (doall (take 3 consumer))]
                    (for [{message :message} cseq]
                        (-> message
                            (util/bytes->str)
                            (json/read-str :key-fn keyword)
                        )
                    )
                )
            )
        )
        :eq
        (fn [& _]
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
                c (new-consumer)
                ]
                (kfk/createTopic "localhost:10240" "hdfs.data-node")
                (test rt lccfg c)
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

(defn wait-until-something [q]
    (if-let [x (read-msg q)]
        x
        (do
            (Thread/sleep 500)
            (recur q)
        )
    )
)

(defn read-until-nothing [lccfg q]
    (with-open [
        lc (start-log-collector lccfg)
        ]
        (let [x (wait-until-something q)]
            (read-until-nothing' q [x])
        )
    )
)

(suite "checkpoint: which logs have been sent"
    (:testbench tb2)
    (:fact main:cpt-effect
        (fn [rt lccfg consumer]
            (let [q (ArrayBlockingQueue. 16)
                stub (kfk->hdfs/assign-consumer-to-queue! consumer "hdfs.data-node" q)
                _ (Thread/sleep 100)
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
        (fn [& _]
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
