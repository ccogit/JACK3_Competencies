package de.uni_due.s3.jack3.tests.core.stagetypes.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * 
 * @author Kilian.Kraus
 *
 */
@NeedsExercise
class MCAnswerTest extends AbstractContentTest {

	private MCStage stage;

	/**
	 * Return a new stage
	 */
	private MCStage getNewStage() {
		return new MCStage();
	}

	/**
	 * Prepare testing the stage: Add a stage to the exercise
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		stage = getNewStage();
		exercise.addStage(stage);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
	}

	/**
	 * Testing the constructors
	 */
	@Test
	void testConstructors() {
		MCAnswer answer = new MCAnswer();
		assertEquals(EMCRuleType.WRONG, answer.getRule());

		answer = new MCAnswer("Example Text!");
		assertEquals("Example Text!", answer.getText());
		assertEquals(EMCRuleType.WRONG, answer.getRule());
	}

	@Test
	void testVariableName() {
		stage.addAnswerOption("A)");
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		MCAnswer answer = stage.getAnswerOptions().get(0);
		answer.setVariableName("crazyName");
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		answer = stage.getAnswerOptions().get(0);
		assertEquals("crazyName", answer.getVariableName());
	}

	@Test
	void testText() {

		stage.addAnswerOption("Test Text");
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();

		assertEquals("Test Text", stage.getAnswerOptions().get(0).getText());

		stage.getAnswerOptions().get(0).setText("Hello World!");
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();

		assertEquals("Hello World!", stage.getAnswerOptions().get(0).getText());
	}

	@Test
	void testRule() {

		stage.addAnswerOption("TestRule");
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();

		MCAnswer answer = stage.getAnswerOptions().get(0);

		answer.setRule(EMCRuleType.CORRECT);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		answer = stage.getAnswerOptions().get(0);
		assertEquals(EMCRuleType.CORRECT, answer.getRule());

		answer.setRule(EMCRuleType.WRONG);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		answer = stage.getAnswerOptions().get(0);
		assertEquals(EMCRuleType.WRONG, answer.getRule());

		answer.setRule(EMCRuleType.NO_MATTER);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		answer = stage.getAnswerOptions().get(0);
		assertEquals(EMCRuleType.NO_MATTER, answer.getRule());

		answer.setRule(EMCRuleType.VARIABLE);
		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();
		answer = stage.getAnswerOptions().get(0);
		assertEquals(EMCRuleType.VARIABLE, answer.getRule());
	}

	/**
	 * This test checks the deepCopy of a mc answer.
	 */
	@Test
	void deepCopyOfMCAnswer() {
		MCAnswer originAnswer = new MCAnswer();
		MCAnswer deepCopyOfMCAnswer = new MCAnswer();
		MCStage basicStage = new MCStage();

		originAnswer.setText("origin answer");
		originAnswer.setRule(EMCRuleType.VARIABLE);
		originAnswer.setVariableName("variable for rule");
		originAnswer.setMCStage(basicStage);

		deepCopyOfMCAnswer = originAnswer.deepCopy();

		assertNotEquals(originAnswer, deepCopyOfMCAnswer, "The mc answer is the origin itself.");
		assertEquals("origin answer", deepCopyOfMCAnswer.getText(), "The text of mc answers are different.");
		assertEquals(EMCRuleType.VARIABLE, deepCopyOfMCAnswer.getRule(), "The rule type of mc answers are different.");
		assertEquals("variable for rule",
				deepCopyOfMCAnswer.getVariableName(), "The variable name of mc answers are different.");
		
		//check that the McStage doesn't get deep copied.
		try {
			Field privateStringField = MCAnswer.class.getDeclaredField("mcstage");
			privateStringField.setAccessible(true);
			MCStage copiedMCStage = (MCStage) privateStringField.get(deepCopyOfMCAnswer);
			assertNull(copiedMCStage, "The stage of mc answer is set.");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new AssertionError("Das Feld 'mcstage' konnte nicht gefunden werden.");
		}
	}

}
