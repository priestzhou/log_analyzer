(ns smoketest.main
    (:require 
        [smoketest.log-collector disk-scanner core]
        [smoketest.kafka-hdfs core]
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> (load-cases 'smoketest.log-collector.disk-scanner
            'smoketest.log-collector.core
            `smoketest.kafka-hdfs.core
        )
        (main args)
    )
)
