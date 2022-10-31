package de.uni_due.s3.jack3.business.microservices.converterutils;

import java.util.List;
import java.util.stream.Collectors;

public class ConverterException extends Exception {

	private static final long serialVersionUID = 3667557795602520638L;

	public ConverterException(List<ConverterExceptionMessage> exceptions) {
		super(convertToMessage(exceptions));
	}

	public ConverterException(String message) {
		super(message);
	}

	private static String convertToMessage(List<ConverterExceptionMessage> exceptions) {
		return exceptions.stream().map(ConverterExceptionMessage::getMessage).collect(Collectors.joining(";\n"));
	}

}