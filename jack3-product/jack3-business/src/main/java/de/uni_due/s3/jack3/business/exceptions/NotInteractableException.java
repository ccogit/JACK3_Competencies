package de.uni_due.s3.jack3.business.exceptions;

/**
 * This exception is thrown when an action is executed but was not allowed because the user is not allowed to interact
 * with the corresponding object e.g. if an enrollment action is performed but the course offer is not visible to the
 * user.
 */
public class NotInteractableException extends ActionNotAllowedException {

	private static final long serialVersionUID = -2136574126401935615L;
	
	public NotInteractableException() {
		super();
	}

}
