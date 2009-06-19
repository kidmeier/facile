(ns example
  (:use clj.facile
	clj.facile.el
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

(comment
	(data "employees" 
	      {:var "emp" 
	       :value employees,
	       :rows 10, ;(session employee:rows-per-page),
	       :first 0} ;(session employee:first-row)}
	      (-> (column "first-name")
		  (header (output "fst-name-hdr" "First Name"))
		  (footer (output "fst-name-ftr" "First Name"))
		  (children
		   (output "fst-name" (dot 'emp :first-name))))
	      (-> (column "last-name")
		  (header (output "lst-name-hdr" "Last Name"))
		  (footer (output "lst-name-ftr" "Last name"))
		  (children
		   (output "lst-name" (dot 'emp :last-name))))))
