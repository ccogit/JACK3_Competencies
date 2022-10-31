package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.MapKeyColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

// REVIEW lg - Do we still need this class after bz's LDAP work?
/**
 * This class represents a profile field whose contents gets requested from a LDAP query.
 */
@Audited
@Entity
public class LDAPProfileField extends AbstractEntity{

	private static final long serialVersionUID = -1455223481700633630L;

	//TODO: What happens if query fails? Where do we need error messages?
	@Column
	@Type(type = "text")
	String ldapQuery;

	@ElementCollection
	@Type(type = "text")
	@MapKeyColumn(length = 10485760)
	private Map<String, String> defaultContent = new HashMap<String, String>();

	/*
	 * @return unmodifiableMap of defaultContent
	 */
	public Map<String, String> getDefaultContent() {
		return Collections.unmodifiableMap(defaultContent);
	}


	public LDAPProfileField() {
	}
}
