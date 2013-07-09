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


(defn -main []
    (let [jtclient (new org.apache.hadoop.mapred.JobClient jtaddress jobconf)]
        (println (. jtclient getDefaultReduces))
    )
)