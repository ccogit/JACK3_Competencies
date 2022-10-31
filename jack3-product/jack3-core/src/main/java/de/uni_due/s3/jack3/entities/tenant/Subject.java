package de.uni_due.s3.jack3.entities.tenant;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * Representation of a subject to which courses and exercises are assigned.
 */

@Audited
@Entity
@XStreamAlias("Subject")
public class Subject extends AbstractEntity implements Serializable {

	private static final long serialVersionUID = -4498420848759767490L;

	@Column
	@Type(type = "text")
	private String name;

	public Subject() {
	}

	public Subject(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = requireIdentifier(name, "You must provide a non-empty name.");
	}

	@Override public String toString() {
		return name;
	}

}
