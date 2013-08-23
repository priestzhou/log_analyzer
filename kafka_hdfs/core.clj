(ns kafka-hdfs.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clj-time.coerce :as date-coerce]
        [clj-time.format :as date-format]
        [clojure.data.json :as json]
        [clojure.java.io :as io]
        [kfktools.core :as kfk]
    )
    (:import 
        [java.util.concurrent BlockingQueue TimeUnit]
        [java.util Date]
    )
)

(defloggers debug info warn error)

(defn- lazyseq->queue! [topic seq ^BlockingQueue q cnt]
    (when-not (empty? seq)
        (let [[x & xs] seq
            message (:message x)
            message (if message (String. message) nil)
            ]
            (.put q {:topic topic :message message})
            (if (= cnt 1000)
                (do
                    (info "consume 1000 messages")
                    (recur topic xs q 1)
                )
                (recur topic xs q (inc cnt))
            )
            
        )
    )
)

(defn assign-consumer-to-queue! [consumer topic queue]
    (future-call 
        (partial lazyseq->queue! topic (kfk/listenTo consumer topic) queue 0)
    )
)

(defn poll! [^BlockingQueue q timeout]
    (let [x (.poll q timeout TimeUnit/MILLISECONDS)]
        x
    )
)

(defn ->uri [base topic ts]
    (let [d (date-coerce/from-long ts)
        rfc3339-fulldate (date-format/formatter "yyyy-MM-dd")
        rfc3339-datetime (date-format/formatters :date-time)
        ]
        (prn ts)
        (prn d)
        (.resolve base 
            (format "./%s/%s.%s"
                (date-format/unparse rfc3339-fulldate d)
                (date-format/unparse rfc3339-datetime d) 
                topic
            )
        )
    )
)

(defn save->hdfs [fs base timeout queue]
    (let [m (poll! queue timeout)]
        (when m
            (let [{:keys [topic message]} m
                jsoned-message (json/read-str message :key-fn keyword)
                ts (:timestamp jsoned-message)
                f (org.apache.hadoop.fs.Path. (->uri base topic ts))
                ]
                (with-open [out (.create fs f)]
                    (spit out message)
                )
            )
        )
    )
)
