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
        [java.nio.charset Charset]
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

(defn- main-loop [producer opts]
    (while true
        (try
            (let [logs (->>
                    (for [[k v] opts
                            f (->>
                                    (ds/scan sort (:base v) (re-pattern (:pattern v)))
                                    (take 2)
                                    (reverse)
                            )
                            ln (->> f
                                (.toFile)
                                (io/reader)
                                (llp/parse-log-raw)
                            )
                            :let [not-cached-ln (llp/cache-log-line ln)]
                            :when not-cached-ln
                            :let [message 
                                (-> not-cached-ln
                                    (json/write-str)
                                    (.getBytes (Charset/forName "UTF-8"))
                                )
                            ]
                        ]
                        {:topic (name k) :message message}
                    )
                    (doall)
                )]
                (info "Find new logs" :count (count logs))
                (kfk/produce producer logs)
                (info "Sent new logs. Wait for 5 secs." :count (count logs))
            )
        (catch IOException ex
            (error "IO error. Wait for 5 secs." :exception ex)
        ))
        (Thread/sleep 5000)
    )
)

(defn main [opts]
    (while true
        (try
            (with-open [producer (kfk/newProducer (:kafka opts))]
                (main-loop producer (dissoc opts :kafka))
            )
        (catch KafkaException ex
            (error "Cannot create kafka producer. Wait for 15 secs." :exception ex)
            (Thread/sleep 15000)
        ))
    )
)
