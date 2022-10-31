package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
public class VariableValue extends AbstractEntity {

	private static final long serialVersionUID = -3799455052887340921L;

	/**
	 * Stores the OpenObject XML representation of this variable value.
	 */
	@Column
	@Type(type = "text")
	private String content;

	public VariableValue() {
		super();
	}

	/**
	 * Returns the OpenObject representation of this variable value.
	 */
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
