package de.uni_due.s3.jack3.business.exceptions;

/**
 * Indicates that a password entered by a user is required to perform this action.
 */
public class PasswordRequiredException extends Exception {

	private static final long serialVersionUID = -7422785843818664532L;

	public PasswordRequiredException() {
		super();
	}

}
