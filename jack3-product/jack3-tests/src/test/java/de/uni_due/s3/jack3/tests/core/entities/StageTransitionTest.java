package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests for stage transitions, using a MultipleChoice stage
 */
@NeedsExercise
class StageTransitionTest extends AbstractContentTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		exercise.addStage(new MCStage());
		exercise = baseService.merge(exercise);
	}

	/**
	 * Get the default transitiont of the exercise stage
	 */
	private StageTransition getStageTransition() {
		return exercise.getStagesAsList().get(0).getDefaultTransition();
	}

	@Test
	void changeTarget() {
		assertNull(getStageTransition().getTarget());
		getStageTransition().setTarget(exercise.getStagesAsList().get(0));

		exercise = baseService.merge(exercise);
		assertEquals(exercise.getStagesAsList().get(0), getStageTransition().getTarget());
	}

	@Test
	void changeConditionExpression() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		getStageTransition().setConditionExpression(expression);

		exercise = baseService.merge(exercise);
		assertNotNull(getStageTransition().getConditionExpression());
		assertEquals("1+1", getStageTransition().getConditionExpression().getCode());
	}

	@Test
	void changeStageExpression() {
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("1+1");
		getStageTransition().setStageExpression(expression);

		exercise = baseService.merge(exercise);
		assertNotNull(getStageTransition().getStageExpression());
		assertEquals("1+1", getStageTransition().getStageExpression().getCode());
	}

	/**
	 * Test if repeat is enabled when setting the target to a {@link RepeatStage}
	 */
	@Test
	void enableRepeat() {
		getStageTransition().setTarget(new RepeatStage());
		exercise = baseService.merge(exercise);

		assertTrue(getStageTransition().isRepeat());
	}

	/**
	 * Test if repeat is disabled when setting the target to a valid stage
	 */
	@Test
	void disableRepeat() {
		getStageTransition().setTarget(exercise.getStagesAsList().get(0));
		exercise = baseService.merge(exercise);

		assertFalse(getStageTransition().isRepeat());
	}

	/**
	 * This test checks the deepCopy of a stage transition with empty
	 * codition expression and empty stage expression.
	 */
	@Test
	void deepCopyWithoutConditions() {
		StageTransition stageTransition = getStageTransition();
		StageTransition deepCopyOfStageTransition = new StageTransition();

		stageTransition.setTarget(new MCStage());
		stageTransition.setConditionExpression(null);
		stageTransition.setStageExpression(null);

		deepCopyOfStageTransition = stageTransition.deepCopy();

		assertNotEquals(stageTransition, deepCopyOfStageTransition, "The stage transition is the origin itself.");
		assertNull(deepCopyOfStageTransition.getTarget(), "The targets of stage transitions are different.");
		assertNull(deepCopyOfStageTransition.getConditionExpression(),
				"The condition expressions of stage transitions are different.");
		assertNull(deepCopyOfStageTransition.getStageExpression(),
				"The stage expressions of stage transitions are different.");
		assertEquals(stageTransition.isRepeat(), deepCopyOfStageTransition.isRepeat(),
				"The repeat states of stage transitions are different.");
	}

	/**
	 * This test checks the deepCopy of a stage transition with
	 * codition expression and stage expression specified.
	 */
	@Test
	void deepCopyWithBothCoditiontypesUsed() {
		StageTransition stageTransition = getStageTransition();
		StageTransition deepCopyOfStageTransition = new StageTransition();
		EvaluatorExpression stageExpression = new EvaluatorExpression();
		EvaluatorExpression conditionExpression = new EvaluatorExpression();

		stageTransition.setTarget(new MCStage());
		stageExpression.setDomain(EDomain.MATH);
		stageExpression.setCode("1+1");
		stageTransition.setStageExpression(stageExpression);
		conditionExpression.setDomain(EDomain.CHEM);
		conditionExpression.setCode("2+2");
		stageTransition.setConditionExpression(conditionExpression);

		deepCopyOfStageTransition = stageTransition.deepCopy();

		assertNotEquals(stageTransition, deepCopyOfStageTransition, "The stage transition is the origin itself.");
		assertNull(deepCopyOfStageTransition.getTarget(), "The targets of stage transitions are different.");
		assertEquals("1+1", deepCopyOfStageTransition.getStageExpression().getCode(),
				"The condition of stage expression of the stage transitions are different.");
		assertEquals(EDomain.MATH, deepCopyOfStageTransition.getStageExpression().getDomain(),
				"The domain of stage expression of the stage transitions are different.");
		assertEquals("2+2", deepCopyOfStageTransition.getConditionExpression().getCode(),
				"The condition of condition expression of the stage transitions are different.");
		assertEquals(EDomain.CHEM, deepCopyOfStageTransition.getConditionExpression().getDomain(),
				"The domain of condition expression of the stage transitions are different.");
		assertEquals(stageTransition.isRepeat(), deepCopyOfStageTransition.isRepeat(),
				"The repeat states of stage transitions are different.");
	}

	/**
	 * This test checks the deepCopy of a stage transition with
	 * "isRepeat" field set to false.
	 */
	@Test
	void deepCopyOfNonRepeatableStageTransition() {
		StageTransition stageTransition = getStageTransition();
		StageTransition deepCopyOfStageTransition = new StageTransition();
		EvaluatorExpression stageExpression = new EvaluatorExpression();
		EvaluatorExpression conditionExpression = new EvaluatorExpression();

		stageTransition.setTarget(new MCStage());
		stageExpression.setDomain(EDomain.MATH);
		stageExpression.setCode("1+1");
		stageTransition.setStageExpression(stageExpression);
		conditionExpression.setDomain(EDomain.CHEM);
		conditionExpression.setCode("2+2");
		stageTransition.setConditionExpression(conditionExpression);

		deepCopyOfStageTransition = stageTransition.deepCopy();

		assertNotEquals(stageTransition, deepCopyOfStageTransition, "The stage transition is the origin itself.");
		assertNull(deepCopyOfStageTransition.getTarget(), "The targets of stage transitions are different.");
		assertEquals("1+1", deepCopyOfStageTransition.getStageExpression().getCode(),
				"The condition of stage expression of the stage transitions are different.");
		assertEquals(EDomain.MATH, deepCopyOfStageTransition.getStageExpression().getDomain(),
				"The domain of stage expression of the stage transitions are different.");
		assertEquals("2+2", deepCopyOfStageTransition.getConditionExpression().getCode(),
				"The condition of condition expression of the stage transitions are different.");
		assertEquals(EDomain.CHEM, deepCopyOfStageTransition.getConditionExpression().getDomain(),
				"The domain of condition expression of the stage transitions are different.");
		assertFalse(deepCopyOfStageTransition.isRepeat(), "The repeat states of stage transitions are different.");
	}

	/**
	 * This test checks the deepCopy of a stage transition with
	 * "isRepeat" field set to true.
	 */
	@Test
	void deepCopyOfRepeatableStageTransition() {
		StageTransition stageTransition = getStageTransition();
		StageTransition deepCopyOfStageTransition = new StageTransition();
		EvaluatorExpression stageExpression = new EvaluatorExpression();
		EvaluatorExpression conditionExpression = new EvaluatorExpression();

		stageTransition.setTarget(new RepeatStage());
		stageExpression.setDomain(EDomain.MATH);
		stageExpression.setCode("1+1");
		stageTransition.setStageExpression(stageExpression);
		conditionExpression.setDomain(EDomain.CHEM);
		conditionExpression.setCode("2+2");
		stageTransition.setConditionExpression(conditionExpression);

		deepCopyOfStageTransition = stageTransition.deepCopy();

		assertNotEquals(stageTransition, deepCopyOfStageTransition, "The stage transition is the origin itself.");
		assertEquals("1+1", deepCopyOfStageTransition.getStageExpression().getCode(),
				"The condition of stage expression of the stage transitions are different.");
		assertEquals(EDomain.MATH, deepCopyOfStageTransition.getStageExpression().getDomain(),
				"The domain of stage expression of the stage transitions are different.");
		assertEquals("2+2", deepCopyOfStageTransition.getConditionExpression().getCode(),
				"The condition of condition expression of the stage transitions are different.");
		assertEquals(EDomain.CHEM, deepCopyOfStageTransition.getConditionExpression().getDomain(),
				"The domain of condition expression of the stage transitions are different.");
		assertTrue(deepCopyOfStageTransition.isRepeat(), "The repeat states of stage transitions are different.");
	}

}
