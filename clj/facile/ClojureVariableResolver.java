package clj.facile;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

import clojure.lang.Namespace;
import clojure.lang.Symbol;

public class ClojureVariableResolver extends VariableResolver {

	static final Logger log = Logger.getLogger(ClojureVariableResolver.class.getName());
	//
	private VariableResolver parent;
	private static Map<Character,String> specialChars;
	private static Map<String,Character> invertedSpecialChars;
	private static String escapeSpecialBegin = "$";
	private static String escapeSpecialEnd = "$";
	
	static {
		specialChars = new TreeMap<Character,String>();

		specialChars.put(Character.valueOf('*'), "_star_");
		specialChars.put(Character.valueOf('+'), "_plus_");
		specialChars.put(Character.valueOf('-'), "_");
		specialChars.put(Character.valueOf('?'), "_q_");		
		specialChars.put(Character.valueOf('!'), "_bang_");
		specialChars.put(Character.valueOf(':'), "_c_");

		// This allows us to escape symbol's w/ $ 
		specialChars.put(Character.valueOf('$'), "");
		
		// Create the inverse map for unmangling
		invertedSpecialChars = new TreeMap<String,Character>();
		for( Map.Entry<Character,String> e : specialChars.entrySet() ) {
			invertedSpecialChars.put(e.getValue(), e.getKey());
		}
	}
	
	public ClojureVariableResolver(VariableResolver parent) {
		this.parent = parent;
	}

	@Override
	public Object resolveVariable(FacesContext ctx, String name)
			throws EvaluationException {

		log.entering(this.getClass().getName(), "resolveVariable", new Object[] {ctx, name});
		
		// Lookup and return the namespace
		Namespace ns = Namespace.find(Symbol.intern( unmangleIdent(name) ));
		if( null == ns ) {
			log.finest("Unknown namespace " + name + ", delegating to parent");
			final Object value = parent.resolveVariable(ctx, name);
			
			log.exiting(this.getClass().getName(), "resolveVariable", value);			
			return value;
		}
		
		log.exiting(this.getClass().getName(), "resolveVariable", ns);
		return ns;
	}
	
	public static String mangleIdent(String ident) {
		
		StringBuffer mangled = new StringBuffer();
		for( char c : ident.toCharArray() ) {
			
			String subst = specialChars.get(c);
			if( subst != null ) {
				
				mangled.append(escapeSpecialBegin);
					mangled.append(subst);
				mangled.append(escapeSpecialEnd);
				
			} else {
				mangled.append(c);
			}
		}
		
		return mangled.toString();
	}
	
	public static String unmangleIdent(String mangledIdent) {
		
		StringBuffer ident = new StringBuffer();
		
		//
		// This whole thing is pretty gross, but basically what we are doing  
		// is:
		//	1. Finding the beginning and ending of an escaped character,
		//  2. Lookup the escaped character corresponding to the subst-string
		//  3. Replace w/ the escaped char
		//
		int begin = mangledIdent.indexOf(escapeSpecialBegin);
		int end = -1;
		while( begin > 0 ) {
			
			ident.append( mangledIdent.substring(end+1, begin) );			
			end = mangledIdent.indexOf(escapeSpecialEnd, begin+1);
			
			// If we've reached the end, append the rest and break
			if( end < 0 ) {
				break;
			}
			
			final String subst = mangledIdent.substring(begin+1, end);
			final Character specialChar = invertedSpecialChars.get(subst);
		
			if( specialChar != null ) {
				ident.append(specialChar);
			} else {
				
				// We don't recognize the substitution, so pass it through
				ident.append(escapeSpecialBegin);
				ident.append(subst);
				ident.append(escapeSpecialEnd);
				
			}
			begin = mangledIdent.indexOf(escapeSpecialBegin, end+1);
		}
		ident.append( mangledIdent.substring(end+1) );
		
		return ident.toString();
	}
}
