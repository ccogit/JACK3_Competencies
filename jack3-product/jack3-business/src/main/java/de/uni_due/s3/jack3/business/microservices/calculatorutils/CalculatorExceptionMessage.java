package de.uni_due.s3.jack3.business.microservices.calculatorutils;

public class CalculatorExceptionMessage {

	private final String varName;
	private final String exceptionMessage;

	public CalculatorExceptionMessage(String varName, String exceptionMessage) {
		this.varName = varName;
		this.exceptionMessage = exceptionMessage;
	}

	public String getMessage() {
		return "Variable " + varName + " can not be calculated, due to : " + exceptionMessage;
	}

}
