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
