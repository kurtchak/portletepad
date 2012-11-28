package org.webepad.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.webepad.dao.PadDAO;
import org.webepad.dao.hibernate.HibernateDAOFactory;
import org.webepad.exceptions.NotFoundException;
import org.webepad.utils.DateUtils;
import org.webepad.utils.PadColorPalette;

/**
 * The persistent class for the Pad database table.
 * 
 */
public class Pad extends NamedTemporalEntity {
	private static final long serialVersionUID = -8430657959366144619L;

	private PadDAO padDAO;
	private List<Changeset> changesets;
	private Map<Long,Session> userSessions;
	private Map<String,Boolean> usedColors;
	private PadColorPalette colorUtils;
	private Boolean readOnly;
	
	public Pad() {
		padDAO = HibernateDAOFactory.getInstance().getPadDAO();
		log = LoggerFactory.getLogger(Pad.class);
		colorUtils = PadColorPalette.getInstance();
		usedColors = new HashMap<String, Boolean>();
		userSessions = new HashMap<Long, Session>();
		changesets = new ArrayList<Changeset>();
		readOnly = false;
		
		for (String color : colorUtils.getColors()) {
			usedColors.put(color, false);
		}
	}
	
	public void updateUsedColors() {
		for (Session s : userSessions.values()) {
			if (usedColors.containsKey(s.getColorCode())) {
				usedColors.put(s.getColorCode(),true);
			}
		}
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
	
	// Data manipulation methods
	public void save() {
		padDAO.insert(this);
	}
	
	public void update() {
		padDAO.update(this);
	}

	private Session openNewSession(User user) {
		Session session = new Session(user, this);//	public String lockPad() {
//		padBean.lockSelectedPad();
//		return "refresh";
//	}
//

		session.save();
		userSessions.put(user.getId(),session);
		return session;
	}
	
	/**
	 * Tries to find existing session for the user. if not found, opens new session
	 * 
	 * @param user
	 * @return session
	 */
	public Session openSession(User user) {
		Session session;
		if (userSessions.containsKey(user.getId())) {
			session = userSessions.get(user.getId());
		} else {
			session = openNewSession(user);
		}
		session.open();
		updateUsedColors();
		return session;
	}
	
	public Session openSession(Session session) {
		session.open();
		return session;
	}
	
	public void closeSession(Session session) throws NotFoundException {
		if (session == null) {
			throw new NotFoundException();
		} else {
			session.close();
		}
	}
	
	// TEMPORARY IMPLEMENTATION
	public String notUsedColor() {
		updateUsedColors();
		for (String color : usedColors.keySet()) {
			if (!usedColors.get(color)) {
				usedColors.put(color, true);
				return color;
			}
		}
		return "#000000";
	}
	
	// append new localy added changeset to the pad
	public void appendChangeset(Changeset changeset) {
		changeset.setNumber(nextRevisionNumber());
		this.changesets.add(changeset);
//		changeset.printCompact();
		changeset.save();
	}
	
	// append remotely added changesets to the pad
	public void appendRemoteChangesets(List<Changeset> changesets) {
		this.changesets.addAll(changesets);
	}
	
	// append remotely added changeset to the pad
	public void appendRemoteChangeset(Changeset changeset) {
		this.changesets.add(changeset);
		log.info(changeset.toCompactString()); // flag for NOT SAVE PROBABLY NEEDED...
	}
	
	private int nextRevisionNumber() {
		Changeset c = getLatestChangeset();
		if (c != null) {
			return c.getNumber()+1;
		} else {
			return 1;
		}
	}

	private Changeset getLatestChangeset() {
		if (!changesets.isEmpty()) {
			return changesets.get(changesets.size()-1);
		} else {
			return null;
		}
	}
	
	public Date getModified() {
		Changeset latest = getLatestChangeset();
		if (latest != null) {
			return latest.getCreated();
		} else {
			return getCreated();
		}
	}
	
	public User getLastModifier() {
		Changeset latest = getLatestChangeset();
		if (latest != null) {
			return latest.getAuthor();
		} else {
			return getCreator();
		}
	}
	
	public String getShortCreated() {
		return DateUtils.getShortDate(getCreated());
	}

	public String getShortModified() {
		return DateUtils.getShortDate(getModified());
	}
	
	public int getLastRevisionNumber() {
		if (changesets.size() > 0) {
			return changesets.get(changesets.size()-1).getNumber();
		} else {
			return 0;
		}
	}

	public void updateChangesets(List<Changeset> changesets) {
		setChangesets(changesets);
	}
	
	public void takeColor(String color) {
		usedColors.put(color, true);
	}
	
	public void freeColor(String color) {
		usedColors.put(color, false);
	}
	
	public List<String> getFreeColors() {
		updateUsedColors();
		ArrayList<String> freeColors = new ArrayList<String>();
		for (String color : usedColors.keySet()) {
			if (usedColors.get(color).booleanValue() == false) {
				freeColors.add(color);
			}
		}
		return freeColors;
	}

	public void setReadOnly(Boolean ro) {
		readOnly = ro;
	}
	
	public Boolean getReadOnly() {
		return readOnly == null ? false : readOnly;
	}
	
	public Session getSessionByColor(String code) {
		for (Session s : userSessions.values()) {
			if (s.getColorCode().equals(code)) {
				return s;
			}
		}
		return null;
	}
}