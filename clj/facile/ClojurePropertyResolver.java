package clj.facile;

import java.util.logging.Logger;

import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.PropertyResolver;

import clojure.lang.Associative;
import clojure.lang.IDeref;
import clojure.lang.Keyword;
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

	private Object deref(Object o) throws EvaluationException {

		log.entering(this.getClass().getName(), "deref", new Object[] {o});
		
		try {
			
			if( o instanceof Ref ) { 
				Ref ref = (Ref) o;
				return ref.deref();
			}
			
			return o;
			
		} catch( Exception e ) {
			throw new EvaluationException(
					"Caught exception while dereferencing: " + o, e);
		}
		
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
			
		// JSF EL likes to parse integers into Longs, but Clojure wants an Integer
		} else if( key instanceof Long ) {

			Long l = (Long)key;
			return Integer.valueOf(l.intValue());
			
		}
		
		// Pass through
		return key;
	}
	
	private Object resolve(Object base, Object key) 
		throws EvaluationException, PropertyNotFoundException {

		Object value = null;
	
		try {
			if( base instanceof Namespace ) {
				
				final Namespace ns = (Namespace)base;
				final String sym = (String)key;
	
				value = lookupVar(ns, sym);
	
			} else if( base instanceof Associative ) {
				
				Associative m = (Associative)base;
				value = m.valAt(parseKey(key));
				
			} else {
				
				log.finest("Delegating to parent property resolver");
				value = deref(parent.getValue(base, key));
				
			}
		
			// Unwrap it until we either get a value that is not a Ref
			while( (value instanceof IDeref)
					&& !(value instanceof Ref) ) {
				value = ((IDeref)value).deref(); 
			}
			
		} catch( PropertyNotFoundException e ) {
			
			throw e;
			
		} catch( Exception e ) {
			
			throw new EvaluationException(e);
			
		} 
		
		return value;
	}

	@Override
	public Class<?> getType(Object base, int index) throws EvaluationException,
			PropertyNotFoundException {

		// TODO: Clojure types?
		return parent.getType(base, index);
	}

	@Override
	public Class<?> getType(Object base, Object key) throws EvaluationException,
			PropertyNotFoundException {

		log.entering(this.getClass().getName(), "getType");

		final Class c = deref(resolve(base, key)).getClass();;
		
		log.finest("class=" + c);
		log.exiting(this.getClass().getName(), "getType");

		return c;
	}

	@Override
	public boolean isReadOnly(Object base, int index)
			throws EvaluationException, PropertyNotFoundException {

		// TODO implement for Clojure?
		return parent.isReadOnly(base, index);
	}

	@Override
	public boolean isReadOnly(Object base, Object key)
			throws EvaluationException, PropertyNotFoundException {

		log.entering(this.getClass().getName(), "isReadOnly", new Object[] { base, key });

		final Object value = resolve(base,key);
		final boolean readOnly = !(value instanceof Ref);

		log.exiting(this.getClass().getName(), "isReadOnly", readOnly);
		return readOnly;
	}

	@Override
	public Object getValue(Object base, int index) throws EvaluationException,
			PropertyNotFoundException {

		// TODO: Support indexable Clojure types: lists, vectors, (maps?)
		return parent.getValue(base, index);
	}

	@Override
	public Object getValue(Object base, Object key) throws EvaluationException,
			PropertyNotFoundException {
		
		log.entering(this.getClass().getName(), "getValue", new Object[] { base, key });

		final Object value = deref(resolve(base, key));

		log.exiting(this.getClass().getName(), "getValue", value);
		return value;
	}

	@Override
	public void setValue(Object base, int index, Object val)
			throws EvaluationException, PropertyNotFoundException {

		// TODO: Clojure?
		parent.setValue(base, index, val);
	}

	@SuppressWarnings("serial")
	@Override
	public void setValue(Object base, Object key, Object val)
			throws EvaluationException, PropertyNotFoundException {

		log.entering(this.getClass().getName(), "setValue");
		log.finest("base=" + base + ", key=" + key + ", val=" + val);

		Object target = resolve(base, key);
		try {
			// It is only legal to set refs
			if (!(target instanceof Ref))
				throw new IllegalArgumentException(
						"Values are immutable. Can only set a Ref");

			// Set the value in a transaction
			final Ref ref = (Ref)target;
			ref.set(val);
			
			log.exiting(this.getClass().getName(), "setValue");
			
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
	}
}
