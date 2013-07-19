(ns log-consumer.logconsumer
    (:use log-consumer.logparser)
    (:require
        [kfktools.core :as kfk]
    )
    (:import
        [java.util Properties ArrayList HashMap]
        [java.nio.file Path Files]
        [java.nio.file.attribute FileAttribute]
        [kfktools ConsumerWrapper]
    )
)

(defn wait-and-run [log-atom log-reload]
    (Thread/sleep 5000)
    (log-reload log-atom)
    (recur log-atom log-reload)
)

(defn reload-from-file [log-atom]
    (let [fpath "/Users/zhangjun/Desktop/test.log"]
        (->>
            (slurp fpath)
            (#(.split % "\n"))
            (parse-log parse-rules)
            after-parse
            (reset! log-atom)
        )
    )
)
(defn- parse-step-kfk [logstep]
    (->>
        logstep
        (parse-log-kfk parse-rules)
        after-parse
    )
)

(defn consumer-from-kfk [zkstr tpstr goupstr log-atom]
    (with-open [c (kfk/newConsumer
        :zookeeper.connect zkstr
        :group.id goupstr
        :auto.offset.reset "largest")
        ]
        (let [cseq (kfk/listenTo c tpstr)
            pcseq (partition-all 100 cseq)
            ]
            (->>
                pcseq
                (map 
                    #(reset! 
                        (concat
                            @log-atom
                            (parse-step-kfk %) 
                        )
                    )
                )
                doall
            )
        )
    )
)