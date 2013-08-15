(ns log-search.searchparser
    (:use
        [logging.core :only [defloggers]]
    )    
    (:require 
        [clojure.string :as cs]
        [serializable.fn :as sfn]
    )
)

(defloggers debug info warn error)

(defn- splitStr [sStr]
    (cs/split sStr #"\|")
)

(defn- event-func [eStr]
    (sfn/fn [inStr]
        (->>
            (re-find 
                (re-pattern (cs/lower-case (cs/trim eStr) )) 
                (cs/lower-case inStr)
            )
            nil?
            not
        )
    )
)

(def ^:private parser-fun-map
    {
        "parse" 
        (sfn/fn f [secStr]
            (str 
                "(?<="
                (cs/replace 
                    secStr 
                    #"\*"
                    ")[\\\\S]*(?="    
                )
                ")"
            )
        )
        "reparse"
        identity
    }
)

(defn- parser-one [pStr ptag]
    (let [pSeq (cs/split pStr #"\"" ) ;"
            secStr (second pSeq)
            secSeq (cs/split secStr #"\*")
            lastStr (last pSeq)
            tKey (re-find #"(?<= as )[\S]+" lastStr)
        ]
        (println ptag)
        (println ((get parser-fun-map ptag) secStr))
        {   
            :key
            tKey
            :parser 
            #(re-find 
                (re-pattern 
                    ((get parser-fun-map ptag) secStr)
                ) 
                %
            )
        }
    )
)

(defn- build-parse [sSeq pMap]
    (let [pStr (first sSeq)
            tailSeq (rest sSeq)
            plist (:parseRules pMap)
        ]
        (if (and 
                (not (nil? pStr)) 
                (or 
                    (re-find #"^[\s]*parse" pStr) 
                    (re-find #"^[\s]*reparse" pStr)
                )
            )
            (build-parse 
                tailSeq 
                (merge pMap 
                    {:parseRules 
                        (conj 
                            plist 
                            (parser-one 
                                pStr 
                                (re-find #"(?<=^[\s]*)[\S]+" pStr)
                            )
                        )
                    }
                )
            )
            pMap
        )
    )
)

(def ^:private wheresym #"=|<>|>|>=|<|<=|where")
(def ^:private wheresym2 #" = | <> | > | >= | < | <= ")

(defn- where-step [pStr]
    (let [pSeq (->> 
                (cs/split pStr wheresym)
                (map cs/trim)
                (filter #(< 0 (count %)))
                ) ;"
            secStr (second pSeq)
            firstStr (first pSeq)
            tag (re-find wheresym2 pStr)
        ]
        (println pSeq)
            (println secStr)
            (println firstStr)
            (println (read-string secStr))
        (sfn/fn f [log]

            (=
                (get log firstStr)
                secStr
            )
        )
        
    )
)

(defn- build-where [sSeq pMap]
    (let [pStr (first sSeq)
            tailSeq (rest sSeq)
            plist (:whereRules pMap)
        ]
        (if (and 
                (not (nil? pStr)) 
                (re-find #"^[\s]*where" pStr)
            )
            (build-where 
                tailSeq 
                (merge pMap 
                    {:whereRules (conj plist (where-step pStr ))}
                )
            )
            pMap
        )
    )
)

(defn- log-table-parser [sStr]
    (let [sSeq (splitStr sStr)
            eStr (first sSeq)
            tailSeq (rest sSeq)
            whereSeq (filter  #(re-find #"^[\s]*where" %) tailSeq)
        ]
        (if (empty? tailSeq)
            {:eventRules [(event-func eStr)]}
            (->>
                (build-parse  
                    tailSeq  
                    (assoc {:eventRules [(event-func eStr)]} :parseRules []) 
                )
                (#(build-where
                    whereSeq
                    (assoc % :whereRules [])
                ))
            )
        )
    )
)

(defn- get-groupkey [gStr]
    (let [gSeq (cs/split gStr #" by ")]
        (if (< 1 (count gSeq))
            (->>
                gSeq
                last
                (#(cs/split % #","))
                (map cs/trim )
            )
            []
        )
    )
)

(def ^:private static-rules
    {
        "sum"
        (fn [l] 
            (reduce #(+ %1 (read-string %2)) 0 l)
        )
        "count"
        (fn [l] 
            (reduce (fn [a b] (+ a 1)) 0 l)
        )
    }
)

(defn- get-pSeq [sItem]
    (->>
        sItem
        (#(cs/split % #" "))
        (filter #(not (empty? %)) )
    )
)

(defn- static-one [pSeq]
    (let [c1 (count pSeq)
            funcStr (cs/lower-case (first pSeq))
            keyStr (last pSeq)
            statFun (get static-rules funcStr)
        ]
        (if (and (= c1 2) (not (nil? statFun)) )
            {
                :statInKey keyStr,
                :statFun statFun,
                :statOutKey (str funcStr "_" keyStr)
            }
            (println "the input format can't parse as static")
        )
    )
)

(defn- parse-static [gStr]
    (let [gSeq (cs/split gStr #" by ")
            sStr (first gSeq)
            sSeq (cs/split sStr #",")
            pSeqs (map get-pSeq sSeq)
        ]
        (map 
            static-one
            pSeqs
        )
    )
)

(defn- get-group-time [timeStep timeValue]
    (let [modTime (mod timeValue timeStep)
            groupTime (- timeValue modTime)
        ]
        (.format 
            (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss") 
            groupTime
        )
    )
)

(def ^:private timeMap
    {
        "60" {:tw 60000,:tf #(get-group-time 5000 %)},
        "300" {:tw 300000,:tf #(get-group-time 10000 %)}
    }
)

(defn sparser 
    ([sStr]
        (debug "sparser sStr" sStr)
        (let [log-parser (log-table-parser sStr)
                sSeq (splitStr sStr)
                lastStr (last sSeq)
                gKeys (get-groupkey lastStr)
                statRules (parse-static lastStr)
            ]
            (debug "the log-parser" log-parser)
            (if (empty? gKeys)
                log-parser  
                (assoc log-parser :groupKeys gKeys :statRules statRules
                ) 
            )
        )
    )
    ([sStr timeWindow]
        (debug "str timeWindow in sparser" sStr timeWindow)
        (let [psr (sparser sStr)
                timeRule (get timeMap timeWindow)
                tw (:tw timeRule)
                startTime `(- 
                        (System/currentTimeMillis)
                        ~(eval tw)
                    )
            ]
            (debug "sparser s t let in")
            (assoc psr :timeRule (assoc timeRule :startTime startTime))
        )
    )
   ([sStr timeWindow startTime]
        (let [psr (sparser sStr)
                timeRule (assoc (get timeMap timeWindow) :startTime startTime)
            ]
            (assoc psr :timeRule timeRule)
        )
    )
   ([sStr timeWindow startTime]
        (let [psr (sparser sStr)
                timeRule (get timeMap timeWindow)
                
            ]
            (assoc psr :timeRule timeRule)
        )
    )
)
