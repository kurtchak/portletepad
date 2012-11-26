package org.webepad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.webepad.exceptions.NotFoundException;
import org.webepad.exceptions.UninitializedObjectException;

// TODO: vyhodit anotacie a zrevidovat hibernate mapping 
@Entity
@Table(name="AttributePool")
public class AttributePool extends BaseEntity {
	private static final long serialVersionUID = -4336772625064701606L;

	@OneToMany(mappedBy="attributePool")
	private List<AttributeItem> attributeItems;

	@OneToOne
	@JoinColumn(name="changesetId")
	private Changeset changeset;

	private Map<Integer, AttributeItem> attributeMap;
	
	public List<AttributeItem> getAttributeItems() {
		return attributeItems;
	}

	public void setAttributeItems(List<AttributeItem> attributeItems) {
		this.attributeItems = attributeItems;
		fillAttributeMap();
	}
	
	public Changeset getChangeset() {
		return changeset;
	}

	public void setChangeset(Changeset changeset) {
		this.changeset = changeset;
	}

	private void fillAttributeMap() {
		attributeMap = new HashMap<Integer, AttributeItem>();
		for (AttributeItem item : attributeItems) {
			attributeMap.put(item.getNumber(), item);
		}
	}
	
	public AttributeItem getAttribute(Integer number) throws NotFoundException, UninitializedObjectException {
		if (attributeMap == null) {
			throw new UninitializedObjectException();
		} else if (!attributeMap.containsKey(number)) {
			throw new NotFoundException();
		} else {
			return attributeMap.get(number);
		}
	}
}
