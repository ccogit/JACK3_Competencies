package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResponse;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResult;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResultType;

public class CalculatorResponseBooleanizeHandler {

	private final CalculatorResponse response;
	private final List<CalculatorExceptionMessage> errors = new ArrayList<>();
	private final Map<String, Boolean> results = new HashMap<>();

	private CalculatorResponseBooleanizeHandler(CalculatorResponse response) {
		this.response = response;
	}

	public static Map<String, Boolean> convertToBooleanizedMap(CalculatorResponse response)
			throws InternalErrorEvaluatorException {
		try {
			return new CalculatorResponseBooleanizeHandler(response).convertToBooleanizedMap();
		} catch (CalculatorException e) {
			throw new InternalErrorEvaluatorException(e.getMessage());
		}
	}

	private Map<String, Boolean> convertToBooleanizedMap() throws CalculatorException {
		response.getResults().forEach(this::handleResult);
		if (!errors.isEmpty())
			throw new CalculatorException(errors);
		else
			return results;
	}

	private void handleResult(CalculatorResult result) {
		if (CalculatorResultType.BOOLEAN.equals(result.type)) {
			results.put(getNameOf(result), getBooleanizedOf(result));
		} else if (CalculatorResultType.EXCEPTION.equals(result.type)) {
			errors.add(new CalculatorExceptionMessage(result.properties.name, result.content.exceptionMessage));
		} else {
			errors.add(new CalculatorExceptionMessage(result.properties.name, "Evaluation has wrong type!"));
		}
	}

	private static String getNameOf(CalculatorResult result) {
		return result.properties.name;
	}

	private static boolean getBooleanizedOf(CalculatorResult result) {
		return result.content.booleanResult;
	}

}
