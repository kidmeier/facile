(ns clj.facile
  (:import 
   (javax.faces.component UIComponentBase)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; J2EE env
(def #^javax.servlet.ServletContext *servlet-context* nil)
(def #^javax.servlet.http.HttpSession *http-session* nil)

;; Faces application context
(def #^javax.faces.context.FacesContext *faces-context* nil)
(def #^javax.faces.application.Application *faces-app* nil)
(def #^javax.faces.component.UIViewRoot *view-root* nil)
(def *view-id* nil)

;; Scoped data
(def #^java.util.Map *session-map* nil)
(def #^java.util.Map *request-map* nil)
(def #^java.util.Map *cookies* nil)
(def #^java.util.Map *headers* nil)
(def #^java.util.Map *headers-multi* nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Taglib
(defmacro deftaglib 
  "Define a tag library and bind it to the specified symbol. A taglib
   is just a map whichmaps keywords onto JSF component names, which are
   instantiated through the JSF application component factory:

     Application.createComponent(java.lang.String componentType)"
    
  [sym tags]
  `(def ~sym (hash-map ~@tags)))

(defmacro compile-class
  [name]

  (let [qualifiedName (str name),
	basePath (apply 
		  str (.getRealPath *servlet-context* "") 
		  (interpose java.io.File/separator ["WEB-INF" "classes"])),
	path (str 
	      (.replace qualifiedName \. java.io.File/separatorChar) ".class"),
	file (new java.io.File (str basePath path))]
    
    ;; Ensure target directory exists
    (.mkdirs (.getParentFile file))
  
    `(binding [*compile-path* ~basePath]
       (compile ~name))))

;; Forward decls
(declare build-view,
	 build-widget-tree)

(defmulti set-widget-attribute (fn [w a v] (class v)))

(defmethod set-widget-attribute :default
  [#^UIComponentBase widget
   attr
   val]

  (.put (.getAttributes widget) attr val))

(defstruct widget-struct
  :type,
  :attributes,
  :converter,
  :validators,
  :children,
  :facets)

(defn widget
  ([type attributes]
     (widget type attributes nil, [], [], {}))

  ([type attributes children]
     (widget type attributes nil [] children, {}))

  ([type attributes children facets]
     (widget type attributes nil [] children facets))

  ([type attributes converter validators children facets]
     (struct widget-struct
	     type,
	     attributes,
	     converter,
	     validators,
	     children,
	     facets)))

(defn build-widget-tree
  "Build the 'widget' specified by the tag in the given 'taglib' with the 
   specified attributes. 'attributes' can be a map or a single value in which
   case it is assumed to represent the component's 'value' attribute."
  [tree]

  (let [{:keys [type, attributes, converter, validators, 
		children, facets]} tree,
	#^UIComponentBase widget (.createComponent *faces-app* type)]

    ;; If no id was specified, create one
    (when-not (:id attributes)
      (set-widget-attribute widget "id" (.createUniqueId *view-root*)))

    ;; Apply widget attributes (facets are handled separately)
    (doseq [[attr val] attributes]
      (set-widget-attribute widget (name attr) val))
    
    ;; Apply facets if any. Facets are essentially view fragements. We
    ;; build a sub-tree for each facet just as we build the view itself,
    ;; then we add the facet sub-tree to the widget.
    (doseq [[facet-name facet-tree] facets]

      ;; First build the widget tree specified for the facet
      (let [facet-widget (build-widget-tree facet-tree)]
	;; Add facet to the widget
	(.put (.getFacets widget) facet-name (first facet-widget))))

    ;; Add validators
    (if (not (empty? validators))
      (do
	(when (not (instance? javax.faces.component.EditableValueHolder widget))
	  (throw (IllegalArgumentException. 
		  (str "Validators can only be attached to components implementing "
		       "javax.faces.component.EditableValueHolder: "
		       (class widget) 
		       " does not implement javax.faces.component.EditableValueHolder"))))
	
	(doseq [validator validators]
	  (.addValidator widget validator))))

    ;; Set the converter
    (if converter
      (do
	(when (not (instance? javax.faces.component.ValueHolder widget))
	  (throw (IllegalArgumentException.
		  (str "You can only attach converters to components implementing "
		       "javax.faces.component.ValueHolder: "
		       (class widget) 
		       " does not implement javax.faces.component.ValueHolder"))))
	
	(.setConverter widget converter)))

    ;; Add children
    (doseq [child-tree children]
      (let [child-widget (build-widget-tree child-tree)]
	(.add (.getChildren widget) child-widget)))

    ;; Return the constructed widget to the caller
    widget))

(defn build-view 
  "Iterate over the body and build the JSF view using 'root' as the view root."
  [view,
   #^UIComponentBase root]

  (doseq [component-tree (:template view)]
    (let [component (build-widget-tree component-tree)]
      (.add (.getChildren root) component))))

(defstruct view-struct
  :name,
  :locals,
  :template)

(defmacro defview 
  "Define a new JSF view, which is simply a function that, when called, 
   returns a JSF UIViewRoot. The macro calls 'build-view' with the body
   iterates over the view template and builds the view as it goes along.

   'taglibs' is a mapping from keywords to taglibs as specified with the 
   deftaglib macro, providing a mapping from keywords into JSF component 
   names. This map is merged with the default taglib map:

    {:f jsf-core, :h jsf-html}

   where jsf-core and jsf-html are taglibs specifying the standard Core and 
   HTML JSF components as specified by the JSF specification. 

   The view template is a Clojure vector of triples, or a sub-vector which
   specifies that any component listed within are added as children to the
   parent component. Each triple consists of the taglib prefix, the component
   name, followed by a map of attributes, e.g.:

    [:h :form {:id \"name-form\"}
      [:h :outputText \"Name: \"]
      [:h :inputText (binding #'name)]
      [:h :commandButton {:value \"Submit\" :action (action #'process-form)}]]
 
    Specifies a JSF form with id=\"name-form\" and three children:
     - an HtmlOutputText component with the value \"Name: \"
     - an HtmlInputText component whose value is bound to the symbol 'name'
     - an HtmlCommandButton component with value=\"Submit\" that invokes the 
       function named 'process-form' when clicked."
   
  [name & body]

  `(binding [*view-id* ~(str (ns-name *ns*) "/" name)]
     (def ~name (struct-map view-struct
		  :id ~*view-id*,
		  :name (str (quote ~name)),
		  :locals (view-locals *view-id*),
		  :template (vector ~@body),))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation

(defn goto
  "Generates a return value for the navigation handler to go to
   the specified view. Strings are passed through untouched, vars 
   are transformed into a string representing its view ID, nil is 
   passed through untouched (it means reload current view) anything
   else generates an IllegalArgumentException."

  [dest]

  (cond
   (var? dest)
    (let [ns (.name (.ns dest)),
	  sym (.getName (.sym dest))]
      (str ns "/" sym)),

   (string? dest)
    dest,

   (nil? dest)
    nil,

   :else
    (throw (new IllegalArgumentException 
		(str "(clj.facile/goto): Can't  use type " 
		     (class dest) 
		     " in a navigation rule.")))))
    
(defn reload
  "Convenience wrapper for (goto nil)."
  []

  (goto nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scoped data access/storage

(defmacro defsession 
  [sym value]

  `(let [key# (str (ns-name *ns*) "/" '~sym)]
     (def ~sym 
	  (proxy [clojure.lang.IDeref] []
	    (deref []
		   (let [current# (.getAttribute *http-session* key#)]
		     (or current#
			 (do
			   (.setAttribute *http-session* key# ~value)
			   (.getAttribute *http-session* key#)))))))))

(defn view-locals
  [view-id]

  (let [[ns sym] (.split view-id "/")]
    (clj.facile.FacileViewHandler/viewLocals ns sym)))

(defn parm
  [name]
  
  (let [faces-ctx (javax.faces.context.FacesContext/getCurrentInstance),
	ext-ctx (.getExternalContext faces-ctx),
	map (.getRequestParameterMap ext-ctx)]

    (.get map name)))
