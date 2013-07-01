(ns unittest.main
    (:require 
        unittest.log-line-parser
        unittest.log-line-cache
    )
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> 
        (load-cases 
            'unittest.log-line-parser 
            'unittest.log-line-cache
        )
        (main args)
    )
)
