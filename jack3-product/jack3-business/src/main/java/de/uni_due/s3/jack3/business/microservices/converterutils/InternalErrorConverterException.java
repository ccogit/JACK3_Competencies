package de.uni_due.s3.jack3.business.microservices.converterutils;

public class InternalErrorConverterException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 2491112515197029210L;

	private String exceptionType;

	public InternalErrorConverterException(String message, String exceptionType) {
		super(message);
		this.exceptionType = exceptionType;
	}

	public String getExceptionType() {
		return exceptionType;
	}
}
