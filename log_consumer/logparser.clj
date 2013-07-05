(ns log-consumer.logparser)

(defn- common-filter [pstr,str]
    (if (re-find pstr str)
        true
        false
    )
)

(defn- filter-userless-log [str]
    (not (common-filter #"clienttrace" str) )
)

(defn- just-skip [str]
    nil
)
(defn- filter-HDFSWRITE [str]
    (common-filter #"HDFS_WRITE" str)
)

(defn- gen-json-for-hdfswrite [str]
    (concat "HDFS_WRITE" "adf")
)

(defn- filter-HDFSREAD [str]
    (common-filter #"HDFS_READ" str)
)

(defn- gen-json-for-hdfsread [str]
    (concat "HDFS_READ" "adf")
)

(def parse-rules 
    [
        ["rule_filter",filter-userless-log,just-skip],
        (comment ["get_hdfs_write",filter-HDFSWRITE,gen-json-for-hdfswrite],)
        ["get_hdfs_read",filter-HDFSREAD,gen-json-for-hdfsread]
    ]
)


(defn- get-filter [rule]
    (nth rule 1)
)
(defn- get-parse [rule]
    (nth rule 2)
)

(defn parse-log [rules logs]
    (if (or (empty? rules) (empty? logs))
        []
        (let [tr (first rules)
            f-fitler (get-filter tr)
            f-parse (get-parse tr)
            log-group (group-by f-fitler logs)
            t-logs (nth (find log-group true) 1)
            f-logs (nth (find log-group false) 1)
            this-re (map f-parse t-logs)]
            (concat [this-re] 
                (parse-log (rest rules) f-logs)
            )
        )

    ) 
)