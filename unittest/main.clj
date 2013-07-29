(ns unittest.main
    (:require 
        unittest.log-collector.log-line-parser
        unittest.java-parser.core
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-collector.log-line-parser 
            'unittest.java-parser.core
        )
        (main args)
    )
)
