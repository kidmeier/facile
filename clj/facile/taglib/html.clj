(ns clj.facile.taglib.html
  (:use clj.facile)
  (:require [clj.facile.taglib.core :as core]))

(
;; Form ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn form
  [id & children]
  (widget "javax.faces.HtmlForm"
	  {:id id} 
	  (into [] children)))

;; Text input ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- input-html
  [type id value attrs]

  (core/input type id value attrs))

(defn input-hidden
  ([id value]
     (input-hidden id value {}))
  ([id value attrs]
     (input-html "javax.faces.HtmlInputHidden" id value attrs)))

(defn input-secret
  ([id value]
     (input-secret id value {}))
  ([id value attrs]
     (input-html "javax.faces.HtmlInputSecret" id value attrs)))

(defn input-text
  ([id value]
     (input-text id value {}))
  ([id value attrs]
     (input-html "javax.faces.HtmlInputText" id value attrs)))

(defn input-textarea
  ([id value]
     (input-textarea id value {}))
  ([id value attrs]
     (input-html "javax.faces.HtmlInputTextarea" id value attrs)))

;; Messages ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn message
  [id for detail? summary?]
  (core/message "javax.faces.HtmlMessage" id for detail? summary?))

(defn messages
  [id only-global? detail? summary?]
  (core/messages "javax.faces.HtmlMessages" id only-global? detail? summary?))

;; Text output ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn output-format
  ([id value]
     (output-format id value nil []))

  ([id value converter]
     (core/output "javax.faces.HtmlOutputFormat" id value converter)))
 
(defn label
  [id for value] 
  (widget "javax.faces.HtmlOutputLabel"
	  {:id id,
	   :value value
	   :for for}
	  nil ; Converter
	  [] ; Children
	  [] ; Validators
	  ))

(defn link 
  ([id value]
     (link id value nil)) 

  ([id value converter]
     (core/output "javax.faces.HtmlOutputLink" id value converter)))


(defn output-text
  ([id value]
     (output-text id value nil))
  ([id value converter]
     (core/output "javax.faces.HtmlOutputText" id value converter)))

;; Image ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn graphic-image 

  ([id url]
     (graphic-image id url nil))

  ([id url alt]
     (core/graphic "javax.faces.HtmlGraphicImage" id url 
		   {:alt alt}))

  ([id url width height]
     (graphic-image id url "" width height))

  ([id url alt width height]
     (core/graphic "javax.faces.HtmlGraphicImage" id url
		   {:alt alt :width width, :height height})))

;; Data table ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn data-table 
  [id value] 

  (core/data "javax.faces.HtmlDataTable" id {:value value} nil [] nil))

;; Layout ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn panel-grid
  [id columns] 
  (core/panel "javax.faces.HtmlPanelGrid" id {:columns columns}))

(defn panel-group
  [id] 
  (core/panel "javax.faces.HtmlPanelGroup" id {}))

;; Item selection ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn select-boolean-checkbox
  [id checked]
  (core/select-boolean "javax.faces.HtmlSelectBooleanCheckbox" id checked))

(defn select-many-checkbox
  [id value items] 
 
  (core/select-many "javax.faces.HtmlSelectManyCheckbox" id value items))

(defn select-many-listbox
  [id value items]

  (core/select-many "javax.faces.HtmlSelectManyListbox" id value items))

(defn select-many-menu
  [id value items] 
  (core/select-many "javax.faces.HtmlSelectManyMenu" id value items))

(defn select-one-listbox 
  [id value items] 

  (core/select-one "javax.faces.HtmlSelectOneListbox" id value items))

(defn select-one-menu
  [id value items] 

  (core/select-one "javax.faces.HtmlSelectOneMenu" id value items))

(defn select-one-radio 
  [id value items] 

  (core/select-one "javax.faces.HtmlSelectOneRadio" id values items)))

;; Commands ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn command-button

  ([id label action] 
     (command-button id label false action))

  ([id label immediate? action]
     (core/command "javax.faces.HtmlCommandButton" id label immediate? action)))

(defn command-link 
  ([id action]
     (command-link id "" false action))

  ([id link-text action]
     (command-link id link-text false action))

  ([id link-text immediate? action]
     (core/command "javax.faces.HtmlCommandLink" 
		   id link-text immediate? action)))
