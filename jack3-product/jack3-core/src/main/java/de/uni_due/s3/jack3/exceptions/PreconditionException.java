package de.uni_due.s3.jack3.exceptions;

/**
 * This exception indicates that a precondition for an Action was not fulfilled (see also #569).
 *
 * @author Kilian.Kraus
 */

public class PreconditionException extends RuntimeException {

	private static final long serialVersionUID = -5088480254886234059L;

	public PreconditionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PreconditionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PreconditionException(String message) {
		super(message);
	}

	public PreconditionException(Throwable cause) {
		super(cause);
	}

}
