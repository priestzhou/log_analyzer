(ns unittest.main
    (:require 
        unittest.log-collector.log-line-parser
        unittest.log-search.frame
        unittest.log-search.searchparser
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-collector.log-line-parser 
            'unittest.log-search.frame
        )
        (main args)
    )
)
