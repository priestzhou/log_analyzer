(ns unittest.log-line-parser
    (:use testing.core
        log-collector.log-line-parser
        clojure.java.io 
    )
    (:import 
        [java.io StringReader]
    )
)

(suite "parse raw log events from lines"
    (:fact log-line-real
        (parse-log-raw (reader (StringReader. 
"2013-06-01 22:48:58,464 WARN org.apache.hadoop.hdfs.server.datanode.DataNode: java.net.ConnectException: Call to 10.144.44.18/10.144.44.18:8020 failed on connection exception: java.net.ConnectException: Connection refused
        at org.apache.hadoop.ipc.Client.wrapException(Client.java:1136)
        at org.apache.hadoop.ipc.Client.call(Client.java:1112)
        at org.apache.hadoop.ipc.RPC$Invoker.invoke(RPC.java:229)
        at com.sun.proxy.$Proxy5.sendHeartbeat(Unknown Source)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.offerService(DataNode.java:972)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.run(DataNode.java:1527)
        at java.lang.Thread.run(Thread.java:722)
Caused by: java.net.ConnectException: Connection refused
        at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
        at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:692)
        at org.apache.hadoop.net.SocketIOWithTimeout.connect(SocketIOWithTimeout.java:206)
        at org.apache.hadoop.net.NetUtils.connect(NetUtils.java:511)
        at org.apache.hadoop.net.NetUtils.connect(NetUtils.java:481)
        at org.apache.hadoop.ipc.Client$Connection.setupConnection(Client.java:453)
        at org.apache.hadoop.ipc.Client$Connection.setupIOstreams(Client.java:579)
        at org.apache.hadoop.ipc.Client$Connection.access$2100(Client.java:202)
        at org.apache.hadoop.ipc.Client.getConnection(Client.java:1243)
        at org.apache.hadoop.ipc.Client.call(Client.java:1087)
        ... 5 more

2013-06-01 22:48:59,466 INFO org.apache.hadoop.ipc.Client: Retrying connect to server: 10.144.44.18/10.144.44.18:8020. Already tried 0 time(s); retry policy is RetryUpToMaximumCountWithFixedSleep(maxRetries=10, sleepTime=1 SECONDS)
2013-06-01 22:48:59,732 INFO org.apache.hadoop.hdfs.server.datanode.DataNode: SHUTDOWN_MSG:
/************************************************************
SHUTDOWN_MSG: Shutting down DataNode at AY130510232353680d9aZ/10.144.44.181
************************************************************/
"
        )))
        :is
        [
            {:timestamp 1370098138464,
                :level "WARN",
                :location "org.apache.hadoop.hdfs.server.datanode.DataNode",
                :message 
"java.net.ConnectException: Call to 10.144.44.18/10.144.44.18:8020 failed on connection exception: java.net.ConnectException: Connection refused
        at org.apache.hadoop.ipc.Client.wrapException(Client.java:1136)
        at org.apache.hadoop.ipc.Client.call(Client.java:1112)
        at org.apache.hadoop.ipc.RPC$Invoker.invoke(RPC.java:229)
        at com.sun.proxy.$Proxy5.sendHeartbeat(Unknown Source)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.offerService(DataNode.java:972)
        at org.apache.hadoop.hdfs.server.datanode.DataNode.run(DataNode.java:1527)
        at java.lang.Thread.run(Thread.java:722)
Caused by: java.net.ConnectException: Connection refused
        at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
        at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:692)
        at org.apache.hadoop.net.SocketIOWithTimeout.connect(SocketIOWithTimeout.java:206)
        at org.apache.hadoop.net.NetUtils.connect(NetUtils.java:511)
        at org.apache.hadoop.net.NetUtils.connect(NetUtils.java:481)
        at org.apache.hadoop.ipc.Client$Connection.setupConnection(Client.java:453)
        at org.apache.hadoop.ipc.Client$Connection.setupIOstreams(Client.java:579)
        at org.apache.hadoop.ipc.Client$Connection.access$2100(Client.java:202)
        at org.apache.hadoop.ipc.Client.getConnection(Client.java:1243)
        at org.apache.hadoop.ipc.Client.call(Client.java:1087)
        ... 5 more"
            }
            {:timestamp 1370098139466,
                :level "INFO"
                :location "org.apache.hadoop.ipc.Client"
                :message 
"Retrying connect to server: 10.144.44.18/10.144.44.18:8020. Already tried 0 time(s); retry policy is RetryUpToMaximumCountWithFixedSleep(maxRetries=10, sleepTime=1 SECONDS)"
            }
            {:timestamp 1370098139732,
                :level "INFO"
                :location "org.apache.hadoop.hdfs.server.datanode.DataNode"
                :message "SHUTDOWN_MSG:
/************************************************************
SHUTDOWN_MSG: Shutting down DataNode at AY130510232353680d9aZ/10.144.44.181
************************************************************/"
            }
        ]
    )
)

(suite "parse raw log"
    (:fact parse-single-line 
        (parse-log-raw (reader (StringReader.
"1970-01-01 08:00:01,000 INFO Class.func: hello world!"
        )))
        :is
        [{:timestamp 1000, :level "INFO", :location "Class.func",
            :message "hello world!"
        }]
    )
    (:fact parse-multi-line
        (parse-log-raw (reader (StringReader.
"1970-01-01 08:00:01,000 INFO Class.func: hello
world!"
        )))
        :is
        [{:timestamp 1000, :level "INFO", :location "Class.func",
            :message "hello\nworld!"
        }]
    )
    (:fact parse-sl-ml
        (parse-log-raw (reader (StringReader.
"1970-01-01 08:00:00,001 INFO Class.func: xixi
1970-01-01 08:00:00,010 INFO Class.func: hello
world!"
        )))
        :is
        [{:timestamp 1, :level "INFO", :location "Class.func",
                :message "xixi"
            }
            {:timestamp 10, :level "INFO", :location "Class.func",
                :message "hello\nworld!"
            }
        ]
    )
    (:fact parse-ml-sl
        (parse-log-raw (reader (StringReader.
"1970-01-01 08:00:00,001 INFO Class.func: hello
world!
1970-01-01 08:00:00,010 INFO Class.func: xixi"
        )))
        :is
        [{:timestamp 1, :level "INFO", :location "Class.func",
                :message "hello\nworld!"
            }
            {:timestamp 10, :level "INFO", :location "Class.func",
                :message "xixi"
            }
        ]
    )
    (:fact parse-trailing-blank-line
        (parse-log-raw (reader (StringReader.
"1970-01-01 08:00:01,000 INFO Class.func: hello world!

"
        )))
        :is
        [{:timestamp 1000, :level "INFO", :location "Class.func",
            :message "hello world!"
        }]
    )
)

