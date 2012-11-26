package org.webepad.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The persistent class for the User database table.
 * 
 */
public class User extends NamedTemporalEntity {
	private static final long serialVersionUID = 3464508119461977626L;

	private String externId;
	private Map<Long,Session> padSessions;

	public User() {
    }

	public User(String name) {
		setName(name);
	}

	public String getExternId() {
		return externId;
	}

	public void setExternId(String externId) {
		this.externId = externId;
	}

	public Map<Long,Session> getPadSessions() {
		return padSessions;
	}

	public List<Session> getSessions() {
		if (padSessions != null) {
			return new ArrayList<Session>(padSessions.values());
		}
		return null;
	}
	
	public void setPadSessions(Map<Long,Session> padSessions) {
		this.padSessions = padSessions;
	}
}