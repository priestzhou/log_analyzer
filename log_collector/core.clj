(ns log-collector.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clojure.java.io :as io]
        [clojure.data.json :as json]
        [kfktools.core :as kfk]
        [log-collector.disk-scanner :as ds]
        [log-collector.log-line-parser :as llp]
    )
    (:import
        [java.nio.charset StandardCharsets]
        [kafka.common KafkaException]
        [java.io IOException]
    )
)

(defloggers debug info warn error)

(defn- new-producer [kafka-opts]
    (->>
        (for [[k v] kafka-opts] [k v])
        (flatten)
        ((fn [x] (println x) x))
        (apply kfk/newProducer)
    )
)

(defn- scan [opts]
    (let [base (:base opts)
        pattern (:pattern opts)
        sorter (get opts :sorter ds/sort-daily-rolling)
        ]
        (ds/scan sorter base pattern)
    )
)

(defn- read-logs [opts f]
    (let [parser (get opts :parser llp/parse-log-line)
        rdr (io/reader (.toFile f))
        ]
        (llp/parse-log-events! parser rdr)
    )
)

(defn- produce 
    ([producer logs cnt]
        (if (empty? logs)
            cnt
            (let [[bulk remains] (split-at 1000 logs)]
                (kfk/produce producer bulk)
                (if-let [{:keys [topic message]} (first bulk)]
                    (info "Sent logs" 
                        :count (count bulk)
                        :first {
                            :topic topic 
                            :message (-> message
                                (String. StandardCharsets/UTF_8)
                                (json/read-str)
                            )
                        }
                    )
                )
                (Thread/sleep 100)
                (recur producer remains (+ cnt (count bulk)))
            )
        )
    )

    ([producer logs]
        (try
            (if (empty? logs)
                (info "Find no new logs")
                (do
                    (info "Find new logs")
                    (let [total (produce producer logs 0)]
                        (info "Sent all new logs. Wait for 5 secs." :total total)
                    )
                )
            )
        (catch IOException ex
            (error "IO error. Wait for 5 secs." :exception ex)
        ))
    )
)

(defn- fetch-logs [opts file-info]
    (try
        (let [logs (for [
                [k v] opts
                f (->> (scan v)
                    (take 2)
                    (reverse)
                )
                ln (read-logs v f)
                :let [not-cached-ln (llp/cache-log-line ln)]
                :when not-cached-ln
                :let [message (-> not-cached-ln
                    (json/write-str)
                    (.getBytes (StandardCharsets/UTF_8))
                )]
                ]
                {:topic (name k) :message message}
            )
            ]
            [logs {}]
        )
    (catch IOException ex
        (error "IO error. Wait for 5 secs." :exception ex)
        nil
    ))
)

(defn- main-loop [producer opts file-info]
    (if-let [[logs new-file-info] (fetch-logs opts file-info)]
        (do
            (produce producer logs)
            (Thread/sleep 5000)
            (recur producer opts new-file-info)
        )
        (do
            (Thread/sleep 5000)
            (recur producer opts file-info)
        )
    )
)

(defn main [opts]
    (while true
        (try
            (with-open [producer (kfk/newProducer (:kafka opts))]
                (main-loop producer (dissoc opts :kafka) {})
            )
        (catch KafkaException ex
            (error "Cannot create kafka producer. Wait for 15 secs." :exception ex)
            (Thread/sleep 15000)
        ))
    )
)
