package clj.facile;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import clojure.lang.RT;
import clojure.lang.Var;

public class FacileLoader {

	static final Logger log = Logger.getLogger(FacileLoader.class.getName());
	
	static final Var current_ns = RT.var("clojure.core", "*ns*");
	static final Var warn_on_reflection = RT.var("clojure.core", "*warn-on-reflection*");
	static final Var use_context_classloader = RT.var("clojure.core", "*use-context-classloader*");
 
	static final Map<String,Long> fileStamps = new HashMap<String,Long>();
	//
	public static String namespace(String qualifiedSymbol) {
		return qualifiedSymbol.split("/")[0];
	}

	public static String unqualified(String qualifiedSymbol) {
		return qualifiedSymbol.split("/")[1];
	}

	public static String getPath( ServletContext ctx, String qualifiedSymbol ) {

		log.entering(FacileLoader.class.getName(), "getPath");
		log.finest("qualifiedSymbol=" + qualifiedSymbol);
		
		final String ns = namespace(qualifiedSymbol);
		final String fileName = ns.substring(ns.lastIndexOf('.')+1) + ".clj";
		final String path = ns.replace('.', '/');

		log.finest("ns=" + ns + ", fileName=" + fileName + ", path=" + path);
		
		final String realPath = ctx.getRealPath(path + ".clj");
		log.finest("realPath=" + realPath);
		
		log.exiting(FacileLoader.class.getName(), "getPath");
		return realPath;
	}
	
	public static boolean isDirty( ServletContext ctx, String qualifiedSymbol ) {
		return isDirty(getPath(ctx,qualifiedSymbol));
	}
	
	public static boolean isDirty( String filePath ) {

		log.entering(FacileLoader.class.getName(), "isDirty(filePath=" + filePath +")");
		
		final File file = new File(filePath);
		final long timeStamp = fileStamps.containsKey(filePath) ? fileStamps.get(filePath).longValue() : -1;
		final long lastModified = file.lastModified();
		
		log.finest("timeStamp=" + timeStamp + ", lastModified=" + lastModified);
		
		boolean dirty = timeStamp < lastModified;
		
		log.exiting(FacileLoader.class.getName(), "isDirty", dirty);
		return dirty;
	}
	
	public static Var require( String qualifiedSymbol, String filePath ) throws Exception {
		
		log.entering(FacileLoader.class.getName(), "require");
		log.finest("qualifiedSymbol=" + qualifiedSymbol + ", filePath=" + filePath);
		
		if( isDirty(filePath) ) 
			return load(qualifiedSymbol, filePath);

		final String ns = namespace(qualifiedSymbol);
		final String symbol = unqualified(qualifiedSymbol);

		log.exiting(FacileLoader.class.getName(), "require");
		return clojure.lang.RT.var(ns, symbol);
	}
	
	public static Var load( String qualifiedSymbol, String filePath ) throws Exception {
		
		// Try and  load the file
		log.entering(FacileLoader.class.getName(), "load");
		log.finest("qualifiedSymbol=" + qualifiedSymbol + ", filePath=" + filePath);
		
		try {
			final File file = new File(filePath);
			
			//*ns* must be thread-bound for in-ns to work
			//thread-bind *warn-on-reflection* so it can be set!
			//must have corresponding popThreadBindings in finally clause
			clojure.lang.Var.pushThreadBindings(
					clojure.lang.RT.map(
							current_ns, current_ns.get(),
							warn_on_reflection, warn_on_reflection.get(),
							use_context_classloader, Boolean.TRUE));
	

			log.finest("current_ns=" + current_ns.get() + ", warn_on_reflection=" + warn_on_reflection.get() + ", use_context_classloader=" + use_context_classloader.get());
			
			clojure.lang.Compiler.load( 
					new java.io.InputStreamReader( new FileInputStream(file) ),	// reader
					filePath.substring(0, filePath.lastIndexOf(File.separator)),
					filePath.substring(filePath.lastIndexOf(File.separator)+1));
				
			// Update our recorded timestamp
			fileStamps.put(filePath, Long.valueOf(file.lastModified()));
	
			// Return the Var
			final String ns = namespace(qualifiedSymbol);
			final String symbol = unqualified(qualifiedSymbol);

			return clojure.lang.RT.var(ns, symbol);
				
		} catch( Exception e ) {
			throw new Exception("Exception caught while loading " + filePath + ": " + e.getMessage(), e);
		} finally {
			log.exiting(FacileLoader.class.getName(), "load");
			clojure.lang.Var.popThreadBindings();
		}
		
	}
	
}
