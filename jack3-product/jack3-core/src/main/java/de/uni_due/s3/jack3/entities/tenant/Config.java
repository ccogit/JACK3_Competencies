package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
@NamedQuery(name = Config.GET_BY_KEY, query = "SELECT c FROM Config c WHERE c.key = :key")
public class Config extends AbstractEntity {

	private static final long serialVersionUID = -6926830097175929206L;

	public Config(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public Config() {
	}

	public static final String GET_BY_KEY = "Config.getByKey";

	@ToString
	@Column(unique = true)
	@Type(type = "text")
	private String key;

	@ToString
	@Column
	@Type(type = "text")
	private String value;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}