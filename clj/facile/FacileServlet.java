package clj.facile;

import java.io.IOException;

import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import clojure.lang.AFn;
import clojure.lang.IFn;
import clojure.lang.LockingTransaction;
import clojure.lang.RT;
import clojure.lang.Var;

public class FacileServlet extends HttpServlet {
	   
	private FacesServlet facesServlet;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.facesServlet = new FacesServlet();
		this.facesServlet.init(config);
	}

	@Override
	public void destroy() {
		facesServlet.destroy();
	}

	@Override
	public ServletConfig getServletConfig() {
		return facesServlet.getServletConfig();
	}

	@Override
	public String getServletInfo() {
		return facesServlet.getServletInfo();
	}

	@Override
	public void service(final ServletRequest request, final ServletResponse response)
			throws IOException, ServletException {
		
		// Wrap the service call 
		final IFn service = new AFn() {
			public Object invoke() throws IOException, ServletException {
				facesServlet.service(request, response);
				return null;
			}
		};
		
		// Run the service method in a transaction
		try {
			LockingTransaction.runInTransaction(service);
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public String toString() {
		return "FacileServlet(" + facesServlet.toString() + ")";
	}
	
}
