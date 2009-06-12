(ns clj.facile.html)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HTML templating

(defn- html-element
  [elm]  
  (let [tag (.substring (str (first elm)) 1),
	attrs (if (map? (second elm)) (second elm)),
	content (if attrs (rrest elm) (rest elm)),
	open-tag (str "<" tag (reduce (fn [s [attr val]] (str s " " (.substring (str attr) 1) "=\"" val "\"")) "" attrs) ">"),
	body (reduce (fn [s child]
		       (if (vector? child)
			 (str s (html-element child))
			 (if (keyword? child)
			   (str s "<" (.substring (str child) 1) "/>")
			   ;; Else a string
			   (str s child))))
		     "" content),
	close-tag (str "</" tag ">")]
    (str open-tag body close-tag)))

(defn html
  "Emits a string of HTML from the tree of elements/attributes passed in"
  [& body]
  
  (reduce (fn [s elm] (str s (html-element elm))) "" body))

