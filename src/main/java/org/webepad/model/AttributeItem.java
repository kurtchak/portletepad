package org.webepad.model;


/**
 * The persistent class for the AttributePoolItem database table.
 * 
 */
public class AttributeItem extends BaseEntity {
	private static final long serialVersionUID = -2620088145479389056L;

	private Integer number;
	private Attribute attribute;
	private String value;
	private AttributePool attributePool;

    public AttributeItem() {
    }

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public AttributePool getAttributePool() {
		return this.attributePool;
	}

	public void setAttributePool(AttributePool attributePool) {
		this.attributePool = attributePool;
	}
	
}