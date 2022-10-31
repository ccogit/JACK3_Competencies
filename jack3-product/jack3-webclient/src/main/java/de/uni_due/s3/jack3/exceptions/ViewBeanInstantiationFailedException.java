package de.uni_due.s3.jack3.exceptions;

public class ViewBeanInstantiationFailedException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -6606400419952355404L;

	public ViewBeanInstantiationFailedException() {
		super();
	}

	public ViewBeanInstantiationFailedException(String message) {
		super(message);
	}

	public ViewBeanInstantiationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
