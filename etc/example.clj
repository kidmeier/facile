(ns example
  (:use clj.facile
	clj.facile.el
	clj.facile.taglib.core))

(defview controls
  (output "output-tst" "An example output control.")
  (form "example-form" 
	(input "name" "Input control")
	(select-boolean "select-boolean" false)
;	(select-one "example-select-one"
;		    "Value 0"
;		    (select-item "Item 0" "Value 0")
;		    (select-item "Item 1" "Value 1")
;		    (select-item "Item 2" "Value 2")
;		    (select-item "..." "..."))
;	(select-many "example-select-many"
;		     0
;		     (select-item "Item 0" "Value 0")
;		     (select-item "Item 1" "Value 1")
;		     (select-item "Item 2" "Value 2")
;		     (select-item "..." "..."))
	
	(command "example-cmd-button" 
		 "Command button"
		 (fn []
		   (println "You clicked the button!")))))

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