(ns unittest.main
    (:require unittest.log-line-parser)
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> (load-cases 'unittest.log-line-parser)
        (main args)
    )
)
