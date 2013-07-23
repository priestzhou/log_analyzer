(ns log-consumer.logconsumer
    (:use log-consumer.logparser)
    (:require
        [kfktools.core :as kfk]
        [clojure.data.json :as json]
    )
    (:import
        [java.util Properties ArrayList HashMap]
        [java.nio.file Path Files]
        [java.nio.file.attribute FileAttribute]
        [kfktools ConsumerWrapper]
        java.nio.charset.Charset
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

(defn- decode-kfk [message]
    (-> message
        (#(get % :message))
        (String. (Charset/forName "UTF-8"))
        (json/read-str :key-fn keyword)
    )
)

(defn consumer-from-kfk [zkstr tpstr goupstr log-atom]
    (with-open [c (kfk/newConsumer
        :zookeeper.connect zkstr
        :group.id goupstr
        :auto.offset.reset "smallest")
        ]
        (let [cseq (kfk/listenTo c tpstr)
            mapseq (map decode-kfk cseq)
            pcseq (partition-all 10 mapseq)
            
            ]
            (->>
                pcseq
                (map 
                    #(reset! 
                        log-atom 
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
