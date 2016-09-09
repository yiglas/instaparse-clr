(ns instaparseclr.combinators
  "The combinator public API for instaparse"
  (:refer-clojure :exclude [cat])
  (:use instaparseclr.clone)
  (:require [instaparseclr.combinators-source :as c])
  (:require [instaparseclr.cfg :as cfg])
  (:require [instaparseclr.abnf :as abnf]))

;; The actual source is in combinators-source.
;; This was necessary to avoid a cyclical dependency in the namespaces.

(defclone Epsilon c/Epsilon)
(defclone opt c/opt)
(defclone plus c/plus)
(defclone star c/star)
(defclone rep c/rep)
(defclone alt c/alt) 
(defclone ord c/ord)
(defclone cat c/cat)
(defclone string c/string)
(defclone string-ci c/string-ci)
(defclone unicode-char c/unicode-char)
(defclone regexp c/regexp)
(defclone nt c/nt)
(defclone look c/look)
(defclone neg c/neg)
(defclone hide c/hide)
(defclone hide-tag c/hide-tag)

(defclone ebnf cfg/ebnf)
(defclone abnf abnf/abnf)
       
