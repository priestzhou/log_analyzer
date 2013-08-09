(ns astparser.core
    (:import (org.antlr.runtime ANTLRStringStream
                                ANTLRFileStream
                                CommonTokenStream
                                TokenRewriteStream
                                RuleReturnScope)
             (org.antlr.runtime.tree CommonTree
                                     CommonTreeNodeStream
                                     DOTTreeGenerator) 
             (org.anglr.stringtemplate)
             (astparser JavaLexer JavaParser JavaTreeParser)
             (java.io.File)
    )
    (:require clojure.java.io)
)

;TODO: special case: "(.*)" appears in any string --> escape it!
;Step 1: find all objects of org.apache.commons.logging.Log 
;Step 2: find all log printing calls of those objects. (The list of possible printing calls: http://commons.apache.org/proper/commons-logging/guide.html
;currently they are: 
;    log.fatal(Object message);
;    log.fatal(Object message, Throwable t);
;    log.error(Object message);
;    log.error(Object message, Throwable t);
;    log.warn(Object message);
;    log.warn(Object message, Throwable t);
;    log.info(Object message);
;    log.info(Object message, Throwable t);
;    log.debug(Object message);
;    log.debug(Object message, Throwable t);
;    log.trace(Object message);
;    log.trace(Object message, Throwable t);

(def logvarlist (hash-map)) ;pairs of "org.apache.commons.logging.Log" class and objects
(def logvartype (list)) ;objects instanced by "org.apache.commons.logging.Log", (first logvartype) indicates the class in this scope
(def tostringtable (hash-map))
(def loglinevartypetable (hash-map))
(def vartypetable (hash-map))
(def classhierarchy (hash-map))    ; store the (childclass, parentclass) paris

(defn debug [tree]
    (if (not (.isNil tree))
        (do
            (.println (System/out) (.getText tree ))
            (.print (System/out) "Type: ")
            (.println (System/out) (.getType  tree ))
            (.print (System/out) "Line: ")
            (.println (System/out) (.getLine (.getToken tree)))
            (.print (System/out) "ChildCount: " )
            (.println (System/out) (.getChildCount tree))
        )
    )
)

(defn debug-hashmap [hm]
    (.print (System/out) " keys: ")
    (.print (System/out) (keys hm))
    (.print (System/out) " vals: ")
    (.println (System/out) (vals hm))
)
            
;in-order traversal to concanate the whole import line
(defn walk-import-line [tree]
    (if (.isNil tree)
        "" 
        (if (or (some #(= (.getType tree) %) [23 24 49 50 52 53 117 118 119 121 134 135 136 137 138 139]) (and (= 0 (.getChildCount tree)) (not (= (.getType tree) 15))))   ;if the node is "." or a leaf node (which is not ARGUMENT_LIST (no arguments in this situation)
            (case (.getChildCount tree)
                2 (str (walk-import-line (.getChild tree 0)) (.getText tree) (walk-import-line (.getChild tree 1)))
                1 (str (walk-import-line (.getChild tree 0)) (.getText tree))
                0 (.getText tree)
                nil
            ) 
            (case (.getChildCount tree)
                2 (str (walk-import-line (.getChild tree 0)) (walk-import-line (.getChild tree 1)))
                1 (walk-import-line (.getChild tree 0))
                0 ""
                nil
            )         
        )
    )
)

(defn get-import-line [tree]
    (if (.isNil tree)
        ""
        (if (= 93 (.getType tree))
            (do
                (let [result (walk-import-line tree)]
                    (case result
                        "org.apache.commons.logging.Log" result
                        "org.apache.commons.logging.*" "org.apache.commons.logging.Log"
                        "org.apache.commons.*" "org.apache.commons.logging.Log"
                        "org.apache.*" "org.apache.commons.logging.Log"
                        "org.*" "org.apache.commons.logging.Log"
                        ""
                    )  
                )
            )
        )
    )
)

;get the closest parent with the specific type
(defn get-parent [tree type]
    (if (= type (.getType tree))
        tree
        (get-parent (.getParent tree) type)
    )
)

(defn get-left-vardeclarator [tree]
    (let [vardeclarationlist (.getChild tree (- (.getChildCount tree) 1))]  ;VAR_DECLARATION_LIST
        (.getChild (.getChild vardeclarationlist 0) 0)
    )
)

(defn get-parentclass-tostring [classname]
    (let [parentclass (get classhierarchy classname)]
        (if (nil? parentclass)
            nil
            (let [parenttostring (find tostringtable parentclass)]
                (if (nil? parenttostring)
                    nil
                    (if (empty? (get tostringtable parentclass))
                        (get-parentclass-tostring parentclass)
                        parenttostring
                    )
                )
            )
        )
    )   
)

;get the line number of the "partial message template" mentioned in the paper's Appendix
(defn get-line-number [tree]
    (.getLine tree)
)

(defn get-var-type [tree type]
    (let [count (.getChildCount tree)]
        (loop [x (- count 1)]
            (if (= type (.getType (.getChild tree x)))
                (if (= 143 (.getType (.getChild (.getChild tree x) 0))) ;"QUALIFIED_TYPE_IDENT"
                    (.getText (.getChild (.getChild (.getChild tree x) 0) 0))
                    (.getText (.getChild (.getChild tree x) 0))
                )
                (recur (dec x))
            )
        )
    )
)

(defn get-var-name [tree type]
    (let [count (.getChildCount tree)]
        (loop [x (- count 1)]
            (if (= type (.getType (.getChild tree x)))
                (.getText (.getChild (.getChild (.getChild tree x) 0) 0))
                (recur (dec x))
            )
        )
    )
)

;another form of variable declation
(defn get-formalparamstddecl-name [tree type]
    (let [count (.getChildCount tree)]
        (loop [x (- count 1)]
            (if (= type (.getType (.getChild tree x)))
                (.getText (.getChild tree x))
                (recur (dec x))
            )
        )
    )
)


;return the list of all var types from the given var list
(defn get-vartype-list [varlist]
    (let [s (seq varlist)]
        (if (not (nil? s))
            (let [type (get vartypetable (first s)) rest-type (get-vartype-list (rest s))]
                (if (empty? rest-type)
                    (list type)
                    (list type rest-type)
                )
            )
            ()
        )
    )
)

;return the string result from the list of variables' names
(defn get-varname-result [varlist]
    (let [s (seq varlist)]
        (if (nil? s)
            ""
            (if (= 0 (count (rest s)))
                (str (first s) (get-varname-result (rest s)))
                (str (first s) "," (get-varname-result (rest s)))
            )
        )
    )
)

;return the string result from the list of variables' types
(defn get-vartype-result [varlist vartypetable]
    (let [s (seq varlist)]
    (if (nil? s)
        ""
        (if (= 0 (count (rest s)))
            (do
            (if (some #(= (first s) %) ["PARENTESIZED_EXPR" "METHOD_CALL"])
                (do
                  (str (first s) (get-vartype-result (rest s) vartypetable))
                )
                (str (get vartypetable (first s)) (get-vartype-result (rest s) vartypetable))
            )
            )
            (do
            (if (some #(= (first s) %) ["PARENTESIZED_EXPR" "METHOD_CALL"])
                (do
                  (str (first s) "," (get-vartype-result (rest s) vartypetable))
                )
                (str (get vartypetable (first s)) "," (get-vartype-result (rest s) vartypetable))
            )
            )
        )
    )
    )
)

;when encounter an object, replace it with its toString value
(defn walk-logline-replace-tostring [tree root]
    (if (.isNil tree)
        ""
        (if (some #(= (.getType tree) %) [133 159 37])  ;in some cases, we currently do not parse the sub-tree, e.g. "PARENTESIZED_EXPR". Or it is a method call （not A.toString()). Or "CLASS_CONSTRUCTOR_CALL"
            "(METHODCALL.*)"
            (if (and (= (.getType tree) 116) (not (= tree root))) ;method call (non-root)
                ;check whether it is a toString call like A.toString()
                (if (= 52 (.getType (.getChild tree 0))) 
                    (if (= (.getText (.getChild (.getChild tree 0) 1)) "toString")
                        ;case: getXXX().toString(). Currently we replace it with (METHODCALL.*)
                        (if (= (.getType (.getChild (.getChild tree 0) 0)) 116)
                            "(METHODCALL.*)"
                            (let [name (.getText (.getChild (.getChild tree 0) 0))]
                                ;"name" may be the object's name, we need to do one more step to find which class it is instantiated from
                                (let [classname (get loglinevartypetable name)]
                                    (if (nil? (find tostringtable classname))  ;if it is not a class name
                                        "(METHODCALL.*)"
                                        (let [vec (get tostringtable classname)]
                                            (if (empty? vec); remember that if we do not find toString() declaration for a class, we use "" as default value
                                                ;try to find the parent class
                                                (let [parentclass (get-parentclass-tostring classname)]
                                                    (if (empty? parentclass)
                                                        "(TOSTRING.*)"
                                                        (first parentclass)
                                                    )
                                                )
                                                (first vec)
                                            )
                                        )    
                                    )
                                )
                            )
                        )
                        "(METHODCALL.*)"
                    )
                    "(METHODCALL.*)"
                )
                ;if the node is "." or a leaf node (which is not ARGUMENT_LIST (no arguments in this situation)
                (if (or (some #(= (.getType tree) %) [52]) (= 0 (.getChildCount tree))) 
                    (case (.getChildCount tree)
                        2 
                        ;if there are more than 1 arguments for the root METHOD_CALL, we only care abou the first one. e.g. LOG.error("Failed to kill task " + status.getTaskID(), e);
                        (if (and (and (= (.getParent tree) root) (= 15 (.getType tree))) (> 1 (.getChildCount tree)))
                            (str (walk-logline-replace-tostring (.getChild tree 0) root) (.getText tree))
                            (str (walk-logline-replace-tostring (.getChild tree 0) root) (.getText tree) (walk-logline-replace-tostring (.getChild tree 1) root))
                        )
                        1 (str (walk-logline-replace-tostring (.getChild tree 0) root) (.getText tree))
                        0
                        (if (= (.getType tree) 15)   ;empty argument list
                            ""
                            (let [name (.getText tree)]
                                ;if it is the "info" "debug" in "Log.info" "Log.debug"...
                                (if (and (and (= 52 (.getType (.getParent tree))) (= 2 (.getChildCount (.getParent tree)))) (not (nil? (find logvarlist (.getText (.getChild (.getParent tree) 0))))))
                                    name
                                    (let [classname (get loglinevartypetable name)]
                                        (if (nil? (find tostringtable classname))
                                            (if (= 161 (.getType tree))   ;STRING_LITERAL (means that is is a string
                                                (subs name 1 (- (.length name) 1))
                                                (if (nil? (find loglinevartypetable name)) ;TODO: See if it is a defined variable   ;if it is not a stirng, then it could be a variable or ...
                                                    ;if it is not a Log variable (also won't be method name like "info" "debug" since it is already a variable's name
                                                    name
                                                    (if (nil? (find logvarlist name))
                                                        ;special case: some variables are defined as "info", "debug" ...
                                                        ;check its parent, if its parent is a "+" then it is a variable concated in the string
                                                        (let [parentnode (.getParent tree)]
                                                            (if (= 134 (.getType parentnode))
                                                                "(.*)"   ;TODO, variable
                                                                ;deal with the case like this.info
                                                                (do
                                                                    (if (= 52 (.getType parentnode))
                                                                        (if (= 116 (.getType (.getParent parentnode))) ;it if s method call like Log.info
                                                                            name
                                                                            "(.*)" ;TODO, variable
                                                                        )
                                                                    )  
                                                                    ;special case: LOG.info(message). message is an EXPR here
                                                                    (if (and (and (= 15 (.getType (.getParent parentnode))) (= 62 (.getType parentnode))) (= 1 (.getChildCount (.getParent parentnode))))
                                                                        "(.*)" ;TODO, variable
                                                                    )
                                                                )
                                                            )
                                                        )
                                                        name
                                                    )
                                                )
                                            )
                                            (let [vec (get tostringtable classname)]
                                                (if (empty? vec)
                                                    "(TOSTRING.*)"
                                                    (first vec)
                                                )
                                            )    
                                        )
                                    )
                                )
                            )
                        )
                        nil
                    ) 
                    (case (.getChildCount tree)
                        2 
                        (if (and (= (.getParent tree) root) (= 15 (.getType tree)))
                            (str (walk-logline-replace-tostring (.getChild tree 0) root))
                            (str (walk-logline-replace-tostring (.getChild tree 0) root) (walk-logline-replace-tostring (.getChild tree 1) root))
                        )
                        1 (walk-logline-replace-tostring (.getChild tree 0) root)
                        0 ""
                       nil
                    )         
                )
            )
        )
    )
)

;get the var name list for all vars appeared in the log line        
(defn get-varname-vartype-list [tree root]
    (if (.isNil tree)
        ()
        (if (some #(= (.getType tree) %) [133 159 37])  ;in some cases, we do not need to parse the sub-tree, e.g. "PARENTESIZED_EXPR". Or it is a method call （not A.toString())
            ()
            (if (and (= (.getType tree) 116) (not (= tree root))) ;method call (non-root)
                (if (= 52 (.getType (.getChild tree 0))) 
                    (if (= (.getText (.getChild (.getChild tree 0) 1)) "toString")
                        ;case: getXXX().toString(). Currently we replace it with (.*)
                        (if (= (.getType (.getChild (.getChild tree 0) 0)) 116)
                            ()
                            (let [name (.getText (.getChild (.getChild tree 0) 0))]
                                (let [classname (get loglinevartypetable name)]
                                    (if (nil? (find tostringtable classname))
                                        ()
                                        (let [vec (get tostringtable classname)]
                                            (if (empty? vec)
                                                ;try to find the parent class
                                                (let [parentclass (get-parentclass-tostring classname)]
                                                    (if (nil? parentclass)
                                                        ()
                                                        (doseq [x (second parentclass) y (last parentclass)]
                                                            (def logline-varnamelist (conj logline-varnamelist x))
                                                            (def logline-vartypelist (conj logline-vartypelist y))
                                                        )
                                                    )
                                                )
                                                (doseq [x (second vec) y (last vec)]
                                                    (def logline-varnamelist (conj logline-varnamelist x))
                                                    (def logline-vartypelist (conj logline-vartypelist y))
                                                )
                                            )
                                        )    
                                    )
                                )
                            )
                        )
                    )
                )
                (if (or (some #(= (.getType tree) %) [52]) (= 0 (.getChildCount tree))) 
                    (case (.getChildCount tree)
                        2 
                        (if (and (and (= (.getParent tree) root) (= 15 (.getType tree))) (> 1 (.getChildCount tree)))
                            (get-varname-vartype-list (.getChild tree 0) root)
                            (do 
                                (get-varname-vartype-list (.getChild tree 0) root)
                                (get-varname-vartype-list (.getChild tree 1) root)
                            )
                        )
                        1 (get-varname-vartype-list (.getChild tree 0) root)
                        0
                        (if (not (= (.getType tree) 15))   ;empty argument list
                            (let [name (.getText tree)]
                            (if (and (and (= 52 (.getType (.getParent tree))) (= 2 (.getChildCount (.getParent tree)))) (not (nil? (find logvarlist (.getText (.getChild (.getParent tree) 0))))))
                                name
                                (let [classname (get loglinevartypetable name)]
                                (if (nil? (find tostringtable classname))
                                    (if (not (= 161 (.getType tree)))   ;STRING_LITERAL (means that is is a string
                                        (if (find loglinevartypetable name)
                                            (if (nil? (find logvarlist name))
                                                ;special case: some variables are defined as "info", "debug" ...
                                                ;check its parent, if its parent is a "+" then it is a variable concated in the string
                                                (let [parentnode (.getParent tree)]
                                                    (if (= 134 (.getType parentnode))
                                                        (if (find loglinevartypetable name)
                                                            (do
                                                                (def logline-varnamelist (conj logline-varnamelist name))
                                                                (def logline-vartypelist (conj logline-vartypelist (get loglinevartypetable name)))
                                                            )
                                                        )
                                                        ;deal with the case like this.info
                                                        (do
                                                        (if (= 52 (.getType (.getParent tree)))
                                                            (if (not (= 116 (.getType (.getParent (.getParent tree)))))
                                                                (do
                                                                    (def logline-varnamelist (conj logline-varnamelist (.getText tree)))
                                                                    (def logline-vartypelist (conj logline-vartypelist (get loglinevartypetable (.getText tree))))
                                                                )
                                                            )
                                                        )  
                                                        ;special case: LOG.info(message). message is an EXPR here
                                                        (if (and (and (= 15 (.getType (.getParent parentnode))) (= 62 (.getType parentnode))) (= 1 (.getChildCount (.getParent parentnode))))
                                                            (do
                                                               (def logline-varnamelist (conj logline-varnamelist (.getText tree)))
                                                               (def logline-vartypelist (conj logline-vartypelist (get loglinevartypetable (.getText tree))))
                                                            )
                                                        )
                                                        )
                                                    )
                                                )  
                                            )
                                            ;special case: LOG.info(message). message is an EXPR here
                                            (if (and (and (= 15 (.getType (.getParent (.getParent tree)))) (= 62 (.getType (.getParent tree)))) (= 1 (.getChildCount (.getParent (.getParent tree)))))
                                                (do
                                                    (def logline-varnamelist (conj logline-varnamelist (.getText tree)))
                                                    (def logline-vartypelist (conj logline-vartypelist (get loglinevartypetable (.getText tree))))
                                                )
                                            )
                                        )
                                    )
                                    (let [vec (get tostringtable classname)]
                                        (doseq [x (second vec) y (last vec)]
                                            (def logline-varnamelist (conj logline-varnamelist x))
                                            (def logline-vartypelist (conj logline-vartypelist y))
                                        )
                                    )
                                )
                                )
                            )
                        )
                        )
                        ()
                    )
                    (case (.getChildCount tree)
                        2 
                        (if (and (and (= (.getParent tree) root) (= 15 (.getType tree))) (> 1 (.getChildCount tree)))
                            (get-varname-vartype-list (.getChild tree 0) root)
                            (do 
                                (get-varname-vartype-list (.getChild tree 0) root)
                                (get-varname-vartype-list (.getChild tree 1) root)
                            )
                        )
                        1 (get-varname-vartype-list (.getChild tree 0) root)
                        0 ()
                        ()
                        )         
                    )
            )
        )
    )
)

;(The list of possible printing calls: http://commons.apache.org/proper/commons-logging/guide.html#Logging_a_Message
;get the whole log line as a string
(defn get-log-line [tree]
    (def logline-varnamelist (vector))
    (def logline-vartypelist (vector))
    (let [methodcallnode (get-parent tree 116)]
        (get-varname-vartype-list methodcallnode methodcallnode)
        (let [logline (walk-logline-replace-tostring methodcallnode methodcallnode) varname (.getText tree)]
            ;(.println (System/out) logline)
            (if (nil? logline)
                nil
                (loop [x ["\\.info(.*)" "\\.fatal(.*)" "\\.error(.*)" "\\.warn(.*)" "\\.debug(.*)" "\\.trace(.*)"]]
                    (when (not (empty? x))
                        (let [matching (re-matches (re-pattern (str varname (first x))) logline)]
                            (if (empty? matching)
                                (recur (rest x))
                                (str (second matching) "[" (get-varname-result logline-varnamelist) "][" (get-varname-result logline-vartypelist) "]")
                            )
                        )
                    )
                )
            )
        )
    )
)

;TODO: We should pop the element from logvartype when the scope is done
;traverse the ast tree to get all log lines
(defn traverse [tree f]
    ;(debug tree)
    (if (.isNil tree)
        nil
        (let [count (.getChildCount tree)]
            (doseq [x (range count)] 
                (case (.getText (.getChild tree x))
                    ;TODO: to decide when we need to pop the log type!
                    "import" 
                    (let [importlog (get-import-line (.getChild tree x))] 
                        (if (= importlog "") 
                            () 
                            (do 
                                (def logvartype (conj logvartype importlog))
                                (traverse (.getChild tree x) f)
                            )
                        )
                    ) ; (first logvartype) will give us the current scope of "Log"
                    "Log" 
                    (if (and (= (.getType tree) 143) (= (.getType (.getParent (.getParent tree))) 179)) ;QUALIFIED_TYPE_IDENT, which means this is a var declaration like Log LOG = ...
                        (let [vardeclarenode (get-parent tree 179)] 
                            (let [logvarname (.getText (get-left-vardeclarator vardeclarenode))]
                                (def logvarlist (assoc logvarlist logvarname (first logvartype)))
                                (traverse (.getChild tree x) f)
                            )
                        )
                    )
                
                    ;if we encounter a variable whose name matches with one log variable in logvarlist, check its corresponding type. If its type is 
                    ;"org.apache.commons.logging.Log", continue checking the current scope, which is indicated by (first logvartype)
                    (let [varname (.getText (.getChild tree x))]
                        ;if this is not a new variable declaration, and not an assignment nor an parameter in any func call like A(Log, XX)
                        (if (and (find logvarlist varname) (and (and (not (= (.getType tree) 180)) (not (= (.getText tree) "="))) (not (= (.getType tree) 62))))
                            (do
                                (if (= "org.apache.commons.logging.Log" (val (find logvarlist varname)))
                                    (if (= "org.apache.commons.logging.Log" (first logvartype))
                                        (if (= (.getType (.getParent tree)) 116)
                                        (with-open [wrtr (clojure.java.io/writer "C:/Users/chenl_000/log_logtemplate.out" :append true)]
                                            (let [logline (get-log-line (.getChild tree x))]
                                                (if (not (nil? logline))
                                                    (.write wrtr (str logline "[" f "：" (get-line-number (.getChild tree x)) "]\r\n\r\n"))
                                                )
                                            )
                                        )
                                        )
                                    )
                                )
                            )
                        )
                        ;if this is a variable declaration. Note: there could be multiple ways of var declaration:
                        ;VAR_DECLARATION， FORMAL_PARAM_STD_DECL，FORMAL_PARAM_VARARG_DECL?
                        ;currently we do not handle the scope of variables, we use the latest declaration of a variable with the same name
                        (do
                            (case (.getType (.getChild tree x))
                                179 (def loglinevartypetable (assoc loglinevartypetable (get-var-name (.getChild tree x) 181) (get-var-type (.getChild tree x) 175)))
                                74 (def loglinevartypetable (assoc loglinevartypetable (get-formalparamstddecl-name (.getChild tree x) 89) (get-var-type (.getChild tree x) 175)))
                                75 (def loglinevartypetable (assoc loglinevartypetable (get-formalparamstddecl-name (.getChild tree x) 89) (get-var-type (.getChild tree x) 175)))
                                nil
                            )
                        )
                        (traverse (.getChild tree x) f)
                     )                                     
                );end of case
            )
        )
    )
)

;Do not output DOT, instead, we walk through the AST tree to look for log printing functions
;Some notes:
;(.getType tree) returns an integer indicating the type, which is pre-defined in the file Java.tokens
(defn get-log-func [f] 
    (let [lexer (JavaLexer. (ANTLRFileStream. f))
          tokens (CommonTokenStream. lexer)
          parser (JavaParser. tokens)
          r (.javaSource parser)
          tree (.getTree r)         ;generate a common tree        
          ]
          (traverse tree f)
     )
)    

;given a "FUNCTION_METHOD_DECL" node, check if it is a public String toString() method declaration
(defn check-if-tostring [tree]
    (let [count (.getChildCount tree)]
        (if (not (= 5 count))
            false
            (if (and (and (= 120 (.getType (.getChild tree 0))) (= 175 (.getType (.getChild tree 1)))) (= "toString" (.getText (.getChild tree 2))))
                true
                false
            )
        )
    )
)   

;process the string returned by walk function, analyze the var type if there is any
;(defn process-walk-tostring [s]
;    (if (empty? s)
;        ""
;        (if (string? (read-string s))
;            (str (read-string s) (process-walk-tostring (subs s (+ 2 (.length (read-string s))))))
;        )
;    )
;)

;in-order traversal to get prefix of the log template (the string part)
(defn walk-prefix-tostring [tree]
    (if (.isNil tree)
        ""
        (if (some #(= (.getType tree) %) [133 116 159 37])  ;in some cases, we do not need to parse the sub-tree, e.g. "PARENTESIZED_EXPR"
            "(METHODCALL.*)"
            (if (or (some #(= (.getType tree) %) [52]) (= 0 (.getChildCount tree)))   ;if the node is "." or a leaf node (which is not ARGUMENT_LIST (no arguments in this situation)
                (case (.getChildCount tree)
                    2 
                    (if (= (.getType (.getChild tree 0)) 167) 
                        (walk-prefix-tostring (.getChild tree 1))
                        (str (walk-prefix-tostring (.getChild tree 0)) (.getText tree) (walk-prefix-tostring (.getChild tree 1)))
                    )
                    1 (str (walk-prefix-tostring (.getChild tree 0)) (.getText tree))
                    0 
                    (if (= 161 (.getType tree))   ;STRING_LITERAL (means that is is a string
                        (subs (.getText tree) 1 (- (.length (.getText tree)) 1))
                        "(.*)"
                    )
                    ""
                ) 
                (case (.getChildCount tree)
                    2 (str (walk-prefix-tostring (.getChild tree 0)) (walk-prefix-tostring (.getChild tree 1)))
                    1 (walk-prefix-tostring (.getChild tree 0))
                    0 ""
                    ""
                )         
            )
        )
    )
)

;in-order traversal to get the list of all variables appeared in the tostring line
(defn walk-get-varlist-tostring [tree]
    (if (.isNil tree)
        ()
        (if (some #(= (.getType tree) %) [133 116 159 37])  ;in some cases, we do not need to parse the sub-tree, e.g. "PARENTESIZED_EXPR" "STATIC_ARRAY_CREATOR" CLASS_CONSTRUCTOR_CALL
            ()
            (if (or (some #(= (.getType tree) %) [52]) (= 0 (.getChildCount tree)))   ;if the node is "." or a leaf node (which is not ARGUMENT_LIST (no arguments in this situation)
                (case (.getChildCount tree)
                    2 
                    (if (= (.getType (.getChild tree 0)) 167) ;this
                        (walk-get-varlist-tostring (.getChild tree 1))
                        (list (walk-get-varlist-tostring (.getChild tree 0)) (walk-get-varlist-tostring (.getChild tree 1)))
                    )
                    1  (walk-get-varlist-tostring (.getChild tree 0))
                    0 
                    (if (not (= 161 (.getType tree)))   ;if it is not STRING_LITERAL
                        (.getText tree)
                    )
                    ()
                ) 
                (case (.getChildCount tree)
                    2 (list (walk-get-varlist-tostring (.getChild tree 0)) (walk-get-varlist-tostring (.getChild tree 1)))
                    1 (walk-get-varlist-tostring (.getChild tree 0))
                    0 ()
                    ()
                )         
            )
        )
    )
)

;given the blockscope node under the tostring FUNCTION_METHOD_DECL, return the "partial message template" mentioned in the paper's Appendix
(defn parse-tostring [tree classname]
    (let [count (.getChildCount tree)]
        (loop [x 0]
            (when (< x count)
                (case (.getType (.getChild tree x))
                    179 (def vartypetable (assoc vartypetable (get-var-name (.getChild tree x) 181) (get-var-type (.getChild tree x) 175)))
                    74 (def vartypetable (assoc vartypetable (get-formalparamstddecl-name (.getChild tree x) 89) (get-var-type (.getChild tree x) 175)))
                    75 (def vartypetable (assoc vartypetable (get-formalparamstddecl-name (.getChild tree x) 89) (get-var-type (.getChild tree x) 175)))
                    147
                    (if (= 62 (.getType (.getChild (.getChild tree x) 0))) ;EXPR
                        (do
                            (let [varlist (filter #(not (nil? %)) (flatten (walk-get-varlist-tostring (.getChild tree x))))]
                                (def tostringtable (assoc tostringtable classname (vector (walk-prefix-tostring (.getChild (.getChild tree x) 0)) varlist (flatten (get-vartype-list varlist)))))
                            )
                        )
                    )
                    ()
                )
                (recur (inc x))
            )
        )
        
    )
)
  
;given a "CLASS_TO...EL_SCOPE" node, dig into it to see whether there is any toString method call
;consider different possible cases in the toString call:
;"xxx" + *
;"xxx" + this.*
;"xxx" + classA.getXXX()
(defn find-tostring-declare [tree classname]
    (def vartypetable (hash-map))
    (if (.isNil tree)
        nil
          (let [count (.getChildCount tree)]
              (loop [x 0]
                  (if (= x count)
                      (if (= (get tostringtable classname "no_tostring_in_the_table") "no_tostring_in_the_table")
                          (def tostringtable (assoc tostringtable classname [])); if there is no toString method in the class
                      )
                      (do
                          ;at the same time, we build a map for (var, vartype) as vartypetable
                          (if (= 179 (.getType (.getChild tree x))) ;VAR_DECLARATION
                              (def vartypetable (assoc vartypetable (get-var-name (.getChild tree x) 181) (get-var-type (.getChild tree x) 175)))  ;("VAR_DECLARATOR"， "TYPE")
                          )
                          (if (and (= 80 (.getType (.getChild tree x))) (check-if-tostring (.getChild tree x)))   ;if it is a FUNCTION_METHOD_DECL for public String toString
                              (parse-tostring (.getChild (.getChild tree x) (- (.getChildCount (.getChild tree x)) 1)) classname)
                          )
                          (recur (inc x))
                      )
                  )
              )
          )
    )
)

;This func finds all "class" node (including inner classes) and then put(classname, (find-tostring-declare classelscopenode) pairs in the tostring hash-map              
(defn tostring-traverse [tree]
    (if (.isNil tree)
        nil
        (let [count (.getChildCount tree)]
            (doseq [x (range count)] 
                (if (and (= 36 (.getType (.getChild tree x))) (and (> (.getChildCount (.getChild tree x)) 0) (= (.getType (.getChild (.getChild tree x) 0)) 120)))  ;class
                    (let [classname (.getChild (.getChild tree x) 1)] 
                        ;start from "CLASS_TO...EL_SCOPE" node
                        (let [classelscopenode (.getChild (.getChild tree x) (- (.getChildCount (.getChild tree x)) 1))]
                            (find-tostring-declare classelscopenode (.getText classname))
                        )
                    )
                )
                (tostring-traverse (.getChild tree x))
            )
        )
    )
)

;find "EXTENDS_CLAUSE" node
(defn class-hierarchy-traverse [tree]
    (if (.isNil tree)
        nil
        (let [count (.getChildCount tree)]
            (doseq [x (range count)] 
                (if (and (= 36 (.getType (.getChild tree x))) (and (> (.getChildCount (.getChild tree x)) 2) (= (.getType (.getChild (.getChild tree x) 2)) 65)))  ;class has an "EXTENDS_CLAUSE"
                    (let [classname (.getChild (.getChild tree x) 1) extendclause (.getChild (.getChild tree x) 2)] 
                        (let [parentclass (.getChild (.getChild (.getChild extendclause 0) 0) 0)]
                            (def classhierarchy (assoc classhierarchy (.getText classname) (.getText parentclass)))
                        )
                    )
                )
                (class-hierarchy-traverse (.getChild tree x))
            )
        )
    )
)

(defn build-class-hierarchy [f]
    (let [lexer (JavaLexer. (ANTLRFileStream. f))
        tokens (CommonTokenStream. lexer)
        parser (JavaParser. tokens)
        r (.javaSource parser)
        tree (.getTree r)         ;generate a common tree        
        ]
        (class-hierarchy-traverse tree)
    )
)

(defn build-tostring-table [f]  
    (let [lexer (JavaLexer. (ANTLRFileStream. f))
        tokens (CommonTokenStream. lexer)
        parser (JavaParser. tokens)
        r (.javaSource parser)
        tree (.getTree r)         ;generate a common tree        
        ]
        (tostring-traverse tree)
    )
)      

;Used to get the java file list of a project
(defn get-fileslist [dir]
    (let [files (map #(.getCanonicalPath %) (file-seq (clojure.java.io/file dir)))]
        (filter #(re-matches #".*\.java" %) files)
    )
)

;Used to parse all java files under a project
(defn parse-project [dir sourceFiles]
    (doseq [f sourceFiles] 
        (with-open [wrtr (clojure.java.io/writer "C:/Users/chenl_000/log_logtemplate.out" :append true)]
            (.write wrtr (str f "\r\n"))
        )
        (def loglinevartypetable (hash-map))
        (get-log-func f)
    )
)

(defn build-tostring-table-project [dir sourceFiles]
    (doseq [f sourceFiles] 
        (def vartypetable (hash-map))
        (def logvarlist (hash-map))
        (def logvartype (list))
        (build-tostring-table f)
    )
    (with-open [wrtr (clojure.java.io/writer "C:/Users/chenl_000/log_tostringtable.out" :append true)]
        (doseq [x (seq tostringtable)]
            (.write wrtr (str x "\r\n"))
        )
    )
)

(defn build-class-hierarchy-project [dir sourceFiles]
    (doseq [f sourceFiles]
        (build-class-hierarchy f)
    )
    (with-open [wrtr (clojure.java.io/writer "C:/Users/chenl_000/log_classhierarchy.out" :append true)]
        (doseq [x (seq classhierarchy)]
            (.write wrtr (str x "\r\n"))
        )
    )
)
    
(defn get-log-template [dir]
    (let [sourceFiles (get-fileslist dir)]
        (build-class-hierarchy-project dir sourceFiles)
        (build-tostring-table-project dir sourceFiles)
        (parse-project dir sourceFiles)
    )
)