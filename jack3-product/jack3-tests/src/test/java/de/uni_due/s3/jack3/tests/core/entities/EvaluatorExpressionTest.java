package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests for EvaluatorExpression, using a MultipleChoice stage and the default stage transition
 */
@NeedsExercise
class EvaluatorExpressionTest extends AbstractContentTest {

	private EvaluatorExpression expression = new EvaluatorExpression();

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);
		expression = getStageTransition().getConditionExpression();
	}

	/**
	 * Save a new EvaluatorExpression into default stage transition, merge Exercise, return new Expression
	 */
	private EvaluatorExpression updateDefaultConditionExpression(EvaluatorExpression newExpression) {
		getStageTransition().setConditionExpression(newExpression);
		exercise = baseService.merge(exercise);
		return getStageTransition().getConditionExpression();
	}

	/**
	 * Get default stage transition from database
	 */
	private StageTransition getStageTransition() {
		return exercise.getStagesAsList().get(0).getDefaultTransition();
	}

	@Test
	void changeToMathDomain() {
		expression.setDomain(EvaluatorExpression.EDomain.MATH);
		expression = updateDefaultConditionExpression(expression);

		assertEquals(EvaluatorExpression.EDomain.MATH, expression.getDomain());
	}

	@Test
	void changeToChemDomain() {
		expression.setDomain(EvaluatorExpression.EDomain.CHEM);
		expression = updateDefaultConditionExpression(expression);

		assertEquals(EvaluatorExpression.EDomain.CHEM, expression.getDomain());
	}

	@Test
	void changeCode() {
		expression.setCode("1+1");
		expression = updateDefaultConditionExpression(expression);

		assertEquals("1+1", expression.getCode());
	}

	/**
	 * This test checks the deepCopy of evaluator expression without a
	 * code (code is null).
	 */
	@Test
	void deepCopyOfEvaluatorExpressionWithoutCode() {
		EvaluatorExpression deepCopyOfEvaluatorExpression;

		expression.setCode(null);

		deepCopyOfEvaluatorExpression = expression.deepCopy();

		assertNotEquals(expression, deepCopyOfEvaluatorExpression, "The evaluator expression is the origin itself.");
		assertEquals(expression.getDomain(), deepCopyOfEvaluatorExpression.getDomain(),
				"The type of evaluator expressions are different");
		assertNull(deepCopyOfEvaluatorExpression.getCode(), "The code of evaluator expression is set");
	}

	/**
	 * This test checks the deepCopy of evaluator expression with an empty
	 * code (code = "").
	 */
	@Test
	void deepCopyOfEvaluatorExpressionWithEmptyCode() {
		EvaluatorExpression deepCopyOfEvaluatorExpression;

		expression.setCode("");

		deepCopyOfEvaluatorExpression = expression.deepCopy();

		assertNotEquals(expression, deepCopyOfEvaluatorExpression, "The evaluator expression is the origin itself.");
		assertEquals(expression.getDomain(), deepCopyOfEvaluatorExpression.getDomain(),
				"The type of evaluator expressions are different");
		assertTrue(deepCopyOfEvaluatorExpression.getCode().isEmpty(), "The code of evaluator expression is set");
	}

	/**
	 * This test checks the deepCopy of evaluator expression with
	 * math expression.
	 */
	@Test
	void deepCopyOfEvaluatorExpressionWithMathExpression() {
		EvaluatorExpression deepCopyOfEvaluatorExpression;

		expression.setDomain(EDomain.MATH);
		expression.setCode("5=4");

		deepCopyOfEvaluatorExpression = expression.deepCopy();

		assertNotEquals(expression, deepCopyOfEvaluatorExpression, "The evaluator expression is the origin itself.");
		assertEquals(EDomain.MATH, deepCopyOfEvaluatorExpression.getDomain(),
				"The type of evaluator expressions are different");
		assertEquals("5=4", deepCopyOfEvaluatorExpression.getCode(), "The code of evaluator expression is set");
	}

	/**
	 * This test checks the deepCopy of evaluator expression with
	 * chem expression.
	 */
	@Test
	void deepCopyOfEvaluatorExpressionWithChemExpression() {
		EvaluatorExpression deepCopyOfEvaluatorExpression;

		expression.setDomain(EDomain.CHEM);
		expression.setCode("3*10^-12");

		deepCopyOfEvaluatorExpression = expression.deepCopy();

		assertNotEquals(expression, deepCopyOfEvaluatorExpression, "The evaluator expression is the origin itself.");
		assertEquals(EDomain.CHEM, deepCopyOfEvaluatorExpression.getDomain(),
				"The type of evaluator expressions are different");
		assertEquals("3*10^-12", deepCopyOfEvaluatorExpression.getCode(), "The code of evaluator expression is set");
	}

}
