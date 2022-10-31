package de.uni_due.s3.jack3.exceptions;

import java.util.NoSuchElementException;

/**
 * This exception indicates that a service didn't find an entity, but the business-logic has asserted that this entity
 * has to be in the database.
 *
 * We inherit from java.util.NoSuchElementException (which is a RuntimeException) so we can use Java11`s
 * Optional.orElseThrow() (without parameters) and just catch NoSuchElementException to catch both in higher tier code.
 *
 * @author Benjamin.Otto
 */
public class NoSuchJackEntityException extends NoSuchElementException {

	private static final long serialVersionUID = -5088480254886234059L;

	public NoSuchJackEntityException() {
		super();
	}

	public NoSuchJackEntityException(String message) {
		super(message);
	}
}
