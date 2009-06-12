package clj.facile;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import clj.facile.repl.History;
import clj.facile.repl.ReplServer;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Var;

public class ReplServlet extends HttpServlet {

	// Session keys //////////////////////////////////////////////////////////
	private static final String HISTORY = "clj.facile.repl.history";
	private static final String FORM = "clj.facile.repl.form";
	private static final String REPL_SERVER = "clj.facile.repl";
	private static final String NAMESPACE = "clj.facile.repl.current-ns";
	private static final String REPL_DEFAULT_NAMESPACE = "clj.facile.repl.default-ns";
	private static final String EDITOR = "clj.facile.buffer";
	
	private static final Var FACES_APPLICATION = RT.var("clj.facile", "*facesApp*");
	
    private List<ReplServer> runningRepls;
    private String defaultNamespace;
    
    private void initSession(HttpSession session) {
    
    	// Clear history object
    	clearHistory(session);
    	
    	session.setAttribute(FORM, "");
    	
    	// Get the Faces application
    	final ApplicationFactory appFactory = (ApplicationFactory)FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
    	final Application facesApp = appFactory.getApplication();
    	
    	// Instantiate a new REPL server
    	final ReplServer repl = new ReplServer(session.getId(),
    			session.getMaxInactiveInterval(), 
    			this.defaultNamespace, 
    			this.runningRepls,
    			RT.map(FACES_APPLICATION, facesApp));
    	session.setAttribute(REPL_SERVER, repl);
    	
    	// Start the server and get the namespace
    	repl.start();
    	session.setAttribute(NAMESPACE, repl.currentNamespace());
    	
    	// Clear the editor
    	session.setAttribute(EDITOR, "");
    }
    
    private void clearHistory(HttpSession session) {
    	session.setAttribute(HISTORY, new History());
    }
    
    private History getHistory(HttpSession session) {
    	
    	return (History)session.getAttribute(HISTORY);
    }
    
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		final ServletContext ctx = this.getServletContext();
		
		this.defaultNamespace = config.getInitParameter(REPL_DEFAULT_NAMESPACE);
		if( null == this.defaultNamespace || this.defaultNamespace.length() <= 0 )
			this.defaultNamespace = "repl";
		
		// Configure the Clojure class-loader with the WAR resource path
		try {
			RT.addURL("file://" + ctx.getRealPath("/"));
		} catch( Exception e ) {
			throw new ServletException("Couldn't add servlet context to Clojure classpath", e);
		}
		
		this.runningRepls = new LinkedList<ReplServer>();
	}
	
	@Override
	public void destroy() {
	
		// Clean up running REPLs (we create a copy of the list to prevent any ConcurrentModificationExceptions
		for( ReplServer repl : this.runningRepls.toArray(new ReplServer[this.runningRepls.size()]) ) {
			repl.interrupt();
		}
	}

	// Rendering /////////////////////////////////////////////////////////////
	
	private String escapeHtml(String text) {
	
		final Map<Character,String> specialChars = new HashMap<Character,String>();
		specialChars.put('<', "&lt;");
		specialChars.put('>', "&gt;");
		specialChars.put('&', "&amp;");
		specialChars.put('"', "&quot;");
		specialChars.put('\n', "<br/>");
		
		final StringBuffer html = new StringBuffer();
		
		for( int i=0; i<text.length(); i++ ) {
			
			char c = text.charAt(i);
			String escape = specialChars.get(c);
			if( null != escape ) {
				html.append(escape);
			} else {
				html.append(c);
			}
			
		}
		return html.toString();
	}
	
	private void renderHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {

		final PrintWriter out = response.getWriter();
		
		out.println("<h3>Clojure</h3>");		
	}
	
	private void renderFooter(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		final PrintWriter out = response.getWriter();
		final Runtime rt = Runtime.getRuntime();
		
		out.println("<p style=\"font-size: x-small;\">");

		out.println("Free: " + (rt.freeMemory()/1024) + " kb / " + (rt.maxMemory()/1024) + " kb<br/>");
		out.println( System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.info") +") ");
		out.println( System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version") + "<br/>");
		
		out.println("</p>");
	}
	
	private void renderHistory(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		final HttpSession session = request.getSession();

		final PrintWriter out = response.getWriter();
		final History history = getHistory(session);
		
		out.println("<table id=\"history\" class=\"history\">");
		
		int minute = -1;
		int line = 0;
		for( History.Item item : history ) {
		
			// Print a timestamp every minute
			if( item.when.get(Calendar.MINUTE) != minute ) {
					
					out.println("<tr>");
				
					// Format the date
					final String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(item.when.getTime());
					out.println("<td class=\"time\">" + time + "</td>");
					
					minute = item.when.get(Calendar.MINUTE);
					out.println("<td></td></tr>");
			} else {
				out.print("<tr><td></td><td></td></tr>");
			}

			out.println("<tr>");
			
				// Line numbers
				out.println("<td class=\"lineno\">line&nbsp;" + line + ":&nbsp;</td>");	
				
				// Generate back-links to the history
				out.println("<td><a href=\"" + request.getRequestURI() + "?history=" + line + "\">" 
									+ escapeHtml(item.eval.form) 
									+ "</a></td>");
			out.println("</tr>");
			
			// Print the output on stdout and stderr
			out.println("<tr>");
				out.println("<td></td><td class=\"output\">");
				// Stdout
				final String[] outLines = item.eval.stdout.split("\n"); 
				out.println("<span class=\"stdout\">");
					for( int i=0; i<outLines.length; i++ )
						if( outLines[i].length() > 0 )
							out.println(outLines[i] + "<br />");
				out.println("</span>");
				// Stderr
				final String[] errLines = item.eval.stderr.split("\n"); 
				out.println("<span class=\"stderr\">");
					for( int i=0; i<errLines.length; i++ )
						if( errLines[i].length() > 0 )
							out.println(errLines[i] + "<br />");
				out.println("</span>");
				out.println("</td>");
			out.println("</tr>");
			
			// Print the result
			out.println("<tr>");
							
				out.println("<td></td>");
				out.println("<td><pre style=\"font-style: italic\">"
						+ escapeHtml(item.eval.valueString) 
						+ "</pre></td>");
			
			out.println("</tr>");
			line++;
		}
		
		out.println("</table>");
	}

	private void renderEditor(HttpServletRequest request, HttpServletResponse response) throws IOException {

		final HttpSession session = request.getSession();
		final PrintWriter writer = response.getWriter();
		
		writer.println("<form name=\"editor\" method=\"POST\" action=\"" + request.getRequestURI() + "\">");
		writer.println("<table>");
			writer.println("<tr>");
			writer.println("<td>Editor:</td>");
			writer.println("<td>");				
				writer.println("<textarea id=\"editor\" name=\"editor\" rows=\"24\" cols=\"100\">");
				writer.print(session.getAttribute(EDITOR));
				writer.println("</textarea>");
			writer.println("</td>");
			writer.println("<td>");
				writer.println("<input name=\"compile\" type=\"submit\" value=\"compile\">");
			writer.println("</td>");
		writer.println("</form>");
	}
	
	private void render(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		final HttpSession session = request.getSession();
		
		// Header
		response.setStatus(HttpServletResponse.SC_OK);
		
		// Check if we are asking to load a particular element in the history, and validate the input
		final String form = (String)request.getAttribute(FORM);
		final PrintWriter writer = response.getWriter();
		writer.println("<html>");
			writer.println("<head>");
				writer.println("<title>REPL</title>");
				
				// CSS 
				writer.println("<style type=\"text/css\">");
				writer.println("A:link, A:visited, A:active { " +
						"font-family: monospace; text-decoration: none; color: inherit;"
						+ "}");
				writer.println("A:hover { background-color: silver }");
				writer.println("table.history {" +
						"height: 16em;" +
						"overflow: auto;" +
						"display: block;" +
						"}");
						
				writer.println("td.time { font-size: small; font-weight: bold; }");
				writer.println("td.lineno { font-size: small; font-family: monospace; }");
				writer.println("td.output { font-family: monospace; }");
				writer.println("span.stdout { " +
						"color: gray; " +
						"border: thin;" +
						"overflow: auto;" +
						"width: 80em;" +
						"white-space: nowrap;" +
						"}");
				writer.println("span.stderr { " +
						"color: red; " +
						"border: thin;" +
						"overflow: auto;" +
						"width: 80em;" +
						"white-space: nowrap;" +
						"}");
				//writer.println("input.repl {");
				
				writer.println("</style>");				
			writer.println("</head>");
			writer.println("<body onload=\"document.forms.repl.input.focus();\">");

			renderHeader(request,response);
			
			// 	Dump the REPL history to the browser
			renderHistory(request, response);
			
			// Render the input form
			writer.println("<form name=\"repl\" method=\"POST\" action=\"" + request.getRequestURI() + "\">");
				Namespace ns = (Namespace)session.getAttribute(NAMESPACE);
				writer.println( "<span style=\"font-family: monospace\">" + ns.getName() + "&nbsp;&gt;&nbsp;</span>" );
				
				writer.println("<input class=\"repl\" style=\"width: 80em; font-family: monospace\" name=\"input\" type=\"text\" value=\"" + form + "\">");
				writer.println("<input style=\"width: 5em;\" name=\"eval\" type=\"submit\" value=\"eval\" >");
				writer.println("<input style=\"width: 6em;\" name=\"clear\" type=\"submit\" value=\"clear\" >");
			writer.println("</form>");
			
			renderEditor(request,response);
			
			renderFooter(request,response);
			
			writer.println(
					"<script>" +
						"var hist = document.getElementById('history');\n" +
						"hist.scrollTop=hist.scrollHeight;" +
					"</script>");
			
			writer.println("</body>");
		writer.println("</html>");
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		final HttpSession session = req.getSession(true);
		
		req.setAttribute(FORM, "");
		
		// A new session?
		if( null == getHistory(session) 
			|| null == session.getAttribute(REPL_SERVER) 
			|| null == session.getAttribute(NAMESPACE)) {
			initSession(session);
		}

		final String history = req.getParameter("history");
		if( history != null ) {
			int index = Integer.valueOf(history);
			if( index >= 0 && index < getHistory(session).size() ) {
				final ReplServer.Evaluation eval = (ReplServer.Evaluation)getHistory(session).get(index).eval;
				req.setAttribute(FORM, escapeHtml(eval.form));
			// No such history item, redirect to the initial page
			} else {
				resp.sendRedirect(req.getRequestURI());
				return;
			}
		}

		render(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		final HttpSession session = req.getSession();
		final ReplServer repl = (ReplServer)session.getAttribute(REPL_SERVER);
		
		// No repl?
		if( null == repl ) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.sendRedirect(req.getRequestURI());
			return;
		}
		
		// Clear?
		if( req.getParameter("clear") != null )
			clearHistory(session);
		
		if( req.getParameter("eval") != null ) {
			// Get the form to evaluate
			final String input = req.getParameter("input");
			if( null != input ) { 
				if( input.trim().length() > 0 ) {
					// Evaluate it on the repl server.
					ReplServer.Evaluation eval = repl.eval(input);
					
					// Store the current ns for the renderer
					req.setAttribute(NAMESPACE, eval.ns);
				
					// Add to the history
					getHistory(session).enqueue(eval);
				}
			}
		}
		
		// Compile the buffer?
		if( req.getParameter("compile") != null ) {
			final String buffer = req.getParameter("editor");
			if( buffer != null ) {
				if( buffer.trim().length() > 0 ) {
	
					ReplServer.Evaluation eval = repl.eval(buffer);
					eval.form = "<i>;; compiled buffer</i>";
	
					// Add to the history
					getHistory(session).enqueue(eval);
	
					// Save the buffer
					session.setAttribute(EDITOR, buffer);
				}
			}
		}
		
		// Render
		req.setAttribute(FORM, "");
		render(req, resp);
	}
}
