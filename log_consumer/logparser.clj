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
    []
)
(defn- filter-HDFSWRITE [str]
    (common-filter #"HDFS_WRITE" str)
)

(defn- parse-hdfswrite [str]
    (concat "HDFS_WRITE" "adf")
)

(defn- filter-HDFSREAD [str]
    (common-filter #"HDFS_READ" str)
)

(defn- parse-hdfsread [instr]
    (let [srcip (re-find #"(?<=src: /)[0-9.]+(?=:)" instr)
        destip (re-find #"(?<=dest: /)[0-9.]+(?=:)" instr)
        filesize (re-find #"(?<=bytes: )[0-9]+" instr)]
        {
            "srcip" (str srcip),"destip" (str destip),
            "filesize"  (read-string filesize), "tag" "read1"
        }

    )
)

(defn- count-for-json [grouped-item]
    (let [datalist (val grouped-item)]
        (reduce 
            #(+ %1 (val (find %2 "filesize")))
            0
            datalist
        )
    )
)

(defn gen-json [dlist]
    (let [group-list 
            (group-by 
                #(list 
                    (val (find % "srcip")) 
                    (val (find % "destip"))
                ) 
                dlist
            )
        allkey (keys group-list)
        relist (map 
                    #(hash-map % (count-for-json (find group-list %) )
                    )
                    allkey
                )
        ]
        (->>
            relist
            (map
                #(str "{\"src\": \"" (first  (first(keys %))) "\",\"dest\":\"" 
                    (last (first(keys %)))
                    "\", \"dataflow\":"  (first (vals %)) "},"
                ) 
            )
            (reduce str "")
            (#(str "{ \"info\":[" % "]}"))
        )
    )

)
(comment reduce concat #(str "{ info:[" % "]}"))
            

(def parse-rules 
    [
        ["get_hdfs_read",filter-HDFSREAD,parse-hdfsread],
        ["rule_filter",filter-userless-log,just-skip]

    ]
)
 (comment ["get_hdfs_write",filter-HDFSWRITE,parse-hdfswrite],)

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
            t-logs (val (find log-group true) )
            f-logs (val (find log-group false) )
            this-re (map f-parse t-logs)]
            (concat [this-re] 
                (parse-log (rest rules) f-logs)
            )
        )

    ) 
)