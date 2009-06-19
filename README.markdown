# Facile

Facile is a Clojure library aims to provide a compact, elegant DSL for writing
JSF applications entirely in Clojure. Unless you are further extending JSF,
there is no need to alter `faces-config.xml`.

## Defining Views

A JSF view is defined using the macro `clj.facile/defview` which takes a symbol
followed by a list of widget struct-maps. The `clj.facile.taglib.core` and
`clj.facile.taglib.html` namespaces provide functions for building trees of
the standard JSF widgets (the 'f' and 'h' taglibs in the standard .jsp 
approach). 

In general, the arguments take the order: id, value, [extras]. Where 'extras' 
is usually a component-specific value where required.

Additionally, there are a few combinators to provide the full flexibility in
specifying the component tree:

*   `(defn attributes [widget & keyvals] ...)`

    Merges `keyvals` into `widget`'s attribute map. Duplicate attributes
    specified in `keyvals` take precedence over those already in `widget`.
    
*   `(defn facet [widget facet-name facet] ...)`

    Adds/replaces a facet named `facet-name` to `widget`; `facet` must be
    a `widget-struct`.

*   `(defn header [widget facet] ...)`
    
    Convenience wrapper for `facet` to add/replace a "header" facet.

*   `(defn footer [widget facet] ...)`
    
    Convenience wrapper for `facet` to add/replace a "footer" facet.

*   `(defn children [widget & children] ...)`
    
    Appends `children` (& `widget-struct`) to `widget`'s children.

*   `(defn parameter [widget id name value] ...)`

    Adds a parameter component to the list of `widget`'s children.

These can then be combined with any `widget-struct` with the `->` operator:

      (-> (output-text "name" "John Doe")
      	  (attributes :styleClass "nameStyle"
	  	      :escape "false"))

### URL routing

Currently URLs must be routed through clj.facile.FacileServlet. Then, the 
`pathInfo` of the request is used to form a qualified symbol which is
resolved to a clojure.lang.Var. The view handler requires this Var to refer
to a `widget-struct` map like one created w/ the `defview` macro.

The `widget-struct` map is passed into `clj.facile/build-view` which walks
over the tree and constructs the UIViewRoot object for use in the remainder
of the JSF lifecycle.

## Application state

### Value bindings

Value bindings are automatically generated for any Var that is given as a 
value for a component attribute. Further, if the Var refers to a Ref then
the JSF framework can update the value of the Ref in the UPDATE_MODEL_VALUES
phase of the request processing lifecycle.

### Method bindings

Similarly functions may be specified for any component attribute 
which expects a MethodBinding. Both inline functions (i.e. `(fn [...] ...)`)
and bound Vars are accepted:

    (command "hello-anon" "Say hello" (fn [] (println "Hello, world!")))
    
or

    (defn hello-world [] (println "Hello, world!"))
    ...
    (command "hello" "Say hello" #'hello-world)

### Session bindings

Session variables can be created through the `defsession` macro:

    (defsession name (ref "Enter your name"))

and then can be used anywhere a value binding is expected:

    (input-text "name" #'name)

Evaluating the Var `name` fetches the corresponding attribute from the
session.

## Putting it all together

The following example demonstrates some of the concepts introduced above:

    (ns example
      (:use clj.facile
	    clj.facile.taglib.core))

    (defsession input-session (ref "Type some text"))
    (def input-val (ref "Type some text"))
    (def select-boolean-val (ref nil))
    (def select-one-val (ref nil))
    (def select-many-val (ref []))
    
    (defview core
        (output "output-tst" "An example output control.")
        (form "example-form"
	      (input "name" #'input-session)
	      (select-boolean "select-boolean" #'select-boolean-val)
	      (select-one "example-select-one"
              		  #'select-one-val
                    	  (select-items {"Item 0" "Value 0"
                          		 "Item 1" "Value 1"
					 "Item 2" "Value 2"
                                   	 "..." "..."}))
              (select-many "example-select-many"
              		   #'select-many-val
                     	   (select-items {"Item 0" "Value 0"
                                    	  "Item 1" "Value 1"
                                    	  "Item 2" "Value 2"
                                    	  "..." "..."}))
	      (command "example-cmd-button"
                       "Command button"
                       (fn []
                       	   (println "State:"
                            	    "\n\tinput:\t" @input-session
                            	    "\n\tselect-boolean:\t" @select-boolean-val
                            	    "\n\tselect-one:\t" @select-one-val
                            	    "\n\tselect-many:\t" @select-many-val
                            	    "\n")))))

We start by declaring a namespace 
