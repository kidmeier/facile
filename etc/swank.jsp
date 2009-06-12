<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd"><%@page
	language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%><html>

<head>
	<title>Swank</title>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="GENERATOR"
		content="Rational® Application Developer™ for WebSphere® Software">
</head>
<body>
<c:choose>
	<c:when test="${applicationScope.swankStarted}">
		<pre>Swank is running on port: ${applicationScope.swankPort}</pre>
	</c:when>
	<c:otherwise>
		<pre>Starting swank: <jsp:scriptlet>
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
				"  (swank.swank/start-server \"nul\" :encoding \"utf-8-unix\" :port " + port + "))\n";
			String message = "";
			try {
				clojure.lang.Compiler.load(new java.io.StringReader(swankScript));
				application.setAttribute("swankStarted", Boolean.TRUE);
				application.setAttribute("swankPort", port);
				message = "success; running on port " + port;
			} catch( Throwable e ) {
				final java.io.StringWriter stackTrace = new java.io.StringWriter();
				 
				e.printStackTrace();
				e.printStackTrace(new java.io.PrintWriter(stackTrace));
				
				message = "Error starting Swank:\n\n" + stackTrace.toString();
			}
			</jsp:scriptlet><%=message%></pre>
	</c:otherwise>
</c:choose>
</body>
</html>