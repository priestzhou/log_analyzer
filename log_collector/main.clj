(ns log-collector.main
    (:require
        [utilities.core :as util]
        [argparser.core :as arg]
        [log-collector.core :as lc]
    )
    (:gen-class)
)

(defn- parseArgs [args]
    (let [arg-spec {
                :usage "Usage: config"
                :args [
                    (arg/opt :help
                         "-h|--help" "show this help message")
                    (arg/opt :config
                         "config" "config file")
                ]
            }
            opts (arg/transform->map (arg/parse arg-spec args))
        ]
        (when (:help opts)
            (println (arg/default-doc arg-spec))
            (System/exit 0)
        )
        (util/throw-if-not (= 1 (count (:config opts)))
            IllegalArgumentException. 
            "require exactly one config file"
        )
        (first (:config opts))
    )
)

(defn -main [& args]
    (-> (parseArgs args)
        (slurp)
        (read-string)
        (lc/main)
    )
)
