package de.uni_due.s3.jack3.exceptions;

/**
 * This exception can be thrown anytime a user attemts to do something she isn't allowed to.
 *
 */
public class JackSecurityException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JackSecurityException() {
		super();
	}

	public JackSecurityException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JackSecurityException(String message, Throwable cause) {
		super(message, cause);
	}

	public JackSecurityException(String message) {
		super(message);
	}

	public JackSecurityException(Throwable cause) {
		super(cause);
	}

}
