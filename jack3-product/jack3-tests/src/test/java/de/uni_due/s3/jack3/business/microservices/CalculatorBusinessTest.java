package de.uni_due.s3.jack3.business.microservices;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;

@NeedsEureka
class CalculatorBusinessTest extends AbstractTest {

	@Inject
	private CalculatorBusiness evaluator;

	private EvaluatorMaps maps = new EvaluatorMaps();

	@BeforeEach
	void setUp() throws Exception {
		maps.getExerciseVariableMap().put("var1", VariableValueFactory.createVariableValueForOpenMathInteger(3));
		maps.getInputVariableMap().put("field1", VariableValueFactory.createVariableValueForOpenChemString("Hello"));
	}

	@Test
	void resultIsTrue_forTrueEvaluatorExpression() throws Exception {
		EvaluatorExpression expr = createEvaluatorExpression("true()", EDomain.MATH);
		boolean actual = evaluator.calculateToBoolean(expr, maps);
		assertEquals(true, actual);
	}

	@Test
	void resultIsFalse_forFalseEvaluatorExpression() throws Exception {
		EvaluatorExpression expr = createEvaluatorExpression("false()", EDomain.MATH);
		boolean actual = evaluator.calculateToBoolean(expr, maps);
		assertEquals(false, actual);
	}

	@Test
	void resultIsEvaluated_forMath1Plus3EvaluatorExpression() throws Exception {
		EvaluatorExpression expr = createEvaluatorExpression("1+3", EDomain.MATH);
		VariableValue actual = evaluator.calculateToVariableValue(expr, maps);
		assertEquals(VariableValueFactory.createVariableValueForOpenMathInteger(4).getContent(), actual.getContent());
	}

	@Test
	void resultIsEvaluated_forChemContainsEvaluatorExpression() throws Exception {
		EvaluatorExpression expr = createEvaluatorExpression("'Hello'", EDomain.CHEM);
		VariableValue actual = evaluator.calculateToVariableValue(expr, maps);
		assertEquals(VariableValueFactory.createVariableValueForOpenChemString("Hello").getContent(),
				actual.getContent());
	}

	private static EvaluatorExpression createEvaluatorExpression(String code, EDomain domain) {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode(code);
		expression.setDomain(domain);
		return expression;
	}
}
