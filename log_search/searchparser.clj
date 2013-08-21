(ns log-search.searchparser
    (:use
        [logging.core :only [defloggers]]
        [log-search.parse-util]
    )    
    (:require 
        [clojure.string :as cs]
        [serializable.fn :as sfn]
        [utilities.parse :as ups]
    )
    (:import
        utilities.parse.InvalidSyntaxException
    )    
)

(defloggers debug info warn error)

(defn- numberchar [stream]

)

(defn- parse-event-str [stream]
    (let [[strm parsed] (
                (ups/many1 
                    (ups/choice
                        (ups/expect-char-if ups/letter)
                        (ups/expect-char-if ups/digit)
                        (ups/expect-char-if usersym)
                    )
                )
                stream
            )
            pStr (ups/extract-string-between stream strm)
        ]
        [strm pStr]
    )
)

(defn- parse-parser-str [stream]
    (let [[strm parsed] (
                (ups/many1 
                    (ups/choice
                        (ups/expect-char-if ups/letter)
                        (ups/expect-char-if ups/digit)
                        (ups/expect-char-if parsersym)
                        (ups/expect-char-if whitespace)
                    )
                )
                stream
            )
            pStr (ups/extract-string-between stream strm)
        ]
        [strm pStr]
    )
)

(defn- star-parser [stream]
    (let [[strm parsed]
            ((ups/expect-char \*) stream)
        ]
        [strm :star]
    )
)

(defn- parse-event [stream]
    (let [[strm parsed] (
                (ups/chain
                    (ups/optional star-parser )
                    parse-event-str
                    (ups/optional star-parser )
                )
                stream
            )
            pStr (first (filter string? parsed))
            leftFlag (= :star (first parsed))
            rightFlag (= :star (last parsed))
            rstr (cond
                    (and leftFlag rightFlag) pStr
                    leftFlag (str pStr "(\\s|$)+")
                    rightFlag (str "(^|\\s)+" pStr)
                    :else (str "(^|\\s)+" pStr "(\\s|$)+")
                )
            t1 (println rstr)
            efunc  (sfn/fn [inStr]
                (->>
                    (re-find 
                        (re-pattern (cs/lower-case rstr )) 
                        (cs/lower-case inStr)
                    )
                    nil?
                    not
                )
            )
        ]
        [strm {:eventRules [efunc]}]
    )
)

(defn- parse-split [stream]
    (   (ups/chain
            (ups/expect-char \|)
            (ups/optional whitespaces)
        )
        stream
    )
)

(defn- parser-for-string' [str1]
    (debug "parse1 " str1)
    (let [
            stream (ups/positional-stream str1)
            [strm parsed] (
                (ups/chain 
                    (ups/optional parse-parser-str)
                    star-parser
                    (ups/optional parse-parser-str)
                )
                stream
            )
            leftStr (if (string? (first parsed))
                    (str "(?<=" (first parsed) ")")
                    ""
                )
            rightStr (if (string? (last parsed))
                    (str "(?=" (last parsed) ")" )
                    ""
                )
        ]
        (re-pattern (str  leftStr "[\\S]*" rightStr))
    )
 
)

(defn- parser-for-string [stream]
    (let [[strm parsed] (
                (ups/expect-string "parse")
                stream
            )
        ]
        [strm parser-for-string']
    )
)

(defn- parser-for-reg [stream]
    (let [[strm parsed] (
                (ups/expect-string "parse-re")
                stream
            )
        ]
        [strm (sfn/fn [instr] (re-pattern instr))]
    )
)

(defn- parse-parser [stream]
    (let [[strm parsed] (
                (ups/chain
                    parse-split
                    (ups/choice parser-for-string parser-for-reg)
                    whitespaces
                    jliteral-string-parser
                    whitespaces
                    (ups/expect-string "as")
                    whitespaces
                    parse-event-str
                    (ups/optional whitespaces)
                )
                stream
            )
            rfun (nth parsed 1)
            tKey (nth parsed 7)
            parseStr (last (nth parsed 3))
            preg (rfun parseStr)
        ]
    (debug "parser key" tKey)
    [strm  
        {            
            :key
            tKey
            :parser
            (sfn/fn [inStr] 
                (re-find preg inStr)
            )
        }
    ]
    )
)

(defn- parse-parsers [stream]
    (let [[strm parsed] ((ups/many 
                parse-parser    
            )
            stream
        )
        ]
        [strm 
            (if (empty? parsed)
                []
                {:parseRules parsed}
            )
        ]
    )
)

(defn- parse-group-keys [stream]
    (let [[strm parsed] (
            (ups/chain
                parse-event-str
                (ups/optional whitespaces)
                (ups/many
                    (ups/chain
                        (ups/expect-char \,)
                        (ups/optional whitespaces)
                        parse-event-str
                        (ups/optional whitespaces)
                    )
                )
            )
            stream
        )
        keyStr (->>
                parsed
                flatten
                (filter string?)
            )
        ]
        [stream keyStr]
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

(defn- parse-static-fun' [inStr inFun stream]
    (let [[strm parsed] (
                (ups/expect-string inStr)
                stream
            )
        ]
        [strm [inStr inFun] ]
    )
)

(defn- parse-static-fun-list []
    (let [keylist (keys static-rules)
        ]
        (debug "keylist " keylist)
        (map 
            (sfn/fn [tKey] 
                (partial parse-static-fun' tKey (get static-rules tKey))
            )
            keylist
        )
    )
)

(defn- parse-static-fun'' [stream]
    (let [[strm parsed] (
                (ups/chain
                    (apply 
                        ups/choice
                        (parse-static-fun-list)
                    )
                    whitespaces
                    parse-event-str
                )
                stream
            )
            sFun (last (first parsed))
            fStr (first (first parsed))
            sKey (last parsed)
        ]
        [strm 
            {
                :statInKey sKey,
                :statFun sFun,
                :statOutKey (str fStr "_" sKey)
            }
        ]
    )
)

(defn- parse-static-fun [stream]
    (let [[strm parsed] (
            (ups/chain
                parse-static-fun'' 
                (ups/many 
                    (ups/chain 
                        (ups/optional whitespaces)
                        (ups/expect-char \,)
                        (ups/optional whitespaces)
                        parse-static-fun''
                        (ups/optional whitespaces)
                    )
                )
            )
            stream
        )
        maps (filter  map? (flatten parsed))
        ]
        [strm maps]
    )
)

(defn- parse-group [stream]
    (let [[strm parsed] (
                (ups/chain
                    parse-split
                    parse-static-fun
                    whitespaces
                    (ups/expect-string "by")
                    whitespaces
                    parse-group-keys
                )
                stream
            )
            gKeys (last parsed)
            statRules (nth parsed 1)
        ]
        [strm {:groupKeys gKeys :statRules statRules}]
        )
)

(defn parse-all [inStr]
    (let [stream (ups/positional-stream inStr)
            [strm rst](
                (ups/chain 
                    (ups/optional whitespaces)
                    parse-event
                    (ups/optional whitespaces)
                    (ups/optional parse-parsers)
                    (ups/optional whitespaces)
                    (ups/optional parse-group)
                    (ups/optional whitespaces)
                )
                stream
            )
        ]
    ;[
    ;    (ups/extract-string-between stream strm) 
    ;    rst
    ;] 
    (->>
        rst
        flatten
        (filter map?)
        (apply merge)
    )
        
    )
)


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
        (let [psr (parse-all sStr)
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
        (let [psr (parse-all sStr)
                timeRule (assoc (get timeMap timeWindow) :startTime startTime)
            ]
            (assoc psr :timeRule timeRule)
        )
    )
)
