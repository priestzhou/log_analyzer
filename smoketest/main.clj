(ns smoketest.main
    (:require smoketest.disk-scanner)
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> (load-cases 'smoketest.disk-scanner)
        (main args)
    )
)
