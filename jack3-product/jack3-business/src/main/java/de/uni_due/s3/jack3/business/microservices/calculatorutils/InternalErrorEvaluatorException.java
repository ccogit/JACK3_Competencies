package de.uni_due.s3.jack3.business.microservices.calculatorutils;

public class InternalErrorEvaluatorException extends Exception {

	private static final long serialVersionUID = 2491112515197029210L;

	public InternalErrorEvaluatorException(String message) {
		super(message);
	}

	public InternalErrorEvaluatorException(String message, String exceptionType) {
		super(message);
	}

}
