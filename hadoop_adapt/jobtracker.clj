(ns hadoop-adapt.jobtracker
    (:import org.apache.hadoop.mapred.JobClient)
    (:import java.net.InetSocketAddress)
    (:import org.apache.hadoop.mapred.JobConf)
    (:import java.net.InetAddress)
    (:gen-class)
)

(def jtaddress 
    (new java.net.InetSocketAddress 
        (java.net.InetAddress/getByName "115.28.40.198")
        8021
    )
)

(def jobconf (new org.apache.hadoop.mapred.JobConf))

(defn- count-state [stat-list]
    (let [stat-key-list (keys stat-list)
        stat-str-list         
            (map 
                #(str % ":" 
                    (->>
                        %
                        (find stat-list)
                        val
                        count
                    )
                    ","
                )
                stat-key-list
            )
            ]
        (reduce 
            #(str %1 "{" %2 "}," ) 
            stat-str-list
        )
        
    )
)

(defn- get-job-info [jt jobid]
    (let [maptr (. jt   getMapTaskReports jobid)
        reducetr (. jt getReduceTaskReports jobid)
        mapState (group-by  #(str %) (map  #(. % getCurrentStatus) maptr))
        reduceState (group-by #(str %) (map #(. % getCurrentStatus) reducetr))
        ]
        (println mapState)
        (println reduceState)
        (str 
            "mapstate:["
            (count-state mapState)
            "]},{reducestate:["
            (count-state reduceState)
            "]"
        )
    )
)

(defn- get-jt-info [jt]
    (let [maxR (. jt getDefaultReduces)
        maxM (. jt getDefaultMaps)
        cs (. jt getClusterStatus)
        curR (. cs getReduceTasks)
        curM (. cs getMapTasks)
        jobid-list (map #(. % getJobID) (. jt jobsToComplete))
        ]
        (println 
            (str "{reduceSlot:" maxR ",mapSlot:" maxM 
                ",runningReduce:" curR ",runningMap:" curM "}"
            )
        )
        (println 
            (map 
                (fn [a]
                    (str 
                        "{jobid:"
                        a
                        ",taskstat:{"
                        (get-job-info jt a)
                        "}"
                    )
                ) 
                jobid-list
            )
        )
    )
)

(defn monitor-jt [jt]
    (Thread/sleep 5000)
    (get-jt-info jt)
    (recur jt)
)

(defn -main []
    (let [jtclient (new org.apache.hadoop.mapred.JobClient jtaddress jobconf)]
        (monitor-jt jtclient)
    )
)