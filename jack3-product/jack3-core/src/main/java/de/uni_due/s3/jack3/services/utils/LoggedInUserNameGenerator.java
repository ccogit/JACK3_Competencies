package de.uni_due.s3.jack3.services.utils;

import org.hibernate.Session;
import org.hibernate.tuple.ValueGenerator;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Used by Hibernate to fill the updatedBy(username)-attribute of {@link AbstractEntity} on INSERT or UPDATE.
 * 
 * @author Benjamin Otto
 * @see LoggedInUserName
 */
public class LoggedInUserNameGenerator implements ValueGenerator<String> {

	@Override
	public String generateValue(Session session, Object owner) {
		return LoggedInUserName.get();
	}

}