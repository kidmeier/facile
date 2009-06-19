(ns clj.facile.el
  (:use clj.facile)
  (:import 
   (clojure.lang AFn IPersistentCollection IPersistentList Keyword RT Symbol 
		 Var)
   (javax.faces.component UIComponentBase)
   (javax.faces.context FacesContext)
   (javax.faces.el ValueBinding)
   (clj.facile ClojureVariableResolver)))

;; Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- mangle
  "Mangle a Clojure identifier into a legal JSF EL identifier"
  [s]

  (ClojureVariableResolver/mangleIdent s))

(defn- demangle
  "De-mangle a legal JSF EL identifier into the corresponding Clojure 
   identifier"
  [s]
  
  (ClojureVariableResolver/unmangleIdent s))

(defn- el-get-ns-name 
  "Return the namespace that the passed in 'var' resides in."
  [var]
  
  (mangle (.getName (.. var ns (getName)))))

(defn- el-get-var-name 
  "Return the name of the 'var' as a String"
  [var]
  (mangle (.. var sym (getName))))

(defn- inline-el
  "Takes a string to be used as a snippet of inline EL and a list of inline-el 
   expressions and returns the concatenation of the expressions as a single 
   inline-el expression."
   
  [inline & exprs]

  (list 'inline-el
	(reduce (fn [inline expr]
		  (if (not (instance? IPersistentList expr))
		    (throw (new IllegalArgumentException 
				(str #'inline-el ": "
				     expr " is not an inline expression")))
		    
		    (str inline (second expr))))
		inline
		exprs)))

;; Polymorphic value handlers
(defmulti #^{:private true
	     :doc "Generates an EL lookup fragment."} 
  dot-fn class)

;; Clojure types
(defmethod dot-fn Keyword [kw] (str "['" kw "']"))
(defmethod dot-fn IPersistentList [l] 
  (when (not= 'inline-el (first l))
    (throw (new IllegalArgumentException 
		(str "Form: " l " is an invalid dot-expr."))))
  (second l))
(defmethod dot-fn Var [v] 
  (str "['" (el-get-ns-name v) "']['" (el-get-var-name v) "']"))
(defmethod dot-fn Symbol [s] (str "['" s "']"))

;; Java types
(defmethod dot-fn String [s] (str "['" s "']"))
(defn- dot-number [n] (str "[" n "]"))
(defmethod dot-fn Byte [b] (dot-number b))
(defmethod dot-fn Integer [i] (dot-number i))
(defmethod dot-fn Long [l] (dot-number l))
(defmethod dot-fn Short [s] (dot-number s))

(defmethod dot-fn :default [x]
  (throw (new IllegalArgumentException 
	      (str "Don't know how to handle property: " x))))

(defn dot
  "Used in conjunction with bind. Returns an inline-el expression that looks 
   up properties of the base variable described by (bind)."
  [& props]

  ;; We start with the inline-el marker
  (inline-el
   (reduce (fn [base prop]
	     (str base (dot-fn prop)))
	   ""
	   props)))

(defn bind 
  "Create an inline-el expression that refers to the specified Clojure 
   variable or symbol. Can optionally accept a secondary inline-el that is 
   assumed to be the result of dot, to refer to properties of the binding.

   If val is a Var, a proper reference to the namespace and then the variable
   is generated. If val is a Symbol, it is assumed to refer to an implicit or 
   locally scoped EL variable, e.g. request, or the variable introduced by 
   the :dataTable :var attribute."
  ([val]
     (bind val '(inline-el "")))

  ([val dot-expr]

     ;; If it is a variable, we create a value binding to the 
     ;; ClojureVariableResolver
     (if (var? val)

       ;; Generate reference to a variable
       (inline-el
	(str (el-get-ns-name val) "['" (el-get-var-name val) "']") dot-expr)
       
       (if (symbol? val)
	 ;; Generate reference to symbol
	 (inline-el (str val) dot-expr)
	 
	 ;; Otherwise throw an error, we don't handle this type of binding
	 (throw 
	  (new IllegalArgumentException 
	       (str #'bind ": can only bind to a Var or Symbol: " val)))))))
  
(def el-expr)
(defn- el-unary-op
  "Return a string corresponding to an application of the unary EL operator 
   'op'."
  [[op operand]]

  (cond
   (= op :empty)
   (str "(empty " (el-expr operand) ") ")

   (= op :negate)
   (str "(-" (el-expr operand) ") ")

   (= op :not)
   (str "(not " (el-expr operand) ") ")))

(defn- el-n-ary-op
  "Return a string corresponding to a fold of the specified binary EL operator 
   'op' across the supplied operands. i.e.:

   [:and e0 e1 e2 e3] -> (e0) && (e1) && (e2) && (e3)"
  [[op & expr]]
  
  (let [operators {:subtract "-",
		   :plus "+",
		   :mul "*",
		   :div "/",
		   :mod "mod",
		   :and "&&",
		   :or "||",
		   :eq "==",
		   :not= "!=",
		   :lt "<",
		   :lte "<=",
		   :gt ">",
		   :gte ">="}]
    
    (str "( "
	 (reduce (fn [e arg] (str e " " (operators op) " " (el-expr arg)))
		 (el-expr (first expr))
		 (rest expr))
	 ")")))

(defn- el-expr
  "Translates the supplied expression into a JSF EL string. Operators are 
   applied with el-unary-op and el-n-ary-op, literals are converted to an 
   appropriate string within the EL string, inline-el is passed through 
   directly into the string."
  [expr]

  (cond 
   (vector? expr) 
   (if (or (= :empty (first expr))
	   (= :negate (first expr))
	   (= :not (first expr)))
     (el-unary-op expr)
     
     (el-n-ary-op expr)),
   
   ;; Literals
   (string? expr) 
   (str "'" expr "' "),

   (instance? java.lang.Number expr)
   (str expr)

   ;; In some cases we have helper functions that generate EL strings to be 
   ;; inlined directly into the expression. We have these functions return a 
   ;; list prepended w/ the symbol 'inline-el so that we can distinguish these 
   ;; strings from string literals and pass them right through as valid el
   (and (instance? IPersistentList expr)
	(= 'inline-el (first expr)))
   (second expr)

   ;; Assume its some other kind of literal and translate it into a string
   :else (throw 
	  (new IllegalArgumentException 
	       (str #'el-expr ": Don't know how handle expression: " expr)))))
	
(defn el
  "Create a JSF EL string. Walks over the list of arguments deferring to 
   el-expr to generate appropriate EL strings for the passed in 
   sub-expressions."
  [& args]

  (let [el (str "#{" 
		(reduce (fn [e arg] (str e (el-expr arg))) "" args)
		"}")]
    (. *faces-app* (createValueBinding el))))

(def $ el)

;; Bindings ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- bind-view-local [val & dotargs]

  (let [key (str (gensym)),
	[view-ns view-sym] (.split *view-id* "/"),
	view-var (RT/var view-ns view-sym)]
    
    ;; First, store it in the locals map for this view
    (dosync
     (alter (view-locals *view-id*) assoc key val))

    ;; Bind to the locals map
    (bind view-var (apply #'dot :locals key dotargs))))
    
(defn method-binding 
  "Create a method-binding that targets the given function accepting 'parms'
   as arguments and returning a value of type 'ret'".
   [inline-el parms]
   
   (let [el (str "#{" (second inline-el) "}")]
     (. *faces-app* (createMethodBinding el parms))))

(defn action 
  "Wraps the given function in a method-binding suitable for use as a JSF
   action. The function should take no arguments and return a String."
  [bind-fn]

  ;; Bind to 'var.invoke
  (method-binding 
   (bind bind-fn (dot 'invoke))
   (make-array java.lang.Class 0)))

(defn action-listener 
  "Wraps the given function in a method-binding suitablefor use as a JSF 
   action listener. The function should take a single argument of type 
   javax.faces.event.ActionEvent and return nothing (void)."
  [bind-fn]
  
  (method-binding bind-fn (into-array [javax.faces.event.ActionEvent]) nil))

(defmethod clj.facile/set-widget-attribute IPersistentCollection
  [#^UIComponentBase widget
   #^String attr
   #^IPersistentCollection val]

  (.setValueBinding widget attr (el (bind-view-local val))))

;; We choose the collection over AFn in case of conflict
(prefer-method clj.facile/set-widget-attribute IPersistentCollection AFn)

(defmethod clj.facile/set-widget-attribute Var
  [#^UIComponentBase widget
   #^String attr
   #^Var val]

  ;; If the value is a function, create a method-binding
  (if (instance? AFn (var-get val))
    (-> (.getAttributes widget) 
	(.put attr (method-binding (bind val (dot 'invoke)) nil)))

    ;; Else bind a reference to the variable
    (.setValueBinding widget attr (el (bind val)))))

(defmethod clj.facile/set-widget-attribute ValueBinding 
  [#^UIComponentBase widget 
   #^String attr 
   #^ValueBinding val]
  
  (.setValueBinding widget attr val))

(defmethod clj.facile/set-widget-attribute AFn
  [#^UIComponentBase widget
   #^String attr
   #^AFn bind-fn]
  
  (-> (.getAttributes widget) 
      (.put attr (method-binding (bind-view-local bind-fn 'invoke)
				 (make-array java.lang.Class 0)))))
