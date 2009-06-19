package clj.facile;

import java.io.File;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletContext;

import clojure.lang.RT;
import clojure.lang.Var;

public class FacilePhaseListener implements PhaseListener {

	// Clojure fields ////////////////////////////////////////////////////////

	static final Var compileFlag = RT.var("clojure.core", "*compile-files*");
	static final Var compilePath = RT.var("clojure.core", "*compile-path*");

	static final Var servletContext = RT.var("clj.facile", "*servlet-context*");
	static final Var httpSession = RT.var("clj.facile", "*http-session*");

	static final Var facesApp = RT.var("clj.facile", "*faces-app*");
	static final Var facesContext = RT.var("clj.facile", "*faces-context*");

	static final Var sessionMap = RT.var("clj.facile", "*session-map*");
	static final Var requestMap = RT.var("clj.facile", "*request-map*");
	static final Var cookieMap = RT.var("clj.facile", "*cookies*");
	static final Var headerMap = RT.var("clj.facile", "*headers*");
	static final Var headerMultiMap = RT.var("clj.facile", "*headers-multi*");

	protected void pushBindings(final FacesContext ctx) {
		
		System.out.println("Beginning a Faces request, pushing thread bindings");
		
		final ExternalContext extCtx = ctx.getExternalContext();
		final ServletContext servletCtx = (ServletContext)extCtx.getContext();
		
		// Bind base vars while we run the 
		Var.pushThreadBindings(
				RT.map(
						servletContext, servletCtx,
						httpSession, extCtx.getSession(true),
						
						facesApp, ctx.getApplication(),
						facesContext, ctx,
						
						sessionMap, extCtx.getSessionMap(),
						requestMap, extCtx.getRequestParameterMap(),
						cookieMap, extCtx.getRequestCookieMap(),
						headerMap, extCtx.getRequestHeaderMap(),
						headerMultiMap, extCtx.getRequestHeaderValuesMap(),
						
						// We need to set the compile flag to true and give 
						// a path so that if the view uses forms like 'proxy
						// or :gen-class then the servlet container can also
						// resolve them.
						compileFlag, Boolean.TRUE,
						compilePath, servletCtx.getRealPath("") + File.separatorChar + "WEB-INF"	+ File.separatorChar + "classes"));
	}
	
	protected void popBindings() {
		System.out.println("Done the request, popping bindings");
		Var.popThreadBindings();
	}
	
	public void beforePhase(PhaseEvent ev) {

		if( ev.getPhaseId().compareTo(PhaseId.RESTORE_VIEW) == 0 ) {
			
			pushBindings(ev.getFacesContext());
			
		}
		
	}

	public void afterPhase(PhaseEvent ev) {

		if( ev.getPhaseId().compareTo(PhaseId.RENDER_RESPONSE) == 0 ) {
		
			popBindings();
			
		}
	}


	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

}
