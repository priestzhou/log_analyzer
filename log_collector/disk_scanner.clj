(ns log-collector.disk-scanner
    (:use
        [logging.core :only [defloggers]]
    )
    (:require 
        [utilities.shutil :as sh]
        [clojure.java.io :as io]
    )
    (:import 
        [java.nio.file Files LinkOption]
    )
)

(defloggers debug info warn error)

(defn- compare-daily-rolling [a b]
    (cond
        (= a b) 0
        (.endsWith a ".log") 1
        (.endsWith b ".log") -1
        :else (.compareTo a b)
    )
)

(defn- sort-daily-rolling [files]
    (sort-by str compare-daily-rolling files)
)

(def ^:private numeric-pattern #".*[.]log[.](\d+)")

(defn- extract-num [a]
    (let [a (str a)]
        (cond
            (.endsWith a ".log") 0
            :else (let [ma (re-find numeric-pattern a)]
                (assert ma)
                (read-string (get ma 1))
            )
        )
    )
)

(defn- sort-numeric [files]
    (reverse (sort-by extract-num files))
)

(defn scan-files [sorter base pat]
    (with-open [files (Files/newDirectoryStream (sh/getPath base))]
        (let [logs (->> files
                (filter #(Files/isRegularFile % (into-array LinkOption [])))
                (filter #(re-find pat (str (.getFileName %))))
                (sorter)
            )]
            (info "updated log files" :count (count logs) :1st (str (first logs)))
            logs
        )
    )
)

(defn- get-sorter [opt]
    (let [sorter (get opt :sorter :daily-rolling)]
        (cond
            (= sorter :daily-rolling) sort-daily-rolling
            (= sorter :numeric) sort-numeric
            (ifn? sorter) sorter
            :else (assert false 
                ":sorter requires :daily-rolling, :numeric or a sorting function"
            )
        )
    )
)

(defn scan [opts]
    (for [[topic opt] opts
        :let [base (:base opt)]
        :let [_ (assert base)]
        :let [pattern (:pattern opt)]
        :let [_ (assert pattern)]
        :let [sorter (get-sorter opt)]
        f (scan-files sorter base pattern)
        ]
        [(assoc opt :topic topic) f]
    )
)


(defn- first-line [f]
    (with-open [rdr (-> f (.toFile) (io/reader))]
        (.readLine rdr)
    )
)

(defn- file-infos [files result]
    (if (empty? files)
        result
        (let [[[opt f] & fs] files]
            (if-let [ln (first-line f)]
                (do
                    (if-let [[f'] (get result ln)]
                        (error "duplicated logs found" :1st (str f') :2nd (str f))
                    )
                    (recur fs (conj result [ln [opt f (Files/size f)]]))
                )
                (recur fs result)
            )
        )
    )
)

(defn- filter-updated-files [old-file-info new-file-info result]
    (if (empty? new-file-info)
        result
        (let [[[ln [opt f new-size]] & fs] new-file-info]
            (if-let [[_ _ old-size] (old-file-info ln)]
                (do
                    (when (> old-size new-size)
                        (error "new size should be larger than old one"
                            :file f
                        )
                    )
                    (if (> new-size old-size)
                        (do
                            (recur old-file-info fs (conj result [opt f old-size]))
                        )
                        (do
                            (recur old-file-info fs result)
                        )
                    )
                )
                (do
                    (recur old-file-info fs (conj result [opt f 0]))
                )
            )
        )
    )
)

(defn filter-files [file-info files]
    (let [new-file-info (file-infos files [])]
        [
            (into {} new-file-info)
            (filter-updated-files file-info new-file-info [])
        ]
    )
)
