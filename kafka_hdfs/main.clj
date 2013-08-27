(ns kafka-hdfs.main
    (:require
        [utilities.core :as util]
        [argparser.core :as arg]
        [kfktools.core :as kfk]
        [kafka-hdfs.core :as kh]
    )
    (:import
        [java.util.concurrent ArrayBlockingQueue]
        [java.util ArrayDeque]
        [java.net URI]
        [org.apache.hadoop.conf Configuration]
        [org.apache.hadoop.fs FileSystem]
    )
    (:gen-class)
)

(defn- parseArgs [args]
    (let [arg-spec {
                :usage "Usage: config"
                :args [
                    (arg/opt :help
                         "-h|--help" "show this help message")
                    (arg/opt :config
                         "config" "config file")
                ]
            }
            opts (arg/transform->map (arg/parse arg-spec args))
        ]
        (when (:help opts)
            (println (arg/default-doc arg-spec))
            (System/exit 0)
        )
        (util/throw-if-not (= 1 (count (:config opts)))
            IllegalArgumentException. 
            "require exactly one config file"
        )
        (first (:config opts))
    )
)

(defn- new-consumer [cfg]
    (->>
        (for [[k v] cfg] [k v])
        (flatten)
        (apply kfk/newConsumer)
    )
)

(defn -main [& args]
    (let [cfg (-> (parseArgs args)
            (slurp)
            (read-string)
        )
        c (new-consumer (:consumer cfg))
        q (ArrayBlockingQueue. 1024)
        stubs (doall
            (for [topic (:topics cfg)]
                (kh/assign-consumer-to-queue! c topic q)
            )
        )
        base (URI/create (:base cfg))
        hdfs-cfg (Configuration.)
        cache (ArrayDeque.)
        ]
        (with-open [fs (FileSystem/get base hdfs-cfg)]
            (let [existents (kh/scan-existents fs base)]
                (while true
                    (kh/save->hdfs! q base fs existents cache)
                )
            )
        )
    )
)
