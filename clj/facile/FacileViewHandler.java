package clj.facile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.application.StateManager.SerializedView;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;

import clojure.lang.RT;
import clojure.lang.Ref;
import clojure.lang.Var;

public class FacileViewHandler extends ViewHandler {

	static final Logger log = Logger.getLogger(FacileViewHandler.class.getName());
	
	// Clojure fields ////////////////////////////////////////////////////////
	
	static final Var facesApp = RT.var("clj.facile", "*faces-app*");
	static final Var facesContext = RT.var("clj.facile", "*faces-context*");
	static final Var currentView = RT.var("clj.facile", "*current-view*");
	static final Var httpSession = RT.var("clj.facile", "*http-session*");
	static final Var viewId = RT.var("clj.facile", "*view-id*");
	static final Var viewRoot = RT.var("clj.facile", "*view-root*");
	static final Var servletContext = RT.var("clj.facile", "*servlet-context*");

	static final Var buildView = RT.var("clj.facile", "build-view");

	// ///////////////////////////////////////////////////////////////////////
	
	static final String STATE_VAR = "clj.facile.viewState";
	static final Map<String,Ref> viewLocalsMap = new HashMap<String,Ref>(); 
		
	private ViewHandler parentHandler;

	public static Ref viewLocals(Object ns, Object sym) {
		
		log.entering(ViewHandler.class.getName(), "viewLocals(ns=" + ns + ", sym=" + sym +")");
		
		final String key = ns + "/" + sym;
		
		if( viewLocalsMap.containsKey(key) ) {
			
			final Ref ref = viewLocalsMap.get(key);
			
			log.exiting(ViewHandler.class.getName(), "viewLocals", ref);
			return ref;
		}
			
		// Create the view-locals map
		try {
			final Ref ref = new Ref(RT.map());
			viewLocalsMap.put(key, ref);

			log.exiting(ViewHandler.class.getName(), "viewLocals", ref);
			return ref;
			
		} catch( Exception e ) {
	
			log.throwing(ViewHandler.class.getName(), "viewLocals", e);
			log.exiting(ViewHandler.class.getName(), "viewLocals", null);
			return null;
		}
	}
	
	public FacileViewHandler(ViewHandler parentHandler) throws Exception {
		this.parentHandler = parentHandler;
		
		log.setLevel(Level.ALL);
	}

	@Override
	public Locale calculateLocale(FacesContext ctx) {
		return parentHandler.calculateLocale(ctx);
	}

	@Override
	public String calculateRenderKitId(FacesContext ctx) {
		return parentHandler.calculateRenderKitId(ctx);
	}

	private static String symbolFromViewId(FacesContext ctx, String requestViewId) {
		log.entering(FacileViewHandler.class.getName(), "symbolFromViewId(ctx=" + ctx + ", requestViewId=" + requestViewId + ")");
		
		final String servletPath = ctx.getExternalContext().getRequestServletPath();
		final String symbol = requestViewId.replaceFirst(servletPath, "").substring(1);
		log.finest("symbol=" + symbol);
		
		log.exiting(FacileViewHandler.class.getName(), "symbolFromViewId");
		return symbol;
	}
	
	@Override
	public UIViewRoot createView(FacesContext ctx, String requestViewId) {
		
		final String qualifiedSymbol = symbolFromViewId(ctx, requestViewId);

		log.entering(FacileViewHandler.class.getName(), "createView(ctx=" + ctx + ", requestViewid=" + requestViewId + ")");
		try {

			final ServletContext servletCtx = (ServletContext)ctx.getExternalContext().getContext();
			final String filePath = FacileLoader.getPath(servletCtx, qualifiedSymbol);
			final Object session = ctx.getExternalContext().getSession(false);

			// Bind base vars while we run the 
			clojure.lang.Var.pushThreadBindings(
					clojure.lang.RT.map(
							servletContext, servletCtx,
							facesApp, ctx.getApplication(),
							httpSession, ctx.getExternalContext().getSession(true)));

			// This is a development hack so that we reload the file in case 
			// the session is null, which usually corresponds to the web 
			// module having been reloaded (so we must reload the .clj)
			final clojure.lang.Var viewTemplate = 
				(session == null) 
					? FacileLoader.load(qualifiedSymbol, filePath)
					: FacileLoader.require(qualifiedSymbol, filePath);
		
			log.finest("viewTemplate=" + viewTemplate);
			
			Var.popThreadBindings();
			
			// Create the top-level view root
			final UIViewRoot theView = new UIViewRoot();
			theView.setViewId(requestViewId);
			
			// Establish bindings
			clojure.lang.Var.pushThreadBindings(
					clojure.lang.RT.map(
							servletContext, servletCtx,
							facesApp, ctx.getApplication(),
							facesContext, ctx,
							currentView, viewTemplate.get(),
							viewId, qualifiedSymbol,
							viewRoot, theView,
							httpSession, session));
			
			// Build the view
			buildView.invoke(viewTemplate.get(), theView);

			// Prepare the view for rendering
			final UIViewRoot old = ctx.getViewRoot();
			if( null != old ) {
				theView.setLocale(old.getLocale());
				theView.setRenderKitId(old.getRenderKitId());
			} else {
				final ViewHandler vh = ctx.getApplication().getViewHandler();

				theView.setLocale(vh.calculateLocale(ctx));
				theView.setRenderKitId(vh.calculateRenderKitId(ctx));
			}

			return theView;
			
		} catch( Exception e ) {
			
			e.printStackTrace();
			throw new RuntimeException(e);
			
		} finally {
			Var.popThreadBindings();
			log.exiting(FacileViewHandler.class.getName(), "createView");
		}
	}

	@Override
	public String getActionURL(FacesContext ctx, String requestViewId) {
		return parentHandler.getActionURL(ctx, requestViewId);
	}

	@Override
	public String getResourceURL(FacesContext ctx, String requestViewId) {
		return parentHandler.getResourceURL(ctx, requestViewId);
	}

	@Override
	public void renderView(FacesContext ctx, UIViewRoot view)
			throws IOException, FacesException {
		
		log.entering(FacileViewHandler.class.getName(), "renderView(ctx=" + ctx + ", view=" + view + ")");

		setupResponse(ctx);
		
		StateManager sm = ctx.getApplication().getStateManager();
		SerializedView state = sm.saveSerializedView(ctx);
		ctx.getExternalContext().getRequestMap().put(STATE_VAR, state);

		final ResponseWriter out = ctx.getResponseWriter();
		out.startDocument();

		// Write the HTML header
		out.startElement("html", null);
		out.writeAttribute("xmlns", "http://www.w3.org/1999/xhtml", null);
			out.startElement("head", null);
				out.startElement("meta", null);
					out.writeAttribute("http-equiv", "Content-Type", null);
				out.endElement("meta");
				out.startElement("title", null);
					out.writeText("Clojure view title", null);
				out.endElement("title");
			out.endElement("head");
			
			out.startElement("body", null);
				// Render the view
				renderResponse(ctx,view);
			out.endElement("body");

		out.endElement("html");
		out.endDocument();

		log.exiting(FacileViewHandler.class.getName(), "renderView");
		return;
	}

	@Override
	public UIViewRoot restoreView(FacesContext ctx, String requestViewId) {
		
		log.entering(FacileViewHandler.class.getName(), "restoreView(ctx=" + ctx + ", requestViewId=" + requestViewId +")");
		
		final String qualifiedSymbol = symbolFromViewId(ctx, requestViewId);
		final ServletContext servletCtx = (ServletContext)ctx.getExternalContext().getContext();
		
		// Reload the view if it has changed.
		if( FacileLoader.isDirty(servletCtx, qualifiedSymbol) ) {
			return null;
		}
		
		// No change, restore the view.
		String renderKitId = ctx.getApplication().getViewHandler().calculateRenderKitId(ctx);
		StateManager sm = ctx.getApplication().getStateManager();
		final UIViewRoot restoredView = sm.restoreView(ctx, requestViewId, renderKitId);

		log.exiting(FacileViewHandler.class.getName(), "restoreView");
		return restoredView;
	}

	@Override
	public void writeState(FacesContext ctx) throws IOException {

		log.entering(FacileViewHandler.class.getName(), "writeState(ctx=" + ctx + ")");

		SerializedView state = (SerializedView)ctx.getExternalContext().getRequestMap().get(STATE_VAR);
		if( null != state ) {
			StateManager sm = ctx.getApplication().getStateManager();
			sm.writeState(ctx, state);
		}
		
		log.exiting(FacileViewHandler.class.getName(), "writeState");
	}

	// Private ///////////////////////////////////////////////////////////////
	
	private void setupResponse(FacesContext ctx) throws IOException {
		
		log.entering(FacileViewHandler.class.getName(), "setupResponse");

		ServletResponse response = (ServletResponse)ctx.getExternalContext().getResponse();
		OutputStream os = response.getOutputStream();
		Map<String,String> headers = ctx.getExternalContext().getRequestHeaderMap();
		String accept = headers.get("Accept");
		
		RenderKitFactory renderFactory = (RenderKitFactory)FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
		RenderKit renderKit = renderFactory.getRenderKit(ctx, ctx.getViewRoot().getRenderKitId());
		ResponseWriter writer = renderKit.createResponseWriter(
				new OutputStreamWriter(os), 
				accept, 
				response.getCharacterEncoding());
		ctx.setResponseWriter(writer);
		response.setContentType(writer.getContentType());
		
		log.exiting(FacileViewHandler.class.getName(), "setupResponse");
	}
	
	protected void renderResponse(FacesContext ctx, UIComponent component) throws IOException {
		
		log.entering(FacileViewHandler.class.getName(), "renderResponse", new Object[] { ctx, component } );

		component.encodeBegin(ctx);
		if( component.getRendersChildren() ) {
			component.encodeChildren(ctx);
		} else {
			for( UIComponent child : (List<UIComponent>)component.getChildren() ) {
				renderResponse(ctx, child);
			}
			
		}
		component.encodeEnd(ctx);
		
		log.exiting(FacileViewHandler.class.getName(), "renderResponse");	
	}
}
