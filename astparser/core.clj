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
)

;Note: Make sure you run the following commands to generate neccessary classes regarding the Java grammer
;java org.antlr.Tool Java.g
;java org.antlr.Tool JavaTreeParser.g
;javac *.java

;Used to generate the DOT representation of a single java file's AST tree. Currently the DOT file is dumped to the console
;The walker is used to walk through the AST tree. Will add more contents to this part.
(defn generate-DOT [f] 
    (let [lexer (JavaLexer. (ANTLRFileStream. f))
          tokens (CommonTokenStream. lexer)
          parser (JavaParser. tokens)
          r (.javaSource parser)
          tree (.getTree r)
          nodes (CommonTreeNodeStream. tree)          
          ]
          (.println (System/out) (.toDOT (DOTTreeGenerator.) tree))
          (.setTokenStream nodes tokens)
          (.println (System/out) (.toStringTree tree))
     (let 
          [walker (JavaTreeParser. nodes)]
          (.println (System/out) "\nWalk tree:\n")
          (.javaSource walker)
          (.println (System/out) (.toString tokens))
     )
     )
)

;This can be used to dump all tokens
(defn dump-tokens [f] 
    (let [lexer (JavaLexer. (ANTLRFileStream. f))
          tokens (CommonTokenStream. lexer)
          parser (JavaParser. tokens)]
          (.dumpTokens parser)
     )
)


;Still in progress
;Used to parse all java files under a project
#_ 
(defn parse-project [dir]
    (def sourceFiles (get-fileslist dir))
    (doseq [f sourceFiles] (generate-DOT f))
)
    
;Still in progress
;Used to get the java file list of a project
#_
(defn get-fileslist [dir]
    (def files (map #(.getName %) (file-seq (File. dir))))
    (def javafiles (filter #(re-matches #".*\.java" %) files))
;    (println javafiles)
    (map #(str dir %) javafiles)
)



