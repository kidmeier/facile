<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<%@page	language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<html>
<head>
	<title>Swank</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>
<%	if( Boolean.TRUE == application.getAttribute("swank-started") ) { %>

		<pre>Swank is running on port: ${applicationScope['swank-port']}</pre>
		
<%	} else { %>
		<pre>Starting swank: <%
			final String port = (request.getAttribute("port") == null) 
									? ( (application.getInitParameter("swank-port") == null) 
										 ? "4005"
										 : (application.getInitParameter("swank-port")) ) 
									: (String)request.getAttribute("port");
			final String swankPath = application.getInitParameter("swank-path");			
			final String swankScript = 
				"(add-classpath \"file://" + swankPath + "\")\n" +
				"(require (quote swank.swank) (quote clojure.main))\n" +
				"(clojure.main/with-bindings\n" +
				"  (swank.swank/start-server \"swank-port\" :encoding \"utf-8-unix\" :port " + port + "))\n";
			String message = "";
			try {
				clojure.lang.Compiler.load(new java.io.StringReader(swankScript));
				application.setAttribute("swank-started", Boolean.TRUE);
				application.setAttribute("swank-port", port);
				message = "success; running on port " + port;
			} catch( Throwable e ) {
				final java.io.StringWriter stackTrace = new java.io.StringWriter();
				 
				e.printStackTrace();
				e.printStackTrace(new java.io.PrintWriter(stackTrace));
				
				message = "Error starting Swank:\n\n" + stackTrace.toString();
			}
			%> <%=message%></pre>
<%	} %>
</body>
</html>