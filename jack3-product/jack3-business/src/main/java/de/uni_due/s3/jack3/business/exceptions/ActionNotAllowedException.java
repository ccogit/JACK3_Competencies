package de.uni_due.s3.jack3.business.exceptions;

/**
 * This generic Exception indicates that an action performed by a user was not allowed.
 */
public class ActionNotAllowedException extends Exception {

	private static final long serialVersionUID = -6045214555437563328L;

	public ActionNotAllowedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ActionNotAllowedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActionNotAllowedException(String message) {
		super(message);
	}

	public ActionNotAllowedException(Throwable cause) {
		super(cause);
	}

	public ActionNotAllowedException() {
		super();
	}

}
