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

(defn scan [sorter base pat]
    (let [logs (->> (Files/newDirectoryStream (sh/getPath base))
            (filter #(Files/isRegularFile % (into-array LinkOption [])))
            (filter #(re-find pat (str (.getFileName %))))
        )]
        (info "Scanned logs." :count (count logs))
        (sorter logs)
    )
)
