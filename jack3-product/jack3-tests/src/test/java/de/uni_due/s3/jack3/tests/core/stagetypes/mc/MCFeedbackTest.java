package de.uni_due.s3.jack3.tests.core.stagetypes.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * 
 * @author Kilian.Kraus
 *
 */
@NeedsExercise
class MCFeedbackTest extends AbstractContentTest {

	private MCStage stage;

	/**
	 * Prepare testing the stage: Add a stage to the exercise
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		stage = new MCStage();
		exercise.addStage(stage);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
	}

	@Test
	void testConstructors() {
		MCFeedback feedback1 = new MCFeedback();
		MCFeedback feedback2 = new MCFeedback(new EvaluatorExpression());

		assertTrue(feedback1.getExpression().isEmpty());
		assertTrue(feedback2.getExpression().isEmpty());

		EvaluatorExpression evaluatorExpression = new EvaluatorExpression();
		evaluatorExpression.setCode("Fancy code");
		feedback2 = new MCFeedback(evaluatorExpression);
		assertEquals(feedback2.getExpression().getCode(), evaluatorExpression.getCode());
	}

	@Test
	void testConstructorException() {
		assertThrows(NullPointerException.class, () -> {
			new MCFeedback(null);
		});
	}

	@Test
	void testResult() {
		MCFeedback feedback = new MCFeedback();
		feedback.setResult(98);
		assertEquals(98, feedback.getResult());
		feedback.setResult(32);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		assertEquals(32, feedback.getResult());
	}

	@Test
	void testSetResultExceptionTooLowNumber() {
		MCFeedback feedback = new MCFeedback();
		assertThrows(IllegalArgumentException.class, () -> {
			feedback.setResult(-101);
		});
	}

	@Test
	void testSetResultExceptionTooBigNumber() {
		MCFeedback feedback = new MCFeedback();
		assertThrows(IllegalArgumentException.class, () -> {
			feedback.setResult(101);
		});
	}

	@Test
	void testFeedbackText() {
		MCFeedback feedback = new MCFeedback();
		assertNull(feedback.getFeedbackText());
		feedback.setFeedbackText("WOW! You did a great job :)");
		assertEquals("WOW! You did a great job :)", feedback.getFeedbackText());
	}

	@Test
	void testAddingFeedbackToStage() {
		EvaluatorExpression exp = new EvaluatorExpression();
		exp.setCode("[1] == 42");
		exp.setDomain(EDomain.MATH);

		stage.addFeedbackOption(exp);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		assertEquals(1, stage.getExtraFeedbacks().size());
		MCFeedback feedback = stage.getExtraFeedbacks().get(0);
		assertEquals("[1] == 42", feedback.getExpression().getCode());
	}

	/**
	 * This test checks the deepCopy of default mc feedback.
	 */
	void deepCopyOfDefaultMCFeedback() {
		MCFeedback originFeedback = new MCFeedback();
		MCFeedback deepCopyOfMCFeedback = new MCFeedback();

		originFeedback.setFeedbackText("deep copy test of mc test");
		originFeedback.setResult(42);

		deepCopyOfMCFeedback = originFeedback.deepCopy();

		assertNotEquals(originFeedback, deepCopyOfMCFeedback, "The mc feedback is the origin itself.");
		assertEquals("deep copy test of mc test", deepCopyOfMCFeedback.getFeedbackText(),
				"The feedback text of mc feedbacks are different.");
		assertEquals(42, deepCopyOfMCFeedback.getResult(), "The result of mc feedbacks are different.");
		assertEquals(originFeedback.getExpression().getDomain(), deepCopyOfMCFeedback.getExpression().getDomain(),
				"The domains of expression are different.");
		assertEquals(originFeedback.getExpression().getCode(), deepCopyOfMCFeedback.getExpression().getCode(),
				"The codes of expression are different.");
		assertEquals(originFeedback.getCondition().getDomain(), deepCopyOfMCFeedback.getCondition().getDomain(),
				"The domains of condition are different.");
		assertEquals(originFeedback.getCondition().getCode(), deepCopyOfMCFeedback.getCondition().getCode(),
				"The codes of condition are different.");
	}

	/**
	 * This test checks the deepCopy of mc feedback, which contains an
	 * expression.
	 */
	@Test
	void deepCopyOfMCFeedbackWithStageExpression() {
		EvaluatorExpression stageExpression = new EvaluatorExpression();
		MCFeedback originFeedback = new MCFeedback(stageExpression);
		MCFeedback deepCopyOfMCFeedback = new MCFeedback();

		originFeedback.setFeedbackText("deep copy test of mc test");
		originFeedback.setResult(42);
		stageExpression.setDomain(EDomain.CHEM);
		stageExpression.setCode("1*1");

		deepCopyOfMCFeedback = originFeedback.deepCopy();

		assertNotEquals(originFeedback, deepCopyOfMCFeedback, "The mc feedback is the origin itself.");
		assertEquals("deep copy test of mc test", deepCopyOfMCFeedback.getFeedbackText(),
				"The feedback text of mc feedbacks are different.");
		assertEquals(42, deepCopyOfMCFeedback.getResult(), "The result of mc feedbacks are different.");
		assertEquals(EDomain.CHEM, deepCopyOfMCFeedback.getExpression().getDomain(),
				"The domain of the stage expression of mc feedbacks are different.");
		assertEquals("1*1", deepCopyOfMCFeedback.getExpression().getCode(),
				"The code of the stage expression of mc feedbacks are different.");
		assertEquals(originFeedback.getCondition().getDomain(), deepCopyOfMCFeedback.getCondition().getDomain(),
				"The domains of condition are different.");
		assertEquals(originFeedback.getCondition().getCode(), deepCopyOfMCFeedback.getCondition().getCode(),
				"The codes of condition are different.");
	}

	/**
	 * This test checks the deepCopy of mc feedback, which contains a
	 * condition expression.
	 */
	@Test
	void deepCopyOfMCFeedbackWithConditionExpression() {
		//EvaluatorExpression stageExpression = new EvaluatorExpression();
		MCFeedback originFeedback = new MCFeedback();
		MCFeedback deepCopyOfMCFeedback = new MCFeedback();

		originFeedback.setFeedbackText("deep copy test of mc test");
		originFeedback.setResult(42);
		originFeedback.getCondition().setDomain(EDomain.CHEM);
		originFeedback.getCondition().setCode("1*1");

		deepCopyOfMCFeedback = originFeedback.deepCopy();

		assertNotEquals(originFeedback, deepCopyOfMCFeedback, "The mc feedback is the origin itself.");
		assertEquals("deep copy test of mc test", deepCopyOfMCFeedback.getFeedbackText(),
				"The feedback text of mc feedbacks are different.");
		assertEquals(42, deepCopyOfMCFeedback.getResult(), "The result of mc feedbacks are different.");
		assertEquals(originFeedback.getExpression().getDomain(), deepCopyOfMCFeedback.getExpression().getDomain(),
				"The domain of the stage expression of mc feedbacks are different.");
		assertEquals(originFeedback.getExpression().getCode(), deepCopyOfMCFeedback.getExpression().getCode(),
				"The code of the stage expression of mc feedbacks are different.");
		assertEquals(EDomain.CHEM, deepCopyOfMCFeedback.getCondition().getDomain(),
				"The domains of condition are different.");
		assertEquals("1*1", deepCopyOfMCFeedback.getCondition().getCode(), "The codes of condition are different.");
	}

}
