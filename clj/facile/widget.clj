(ns clj.facile.widget
  (:import (javax.faces.component.UIComponentBase)))

(defn- gen-accessors
  "Generate a vector of method definitions suitable for use in gen-class
   which define the interface for getters and setters of the map of attributes
   passed in. The method names follow the JavaBean naming convention."

  [attribs]
  (let [gen-attrib 
	(fn [methods attrib]
	  (let [[name type] attrib
		prop (. (str name) (substring 1))]
	    (conj methods 
		  [`(symbol (str "get" 
				~(.. prop (substring 0 1) (toUpperCase))
				~(. prop (substring 1))))
		   [] type]
		  [`(symbol (str "set"
				~(.. prop (substring 0 1) (toUpperCase))
				~(. prop (substring 1))))
		   [type] (. Void TYPE)])))]
    (reduce gen-attrib [] attribs)))
  
(defmacro defcomponent
  "Defines the class for a custom JSF component. In addition, provides
   default implementations for any named attributes and also defines a 
   struct-map to contain the state."
  [name & args]

  (let [;; parms to genclass; merge default args to gen-class with those
	;; supplied by the caller
	parms (merge {;; Defaults
		      :extends UIComponentBase,
		      :attribs {}}
		     ;; allow caller provided values to override
		     (apply #'hash-map args)),

	;; qualified class name
	qualifiedClazz (str name),
	
	;; package name
	pkg (if (< (. qualifiedClazz (indexOf ".")) 0) 
	      ;; default package is not allowed; it cannot be referenced from within Clojure
	      (throw (new IllegalArgumentException "Cannot create a component in the default package, Clojure doesn't like that."))
	      
	      (. qualifiedClazz 
		 (substring 0 (.lastIndexOf qualifiedClazz ".")))),

	;; unqualified class name
	clazz (.substring qualifiedClazz (+ (.lastIndexOf qualifiedClazz ".") 1)),

	;; accessors to be passed into gen-class
	accessors (gen-accessors (:attribs parms)),
	struct-name (symbol (.toLowerCase (str clazz "-state"))),
	init-name (symbol (str clazz "-init")),
	basePath (str (.getRealPath *servletContext* "") "/WEB-INF/classes/"),
	path (str (.replace qualifiedClazz \. java.io.File/separatorChar)
		  ".class")]

    ;; Generate the class
    `(let [parms# ~parms,
	   attribs# (:attribs parms#),
	   file# (new java.io.File ~basePath ~path),
	   genclass# (gen-class (str '~name)
				:extends (:extends parms#),
				:methods (if (:methods parms#),
					   (conj ~accessors (:methods parms#))
					   ~accessors),
				:init 'init,
				:state 'attribs)]

       ;; Def attributes struct and init func
       (def ~struct-name
	    (apply #'create-struct (map #'first attribs#)))

       (defn ~init-name []
	 [[] (apply #'struct-map ~struct-name
		    ~(reduce (fn [v k] (conj v k '(ref nil)))
			     [] (map #'first (:attribs parms))))])
       ;; Def accessors (getters/setters)
       ~@(reduce 
	  (fn [defs attrib]
	    (let [Attrib (str (.toUpperCase (.substring (str attrib) 1 2))
			      (.substring (str attrib) 2))
		  getter (symbol (str clazz "-get" Attrib))
		  setter (symbol (str clazz "-set" Attrib))]
	      (conj defs
		    `(defn ~getter [this#]
		       (let [value# @(~attrib (. this# attribs))
			     vb# (. this# (getValueBinding ~(str attrib)))
			     ctx# (. this# (getFacesContext))]
			 (if (not value#)
			   (when vb#
			     (. vb# (getValue ctx#)))
			   value#)))
		    
		    `(defn ~setter [this# value#]
		       (dosync
			(ref-set (~attrib (. this# attribs)) value#))))))
	  '() 
	  (map #'first (:attribs parms)))
	  
       ;; Remove the existing .class if it exists
       (if (.exists file#)
	 (.delete file#))

       ;; Ensure path exists
       (.mkdirs (.getParentFile file#))
       (.createNewFile file#)
       
       ;; Write the class
       (with-open f# (new java.io.FileOutputStream file#)
	 (.write f# (:bytecode genclass#))))))

