package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQuery;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Abstract base class for user profile fields.
 */
	@NamedQuery(
		name = ProfileField.GET_ALL,
		query = "SELECT p FROM ProfileField p")
	@NamedQuery(
		name = ProfileField.GET_ALL_PUBLIC,
		query = "SELECT p FROM ProfileField p WHERE p.isPublic = true")
	@NamedQuery(
		name = ProfileField.GET_ALL_MANDATORY,
		query = "SELECT p FROM ProfileField p WHERE p.isMandatory = true")

@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ProfileField extends AbstractEntity {

	private static final long serialVersionUID = -8775320160261067477L;

	public static final String GET_ALL = "ProfileField.getAll";

	public static final String GET_ALL_PUBLIC = "ProfileField.getAllPublic";

	public static final String GET_ALL_MANDATORY = "ProfileField.getAllMandatory";

	@ToString
	@Column
	protected String name;

	@Column(columnDefinition = "boolean default true")
	private boolean isPublic = true;

	@Column
	private boolean isMandatory;

	@Column
	private int fieldOrder;

	public ProfileField() {
		super();
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultContent() {
		return "";
	}
}
