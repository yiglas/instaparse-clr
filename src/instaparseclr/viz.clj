(ns instaparseclr.viz
  (:import java.io.IOException))   

(try 
  (require '[rhizome.viz :as r])
  (catch Exception e
    (require '[instaparseclr.viz-not-found :as r])))

(defn span
  "Takes a subtree of the parse tree and returns a [start-index end-index] pair
   indicating the span of text parsed by this subtree.
   start-index is inclusive and end-index is exclusive, as is customary
   with substrings.
   Returns nil if no span metadata is attached."
  [tree]
  (let [m (meta tree)
        s (:instaparseclr.gll/start-index m)
        e (:instaparseclr.gll/end-index m)]
    (when (and s e)
      [s e])))

(def rhizome-newline
  ;; Prior to Rhizome 0.2.5., \ was not an escape character so \n needed extra escaping.
  (when-let [escape-chars (try (ns-resolve (find-ns 'rhizome.dot) 'escapable-characters)
                               (catch Exception e nil))]
    (if (= escape-chars "|{}\"")
      "\\n"
      "\n")))


(defn- hiccup-tree-viz
    "visualize instaparse hiccup output as a rhizome graph. Requires rhizome: https://github.com/ztellman/rhizome"
    [mytree options]         
    (r/tree->image sequential? rest mytree 
                 :node->descriptor (fn [n] {:label (if (sequential? n) 
                                                     (apply str (first n)
                                                            (when (span n)
                                                              [rhizome-newline (span n)]))
                                                     (with-out-str (pr n)))})
                 :options options))
      
(defn- enlive-tree-viz
  "visualize enlive trees"
  [mytree options]
  (r/tree->image (comp seq :content) :content mytree 
             :node->descriptor (fn [n] 
                                 {:label (if (and (map? n) (:tag n))
                                           (apply str (:tag n)
                                                  (when (span n)
                                                    [rhizome-newline (span n)]))
                                           (with-out-str (pr n)))})
             :options options))

(defn tree-type
  [tree]
  (cond
    (and (map? tree) (:tag tree)) :enlive
    (and (vector? tree) (keyword? (first tree))) :hiccup
    (empty? tree) :nil
    (seq? tree) :rootless
    :else :invalid))

(defn fake-root
  "Create a root for a rootless tree"
  [children]
  (case (tree-type (first children))
    :enlive {:tag :hidden-root-tag
             :content children}
    :hiccup (into [:hidden-root-tag]
                  children)
    :nil nil
    :invalid))
    
(defn tree-viz
  "Creates a graphviz visualization of the parse tree.
   Optional keyword arguments:
   :output-file output-file (will save the tree image to output-file)
   :options options (options passed along to rhizome)

Important: This function will only work if you have added rhizome
to your dependencies, and installed graphviz on your system.  
See https://github.com/ztellman/rhizome for more information."
  [tree & {output-file :output-file options :options}]
  {:pre [(not= (tree-type tree) :invalid)]}
  (let [ttype (tree-type tree)]
    (if (= ttype :rootless)
      (tree-viz (fake-root tree) :options options)
      (let [image
            (try
              (case (tree-type tree)
                :enlive (enlive-tree-viz tree options)
                (:hiccup :nil) (hiccup-tree-viz tree options))
              (catch IOException e
                (throw (UnsupportedOperationException. 
                         "\n\nYou appear to have rhizome in your dependencies, but have not installed GraphViz on your system.
\nSee https://github.com/ztellman/rhizome for more information.\n"))))]
        (if output-file 
          (r/save-image image output-file)
          (r/view-image image))))))