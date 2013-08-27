(ns unittest.main
    (:require 
        unittest.log-collector.log-line-parser
        unittest.log-search.frame
        unittest.kafka-hdfs.core
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-collector.log-line-parser 
            'unittest.log-search.frame
            `unittest.kafka-hdfs.core
        )
        (main args)
    )
)
