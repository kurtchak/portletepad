package org.webepad.beans;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.portlet.PortletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.control.PushControl;
import org.webepad.model.Pad;
import org.webepad.model.Session;
import org.webepad.model.User;

@ManagedBean(name = "apiBean")
@SessionScoped
public class APIBean {
	public static final String OPEN_PAD_ACTION = "openPad";
	public static final String LEAVE_PAD_ACTION = "padList";
	public static final String REFRESH_ACTION = "refresh";
	public static final String ERROR_ACTION = "error";
	public static final int READWRITE = 0;
	public static final int READONLY = 1;
	
	@ManagedProperty(value="#{padBean}")
	private PadBean padBean;
	@ManagedProperty(value="#{sessionBean}")
	private SessionBean sessionBean;
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;

	private Logger log = LoggerFactory.getLogger(APIBean.class);
	private PushControl pushControl;
	
	private String padname;
	private String username;
	private String password;
	private User user;
	private Boolean auth = false;
	
	public APIBean() {
	}

	@PostConstruct
	public void initialize() {
		initializePushTopic();
		loadLoggedUser();
	}

	private void initializePushTopic() {
		if (pushControl == null) {
			pushControl = new PushControl();
		}
		pushControl.initializeTopic();
	}

	private void loadLoggedUser() {
		PortletRequest req = (PortletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String name = req.getRemoteUser();
		if (name == null) {
			user = null;
		} else if (user != null && name.equals(user.getName())) {
			return;
		} else {
			user = userBean.getUser(name);
			if (user == null) {
				user = new User(name);
				userBean.save(user);
			}
			log.info("LOADED USER: " + user.getName());
		}
	}

	public void reloadActualUser() {
		if (user != null) {
			loadLoggedUser();
		}
	}

	private void refreshUsersInfo() {
		setUser(userBean.getUser(user.getId()));
	}

	public List<Pad> getPads() {
		log.info("APIBean.getPads...");
		return padBean.getPads();
	}

	public synchronized Collection<Pad> getActivePads() {
		return padBean.getActivePads();
	}

	public String createNewPad() {
		if (user != null) {
			Pad pad = padBean.createNewPad(padname, user);
			pad.setPushControl(pushControl); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			return openPadSession(pad, user, READWRITE);
		}
		return ERROR_ACTION;
	}

	public String deletePad(Pad pad) {
		padBean.deletePad(pad);
		refreshUsersInfo(); // TODO: pri spravnom mapovani by nemalo byt potrebne
		return REFRESH_ACTION;
	}
	
	public String togglePadLock(Pad pad) {
		padBean.togglePadLock(pad);
		padBean.update(pad);
		return REFRESH_ACTION;
	}
	
	public String openPad(Pad pad) {
		if (user != null) {
			pad.setPushControl(pushControl); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			return openPadSession(pad, user, READWRITE);
		}
		return ERROR_ACTION;
	}

	public String openReadOnlyPad(Pad pad) {
		if (user != null) {
			return openPadSession(pad, user, READONLY);
		}
		return ERROR_ACTION;
	}

	public String openPadSession(Pad pad, User user, int access) {
		Session session = padBean.openSession(pad, user, access);
		if (session != null) {
			sessionBean.loadSession(session);
			refreshUsersInfo(); // TODO: pri spravnom mapovani by nemalo byt potrebne
			return OPEN_PAD_ACTION;
		}
		return ERROR_ACTION;
	}

	public String openSession(Session session) {
		if (session != null) {
			session.getPad().setPushControl(pushControl); // TODO: load pushBean for AJAX PUSH FUNCTIONALITY
			session = padBean.openSession(session);
			sessionBean.loadSession(session);
			return OPEN_PAD_ACTION;
		}
		return ERROR_ACTION;
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

	public PushControl getPushBean() {
		return pushControl;
	}

	public void setPushBean(PushControl pushBean) {
		this.pushControl = pushBean;
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
	
	public User getUser() {
		if (user == null) {
			loadLoggedUser();
		}
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getTopicAddress() {
		if (pushControl != null) {
			return pushControl.getTopicAddress();
		}
		return null;
	}
}
