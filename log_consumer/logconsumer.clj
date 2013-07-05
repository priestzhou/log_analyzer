(ns log-consumer.logconsumer
    (:use log-consumer.logparser)
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