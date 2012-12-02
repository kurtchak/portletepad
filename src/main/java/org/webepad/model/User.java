package org.webepad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webepad.utils.SessionComparator;
import org.webepad.utils.SessionComparator.Order;

/**
 * The persistent class for the User database table.
 * 
 */
public class User extends NamedTemporalEntity {
	private static final long serialVersionUID = 3464508119461977626L;
	private Map<Long,Session> padSessions = new HashMap<Long,Session>();
	private static SessionComparator comparator = new SessionComparator();
	
	public User() {
    }

	public User(String name) {
		setName(name);
	}

	public Map<Long,Session> getPadSessions() {
		return padSessions;
	}

	public List<Session> getSessions() {
		return new ArrayList<Session>(padSessions.values());
	}
	
	public void setPadSessions(Map<Long,Session> padSessions) {
		this.padSessions = padSessions;
	}
	
	public List<Session> getRecentSessions() {
		List<Session> sessions = getSessions();
		comparator.setSortingBy(Order.LastSeen);
		Collections.sort(sessions, comparator);
		return sessions;
	}
}