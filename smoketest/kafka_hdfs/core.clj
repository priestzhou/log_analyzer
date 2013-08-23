(ns smoketest.kafka-hdfs.core
    (:use
        testing.core
        [logging.core :only [defloggers]]
    )
    (:require
        [clojure.java.io :as io]
        [clojure.data.json :as json]
        [utilities.shutil :as sh]
        [utilities.core :as helpers]
        [kafka-hdfs.core :as kh]
        [kfktools.core :as kfk]
        [zktools.core :as zk]
    )
    (:import
        [java.net URI]
        [java.util NavigableMap]
        [java.util.concurrent ArrayBlockingQueue]
        [org.apache.hadoop.conf Configuration]
        [org.apache.hadoop.fs FileSystem]
    )
)

(defloggers debug info warn error)

(defn jpath->uri [^java.nio.file.Path p]
    (.toUri p)
)

(defn hpath->uri [^org.apache.hadoop.fs.Path p]
    (.toUri p)
)

(suite "basic operations on hdfs"
    (:testbench (fn [test]
        (let [rt (sh/tempdir)
            conf (Configuration.)
            ]
            (try
                (sh/mkdir rt)
                (.set conf "fs.file.impl" "org.apache.hadoop.fs.RawLocalFileSystem")
                (with-open [fs (FileSystem/get (URI/create "file:///") conf)]
                    (test rt fs)
                )
            (finally
                (sh/rmtree rt)
            ))
        )
    ))
    (:fact write-read-local-file
        (fn [rt fs]
            (let [uri (jpath->uri (sh/getPath rt "xixi"))
                f (org.apache.hadoop.fs.Path. uri)
                ]
                (with-open [out (.create fs f)]
                    (spit out "You are my sunshine,\n")
                )
                (with-open [out (.append fs f)]
                    (spit out "my only sunshine.\n")
                )
                (with-open [in (.open fs f)]
                    (slurp in)
                )
            )
        )
        :eq
        (fn [rt fs]
            "You are my sunshine,
my only sunshine.
"
        )
    )
    (:fact list-dir
        (fn [rt fs]
            (let [uri (jpath->uri (sh/getPath rt "xixi"))
                f (org.apache.hadoop.fs.Path. uri)
                ]
                (with-open [out (.create fs f)]
                    (spit out "You are my sunshine,\n")
                )
            )
            (let [uri (jpath->uri rt)
                f (org.apache.hadoop.fs.Path. uri)
                ]
                (let [xs (.listStatus fs f)]
                    (for [i (range (alength xs))
                        :let [x (aget xs i)]
                        ]
                        (->> x
                            (.getPath)
                            (hpath->uri)
                            (.relativize uri)
                            (str)
                        )
                    )
                )
            )
        )
        :eq
        (fn [_ _]
            ["xixi"]
        )
    )
)

(suite "consumer->queue: leverage timeout poll() of java.util.concurrent.BlockingQueue"
    (:testbench (fn [test]
        (let [rt (sh/tempdir)
                zkdir (sh/getPath rt "zk")
                kfkdir (sh/getPath rt "kfk")
                kfkprp (sh/getPath rt "kafka.properties")
                zkport "localhost:10240"
                kfkport "localhost:6667"
                topic "test"
            ]
            (try
                (sh/mkdir zkdir)
                (sh/mkdir kfkdir)
                (with-open [z (zk/start 10240 zkdir)
                        k (kfk/start 
                            :zookeeper.connect zkport
                            :broker.id 0
                            :log.dirs (.toAbsolutePath ^java.nio.file.Path kfkdir)
                        )
                    ]
                    (kfk/createTopic zkport topic)
                    (test zkport kfkport topic)
                )
            (finally
                (sh/rmtree rt)
                (sh/rmtree kfkprp)
            ))
        )
    ))
    (:fact timeout-consuming
        (fn [zkport kfkport topic]
            (let [q (ArrayBlockingQueue. 16)]
                (let [c (kfk/newConsumer 
                        :zookeeper.connect zkport
                        :group.id "me" 
                    )
                    stub (kh/assign-consumer-to-queue! c topic q)
                    m0 (kh/poll! q 500)
                    ]
                    (with-open [p (kfk/newProducer :metadata.broker.list kfkport)]
                        (kfk/produce p [{:topic topic :message (.getBytes "ohayou")}])
                    )
                    (.close c) ; shutdown consumer to flush its cache
                    (let [m1 (kh/poll! q 500)]
                        (shutdown-agents)
                        [m0 m1]
                    )
                )
            )
        )
        :eq
        (fn [zkport kfkport topic]
            [nil {:topic topic, :message "ohayou"}]
        )
    )
)

(declare read-fs')

(defn read-file [^java.io.File f]
    {(.toPath f) (slurp f)}
)

(defn read-dir [^java.io.File f]
    (let [fs (.listFiles f)]
        (apply merge
            (for [i (range (alength fs))
                :let [x (aget fs i)]
                ]
                (read-fs' x)
            )
        )
    )
)

(defn read-fs' [^java.io.File f]
    (if (.isDirectory f)
        (read-dir f)
        (read-file f)
    )
)

(defn read-fs [^java.nio.file.Path rt]
    (let [res (read-fs' (.toFile rt))]
        (into (sorted-map)
            (for [[k v] res]
                [(str (.relativize rt k)) v]
            )
        )
    )
)

(defn format-existents [^NavigableMap existents base]
    (into (sorted-map)
        (for [k (helpers/iterable->lazy-seq (.keySet existents))
            :let [[uri size] (.get existents k)]
            ]
            [k [(str (.relativize base uri)) size]]
        )
    )
)

(suite "save messages to hdfs"
    (:testbench (fn [test]
        (let [rt (sh/tempdir)
            conf (Configuration.)
            ]
            (try
                (sh/mkdir rt)
                (.set conf "fs.file.impl" "org.apache.hadoop.fs.RawLocalFileSystem")
                (with-open [fs (FileSystem/get (URI/create "file:///") conf)]
                    (test rt fs)
                )
            (finally
                (sh/rmtree rt)
            ))
        )
    ))
    (:fact scan-existent-files:empty
        (fn [rt fs]
            (let [uri (jpath->uri rt)]
                (format-existents (kh/scan-existents fs uri) uri)
            )
        )
        :eq
        (fn [rt fs]
            {}
        )
    )
    (:fact scan-existent-files:one
        (fn [rt fs]
            (sh/spitFile 
                (sh/getPath rt "1234-05-06/1234-05-06T07:08:09.123Z.test")
                "0123456789"
            )
            (let [uri (jpath->uri rt)
                tree-map (kh/scan-existents fs uri)
                ]
                (format-existents tree-map uri)
            )
        )
        :eq
        (fn [rt fs]
            {-23215049510877 ["1234-05-06/1234-05-06T07:08:09.123Z.test" 10]}
        )
    )
    (:fact when-hdfs-is-empty
        (fn [rt fs]
            (let [base (jpath->uri rt)
                q (doto
                    (ArrayBlockingQueue. 16)
                    (.put {
                        :topic "test" 
                        :message (json/write-str {
                            :timestamp -23215049510877
                            :level "INFO"
                            :location "Client"
                            :message "msg1"
                        })
                    })
                )
                existents (kh/scan-existents fs base)
                ]
                (kh/save->hdfs! fs base 5000 q existents)
                [
                    (format-existents existents base)
                    (read-fs rt)
                ]
            )
        )
        :eq
        (fn [rt fs]
            [
                {
                    -23215049510877 ["1234-05-06/1234-05-06T07:08:09.123Z.test" 81]
                }
                {
                    "1234-05-06/1234-05-06T07:08:09.123Z.test" 
                    "{\"timestamp\":-23215049510877,\"location\":\"Client\",\"message\":\"msg1\",\"level\":\"INFO\"}\n"
                }
            ]
        )
    )
    (:fact append-existent-file
        (fn [rt fs]
            (sh/spitFile 
                (sh/getPath rt "1234-05-06/1234-05-06T07:08:09.123Z.test")
                "{\"timestamp\":-23215049510877,\"location\":\"Client\",\"message\":\"msg1\",\"level\":\"INFO\"}\n"
            )
            (let [base (jpath->uri rt)
                q (doto
                    (ArrayBlockingQueue. 16)
                    (.put {
                        :topic "test" 
                        :message (json/write-str {
                            :timestamp -23215049510544
                            :level "DEBUG"
                            :location "Client"
                            :message "msg2"
                        })
                    })
                )
                existents (kh/scan-existents fs base)
                ]
                (kh/save->hdfs! fs base 5000 q existents)
                [
                    (format-existents existents base)
                    (read-fs rt)
                ]
            )
        )
        :eq
        (fn [rt fs]
            [
                {
                    -23215049510877 ["1234-05-06/1234-05-06T07:08:09.123Z.test" 164]
                }
                {
                    "1234-05-06/1234-05-06T07:08:09.123Z.test" 
                    "{\"timestamp\":-23215049510877,\"location\":\"Client\",\"message\":\"msg1\",\"level\":\"INFO\"}
{\"timestamp\":-23215049510544,\"location\":\"Client\",\"message\":\"msg2\",\"level\":\"DEBUG\"}
"
                }
            ]
        )
    )
    (:fact create-new-file-because-of-size
        (fn [rt fs]
            (sh/spitFile 
                (sh/getPath rt "1234-05-06/1234-05-06T07:08:09.123Z.test")
                "{\"timestamp\":-23215049510877,\"location\":\"Client\",\"message\":\"msg1\",\"level\":\"INFO\"}\n"
            )
            (let [base (jpath->uri rt)
                q (doto
                    (ArrayBlockingQueue. 16)
                    (.put {
                        :topic "test" 
                        :message (json/write-str {
                            :timestamp -23215049510544
                            :level "DEBUG"
                            :location "Client"
                            :message "msg2"
                        })
                    })
                )
                existents (kh/scan-existents fs base)
                ]
                (binding [kh/size-for-new-file 10]
                    (kh/save->hdfs! fs base 5000 q existents)
                )
                [
                    (format-existents existents base)
                    (read-fs rt)
                ]
            )
        )
        :eq
        (fn [rt fs]
            [
                {
                    -23215049510877 ["1234-05-06/1234-05-06T07:08:09.123Z.test" 82]
                    -23215049510544 ["1234-05-06/1234-05-06T07:08:09.456Z.test" 82]
                }
                {
                    "1234-05-06/1234-05-06T07:08:09.123Z.test" 
                    "{\"timestamp\":-23215049510877,\"location\":\"Client\",\"message\":\"msg1\",\"level\":\"INFO\"}\n"
                    "1234-05-06/1234-05-06T07:08:09.456Z.test"
                    "{\"timestamp\":-23215049510544,\"location\":\"Client\",\"message\":\"msg2\",\"level\":\"DEBUG\"}\n"
                }
            ]
        )
    )
)
