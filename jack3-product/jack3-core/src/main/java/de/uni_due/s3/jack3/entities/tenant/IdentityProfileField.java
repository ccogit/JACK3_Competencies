package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.envers.Audited;

/**
 * This class represents a profile field that was obtained during authentication from a security
 * identity's attributes.
 */
@NamedQuery(
		name=IdentityProfileField.GET_BY_NAME,
		query="SELECT f FROM IdentityProfileField f WHERE f.attributeName = :attributeName"
		)
@Entity
@Audited
public class IdentityProfileField extends ProfileField  {

	public static final String GET_BY_NAME = "IdentityProfileField.getByName";

	private static final long serialVersionUID = 3484636043403585142L;

	@Column(columnDefinition = "TEXT", nullable = false)
	@Unique
	private String attributeName;

	public IdentityProfileField() {
	}

	public IdentityProfileField(final String attributeName) {
		this.attributeName = Objects.requireNonNull(attributeName);
		this.name = attributeName;
	}

	@Override
	public String toString() {
		return attributeName;
	}
}
