(ns java-parser.main
    (:require 
        [utilities.parse :as pst]
    )
    (:gen-class)
)

(defn -main [& args]
    (->> args
        (first)
        (slurp)
        (prn)
    )
)
