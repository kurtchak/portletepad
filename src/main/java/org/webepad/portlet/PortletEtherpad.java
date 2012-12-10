package org.webepad.portlet;

import java.io.IOException;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.UnavailableException;
import javax.portlet.faces.GenericFacesPortlet;

import org.webepad.beans.APIBean;
import org.webepad.beans.PadBean;
import org.webepad.beans.SessionBean;
import org.webepad.beans.UserBean;

public class PortletEtherpad extends GenericFacesPortlet {

	public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
//		String userName = (String) request.getRemoteUser();
//		System.out.println("target:"+request.getPortletSession().getAttribute("com.ibm.faces.portlet.page.view"));
//		if (userName == null) {
//			request.getPortletSession().setAttribute("com.ibm.faces.portlet.page.view","/pages/noauth.xhtml");
//			System.out.println("changed to:"+request.getPortletSession().getAttribute("com.ibm.faces.portlet.page.view"));
//		}
////		getPortletContext().setAttribute("remoteUser",userName);
//		System.out.println("target:"+target);
//		request.getPortletSession()
//		PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher(target);
//		prd.include(request, response);
		Map<String, Object> attribMap = request.getPortletSession().getAttributeMap();
		APIBean apiBean = (APIBean) attribMap.get("apiBean");
		if (apiBean == null) {
		    request.setAttribute("apiBean", new APIBean());
		}
		SessionBean sessionBean = (SessionBean) attribMap.get("sessionBean");
		if (sessionBean == null) {
		    request.setAttribute("sessionBean", new SessionBean());
		}
		PadBean padBean = (PadBean) attribMap.get("padBean");
		if (padBean == null) {
		    request.setAttribute("padBean", new PadBean());
		}
		UserBean userBean = (UserBean) attribMap.get("userBean");
		if (userBean == null) {
		    request.setAttribute("userBean", new UserBean());
		}
		super.doView(request, response);
	}

	public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException,
			UnavailableException {
//		String username = getPortletParam(request.getParameterMap(), "username");
//		String password = getPortletParam(request.getParameterMap(), "password");
//		String loginURL = "/portal/login?username="+username+"&amp;password="+password+"&amp;initialURI=/portal/classic/home/PortletEtherpad";
//		FacesContext.getCurrentInstance().getExternalContext().redirect(loginURL);
//		String userName = (String) request.getParameter(request.getRemoteUser());
//		response.setRenderParameter(request.getRemoteUser(), userName);
//		if (request.getAttribute("padId") != null) {
//			response.setRenderParameter("padId", (String) request.getAttribute("padId"));
//		}
		
		super.processAction(request, response);
	}
	
//	private String getPortletParam(Map<String, String[]> paramMap, String string) {
//	    for (String key : paramMap.keySet()) {
//	        if (key.equalsIgnoreCase(string) || key.endsWith(":" + string)) {
//	            return paramMap.get(key)[0];
//	        }
//	    }
//	    return null;
//	}
}
