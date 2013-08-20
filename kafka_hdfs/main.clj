(ns kafka-hdfs.main
    (:import
        [java.net URI]
        [org.apache.hadoop.conf Configuration]
        [org.apache.hadoop.fs FileSystem Path]
    )
    (:gen-class)
)

(defn -main [& args]
    (let [conf (Configuration.)
        fs (FileSystem/get (URI/create "hdfs://pc03/hehe") conf)
        ]
        (try
            (let [in (.open fs (Path. "hdfs://pc03/hehe"))])
        (finally))
    )
)
