(ns clj.facile.ide
  (:use clj.facile)
  (:import (clj.facile.repl History ReplServer)))

(defstruct repl-state
  :history
  :server)

;; REPL component
(gen-and-save-class 
 (. *servletContext* (getRealPath "/WEB-INF/classes/"))
 'clj.facile.ide.Repl
	 
 :extends javax.faces.component.UIComponentBase
 :methods [['getHistory [] History]
	   ['getServer [] ReplServer] 
	   ['setHistory [History] (. Void TYPE)]
	   ['setServer [ReplServer] (. Void TYPE)]]
 
 :init 'init
 :state 'attribs)

(defcomponent
  'clj.facile.ide.Repl
  :attribs {'history History,
	    'server ReplServer})

(defn Repl-getFamily [this]
  "clj.facile.ide.Repl")

(defn Repl-decode
  [this ctx]

  (prn (str (class this) ": entering Repl-decode"))
  (let [id (. this (getClientId ctx))
	parms (.. ctx (getExternalContext) (getRequestParameterMap))
	val (. parms (get (str "submit" id)))]

    (prn "Repl(" id "): Received value: " val)))

(defn Repl-encodeBegin [this ctx]

  (prn (str (class this) ": Repl-encodeBegin"))
  (let [id (. this (getClientId ctx))
	wr (. ctx (getResponseWriter))

	inputStyleClass (or (. this (getAttribute "inputStyleClass")) 
			    "replInput")
	evalStyleClass (or (. this (getAttribute ("evalStyleClass"))
			      "evalButton"))
	clearStyleClass (or (. this (getAttribute "clearStyleClass"))
			    "clearButton")]

    (doto wr
      ;; Write the input box
      (startElement "input", this)
      (writeAttribute "type", "text", nil)
      (writeAttribute "name", id, "id")
      (writeAttribute "value", "", "form")
      (writeAttribute "class", inputStyleClass, "inputStyleClass")
      (endElement "input")

      ;; 'Eval' button
      (startElement "input", this)
      (writeAttribute "type", "submit", nil)
      (writeAttribute "name", (str "submit" id), nil)
      (writeAttribute "value", "eval", nil)
      (endElement "input")

      ;; 'Clear' button
      (startElement "input", this)
      (writeAttribute "type", "submit", nil)
      (writeAttribute "name", (str "submit" id), nil)
      (writeAttribute "value", "clear", nil)
      (endElement "input"))))

;; Register ourself with the Faces application
(do
  (prn (str "Registering class " clj.facile.ide.Repl " with " *facesApp*))
  (. *facesApp* (addComponent "clj.facile.ide.Repl" "clj.facile.ide.Repl")))
