package org.webepad.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.richfaces.application.push.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.control.PadAssembler;
import org.webepad.control.TextSlice;
import org.webepad.dao.SessionDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.exceptions.NoSuchPadException;
import org.webepad.exceptions.NoSuchUserException;
import org.webepad.exceptions.UninitializedObjectException;
import org.webepad.model.Changeset;
import org.webepad.model.Pad;
import org.webepad.model.Session;
import org.webepad.model.User;
import org.webepad.utils.DateUtils;
import org.webepad.utils.ExceptionHandler;

@ManagedBean(name = "sessionBean")
@SessionScoped
public class SessionBean {

	private static final String JOIN = "J";
	private static final String JOIN_RESPONSE = "R";
	private static final String LEAVE = "L";
	private static final String CHANGESET = "C";
	private static final String USERCOLOR = "UC";

	private Logger log = LoggerFactory.getLogger(SessionBean.class);
	private SessionDAO sessionDAO = HibernateDAOFactory.getInstance().getSessionDAO();

	private List<String> receivedChangesetHashCodes = new ArrayList<String>();
	
	@ManagedProperty(value="#{padBean}")
	private PadBean padBean;
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	@ManagedProperty(value="#{pushBean}")
	private PushBean pushBean;

	private Session session;
	private String number;
	private String changeset;
	
	private LinkedList<String> remoteChangesetsQueue = new LinkedList<String>();
	private List<Session> activeSessions = new ArrayList<Session>();
	
	//////////////////////////////////////////////
	// CONSTRUCTOR
	public SessionBean() {
	}
	
	//////////////////////////////////////////////
	// LOADING OF THE MAIN OBJECT FOR THE VIEW
	public void loadSession(Session session) {
		try {
			this.session = session;
			if (session != null) {
				session.startListener(this);
				pushBean.initializeTopic(session);
			} else {
				throw new UninitializedObjectException();
			}
		} catch (UninitializedObjectException e) {
			ExceptionHandler.handle(e);
		}
	}

	public void loadSession() {
		session = new Session();
	}

	///////////////////////////////////////
	// RETRIEVAL FROM DB
	public Session loadSession(Long id) {
		return sessionDAO.getSession(id);
	}

	public List<Session> getSessions() {
		return sessionDAO.readSessions();
	}

	private Session findSession(Long padId, Long userId) {
		return sessionDAO.findSession(padId, userId);
	}

	//////////////////////////////////////////////
	// ACTIVE SESSIONS
	public List<Session> getActiveSessions() {
		return activeSessions;
	}

	public void addActiveSession(Session session) {
		if (!activeSessions.contains(session)) {
			activeSessions.add(session);
		}
	}

	public void removeActiveSession(Session session) {
		activeSessions.remove(session);
	}

	//////////////////////////////////////////////
	// VALUE LISTENER FOR AJAX PUSH COMPONENT
	public void remoteUserChange(String action, Long padId, Long userId, Integer number, Integer spanId, Integer spanPos, String charbank, Integer leftId, Integer rightId, Integer offset, String fgColor, String prevFgColor, String oper, String hashCode) throws Exception {
		User user = userBean.getUser(userId);
		Pad pad = padBean.getPad(padId);
		if (user == null) {
			throw new NoSuchUserException();
		} else if (pad == null) {
			throw new NoSuchPadException();
		} else {
			Session session = findSession(padId, userId);
			if (JOIN.equals(action) && !activeSessions.contains(session)) {
				addActiveSession(session);
				sendResponse(padId,userId);
			} else if (JOIN_RESPONSE.equals(action) && !activeSessions.contains(session)) {
				addActiveSession(session);
			} else if (LEAVE.equals(action)) {
				removeActiveSession(session);
			} else if (CHANGESET.equals(action)) {
				if (!receivedChangesetHashCodes.contains(hashCode)) {
					receivedChangesetHashCodes.add(hashCode);
					log.info("REMOTE CHANGESET ADDED: "+charbank+"|"+spanId);
					int operation = "W".equals(oper) ? Changeset.WRITE : "D".equals(oper) ? Changeset.DELETE : Changeset.ATTR_CHANGE;
					processRemoteChangeOnServer(spanId, spanPos, charbank, leftId, rightId, session, operation, offset);
					refreshPadContent();
					// TODO: replace with better solution
					String remoteChangeString = generateRemoteChangeString(action, spanId, spanPos, charbank, leftId, rightId, offset, fgColor, oper);
					if (remoteChangesetsQueue.isEmpty()) {
						processToView(remoteChangeString);
					} else {
						publishToQueue(remoteChangeString);
					}
				} else {
					throw new Exception("Received duplicated changeset info message: "+number.toString());
				}
			} else if (USERCOLOR.equals(action)) {
				activeSessions.get(activeSessions.indexOf(session)).setColorCode(fgColor);
				processToView("UC:"+prevFgColor+"to"+fgColor);
			}
		}
	}

	private void processRemoteChangeOnServer(int spanId, int spanPos, String charbank, int leftId, int rightId, Session session, int oper, Integer offset) throws Exception {		
		PadAssembler padAssembler = this.session.getPadAssembler();
		padAssembler.applyRemoteChangeset(spanId, spanPos, charbank, leftId, rightId, offset, session, oper);
	}

	private void publishToQueue(String receivedString) {
//		TODO: AGGREGATE SUBSEQUENT CHANGES INTO ONE MESSAGE
//		if (!remoteChangesetsQueue.isEmpty()) {
//			String lastReceived = remoteChangesetsQueue.getLast();
//			Matcher mLast = Session.CHANGESET_MSG_PATTERN.matcher(lastReceived);
//			Matcher mActual = Session.CHANGESET_MSG_PATTERN.matcher(receivedString);
//			if (mLast.find() && mActual.find() && mLast.group(group)
//		}
		log.info("Adding change to queue: "+receivedString);
		remoteChangesetsQueue.addLast(receivedString);
	}

	private void processToView(String info) throws MessageException {
		log.info("processToView");
		pushBean.sendMessage(info);
	}
	
	private String generateRemoteChangeString(String action, int spanId, int spanPos, String charbank, int leftId, int rightId, Integer offset, String fgColor, String oper) {
		StringBuilder sb = new StringBuilder();
		// IDENTIFICATION OF SENDER
		sb.append("@[p"+session.getPad().getId()+"u"+session.getUser().getId()+"]");
		
		sb.append(action).append(":");
		sb.append(oper).append(":");
		sb.append(spanId).append(":");
		sb.append(spanPos).append(":");
		sb.append(charbank == null ? "" : charbank).append(":");
		sb.append(leftId).append(":");
		sb.append(rightId).append(":");
		sb.append(offset).append(":");
		sb.append(fgColor);
		return sb.toString();
	}

	/**
	 * Sending join response to opposite session
	 * @param toPadId
	 * @param toUserId
	 */
	private void sendResponse(Long toPadId, Long toUserId) {
		session.sendReponseMessage(toPadId, toUserId);
	}

	/**
	 * Reloads the pad content from DB and builds the editor content
	 */
	private void reloadPad() {
		rebuildPadContent(); // aware of manual removal of data in DB
//		refreshPadContent(); // aware just of changes made by application
//		Pad pad = padBean.getPad(session.getPad().getId());
//		session.setPad(pad);
//		session.buildPadContent();
	}

	/**
	 * Refreshes the pad loading the changeset added by remote users
	 */
	private void refreshPadContent() {
		List<Changeset> changesets = padBean.retrieveNewChangesets();
		session.updatePad(changesets);
	}
	
	/**
	 * Refreshes the pad loading the changeset added by remote users
	 */
	private void rebuildPadContent() {
		// TODO:FMI: Be aware of caching of DB objects -> lazy="true"
		Pad pad = padBean.getPad(session.getPad().getId());
//		pad.updateChangesets(padBean.readChangesets(pad));
		session.setPad(pad);
		session.buildPadContent();
	}
	
	/**
	 * Server side of new changeset addition 
	 */
	public void addChangeset() {
		log.info("SUBMITTED CHANGESET: " + changeset);
		Changeset c = new Changeset();
		c.setRule(changeset.substring(0, changeset.indexOf('$')));
		if (c.getAction() == Changeset.NOCHANGE) {
			return;
		} else {
			c.setCharbank(changeset.substring(changeset.indexOf('$') + 1));
			c.setCreated(DateUtils.now());
			c.setAttributePool(null);
			session.addChangeset(c);
		}
	}

	public void processNext() {
		try {
			if (!remoteChangesetsQueue.isEmpty()) {
				log.info("Popping from the queue: "+remoteChangesetsQueue.peek());
				processToView(remoteChangesetsQueue.pop());
			} else {
				log.info("Queue is empty: "+remoteChangesetsQueue.peek());
			}
		} catch (MessageException e) {
			ExceptionHandler.handle(e);
		}
	}

	///////////////////////////////////////
	// ACTIONS
	public String actionLeave() {
		if (session != null) {
			session.close();
			activeSessions.clear();
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

	public PushBean getPushBean() {
		return pushBean;
	}

	public void setPushBean(PushBean pushBean) {
		this.pushBean = pushBean;
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
}
