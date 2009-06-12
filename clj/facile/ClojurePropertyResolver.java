package clj.facile;

import java.util.logging.Logger;

import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.PropertyResolver;

import clojure.lang.AFn;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.LockingTransaction;
import clojure.lang.Namespace;
import clojure.lang.Ref;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class ClojurePropertyResolver extends PropertyResolver {

	static final Logger log = Logger.getLogger(ClojurePropertyResolver.class.getName());
	//
	private PropertyResolver parent;

	public ClojurePropertyResolver(PropertyResolver parent) {
		this.parent = parent;
	}

	private Var lookupVar(Namespace ns, String sym) throws EvaluationException,
			PropertyNotFoundException {
		
		log.entering(this.getClass().getName(), "lookupVar", new Object[] { ns, sym });
	
		try {
			Var var = ns.findInternedVar(Symbol.intern( ClojureVariableResolver.unmangleIdent(sym) ));
			if (null == var)
				throw new PropertyNotFoundException("Cannot find symbol " + sym
						+ " in namespace " + ns.getName());

			log.exiting(this.getClass().getName(), "lookupVar", var);
			return var;
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		
	}

	private Object deref(Object o) throws EvaluationException {

		log.entering(this.getClass().getName(), "deref", new Object[] {o});
		
		if( o instanceof Var ) {
			o = ((Var)o).get();
			log.finest("o.get()=" + o);
		}
		
		try {
			// Automatically dereference references
			if (o instanceof Ref) {
				Ref ref = (Ref) o;

				log.finer("dereferencing ref=" + ref);
				
				o = ref.deref();
				log.exiting(this.getClass().getName(), "deref", o);
				return o;
			}

			log.exiting(this.getClass().getName(), "deref", o);
			return o;

		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	private Object parseKey(Object key) {

		log.entering(this.getClass().getName(), "parseKey", key);
		
		log.finest("key.getClass()=" + key.getClass());
		if( key instanceof String ) {
			
			String s = (String)key;
			
			// Keyword
			if( s.startsWith(":") ) {
				return Keyword.intern( Symbol.intern(null,s.substring(1)) );
			
			// String
			} else if( s.startsWith("\"") ) {
				return s.substring(1, s.length()-1);
			}
			
		// JSF EL likes to parse integers into Longs, but Clojure parses the same into Integer
		} else if( key instanceof Long ) {

			Long l = (Long)key;
			return Integer.valueOf(l.intValue());
			
		}
		
		// Pass through
		return key;
	}
	
	@Override
	public Class<?> getType(Object obj, int index) throws EvaluationException,
			PropertyNotFoundException {

		return parent.getType(obj, index);
	}

	@Override
	public Class<?> getType(Object obj, Object key) throws EvaluationException,
			PropertyNotFoundException {

		Class c = null;
		
		log.entering(this.getClass().getName(), "getType");

		if (obj instanceof Namespace)
			c = getValue(obj, key).getClass();
		else if( obj instanceof IPersistentMap ) {

			IPersistentMap m = (IPersistentMap)obj;
			c = m.valAt(parseKey(key)).getClass();

		}
		else
			c = parent.getType(obj, key);
		
		log.finest("class=" + c);
		log.exiting(this.getClass().getName(), "getType");

		return c;
	}

	@Override
	public Object getValue(Object obj, int index) throws EvaluationException,
			PropertyNotFoundException {

		// TODO: Support indexable Clojure types: lists, vectors, (maps?)
		return parent.getValue(obj, index);
	}

	@Override
	public Object getValue(Object obj, Object key) throws EvaluationException,
			PropertyNotFoundException {

		Object value = null;
		obj = deref(obj);
		
		log.entering(this.getClass().getName(), "getValue", new Object[] { obj, key });

		if( obj instanceof Namespace ) {
			
			final Namespace ns = (Namespace) obj;
			final String sym = (String) key;

			value = deref(lookupVar(ns, sym));

		} else if( obj instanceof IPersistentMap ) {
			
			IPersistentMap m = (IPersistentMap)obj;
			value = deref(m.valAt(parseKey(key)));
			
		} else {
			
			log.finest("Delegating to parent property resolver");
			value = deref(parent.getValue(obj, key));
			
		}
		
		log.exiting(this.getClass().getName(), "getValue", value);
		return value;
	}

	@Override
	public boolean isReadOnly(Object obj, int index)
			throws EvaluationException, PropertyNotFoundException {

		return parent.isReadOnly(obj, index);
	}

	@Override
	public boolean isReadOnly(Object obj, Object key)
			throws EvaluationException, PropertyNotFoundException {

		if (!(obj instanceof Namespace))
			return parent.isReadOnly(obj, key);

		final Namespace ns = (Namespace) obj;
		final String sym = (String) key;

		log.entering(this.getClass().getName(), "isReadOnly", new Object[] { obj, key });
		
		try {
			// Only refs can be altered
			Var var = lookupVar(ns, sym);
			boolean readOnly = (var.get() instanceof Ref);
			
			log.exiting(this.getClass().getName(), "isReadOnly", readOnly);
			return readOnly;
			
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}

	@Override
	public void setValue(Object obj, int index, Object val)
			throws EvaluationException, PropertyNotFoundException {

		parent.setValue(obj, index, val);
	}

	@SuppressWarnings("serial")
	@Override
	public void setValue(Object obj, Object key, Object val)
			throws EvaluationException, PropertyNotFoundException {

		if (!(obj instanceof Namespace)) {
			parent.setValue(obj, key, val);
			return;
		}

		final Namespace ns = (Namespace) obj;
		final String sym = (String) key;

		log.entering(this.getClass().getName(), "setValue");
		log.finest("ns=" + ns + ", symb=" + key + ", val=" + val);
		
		Var var = lookupVar(ns, sym);
		try {
			// It is only legal to set refs
			Object o = var.get();
			if (!(o instanceof Ref))
				throw new IllegalArgumentException(
						"Values are immutable. Can only set a Ref");

			// Set the value in a transaction
			final Ref ref = (Ref) o;
			final Object value = val;
			final IFn setValue = new AFn() {
				public Object invoke() {
					ref.set(value);
					return null;
				}
			};
			
			LockingTransaction.runInTransaction(setValue);
			log.exiting(this.getClass().getName(), "setValue");
			
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}
}
