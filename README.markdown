# Facile

Facile is a Clojure library aims to provide a compact, elegant DSL for writing
JSF applications entirely in Clojure. Unless you are further extending JSF,
there is no need to maintain a `faces-config.xml` file.

## Defining Views

A JSF view is defined using the macro `clj.facile/defview` which takes a symbol
followed by a list of `widget-tree` struct-maps. The `clj.facile.taglib.core` 
and `clj.facile.taglib.html` namespaces provide functions for building 
component trees using the standard JSF widgets.

In general, the arguments take the form: 

   `[id value & extras]`

Where 
      `id` specifies the JSF component id
      `value` specifies the value of the "value" attribute
      `extras` represents component-specific arguments

For example:

    (output-text "foo" "Bar")

constructs a `javax.faces.HtmlOutputText` component with id "foo" and value 
"Bar".

Additionally, where more control is needed, several combinators expose the full
range of JSF component customization:

*   `(attributes widget & keyvals)`

    Merges `keyvals` into `widget`'s attribute map. Duplicate attributes
    specified in `keyvals` take precedence over those already in `widget`.
    
*   `(facet widget facet-name facet)`

    Adds/replaces a facet named `facet-name` to `widget`; `facet` must be
    a `widget-tree`.

*   `(children widget & children)`
    
    Appends `children` (& `widget-tree`) to `widget`'s children.

*   `(parameter widget id name value)`

    Adds a parameter component to the list of `widget`'s children.

*   `(convert-* widget & args)`

    Attaches a converter configured with `args` to `widget`. There are many
    standard converters available.
    
    See `clj/facile/taglib/core.clj` for details.

*   `(validators-* widget & args)`

    Adds a validator configured with `args` to the `widget`.
    See `clj/facile/taglib/core.clj` for details on standard validators.

These can then be combined with any `widget-tree` with the `->` operator:

      (-> (output-text "start-date" (java.util.Date. 0))
      	  (attributes :styleClass "date"
	  	      :escape "false")
	  (convert-datetime :pattern "yyyy-MM-dd"))

### URL routing

Currently URLs must be routed through `clj.facile.FacileServlet`. The 
`pathInfo` of the request is then used to form a qualified symbol which is
resolved to var defined by `defview`.

For example, the view:

    (ns foo (:use clj.facile))
    (defview bar ...)

Is accessed through URL:

   http://host/contextRoot/facileServlet/foo/bar

In JSF terminology, the view id is taken to be "foo/bar". After resolving the 
view, the `widget-tree` map is passed into `clj.facile/build-view` which walks
over the tree and constructs the UIViewRoot object for use in the remainder
of the JSF lifecycle.

## Application state

### Value bindings

Value bindings are automatically generated for any Var that is given as a 
value for a component attribute. Further, if the Var refers to a Ref then
the JSF framework can update the value of the Ref in the UPDATE_MODEL_VALUES
phase of the request processing lifecycle.

### Method bindings

Similarly, functions may be specified for any component attribute which expects
a MethodBinding. Both anonymous functions and named are accepted:

    (defview hello
        (command "hello-anon" "Say hello" (fn [] (println "Hello, world!"))))
    
or

    (defn hello-world [] (println "Hello, world!"))
    (defview hello
    	(command "hello" "Say hello" #'hello-world))

### Session bindings

Session variables can be created with the `defsession` macro:

    (defsession name (ref "Enter your name"))

and can be used anywhere a value binding is expected:

    (input-text "name" #'name)

Evaluating `@name` fetches the corresponding attribute from the session. This
is a little different from refs in that the level of indirection is two. In the
example above, `@name` would evaluate to the ref itself (e.g. 
`#<Ref@757d757d: "Enter your name">`), and `@@name` would evaluate to the 
value of the ref (i.e. `"Enter your name"`).

## Putting it all together

The following example demonstrates some of the concepts introduced above:

    (ns example
      (:use clj.facile
	    clj.facile.taglib.core))

    (defsession input-session (ref "Type some text"))
    (def select-boolean-val (ref nil))
    (def select-one-val (ref nil))
    (def select-many-val (ref []))
    
    (defview example-form
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
                                  "\n\tinput:\t" @@input-session
                            	  "\n\tselect-boolean:\t" @select-boolean-val
                            	  "\n\tselect-one:\t" @select-one-val
                            	  "\n\tselect-many:\t" @select-many-val
                            	  "\n")))))


