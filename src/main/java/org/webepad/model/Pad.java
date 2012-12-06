package org.webepad.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webepad.beans.PushBean;
import org.webepad.control.PadAssembler;
import org.webepad.control.PadContent;
import org.webepad.exceptions.NotFoundException;
import org.webepad.utils.DateUtils;
import org.webepad.utils.ExceptionHandler;
import org.webepad.utils.PadColorPalette;

/**
 * The persistent class for the Pad database table.
 * 
 */
public class Pad extends NamedTemporalEntity {
	private static final long serialVersionUID = -8430657959366144619L;
	private Logger log = LoggerFactory.getLogger(Pad.class);
	
	private static LinkedList<String> freeColors = PadColorPalette.getColors();
	private static Map<Long,Map<Long,Session>> activeSessions = new HashMap<Long,Map<Long,Session>>();
	
	private List<Changeset> changesets = new ArrayList<Changeset>();
	private Map<Long,Session> userSessions = new HashMap<Long, Session>();
	private Boolean readOnly = false;
	private PadAssembler padAssembler = new PadAssembler();
	private PushBean pushBean;

	public Pad() {
	}
	
	public List<Changeset> getChangesets() {
		return changesets;
	}

	public void setChangesets(List<Changeset> changesets) {
		this.changesets = changesets;
	}

	public Map<Long,Session> getUserSessions() {
		return userSessions;
	}

	public void setUserSessions(Map<Long,Session> userSessions) {
		this.userSessions = userSessions;
	}
	
	private synchronized Session openNewSession(User user) throws Exception {
		Session session = new Session(user, this);//	public String lockPad() {
		session.save();
		return session;
	}
	
	/**
	 * Tries to find existing session for the user. if not found, opens new session
	 * 
	 * @param user
	 * @return session
	 */
	public synchronized Session openSession(User user) {
		Session session = null;
		try {
			if (userSessions.containsKey(user.getId())) {
				session = userSessions.get(user.getId());
			} else {
				session = openNewSession(user);
			}
			session.open(pushBean);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		return session;
	}
	
	public synchronized Session openSession(Session session) {
		session.open(pushBean);
		return session;
	}
	
	public synchronized void closeSession(Session session) throws NotFoundException {
		if (session == null) {
			throw new NotFoundException();
		} else {
			session.close();
		}
	}
	
	// append new localy added changeset to the pad
	public synchronized PadContent appendChangeset(Changeset changeset) throws Exception {
		changeset.setNumber(nextRevisionNumber());
		this.changesets.add(changeset);
//		changeset.printCompact();
		changeset.save();
		return padAssembler.applyLocalChangeset(changeset);
	}

//	// append remotely added changesets to the pad
//	public void appendRemoteChangesets(List<Changeset> changesets) {
//		this.changesets.addAll(changesets);
//	}
//	
//	// append remotely added changeset to the pad
//	public void appendRemoteChangeset(Changeset changeset) {
//		this.changesets.add(changeset);
//		log.info(changeset.toCompactString()); // flag for NOT SAVE PROBABLY NEEDED...
//	}
//	
	private synchronized int nextRevisionNumber() {
		Changeset c = getLatestChangeset();
		if (c != null) {
			return c.getNumber()+1;
		} else {
			return 1;
		}
	}

	private synchronized Changeset getLatestChangeset() {
		if (!changesets.isEmpty()) {
			return changesets.get(changesets.size()-1);
		} else {
			return null;
		}
	}
	
	public synchronized Date getModified() {
		Changeset latest = getLatestChangeset();
		if (latest != null) {
			return latest.getCreated();
		} else {
			return getCreated();
		}
	}
	
	public synchronized User getLastModifier() {
		Changeset latest = getLatestChangeset();
		if (latest != null) {
			return latest.getAuthor();
		} else {
			return getCreator();
		}
	}
	
	public synchronized String getShortCreated() {
		return DateUtils.getShortDate(getCreated());
	}

	public synchronized String getShortModified() {
		return DateUtils.getShortDate(getModified());
	}
	
	public synchronized int getLastRevisionNumber() {
		if (changesets.size() > 0) {
			return changesets.get(changesets.size()-1).getNumber();
		} else {
			return 0;
		}
	}

	public void updateChangesets(List<Changeset> changesets) {
		setChangesets(changesets);
	}
	
	public synchronized String getFreeColor() throws Exception {
		if (freeColors.isEmpty()) {
			throw new Exception("Out of free colors.");
		}
		return freeColors.pop();
	}
	
	public List<String> getFreeColors() {
		return freeColors;
	}

	public void setReadOnly(Boolean ro) {
		readOnly = ro;
	}
	
	public Boolean getReadOnly() {
		return readOnly == null ? false : readOnly;
	}
	
	public PushBean getPushBean() {
		return pushBean;
	}

	public void setPushBean(PushBean pushBean) {
		this.pushBean = pushBean;
	}

	public Session getSessionByColor(String code) {
		for (Session s : userSessions.values()) {
			if (s.getColorCode().equals(code)) {
				return s;
			}
		}
		return null;
	}
	
	public synchronized void addActiveSession(Session session) {
		Long padId = session.getPad().getId();
		Long userId = session.getUser().getId();
		if (!activeSessions.containsKey(padId) || activeSessions.get(padId) == null) {
			Map<Long,Session> activeUserSessions = new HashMap<Long,Session>();
			activeSessions.put(padId, activeUserSessions);
		}
		activeSessions.get(padId).put(userId, session);
		log.debug("ADDED COLLAB ACTIVE SESSION: "+session.getUser().getName());
		log.debug("ACTIVE PAD'S SESSIONS: "+activeSessions.get(padId).size());
	}
	
	public synchronized void removeActiveSession(Session session) {
		Long padId = session.getPad().getId();
		Long userId = session.getUser().getId();
		if (activeSessions.containsKey(padId) && activeSessions.get(padId) != null) {
			activeSessions.get(padId).remove(userId);
		}
		log.debug("REMOVED COLLAB ACTIVE SESSION: "+session.getUser().getName());
		log.debug("ACTIVE PAD'S SESSIONS: "+activeSessions.get(padId).size());
	}
	
	public Collection<Session> getActivePadSessions() {
		if (activeSessions.containsKey(getId())) {
			return activeSessions.get(getId()).values();
		}
		return null;
	}

	public Collection<Session> getOtherActivePadSessions(Session session) {
		Collection<Session> sessions = new ArrayList<Session>();
		for (Session s : getActivePadSessions()) {
			if (!s.equals(session)) {
				sessions.add(s);
			}
		}
		return sessions;
	}
	
	public String getContent() {
		log.info("getContent [Pad:"+this.hashCode()+"]:");
		try {
			padAssembler.parseContent(this);
		} catch (Exception e) {
			ExceptionHandler.handle(e);
		}
		log.info(padAssembler.getTextContent());
		return padAssembler.getTextContent();
	}

	public List<Changeset> getChangesetsFrom(Changeset lastKnownChangeset) {
		return changesets.subList(changesets.indexOf(lastKnownChangeset),changesets.size()-1);
	}

	public void reloadPadContent() {
		padAssembler.refreshTextContent();
	}
}