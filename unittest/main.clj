(ns unittest.main
    (:require 
        unittest.log-collector.log-line-parser
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-collector.log-line-parser 
        )
        (main args)
    )
)
