(ns log-consumer.logparser)

(defn- common-filter [pstr,instr]
    (if (re-find pstr instr)
        true
        false
    )
)

(defn- filter-userless-log [instr]
    (and
        (not (common-filter #"clienttrace" instr))
        (not (common-filter #"Received block" instr))
    )
)

(defn- just-skip [instr]
    []
)

(defn- filter-receive-block [instr]
    (common-filter #"Received block" instr)
)

(defn- parse-receive-block [instr]
    (let [srcip (re-find #"(?<=src: /)[0-9.]+(?=:)" instr)
        destip (re-find #"(?<=dest: /)[0-9.]+(?=:)" instr)
        filesize (re-find #"(?<=size )[0-9]+" instr)]
        {
            "srcip" (str destip),"destip" (str srcip ),
            "filesize"  (read-string filesize), "tag" "received"
        }

    )    
)

(defn- filter-HDFSWRITE [instr]
    (common-filter #"HDFS_WRITE" instr)
)

(defn- parse-hdfswrite [instr]
    (let [srcip (re-find #"(?<=src: /)[0-9.]+(?=:)" instr)
        destip (re-find #"(?<=dest: /)[0-9.]+(?=:)" instr)
        filesize (re-find #"(?<=bytes: )[0-9]+" instr)]
        {
            "srcip" (str destip),"destip" (str srcip ),
            "filesize"  (read-string filesize), "tag" "write"
        }

    )
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
            "filesize"  (read-string filesize), "tag" "read"
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
                    (get % "srcip")
                    (get % "destip")
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
(defn filter-by-time [loglist]
    (let [curtime (System/currentTimeMillis)
            timeFrom (- curtime 30000)
            timeTo (+ curtime 1000)
        ]
        (
            filter  
            #(and 
                (not (nil? (get % "timestamp")))
                (> timeTo (get % "timestamp"))
                (> (get % "timestamp") timeFrom)
            )
            loglist
        )
    )
)            

(comment ["rule_filter",filter-userless-log,just-skip],)

(def parse-rules 
    [
        
        ["received_block",filter-receive-block,parse-receive-block ],
        ["get_hdfs_write",filter-HDFSWRITE,parse-hdfswrite],
        ["get_hdfs_read",filter-HDFSREAD,parse-hdfsread]
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


(defn- psr-step [parser log]
    (let [psr (parser (:message log))]
        (if (= [] psr)
            []
            (assoc 
                psr
                "timestamp" (:timestamp log)
            )
        )
    )
)


(defn parse-log-kfk [rules logs]
    (if (or (empty? rules) (empty? logs))
        []
        (let [tr (first rules)
            f-fitler (get-filter tr)
            f-parse (get-parse tr)
            log-group 
                (group-by 
                    #(f-fitler 
                        (:message %)
                    )
                    logs
                )
            t-logs (find log-group true) 
            f-logs (find log-group false)
            this-re (if (nil? t-logs) 
                    []
                    (map 
                        #(psr-step f-parse %)
                        (val t-logs)
                    )
                )
            ]
            (concat [this-re]
                (if (nil? f-logs) 
                    []
                    (parse-log-kfk (rest rules) (val f-logs))
                )
            )
        )

    ) 
)

(defn after-parse [loglist]
    (->> 
        loglist
        (filter 
            #(not (empty? (first %)))
        )
        (reduce concat)
    )
)