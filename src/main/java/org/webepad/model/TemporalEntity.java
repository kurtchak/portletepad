package org.webepad.model;

import java.util.Date;

public abstract class TemporalEntity extends BaseEntity {
	private static final long serialVersionUID = 7661268466776663807L;

	private Date created;

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	private User user;

	public User getCreator() {
		return user;
	}

	public void setCreator(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
