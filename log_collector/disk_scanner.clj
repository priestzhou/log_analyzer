(ns log-collector.disk-scanner
    (:import 
        [java.nio.file Files LinkOption]
    )
    (:require [utilities.shutil :as sh])
)

(defn scan [sorter base pat]
    (->> (Files/newDirectoryStream (sh/getPath base))
        (filter #(Files/isRegularFile % (into-array LinkOption [])))
        (filter #(re-find pat (str (.getFileName %))))
        (sorter)
    )
)
