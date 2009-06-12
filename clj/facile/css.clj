(ns clj.facile.css)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cascading style sheets

(defmacro rule
  [selector & decls]
  
  (str selector " { "
       (reduce (fn [s [k v]] (str s k ": " v "; ")) "" (partition 2 decls))
       "}"))

(comment
  (rule h2
	:color "#666",
	:font-weight bold)
  )