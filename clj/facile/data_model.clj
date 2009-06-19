(ns clj.facile.data_model
  (:gen-class
   :extends javax.faces.model.DataModel,
   :init init,
   :state state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Seq Data Model - for use with UIData components
;;
;; Implements the javax.faces.model.DataModel over the Clojure seq abstraction.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -init []
  [ [] (atom {:data nil, :index 0}) ])

;; Mutator methods
(defn -setWrappedData 
  [this data]

  (swap! (.state this) 
	 assoc :data data :index 0))

(defn -getWrappedData 
  [this]

  (:data @(.state this)))


(defn -setRowIndex
  [this index]

  (swap! (.state this) assoc :index index)

  ;; I think we're supposed to emit a DataModelEvent here(?)
  (if-let [data (:data @(.state this))]
    nil))

(defn -getRowIndex
  [this]

  (:index @(.state this)))


;; Data access
(defn -getRowData
  [this]

  (let [data (:data @(.state this)),
	row-index (:index @(.state this))]
    
    (if data
      (if (not (<= 0 row-index (dec (count data))))
	;; No data at this index
	(throw (new IllegalArgumentException 
		    (str "No data at index: " row-index)))

	;; Return the data
	(nth data row-index))

      ;; No data, so we return nil
      nil)))

(defn -isRowAvailable
  [this]

  (let [data (:data @(.state this)),
	row-index (:index @(.state this))]
    
    (if data
      (if (<= 0 row-index (dec (count data)))
	true
	false)
      false)))

(defn -getRowCount
  [this]

  (if-let [data (:data @(.state this))]
    (count (:data @(.state this)))
    -1))

(defn row-data [data]
  (let [#^javax.faces.model.DataModel 
	seq-model (new clj.facile.data_model)]
    (.setWrappedData seq-model (seq data))
    seq-model))
