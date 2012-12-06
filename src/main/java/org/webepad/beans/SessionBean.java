package org.webepad.beans;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.control.TextSlice;
import org.webepad.dao.SessionDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.model.Changeset;
import org.webepad.model.Session;
import org.webepad.utils.DateUtils;

@ManagedBean(name = "sessionBean")
@SessionScoped
public class SessionBean {

	public static final String JOIN = "J";
	public static final String JOIN_RESPONSE = "R";
	public static final String LEAVE = "L";
	public static final String CHANGESET = "C";
	public static final String USERCOLOR = "UC";

	private static Logger log = LoggerFactory.getLogger(SessionBean.class);
	private SessionDAO sessionDAO = HibernateDAOFactory.getInstance().getSessionDAO();

	@ManagedProperty(value="#{padBean}")
	private PadBean padBean;
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;

	private Session session;
	private String number;
	private String changeset;
	private boolean pollEnabled = true;
	
	//////////////////////////////////////////////
	// CONSTRUCTOR
	public SessionBean() {
	}
	
	//////////////////////////////////////////////
	// LOADING OF THE MAIN OBJECT FOR THE VIEW
	public void loadSession(Session session) {
		this.session = session;
	}

	///////////////////////////////////////
	// RETRIEVAL FROM DB
	public Session getSession(Long id) {
		return sessionDAO.getSession(id);
	}

	public List<Session> getSessions() {
		return sessionDAO.readSessions();
	}

	public Session findSession(Long padId, Long userId) {
		return sessionDAO.findSession(padId, userId);
	}

	//////////////////////////////////////////////
	// ACTIVE SESSIONS
	public Collection<Session> getActiveSessions() {
		Collection<Session> sessions = session.getPad().getOtherActivePadSessions(session);
//		log.info("ActiveSessions ("+session.getUser().getName()+"):"+(sessions.size()+1));
		return sessions;
	}
	
	/**
	 * Reloads the pad content from DB and builds the editor content
	 */
	private void reloadPad() {
		session.reloadEditorContent();
	}

	/**
	 * Server side of new changeset addition 
	 */
	public void addChangeset() {
		log.info("SUBMITTED CHANGESET: " + changeset);
		Changeset c = new Changeset();
		c.setRule(changeset.substring(0, changeset.indexOf('$')));
		if (Changeset.NOCHANGE.equals(c.getAction())) {
			return;
		} else {
			c.setCharbank(changeset.substring(changeset.indexOf('$') + 1));
			c.setCreated(DateUtils.now());
			c.setAttributePool(null);
			session.addChangeset(c);
		}
	}

//	public void processNext() {
//		try {
//			if (!remoteChangesetsQueue.isEmpty()) {
//				log.info("Popping from the queue: "+remoteChangesetsQueue.peek());
//				processToView(remoteChangesetsQueue.pop());
//			} else {
//				log.info("Queue is empty: "+remoteChangesetsQueue.peek());
//			}
//		} catch (MessageException e) {
//			ExceptionHandler.handle(e);
//		}
//	}

	///////////////////////////////////////
	// ACTIONS
	public String actionLeave() {
		if (session != null) {
			padBean.closeSession(session);
		}
		return "padList";
	}

	public String actionRefresh() {
		reloadPad();
		return "refresh";
	}

	public Date getDate() {
		return DateUtils.now();
	}
	
	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getChangeset() {
		return changeset;
	}

	public void setChangeset(String changeset) {
		this.changeset = changeset;
	}

	public int getNextSliceSpanId() {
		return TextSlice.getNextSpanId();
	}

	public List<String> getFreeColors() {
		if (session != null) {
			return session.getPad().getFreeColors();
		}
		return null;
	}
	
	public String changeColor(String colorCode) {
		session.changeUserColor(colorCode);
		return actionRefresh(); // TODO: ZLE ZLE RIESENIE
	}

	public boolean isPollEnabled() {
//		log.info("POLL ENABLED ("+session.getUser().getName()+"): "+pollEnabled);
		return pollEnabled;
	}

	public void setPollEnabled(boolean pollEnabled) {
		this.pollEnabled = pollEnabled;
	}
	
	public void disablePoll() {
		log.info("DISABLING POLL ("+session.getUser().getName()+")");
		pollEnabled = false;
	}

	public void enablePoll() {
		log.info("ENABLING POLL ("+session.getUser().getName()+")");
		pollEnabled = true;
	}
	
	public Changeset getChangeset(Long id) {
		return padBean.getChangeset(id);
	}
	
	public Date getTime() {
		return DateUtils.now();
	}
}
