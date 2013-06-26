(ns unittest.main
    (:require unittest.log-line-parser-unittest)
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> (load-cases 'unittest.log-line-parser-unittest)
        (main args)
    )
)
