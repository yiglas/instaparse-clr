(ns instaparse-clr.core
  (:use instaparse-clr.clone)
  (:require [instaparse-clr.gll :as gll] 
            [instaparse-clr.cfg :as cfg]
            [instaparse-clr.failure :as fail]
            [instaparse-clr.print :as print]
            [instaparse-clr.reduction :as red]
            [instaparse-clr.transform :as t]
            [instaparse-clr.abnf :as abnf]
            [instaparse-clr.viz :as viz]
            [instaparse-clr.repeat :as repeat]
            [instaparse-clr.combinators-source :as c]
            [instaparse-clr.line-col :as lc]))

(def ^:dynamic *default-output-format* :hiccup)
(defn set-default-output-format!
  "Changes the default output format.  Input should be :hiccup or :enlive"
  [type]
  {:pre [(#{:hiccup :enlive} type)]}
  (alter-var-root #'*default-output-format* (constantly type)))

(def ^:dynamic *default-input-format* :ebnf)
(defn set-default-input-format!
  "Changes the default input format.  Input should be :abnf or :ebnf"
  [type]
  {:pre [(#{:abnf :ebnf} type)]}
  (alter-var-root #'*default-input-format* (constantly type)))

(declare failure? standard-whitespace-parsers enable-tracing!)

(defn- unhide-parser [parser unhide]
  (case unhide
    nil parser
    :content 
    (assoc parser :grammar (c/unhide-all-content (:grammar parser)))
    :tags 
    (assoc parser :grammar (c/unhide-tags (:output-format parser) 
                                          (:grammar parser)))
    :all
    (assoc parser :grammar (c/unhide-all (:output-format parser)
                                         (:grammar parser)))))
  
(defn parse 
  "Use parser to parse the text.  Returns first parse tree found
   that completely parses the text.  If no parse tree is possible, returns
   a Failure object.
   
   Optional keyword arguments:
   :start :keyword  (where :keyword is name of starting production rule)
   :partial true    (parses that don't consume the whole string are okay)
   :total true      (if parse fails, embed failure node in tree)
   :unhide <:tags or :content or :all> (for this parse, disable hiding)
   :optimize :memory   (when possible, employ strategy to use less memory)
   :trace true      (print diagnostic trace while parsing)"
  [parser ^CharSequence text &{:as options}]
  {:pre [(contains? #{:tags :content :all nil} (get options :unhide))
         (contains? #{:memory nil} (get options :optimize))]}
  (let [start-production 
        (get options :start (:start-production parser)),
        
        partial?
        (get options :partial false)
        
        optimize?
        (get options :optimize false)
        
        unhide
        (get options :unhide)
        
        trace?
        (get options :trace false)
        
        _ (when (and trace? (not gll/TRACE)) (enable-tracing!))
        
        parser (unhide-parser parser unhide)]
    (gll/bind-trace 
      trace?
      (cond
        (:total options)
        (gll/parse-total (:grammar parser) start-production text 
                         partial? (red/node-builders (:output-format parser)))
        
        (and optimize? (not partial?))
        (let [result (repeat/try-repeating-parse-strategy parser text start-production)]
          (if (failure? result)
            (gll/parse (:grammar parser) start-production text partial?)
            result))
        
        :else
        (gll/parse (:grammar parser) start-production text partial?)))))
  
(defn parses 
  "Use parser to parse the text.  Returns lazy seq of all parse trees
   that completely parse the text.  If no parse tree is possible, returns
   () with a Failure object attached as metadata.
   
   Optional keyword arguments:
   :start :keyword  (where :keyword is name of starting production rule)
   :partial true    (parses that don't consume the whole string are okay)
   :total true      (if parse fails, embed failure node in tree)
   :unhide <:tags or :content or :all> (for this parse, disable hiding)
   :trace true      (print diagnostic trace while parsing)"
  [parser ^CharSequence text &{:as options}]
  {:pre [(contains? #{:tags :content :all nil} (get options :unhide))]}
  (let [start-production 
        (get options :start (:start-production parser)),
        
        partial?
        (get options :partial false)
        
        unhide
        (get options :unhide)
        
        trace?
        (get options :trace false)
        
        _ (when (and trace? (not gll/TRACE)) (enable-tracing!))
        
        parser (unhide-parser parser unhide)]
    (gll/bind-trace 
      trace?
      (cond
        (:total options)
        (gll/parses-total (:grammar parser) start-production text 
                          partial? (red/node-builders (:output-format parser)))
        
        :else
        (gll/parses (:grammar parser) start-production text partial?)))))
  
(defrecord Parser [grammar start-production output-format]
  clojure.lang.IFn
  (invoke [parser text] (parse parser text))
  (invoke [parser text key1 val1] (parse parser text key1 val1))
  (invoke [parser text key1 val1 key2 val2] (parse parser text key1 val1 key2 val2))
  (invoke [parser text key1 val1 key2 val2 key3 val3] (parse parser text key1 val1 key2 val2 key3 val3))
  (applyTo [parser args] (apply parse parser args)))


(defmethod clojure.core/print-method Parser [x writer]
  (binding [*out* writer]
    (println (print/Parser->str x))))

(defn parser
  "Takes a string specification of a context-free grammar,
   or a URI for a text file containing such a specification,
   or a map of parser combinators and returns a parser for that grammar.

   Optional keyword arguments:
   :input-format :ebnf
   or
   :input-format :abnf

   :output-format :enlive
   or
   :output-format :hiccup
   
   :start :keyword (where :keyword is name of starting production rule)

   :string-ci true (treat all string literals as case insensitive)

   :no-slurp true (disables use of slurp to auto-detect whether
                   input is a URI.  When using this option, input
                   must be a grammar string or grammar map.  Useful
                   for platforms where slurp is slow or not available.)

   :auto-whitespace (:standard or :comma)
   or
   :auto-whitespace custom-whitespace-parser"
  [grammar-specification &{:as options}]
  {:pre [(contains? #{:abnf :ebnf nil} (get options :input-format))
         (contains? #{:enlive :hiccup nil} (get options :output-format))
         (let [ws-parser (get options :auto-whitespace)]
           (or (nil? ws-parser)
               (contains? standard-whitespace-parsers ws-parser)
               (and
                 (map? ws-parser)
                 (contains? ws-parser :grammar)
                 (contains? ws-parser :start-production))))]}
  (let [input-format (get options :input-format *default-input-format*)
        build-parser (case input-format 
                       :abnf abnf/build-parser
                       :ebnf (if (get options :string-ci)
                               (fn [spec output-format]
                                 (binding [cfg/*case-insensitive-literals* true]
                                   (cfg/build-parser spec output-format)))
                               cfg/build-parser))
        output-format (get options :output-format *default-output-format*)
        start (get options :start nil)
        
        built-parser
        (cond
          (string? grammar-specification)
          (let [parser
                (if (get options :no-slurp)
                  ; if :no-slurp is set to true, string is a grammar spec
                  (build-parser grammar-specification output-format)                  
                  ; otherwise, grammar-specification might be a URI,
                  ; let's slurp to see
                  (try (let [spec (slurp grammar-specification)]
                         (build-parser spec output-format))
                    (catch java.io.FileNotFoundException e 
                      (build-parser grammar-specification output-format))))]            
            (if start (map->Parser (assoc parser :start-production start))
              (map->Parser parser)))
          
          (map? grammar-specification)
          (let [parser
                (cfg/build-parser-from-combinators grammar-specification
                                                   output-format
                                                   start)]
            (map->Parser parser))
          
          (vector? grammar-specification)
          (let [start (if start start (grammar-specification 0))
                parser
                (cfg/build-parser-from-combinators (apply hash-map grammar-specification)
                                                   output-format
                                                   start)]
            (map->Parser parser))

          :else
          (let [spec (slurp grammar-specification)
                parser (build-parser spec output-format)]
            (map->Parser parser)))]
    
    (let [auto-whitespace (get options :auto-whitespace)
          ; auto-whitespace is keyword, parser, or nil
          whitespace-parser (if (keyword? auto-whitespace)
                              (get standard-whitespace-parsers auto-whitespace)
                              auto-whitespace)]
      (if-let [{ws-grammar :grammar ws-start :start-production} whitespace-parser]
        (assoc built-parser :grammar
               (c/auto-whitespace (:grammar built-parser) (:start-production built-parser)
                                  ws-grammar ws-start))
        built-parser))))
        
(defn failure?
  "Tests whether a parse result is a failure."
  [result]
  (or
    (instance? gll/failure-type result)
    (instance? gll/failure-type (meta result))))

(defn get-failure
  "Extracts failure object from failed parse result."
  [result]
  (cond
    (instance? gll/failure-type result)
    result
    (instance? gll/failure-type (meta result))
    (meta result)
    :else
    nil))

(defn enable-tracing!
  "Recompiles instaparse with tracing enabled.
This is called implicitly the first time you invoke a parser with
`:trace true` so usually you will not need to call this directly."
  []
  (alter-var-root #'gll/TRACE (constantly true))
  (alter-var-root #'gll/PROFILE (constantly true))
  (require 'instaparseclr.gll :reload))

(defn disable-tracing!
  "Recompiles instaparse with tracing disabled.
Call this to restore regular performance characteristics, eliminating
the small performance hit imposed by tracing."
  []
  (alter-var-root #'gll/TRACE (constantly false))
  (alter-var-root #'gll/PROFILE (constantly false))
  (require 'instaparseclr.gll :reload))  

(defclone span viz/span)
   
(defclone transform t/transform)

(defclone visualize viz/tree-viz)

(defclone add-line-and-column-info-to-metadata lc/add-line-col-spans)

(def ^:private standard-whitespace-parsers
  {:standard (parser "whitespace = #'\\s+'")
   :comma (parser "whitespace = #'[,\\s]+'")})