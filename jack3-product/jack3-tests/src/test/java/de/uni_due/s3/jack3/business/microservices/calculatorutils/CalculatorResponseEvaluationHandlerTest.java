package de.uni_due.s3.jack3.business.microservices.calculatorutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectFactory;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.openobject.OpenObject;

class CalculatorResponseEvaluationHandlerTest {

	private OpenObject ooChemInt1 = OpenObjectFactory.createOpenObjectForOpenChemInteger(1);
	private OpenObject ooMathInt1 = OpenObjectFactory.createOpenObjectForOpenMathInteger(1);

	@Test
	void taskMatchingChemVar() throws Exception {
		CalculatorResponse response = new CalculatorResponse();

		response.addResult(new CalculatorResult( //
				new CalculatorResultContent(ooChemInt1), //
				CalculatorResultType.EVALUATED, //
				new EvaluatorProperties("varName1", EvaluatorVariableType.VAR, EvaluatorDomainType.CHEMISTRY)));

		Map<String, VariableValue> map = CalculatorResponseEvaluationHandler.convertToEvaluatedMap(response);

		assertEquals(1, map.size());
		assertTrue(map.containsKey("varName1"));
		assertEquals(VariableValueFactory.createVariableValue(ooChemInt1).getContent(),
				map.get("varName1").getContent());
	}

	@Test
	void taskMatchingMathVar() throws Exception {
		CalculatorResponse response = new CalculatorResponse();

		response.addResult(new CalculatorResult( //
				new CalculatorResultContent(ooMathInt1), //
				CalculatorResultType.EVALUATED, //
				new EvaluatorProperties("varName1", EvaluatorVariableType.VAR, EvaluatorDomainType.MATHEMATICS)));

		Map<String, VariableValue> map = CalculatorResponseEvaluationHandler.convertToEvaluatedMap(response);

		assertEquals(1, map.size());
		assertTrue(map.containsKey("varName1"));
		assertEquals(VariableValueFactory.createVariableValue(ooMathInt1).getContent(),
				map.get("varName1").getContent());
	}

}
