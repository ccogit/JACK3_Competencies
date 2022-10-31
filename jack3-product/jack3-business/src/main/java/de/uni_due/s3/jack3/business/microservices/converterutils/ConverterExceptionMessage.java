package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterExceptionMessage {

	private final Placeholder placeholder;
	private final String exceptionMessage;

	public ConverterExceptionMessage(Placeholder placeholder, String exceptionMessage) {
		this.placeholder = placeholder;
		this.exceptionMessage = exceptionMessage;
	}

	public String getMessage() {
		return "Variable " + placeholder.getWholeRegex() + " could not be converted, due to : " + exceptionMessage;
	}

}
