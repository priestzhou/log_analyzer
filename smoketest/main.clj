(ns smoketest.main
    (:require smoketest.log-collector.disk-scanner)
    (:use testing.core)
    (:gen-class)
)

(defn -main [& args]
    (->> (load-cases 'smoketest.log-collector.disk-scanner)
        (main args)
    )
)
