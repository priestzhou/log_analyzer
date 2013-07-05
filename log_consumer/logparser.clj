(ns log-consumer.logparser)

(defn- common-filter [pstr,instr]
    (if (re-find pstr instr)
        true
        false
    )
)

(defn- filter-userless-log [instr]
    (not (common-filter #"clienttrace" instr) )
)

(defn- just-skip [instr]
    []
)
(defn- filter-HDFSWRITE [instr]
    (common-filter #"HDFS_WRITE" instr)
)

(defn- parse-hdfswrite [instr]
    (concat "HDFS_WRITE" "adf")
)

(defn- filter-HDFSREAD [instr]
    (common-filter #"HDFS_READ" instr)
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
            t-logs (find log-group true) 
            f-logs (find log-group false)
            this-re (if (nil? t-logs) 
                    []
                    (map f-parse (val t-logs))
                )
            ]
            (concat [this-re]
                (if (nil? f-logs) 
                    []
                    (parse-log (rest rules) (val f-logs))
                )
            )
        )

    ) 
)