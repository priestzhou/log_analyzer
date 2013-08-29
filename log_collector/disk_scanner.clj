(ns log-collector.disk-scanner
    (:use
        [logging.core :only [defloggers]]
    )
    (:require 
        [utilities.shutil :as sh]
    )
    (:import 
        [java.nio.file Files LinkOption]
    )
)

(defloggers debug info warn error)

(defn- compare-daily-rolling [a b]
    (let [a (str a)
        b (str b)
        ]
        (cond
            (= a b) 0
            (.endsWith a ".log") -1
            (.endsWith b ".log") 1
            :else (.compareTo b a)
        )
    )
)

(defn- sort-daily-rolling [files]
    (sort-by identity compare-daily-rolling files)
)

(def ^:dynamic sorter sort-daily-rolling)

(defn scan [base pat]
    (with-open [files (Files/newDirectoryStream (sh/getPath base))]
        (let [logs (->> files
                (filter #(Files/isRegularFile % (into-array LinkOption [])))
                (filter #(re-find pat (str (.getFileName %))))
                (sorter)
            )]
            (info "Scanned logs." :count (count logs))
            logs
        )
    )
)
