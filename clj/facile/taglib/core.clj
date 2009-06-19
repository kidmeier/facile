(ns clj.facile.taglib.core
  (:use clj.facile)
  (import (javax.faces.model SelectItem)))

;; Input components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn input 
  ([id value]
     (input "javax.faces.Input" id value))

  ([type id value]
     (widget
       type,
       {:id id,
	:value value}))

  ([type id value attrs]
     (widget type
	     (merge attrs {:id id,
			   :value value}))))

(defn select-boolean
  ([id checked]
     (select-boolean "javax.faces.SelectBoolean" id checked))

  ([type id checked]
     (widget
      type
      {:id id,
       :value checked})))

(defn select-one
  ([id value items]
     (select-one "javax.faces.SelectOne" id value items))

  ([type id value items]
     (widget
      type
      {:id id,
       :value value},
      (if (vector? items) items [items]))))

(defn select-many
  ([id value items]
      (select-many "javax.faces.SelectMany" id value items))

  ([type id value items]
     (widget
      type
      {:id id,
       :value value}
      (if (vector? items) items [items]))))

(defn select-item
  ([label]
     (select-item label label))

  ([label value]
    (select-item label value ""))

  ([label value description]
     (select-item label value description false))

  ([label value description disabled]
     (select-item "javax.faces.SelectItem" label value description disabled))
  
  ([type label value description disabled]
     (widget
      type,
      {:itemLabel label,
       :itemValue value,
       :itemDescription description,
       :itemDisabled disabled})))

(defn- select-items-
  [items]
  (widget "javax.faces.SelectItems" {:value items}))

(defmulti select-items type)
(defmethod select-items java.util.List 
  [l] 
  
  (select-items- (into []
		       (map (fn [i]
			      (if (instance? javax.faces.model.SelectItem i)
				i
				(let [label (if (instance? clojure.lang.Named 
							   i) 
					      (name i) 
					      i)]
				  (SelectItem. i label))))
			    l))))

(defmethod select-items clojure.lang.Associative
  [m]

  (select-items- (into [] 
		       (map (fn [[k v]] 
			      (SelectItem. v 
					   (if (instance? clojure.lang.Named k)
					     (name k) 
					     k))) 
			    m))))

(prefer-method select-items java.util.List clojure.lang.Associative)

;; Output components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn output
  ([id value]
     (output id value nil))

  ([id value converter]
     (output "javax.faces.Output" id value converter))

  ([type id value converter]
     (widget
      type,
      {:id id,
       :value value},
      converter,
      [], ; Validators
      [], ; Children
      {}))) ; Facets      
		  
(defn message
  ([id for detail? summary?]
     (message "javax.faces.Message" id for detail? summary?))

  ([type id for detail? summary?]
     (widget
      type,
      {:id id,
       :for for,
       :showDetail detail?,
       :showSummary summary?})))

(defn messages
  ([id only-global? detail? summary?]
     (messages "javax.faces.Messages" id only-global? detail? summary?))

  ([type id only-global? detail? summary?]
     (widget
      type,
      {:id id,
       :globalOnly only-global?,
       :showDetail detail?,
       :showSummary summary?})))

(defn graphic
  ([id path]
     (graphic "javax.faces.Graphic" id path))

  ([type id path]
     (widget type {:id id, :value path}))

  ([type id path attrs]
     (widget type (merge attrs {:id id, :value path}))))

;; Naming containers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn form
  [id & children] 

  (widget "javax.faces.Form",
	       {:id id},
	       (into [] children)))

(defn data
  ([id value]
     (data id {:value value} nil [] nil))

  ([id attrs columns]
     (data id attrs nil columns nil))

  ([id attrs header columns footer]
     (data "javax.faces.Data" id attrs header columns footer))

  ([type id attrs header columns footer]
     (widget type
	     (merge attrs {:id id}),
	     columns,
	     (merge (if header {"header" header} {})
		    (if footer {"footer" footer} {})))))

(defn subview
  [id & children]

  (widget "javax.faces.NamingContainer"
	  {:id id},
	  (into [] children)))

;; Layout containers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn column
  ([id & children]
     (widget "javax.faces.Column" {:id id} children)))

(defn panel
  [type id attrs]
  (widget type (merge attrs {:id id})))

;; Command component ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn command
  ([id action]
     (command id action false))

  ([id value action]
     (command id value false action))
  
  ([id value immediate? action]
     (command "javax.faces.Command" id value immediate? action))

  ([type id value immediate? action]
     (widget
      type,
      {:id id,
       :value value,
       :action action,
       :immediate immediate?})))

;; Combinators ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn facet
  [w facet value]

  (assoc w :facets
	 (assoc (:facets w)
	   facet
	   value)))

(defn header
  [w header]

  (facet w "header" header))

(defn footer
  [w footer]

  (facet w "footer" footer))

(defn attributes
  [w & keyvals]

  (assoc w :attributes
	 (merge (:attributes w)
		(apply hash-map keyvals))))

(defn children
  [w & children]
  
  (assoc w :children 
	 (into (:children w) children)))

;; Parameter ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn param
  ([id name value]
     (widget "javax.faces.Parameter" 
	     {:id id, :name name, :value value}))

  ([w id name value]

     (assoc w :children
	    (conj (:children w)
		  (param id name value)))))

;; Converters ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn new-simple-converter
  [converter-id]

  (.createConverter *faces-app* converter-id))

(defn simple-converter
  [w converter-id]

  (assoc w :converter (new-simple-converter converter-id)))

(defn convert-big-decimal [w]
  (simple-converter w "javax.faces.BigDecimal"))

(defn convert-big-integer [w]
  (simple-converter w "javax.faces.BigInteger"))

(defn convert-boolean [w]
  (simple-converter w "javax.faces.Boolean"))

(defn convert-byte [w]
  (simple-converter w "javax.faces.Byte"))

(defn convert-character [w]
  (simple-converter w "javax.faces.Character"))

(defn convert-double [w]
  (simple-converter w "javax.faces.Double"))

(defn convert-float [w]
  (simple-converter w "javax.faces.Float"))

(defn convert-integer [w]
  (simple-converter w "javax.faces.Integer"))

(defn convert-long [w]
  (simple-converter w "javax.faces.Long"))

(defn convert-short [w]
  (simple-converter w "javax.faces.Short"))

(defn convert-datetime
  [w & args]

  (let [{:keys [date-style, locale, pattern, time-style, timezone, type]
	 :or {date-style "default",
	      locale (.getLocale *view-root*),
	      pattern "yy-MM-dd hh:mm.ss aa",
	      time-style "default",
	      timezone (java.util.TimeZone/getDefault),
	      type "both"}} (apply hash-map args),
	      converter (.createConverter *faces-app* "javax.faces.DateTime")]
    
    (doto converter
      (.setDateStyle date-style)
      (.setLocale locale)
      (.setPattern pattern)
      (.setTimeStyle time-style)
      (.setTimeZone timezone)
      (.setType type))

    (assoc w :converter converter)))
	   

(defn convert-number
  [w & args]

  (let [{:keys [currency-code, currency-symbol, locale, 
		max-fraction-digits, max-integer-digits, 
		min-fraction-digits, min-integer-digits,
		pattern, type, grouped?, only-integer?]
	 :or {currency-code nil,
	      currency-symbol nil,
	      locale (.getLocale *view-root*),
	      max-fraction-digits nil,
	      max-integer-digits nil,
	      min-fraction-digits nil,
	      min-integer-digits nil,
	      pattern nil,
	      type "number",
	      grouped? false,
	      only-integer? false}} (apply hash-map args),
	      cnvtr (.createConverter *faces-app* "javax.faces.Number")]
    
    (when currency-code (.setCurrencyCode cnvtr currency-code))
    (when currency-symbol (.setCurrencySymbol cnvtr currency-symbol))
    (.setLocale cnvtr locale)
    (when max-fraction-digits 
      (.setMaxFractionDigits cnvtr max-fraction-digits))
    (when max-integer-digits 
      (.setMaxIntegerDigits cnvtr max-integer-digits))
    (when min-fraction-digits 
      (.setMinFractionDigits cnvtr min-fraction-digits))
    (when min-integer-digits 
      (.setMinIntegerDigits cnvtr min-integer-digits))
    (when pattern (.setPattern cnvtr pattern))
    (when type (.setType cnvtr type))
    (when grouped? (.setGroupingUsed grouped?))
    (when only-integer? (.setIntegerOnly only-integer?))

    (assoc w :converter cnvtr)))

(defn convert-currency [w currency-code]
  (convert-number w :currency-code currency-code))

;; Validators ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn new-range-validator
  [validator-id min max]

  (let [v (.createValidator *faces-app* validator-id)]
    (when min (.setMinimum v min))
    (when max (.setMaximum v max))

    v))

(defn range-validator
  [w validator-id min max]

  (let [validators (:validators w)]
    (assoc w
      :validators 
      (conj validators (new-range-validator validator-id min max)))))

(defn validate-double-range
  [w min max]
  
  (range-validator w "javax.faces.DoubleRange" min max))

(defn validate-length
  [w min max]
  
  (range-validator w "javax.faces.Length" min max))

(defn validate-long-range
  [w min max]

  (range-validator w "javax.faces.LongRange" min max))
