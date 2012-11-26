package org.webepad.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseEntity implements Serializable {
	private static final long serialVersionUID = 1251197114011081810L;

	protected Logger log = LoggerFactory.getLogger(BaseEntity.class);
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@Override
	public boolean equals(Object obj) {
//		log.info(this.getId() + ":" + ((BaseEntity) obj).getId());
		if (!(obj instanceof BaseEntity)) {
			return false;
		} else {
			return (this.getId().longValue() == ((BaseEntity) obj).getId().longValue());
		}
	}
}
