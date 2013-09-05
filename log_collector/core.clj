(ns log-collector.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clojure.java.io :as io]
        [clojure.edn :as edn]
        [clojure.data.json :as json]
        [utilities.shutil :as sh]
        [kfktools.core :as kfk]
        [log-collector.disk-scanner :as ds]
        [log-collector.log-line-parser :as llp]
    )
    (:import
        [java.nio.charset StandardCharsets]
        [kafka.common KafkaException]
        [java.io IOException PushbackReader]
        [java.nio.file Paths]
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
        (let [files (ds/scan opts)
            [new-file-info files] (ds/filter-files file-info files)
            ]
            [
                new-file-info
                (for [[opt f offset] files
                    msg (llp/read-logs opt f offset)
                    ]
                    {
                        :topic (name (:topic opt))
                        :message (-> msg
                            (json/write-str)
                            (.getBytes StandardCharsets/UTF_8)
                        )
                    }
                )
            ]
        )
    (catch IOException ex
        (error "IO error. Wait for 5 secs." :exception ex)
        nil
    ))
)

(defn- main-loop [producer opts file-info]
    (if-let [[new-file-info logs] (fetch-logs opts file-info)]
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

(defn- get-checkpoint-path [opts]
    (if-let [cpt-filename (:checkpoint opts)]
        (sh/getPath cpt-filename)
        (Paths/get "." (into-array String ["log_collector.cpt"]))
    )
)

(defn- read-file-info [opts]
    (let [
        my-opts (get opts :myself {})
        opts (dissoc opts :myself)
        cpt (get-checkpoint-path my-opts)
        cpt-file (.toFile cpt)
        ]
        (if-not (.exists cpt-file)
            [opts {}]
            (with-open [rdr (PushbackReader. (io/reader cpt-file))]
                (let [file-info (edn/read rdr)]
                    [
                        opts
                        (into {}
                            (for [[first-line [filename size]] (seq file-info)]
                                [first-line [{} (sh/getPath filename) size]]
                            )
                        )
                    ]
                )
            )
        )
    )
)

(defn main [opts]
    (let [[opts file-info] (read-file-info opts)]
        (try
            (with-open [producer (kfk/newProducer (:kafka opts))]
                (main-loop producer (dissoc opts :kafka) file-info)
            )
        (catch KafkaException ex
            (error "Cannot create kafka producer. Wait for 15 secs." :exception ex)
            (Thread/sleep 15000)
        ))
    )
)
