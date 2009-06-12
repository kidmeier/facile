package clj.facile;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

public class FacileNavigationHandler extends NavigationHandler {

	static final Logger log = Logger.getLogger(FacileViewHandler.class.getName());
	//
	private NavigationHandler parent;
	
	public FacileNavigationHandler(NavigationHandler parent) {
		this.parent = parent;
	}
	
	@Override
	public void handleNavigation(FacesContext ctx, String fromAction, String outcome) {

		log.entering(this.getClass().getName(), "handleNavigation", new Object[] { ctx, fromAction, outcome });
		
		if( null != outcome ) {
			final ViewHandler vh = ctx.getApplication().getViewHandler();
			final String redirectPath = vh.getActionURL(ctx, "/" + outcome);  

			/*
			final Map parmMap = ctx.getExternalContext().getRequestParameterMap();
			if( parmMap.size() > 0 )
				redirectPath = redirectPath + "?";
			
			// We are automatically append request parameters to the forwarded page
			for( Iterator i=parmMap.entrySet().iterator(); i.hasNext(); ) {
				
				Map.Entry e = (Map.Entry)i.next();
				redirectPath = redirectPath + e.getKey() + "=" + e.getValue() + (i.hasNext()?"&":""); 				
			}
			*/
			
			//ctx.setViewRoot( vh.createView(ctx, "/" + outcome) );			

			try {
				
				ExternalContext externalContext = ctx.getExternalContext();  
				externalContext.redirect(externalContext.encodeActionURL(redirectPath));
				ctx.responseComplete();
				
			} catch (IOException e) {  
				throw new FacesException(e.getMessage(), e);  
			}  			
			
			log.exiting(this.getClass().getName(), "handleNavigation");
			return;
		}
		parent.handleNavigation(ctx, fromAction, outcome);
		log.exiting(this.getClass().getName(), "handleNavigation");
	}

}
