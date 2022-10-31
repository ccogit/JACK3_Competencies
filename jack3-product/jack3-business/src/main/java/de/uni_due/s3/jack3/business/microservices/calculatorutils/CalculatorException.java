package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import java.util.List;
import java.util.stream.Collectors;

public class CalculatorException extends Exception {

	private static final long serialVersionUID = 2491112515197029210L;

	public CalculatorException(List<CalculatorExceptionMessage> calculatorExceptions) {
		super(convertToMessage(calculatorExceptions));
	}

	private static String convertToMessage(List<CalculatorExceptionMessage> exceptions) {
		return exceptions.stream().map(CalculatorExceptionMessage::getMessage).collect(Collectors.joining(";\n"));
	}

}
