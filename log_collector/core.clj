(ns log-collector.core
    (:use
        [logging.core :only [defloggers]]
    )
    (:require
        [clojure.java.io :as io]
        [clojure.edn :as edn]
        [clojure.data.json :as json]
        [utilities.core :as util]
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

(defn- get-checkpoint-path [opts]
    (let [myself (:myself opts)]
        (util/throw-if-not myself
            IllegalArgumentException.
            "expect :myself in my config file"
        )
        (let [cpt-filename (:checkpoint myself)]
            (util/throw-if-not cpt-filename
                IllegalArgumentException.
                "expect :checkpoint under :myself in my config file"
            )
            (sh/getPath cpt-filename)
        )
    )
)

(defn- write-checkpoint [opts file-info]
    (let [
        p (get-checkpoint-path opts)
        cpt (into (sorted-map)
            (for [[ln [_ fp size]] (seq file-info)]
                [ln [(str fp) size]]
            )
        )
        to-write (-> cpt
            (prn-str)
        )
        ]
        (with-open [f (io/writer (.toFile p) :encode "UTF-8")]
            (.write f to-write)
        )
    )
)

(defn- main-loop [producer opts file-info]
    (if-let [[new-file-info logs] (fetch-logs (dissoc opts :myself) file-info)]
        (do
            (produce producer logs)
            (write-checkpoint opts new-file-info)
            (Thread/sleep 5000)
            (recur producer opts new-file-info)
        )
        (do
            (Thread/sleep 5000)
            (recur producer opts file-info)
        )
    )
)

(defn- read-file-info [opts]
    (let [
        cpt (get-checkpoint-path opts)
        cpt-file (.toFile cpt)
        ]
        (if-not (.exists cpt-file)
            {}
            (with-open [rdr (PushbackReader. (io/reader cpt-file))]
                (let [file-info (edn/read rdr)]
                    (into {}
                        (for [[first-line [filename size]] (seq file-info)]
                            [first-line [{} (sh/getPath filename) size]]
                        )
                    )
                )
            )
        )
    )
)

(defn main [opts]
    (let [file-info (read-file-info opts)]
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
