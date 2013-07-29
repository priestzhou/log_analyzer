(ns log-consumer.logconsumer
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [kfktools.core :as kfk]
        [clojure.data.json :as json]
        [log-consumer.logparser :as lp]
    )
    (:import
        [java.util Properties ArrayList HashMap]
        [java.nio.file Path Files]
        [java.nio.file.attribute FileAttribute]
        [kfktools ConsumerWrapper]
        java.nio.charset.Charset
    )
)

(defloggers debug info warn error)

(defn- parse-step-kfk [logstep]
    (->>
        logstep
        (lp/parse-log-kfk lp/parse-rules)
        lp/after-parse
    )
)

(defn- decode-kfk [message]
    (-> message
        (#(:message %))
        (String. (Charset/forName "UTF-8"))
        (json/read-str :key-fn keyword)
    )
)

(defn consumer-from-kfk [zkstr tpstr goupstr log-atom]
    (with-open [c (kfk/newConsumer
        :zookeeper.connect zkstr
        :group.id goupstr
        :auto.offset.reset "largest")
        ]
        (let [cseq (kfk/listenTo c tpstr)
            mapseq (map decode-kfk cseq)
            pcseq (partition-all 10 mapseq)
            ]
            (->>
                pcseq
                (map 
                    #(swap! 
                        log-atom 
                        (fn [log]
                            (debug "get new log " :count-input (count %) 
                                :count-parsed (count (parse-step-kfk %))
                            )
                            (concat
                                log
                                (parse-step-kfk %) 
                            )
                        )
                    )
                )
                doall
            )
        )
    )
)
