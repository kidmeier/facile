package clj.facile.repl;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import clojure.lang.IPersistentMap;
import clojure.lang.LineNumberingPushbackReader;
import clojure.lang.LispReader;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

public class ReplServer extends Thread {
	
	static final Var clojure_out = RT.var("clojure.core", "*out*");
	static final Var currentNs = RT.var("clojure.core", "*ns*");
	static final Var warnOnReflection = RT.var("clojure.core", "*warn-on-reflection*");
	static final Var inNs = RT.var("clojure.core", "in-ns");
    static final Var refer = RT.var("clojure.core", "refer");

	private SynchronousQueue<String> in;
	private SynchronousQueue<Evaluation> out;
	
	private int timeout;
	private String defaultNamespace;
	private IPersistentMap bindings;
	
	private List<ReplServer> registry;
	
	public static class Evaluation {
		
		public String form;
		public Object value;
		public String valueString;
		public Namespace ns;
		
		public String stdout;
		public String stderr;
		
		public Evaluation(Namespace ns, String form, Object value, String valueString, String stdout, String stderr) {
			this.ns = ns;
			this.form = form;
			this.value = value;
			this.valueString = valueString;
			this.stdout = stdout;
			this.stderr = stderr;
		}
	}
	
	public ReplServer(String id, int timeout, String defaultNamespace, List<ReplServer> registry) {
		this(id,timeout,defaultNamespace,registry,null);
	}
	
	public ReplServer(String id, int timeout, String defaultNamespace, List<ReplServer> registry, IPersistentMap bindings) {
		super("ReplServer_" + id);
		
		this.timeout = timeout;
		this.defaultNamespace = defaultNamespace;
		this.bindings = bindings;
		
		this.registry = registry;
		
		// Setup i/o queue
		this.in = new SynchronousQueue<String>();
		this.out = new SynchronousQueue<Evaluation>();
	}
	
	public Evaluation eval(String form) {
		try {
			
    		// Queue it up
    		in.put(form);
    		return out.take();
    		
		} catch( InterruptedException e ) {
			return null;
		}
	}
	
	public boolean isActive() {
		return State.TERMINATED != this.getState();
	}
	
	public Namespace currentNamespace() {
		
		// If we are not running, then we can't get the namespace
		if( !isActive() )
			return null;
		
		try {
			
			in.put("'ping");
			return out.take().ns;
			
		} catch( InterruptedException e ) {
			return null;
		}
	}
	
	@Override
	public void run() {
	
		System.out.println(this.getClass().getName() + ": Starting up REPL server thread: " + this.getName());

		// Record ourself in the registry
		if( null != this.registry )
			this.registry.add(this);

		// Change to the into the 'repl namespace and refer 'clojure
		Symbol INITIAL_NS = Symbol.intern(this.defaultNamespace);
		Symbol CLOJURE_CORE = Symbol.intern("clojure.core");
		Var.pushThreadBindings(RT.map(
				currentNs, currentNs.get(),
				warnOnReflection, warnOnReflection.get()));
	
		// Move into the REPL namespace
		try {
			inNs.invoke(INITIAL_NS);
			refer.invoke(CLOJURE_CORE);
		} catch( Exception e ) {
			// Not fatal, keep going
			e.printStackTrace();
		}
		
		// Infinite loop
		while(!Thread.interrupted()) {
				
			String input = null;
				
			// Wait for input, but only as long as the session timeout is set for
			try {
				input = in.poll(this.timeout, TimeUnit.SECONDS);
				if( null == input ) {
					System.out.println(getClass().getName() + ": Timed out waiting for input, shutting down this repl");
					break;
				}
					
			} catch( InterruptedException e ) {
				// We're done.
				break;
			}
					
			try {
				// Parse the input
				Object r = LispReader.read(new LineNumberingPushbackReader(new StringReader(input + "  ")),
								true,		// EOF is error
								null,		// Value returned if EOF is encountered (ignored since eofIsError=true
								false);		// isRecursive; not sure what this means, but Repl.java sets it false
				
				// Bind *out*
				final StringWriter stdout = new StringWriter();
				Var.pushThreadBindings((IPersistentMap)
						RT.map(clojure_out, stdout)
							.cons(this.bindings));

				// Evaluate the form
				Object result = clojure.lang.Compiler.eval(r);
				
				// Convert the result to a string / force evalution of lazy forms
				final StringWriter resultString = new StringWriter();
				try {
					RT.print(result, resultString);
				} catch( Exception e ) {
					PrintWriter wr = new PrintWriter(resultString);
					e.printStackTrace(wr);
					
					wr.flush();
					wr.close();
				}			

				RT.print(result, new StringWriter());
				
				Var.popThreadBindings();
				
				// Store the result
				try {
					out.put( new Evaluation((Namespace)currentNs.get(), input, result, resultString.toString(), stdout.toString(), "") );
				} catch( InterruptedException e ) {
						
					// All done
					break;
				}
				
			} catch( Exception e ) {
				
				// Exceptions are results too
				try {
					final StringWriter stderr = new StringWriter();
					e.printStackTrace(new PrintWriter(stderr));
					out.put( new Evaluation((Namespace)currentNs.get(), input, null, "nil", "", stderr.toString() ) );
				} catch( InterruptedException ee ) {
					
					// Done
					break;
				}
			}
		}

		// Pop thread bindings
		Var.popThreadBindings();

		// Remove ourself from the registry REPLs
		if( null != this.registry )
			this.registry.remove(this);
		
		System.out.println(this.getClass().getName() + ": REPL server thread is done: " + this.getName());
	}
    
}
