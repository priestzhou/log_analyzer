(ns unittest.main
    (:require 
        unittest.log-collector.log-line-parser
        unittest.log-collector.log-line-cache
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-collector.log-line-parser 
            'unittest.log-collector.log-line-cache
        )
        (main args)
    )
)
