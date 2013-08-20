(ns kafka-hdfs.main
    (:import
        [java.net URI]
        [org.apache.hadoop.conf Configuration]
        [org.apache.hadoop.fs FileSystem Path]
    )
    (:gen-class)
)

(defn -main [& args]
    (let [
        host "hdfs://pc03/"
        d (Path. host "smile/")
        f (Path. d "xixi")
        conf (Configuration.)
        ]
        (with-open [fs (FileSystem/get (URI/create host) conf)]
            (with-open [out (.create fs f)]
                (spit out "You are my sunshine,\n")
            )
            (with-open [out (.append fs f)]
                (spit out "my only sunshine.\n")
            )
            (let [xs (.listStatus fs d)]
                (doseq [i (range (alength xs))
                    :let [x (aget xs i)]
                    ]
                    (println (.getPath x))
                )
            )
        )
    )
)
