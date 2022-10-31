package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResponse;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResult;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResultContent;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResultType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;

class CalculatorResponseBooleanizeHandlerTest {

	@Test
	void taskMatchingChemVar() throws Exception {
		CalculatorResponse response = new CalculatorResponse();

		response.addResult(new CalculatorResult( //
				new CalculatorResultContent(true), //
				CalculatorResultType.BOOLEAN, //
				new EvaluatorProperties("varName1", EvaluatorVariableType.VAR, EvaluatorDomainType.CHEMISTRY)));

		Map<String, Boolean> map = CalculatorResponseBooleanizeHandler.convertToBooleanizedMap(response);

		assertEquals(1, map.size());
		assertTrue(map.containsKey("varName1"));
		assertTrue(map.get("varName1"));
	}

	@Test
	void taskMatchingMathVar() throws Exception {
		CalculatorResponse response = new CalculatorResponse();

		response.addResult(new CalculatorResult( //
				new CalculatorResultContent(false), //
				CalculatorResultType.BOOLEAN, //
				new EvaluatorProperties("varName1", EvaluatorVariableType.VAR, EvaluatorDomainType.MATHEMATICS)));

		Map<String, Boolean> map = CalculatorResponseBooleanizeHandler.convertToBooleanizedMap(response);

		assertEquals(1, map.size());
		assertTrue(map.containsKey("varName1"));
		assertFalse(map.get("varName1"));
	}

}
