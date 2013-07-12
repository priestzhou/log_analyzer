(ns log-consumer.tasktracker_log_parser.clj
    (:use log-consumer.logparser)
)

(defn- get-att-id [instr]
    (re-find #"attempt_[0-9]+_\S+" instr)
)

(defn- task-start-filter [instr]
    (and 
        (common-filter #"JVM with ID: jvm_" instr) 
        (common-filter #"given task: attempt_" instr)
    )
)

(defn- task-start-prase [instr]
    (let 
        [attempt-id (get-att-id instr)
        ]
        (   
            {"tag" "task-start","attempt-id" attempt-id
            }

        )
    )
)

(defn- task-kill-filter [instr] 
    (common-filter #"Received KillTaskAction for task: attempt" instr) 
)

(defn- task-kill-prase [instr]
    (let 
        [attempt-id (get-att-id instr)
        ]
        (   
            {"tag" "task-kill","attempt-id" attempt-id
            }

        )
    )
)
(defn- task-done-filter [instr]
    (and 
        (common-filter #"Task attempt_" instr) 
        (common-filter #"is done.$" instr)
    )
)

(defn- task-done-prase [instr]
    (let 
        [attempt-id (get-att-id instr)
        ]
        (   
            {"tag" "task-done","attempt-id" attempt-id
            }

        )
    )
)

(defn- task-r-status-filter [instr]
    (and 
        (common-filter #"Task attempt_" instr) 
        (common-filter #" reduce > " instr)
    )
)

(defn- task-r-status-prase [instr]
    (let 
        [attempt-id (get-att-id instr)
        status (re-find "(?<=reduce > )[\S]+")
        ]
        (   
            {
                "tag" "task-status","attempt-id" attempt-id,
                "status" status
            }

        )
    )
)



