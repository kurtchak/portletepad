package org.webepad.beans;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.webepad.model.Pad;
import org.webepad.model.Session;
import org.webepad.model.User;

@ManagedBean(name = "apiBean")
@SessionScoped
public class APIBean {
	@ManagedProperty(value="#{padBean}")
	private PadBean padBean;
	@ManagedProperty(value="#{sessionBean}")
	private SessionBean sessionBean;
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	@ManagedProperty(value="#{pushBean}")
	private PushBean pushBean;
	
	private String padname;
	private String username;
	private String password;
	
	private Boolean auth = false;
	
	public APIBean() {
	}

	public List<Pad> getPads() {
		return padBean.getPads();
	}

	public List<Session> getSessions() {
		return sessionBean.getSessions();
	}

	public String createNewPad() {
		User user = userBean.getActualUser();
		Pad pad = padBean.createNewPad(padname, user);
		pad.setPushBean(pushBean); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
		return openPadSession(pad, user);
	}

	public String deletePad(Pad pad) {
		padBean.deletePad(pad);
		userBean.reloadActualUser(); // TODO: pri spravnom mapovani by nemalo byt potrebne
		return "refresh";
	}
	
	public String togglePadLock(Pad pad) {
		pad.setReadOnly(!pad.getReadOnly());
		padBean.update(pad);
		return "refresh";
	}
	
	public String openPad(Pad pad) {
		User user = userBean.getActualUser();
		pad.setPushBean(pushBean); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
		return openPadSession(pad, user);
	}

	public String openPadSession(Pad pad, User user) {
		Session session = padBean.openSession(pad, user);
		if (session != null) {
			sessionBean.loadSession(session);
			userBean.reloadActualUser(); // TODO: pri spravnom mapovani by nemalo byt potrebne
			return "openPad";
		}
		return "error";
	}

	public String openSession(Session session) {
		if (session != null) {
			session.getPad().setPushBean(pushBean); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			session = padBean.openSession(session);
			sessionBean.loadSession(session);
			return "openPad";
		}
		return "error";
	}
	
	public PadBean getPadBean() {
		return padBean;
	}

	public void setPadBean(PadBean padBean) {
		this.padBean = padBean;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}

	public SessionBean getSessionBean() {
		return sessionBean;
	}

	public void setSessionBean(SessionBean sessionBean) {
		this.sessionBean = sessionBean;
	}

	public PushBean getPushBean() {
		return pushBean;
	}

	public void setPushBean(PushBean pushBean) {
		this.pushBean = pushBean;
	}

	public String getPadname() {
		return padname;
	}

	public void setPadname(String padname) {
		this.padname = padname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public Boolean getAuth() {
		return auth;
	}

	public void setAuth(Boolean auth) {
		this.auth = auth;
	}
}
