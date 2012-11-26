package org.webepad.model;


public abstract class NamedTemporalEntity extends TemporalEntity {
	private static final long serialVersionUID = -9011321942999756396L;

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
