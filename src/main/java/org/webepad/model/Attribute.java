package org.webepad.model;

/**
 * The persistent class for the AttributePoolItem database table.
 * 
 */
public class Attribute extends BaseEntity {
	private static final long serialVersionUID = -8477649571000348432L;

	private String name;
	
    public Attribute() {
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}