package de.uni_due.s3.jack3.tests.core.stagetypes.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.tests.utils.AbstractStageTest;

public class MCStageTest extends AbstractStageTest<MCStage> { // NOSONAR Javadoc reference

	static String mc0 = PlaceholderPatternProducer.forMcInputVariable(0);
	static String mc1 = PlaceholderPatternProducer.forMcInputVariable(1);
	static String mc2 = PlaceholderPatternProducer.forMcInputVariable(2);
	static String mc3 = PlaceholderPatternProducer.forMcInputVariable(3);
	static String var1 = PlaceholderPatternProducer.forExerciseVariable("var1");
	static String var2 = PlaceholderPatternProducer.forExerciseVariable("var2");

	@Override
	protected MCStage getNewStage() {
		return new MCStage();
	}

	@Override
	protected String getExpectedType() {
		return "mC";
	}

	/**
	 * Add, get and remove answer options
	 */
	@Test
	void testAnswerOptions() {
		assertTrue(stage.getAnswerOptions().isEmpty());

		// All answer options: false-true-false-false
		for (int i = 1; i < 3; i++) {
			stage.addAnswerOption("Opt " + i);
		}

		assertEquals(EMCRuleType.WRONG, stage.getAnswerOptions().get(0).getRule());
		assertEquals(EMCRuleType.WRONG, stage.getAnswerOptions().get(1).getRule());

		assertEquals(2, stage.getAnswerOptions().size());

		stage.getAnswerOptions().get(0).setRule(EMCRuleType.WRONG);
		stage.getAnswerOptions().get(1).setRule(EMCRuleType.CORRECT);

		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());

		// Check correct text and rules

		assertEquals(EMCRuleType.WRONG, stage.getAnswerOptions().get(0).getRule());
		assertEquals(EMCRuleType.CORRECT, stage.getAnswerOptions().get(1).getRule());

		assertEquals("Opt 1", stage.getAnswerOptions().get(0).getText());
		assertEquals("Opt 2", stage.getAnswerOptions().get(1).getText());

		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());

		// Remove first option
		stage.removeAnswerOption(stage.getAnswerOptions().get(0));

		assertEquals(1, stage.getAnswerOptions().size());
		assertEquals(EMCRuleType.CORRECT, stage.getAnswerOptions().get(0).getRule());
		assertEquals("Opt 2", stage.getAnswerOptions().get(0).getText());

		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());
	}

	@Test
	void reorderAnswerOptions() {

		// All answer options: false-true-false-false
		for (int i = 1; i < 5; i++) {
			stage.addAnswerOption("Opt " + i);
		}

		// Order: 0 1 2 3
		List<MCAnswer> originalAnswerOptions = new ArrayList<>(stage.getAnswerOptions());

		// Order: 0 2 1 3
		stage.moveAnswerOption(1, 2);
		assertEquals(originalAnswerOptions.get(0), stage.getAnswerOptions().get(0));
		assertEquals(originalAnswerOptions.get(2), stage.getAnswerOptions().get(1));
		assertEquals(originalAnswerOptions.get(1), stage.getAnswerOptions().get(2));
		assertEquals(originalAnswerOptions.get(3), stage.getAnswerOptions().get(3));
		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());

		// Order: 3 0 2 1
		stage.moveAnswerOption(3, 0);
		assertEquals(originalAnswerOptions.get(3), stage.getAnswerOptions().get(0));
		assertEquals(originalAnswerOptions.get(0), stage.getAnswerOptions().get(1));
		assertEquals(originalAnswerOptions.get(2), stage.getAnswerOptions().get(2));
		assertEquals(originalAnswerOptions.get(1), stage.getAnswerOptions().get(3));
		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());

		// Order: 0 2 3 1
		stage.moveAnswerOption(0, 2);
		assertEquals(originalAnswerOptions.get(0), stage.getAnswerOptions().get(0));
		assertEquals(originalAnswerOptions.get(2), stage.getAnswerOptions().get(1));
		assertEquals(originalAnswerOptions.get(3), stage.getAnswerOptions().get(2));
		assertEquals(originalAnswerOptions.get(1), stage.getAnswerOptions().get(3));
		assertEquals(stage.getAnswerOptions(), stage.getAnswerOptionsForReoder());
	}

	/**
	 * Add, get and remove extra feedback messages
	 */
	@Test
	void testExtraFeedback() {
		// All answer options: false-true-false-false
		for (int i = 1; i < 5; i++) {
			stage.addAnswerOption("Opt " + i);
		}

		assertTrue(stage.getExtraFeedbacks().isEmpty());

		// Add sample feedbacks
		EvaluatorExpression expression = new EvaluatorExpression();
		expression.setCode("True&&True&&" + mc2 + "&&" + mc3);
		stage.addFeedbackOption(expression);

		expression = new EvaluatorExpression();
		expression.setCode(mc0 + "&&!" + mc1 + "&&!" + mc2 + "&&!" + mc3);
		stage.addFeedbackOption(expression);

		expression = new EvaluatorExpression();
		expression.setCode("!" + mc0 + "&&" + mc1 + "==" + var2 + "&&!" + mc2 + "&&" + mc3 + "==" + var1);
		stage.addFeedbackOption(expression);

		assertEquals("True&&True&&" + mc2 + "&&" + mc3, stage.getExtraFeedbacks().get(0).getExpression().getCode());
		assertEquals(mc0 + "&&!" + mc1 + "&&!" + mc2 + "&&!" + mc3,
				stage.getExtraFeedbacks().get(1).getExpression().getCode());
		assertEquals("!" + mc0 + "&&" + mc1 + "==" + var2 + "&&!" + mc2 + "&&" + mc3 + "==" + var1,
				stage.getExtraFeedbacks().get(2).getExpression().getCode());

		assertEquals(4, stage.getAnswerOptions().size());
		assertEquals(3, stage.getExtraFeedbacks().size());

		// Reorder answer options: Change 1<->2
		stage.moveAnswerOption(1, 2);
		assertEquals("True&&" + mc1 + "&&True&&" + mc3, stage.getExtraFeedbacks().get(0).getExpression().getCode());
		assertEquals(mc0 + "&&!" + mc1 + "&&!" + mc2 + "&&!" + mc3,
				stage.getExtraFeedbacks().get(1).getExpression().getCode());
		assertEquals("!" + mc0 + "&&!" + mc1 + "&&" + mc2 + "==" + var2 + "&&" + mc3 + "==" + var1,
				stage.getExtraFeedbacks().get(2).getExpression().getCode());

		exercise = baseService.merge(exercise);
		stage = (MCStage) exercise.getStages().iterator().next();

		// Remove third answer
		stage.removeAnswerOption(stage.getAnswerOptions().get(2));
		assertEquals(3, stage.getExtraFeedbacks().size());

		assertEquals("True&&" + mc1 + "&&" + mc2, stage.getExtraFeedbacks().get(0).getExpression().getCode());
		assertEquals(mc0 + "&&!" + mc1 + "&&!" + mc2, stage.getExtraFeedbacks().get(1).getExpression().getCode());
		assertEquals("!" + mc0 + "&&!" + mc1 + "&&" + mc2 + "==" + var1,
				stage.getExtraFeedbacks().get(2).getExpression().getCode());

		// Remove third feedback option
		MCFeedback feedbackToRemove = stage.getExtraFeedbacks().get(2);
		stage.removeFeedbackOption(feedbackToRemove);
		assertEquals(2, stage.getExtraFeedbacks().size());
		assertFalse(stage.getExtraFeedbacks().contains(feedbackToRemove));
	}

	@Test
	void changeCorrectAnswerFeedback() {
		assertNull(stage.getCorrectAnswerFeedback());

		stage.setCorrectAnswerFeedback("Correct Answer Feedback");
		assertEquals("Correct Answer Feedback", stage.getCorrectAnswerFeedback());
	}

	@Test
	void changeDefaultFeedback() {
		assertNull(stage.getDefaultFeedback());

		stage.setDefaultFeedback("Default Feedback");
		assertEquals("Default Feedback", stage.getDefaultFeedback());
	}

	@Test
	void changeDefaultResult() {
		assertEquals(0, stage.getDefaultResult());

		stage.setDefaultResult(50);
		assertEquals(50, stage.getDefaultResult());
	}

	@Test
	void changeDefaultResultIllegal() {
		// These calls should throw an Exception because defaultResult must be between 0 and 100
		assertThrows(IllegalArgumentException.class, () -> {
			stage.setDefaultResult(-1);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			stage.setDefaultResult(101);
		});
	}

	@Test
	void changeRandomize() {
		assertFalse(stage.isRandomize());

		stage.setRandomize(true);
		assertTrue(stage.isRandomize());
	}

	@Test
	void changeSingleChoice() {
		assertFalse(stage.isSingleChoice());

		stage.setSingleChoice(true);
		assertTrue(stage.isSingleChoice());
	}

	/**
	 * Tests if feedback is updated after a new answer option was added
	 */
	@Test
	void addAnswerOptionAfterAddingFeedback() {
		for (int i = 1; i < 4; i++) {
			stage.addAnswerOption("Opt " + i);
		}

		stage.getAnswerOptions().get(0).setRule(EMCRuleType.WRONG);
		stage.getAnswerOptions().get(1).setRule(EMCRuleType.CORRECT);
		stage.getAnswerOptions().get(2).setRule(EMCRuleType.WRONG);

		// Now the stage has 3 answer options: Wrong-Correct-Wrong

		String oldExpression = "!" + mc0 + "&&" + mc1 + "&&!" + mc2;
		String newExpression = "!" + mc0 + "&&" + mc1 + "&&!" + mc2 + "&&true()"; // With
																					// fourth
																					// answer
																					// option

		// We add a new feedback
		stage.addFeedbackOption(new EvaluatorExpression(oldExpression));

		// We add a fourth answer option
		// -> Then the expression of the feedback option should includes "Don't care" for the fourth answer option

		stage.addAnswerOption("New option");
		assertEquals(newExpression, stage.getExtraFeedbacks().get(0).getExpression().getCode());
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
	 * This test checks the deepCopy of a base MCStage with following
	 * fields sets:
	 * 
	 * - singleChoice
	 * - defaultFeedback
	 * - defaulResult
	 */
	@Test
	void deepCopyOfBaseSCStage() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;

		originMCStage = new MCStage();
		originMCStage.setSingleChoice(true);
		originMCStage.setDefaultFeedback("Single Choice Test");
		originMCStage.setDefaultResult(42);
		deepCopyOfMCStage = originMCStage.deepCopy();

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertTrue(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Single Choice Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different.");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different.");

		// check stage variables
		assertNull(deepCopyOfMCStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfMCStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfMCStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfMCStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfMCStage.getSkipTransitions().isEmpty(), "The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfMCStage.getStageTransitions().isEmpty(), "The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a base MCStage with following
	 * fields sets:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 */
	@Test
	void deepCopyOfBaseMCStage() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Multiple Choice Test");
		originMCStage.setDefaultResult(42);
		deepCopyOfMCStage = originMCStage.deepCopy();

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Multiple Choice Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");

		// check stage variables
		assertNull(deepCopyOfMCStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfMCStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfMCStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfMCStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfMCStage.getSkipTransitions().isEmpty(), "The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfMCStage.getStageTransitions().isEmpty(), "The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a MCStage with following
	 * fields sets:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - all stage variables
	 */
	@Test
	void deepCopyOfMCStageWithStageVariables() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;
		StageTransition defaultTransition;
		StageTransition skipTransition1;
		StageTransition skipTransition2;
		StageTransition stageTransition1;
		StageTransition stageTransition2;
		StageHint stageHint1;
		VariableUpdate variableUpdate1;
		VariableUpdate variableUpdate2;
		StageResource resource1;
		StageResource resource2;
		MCStage stageOfDeepCopiedHintStage = new MCStage();

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Stagevariables Test");
		originMCStage.setDefaultResult(42);

		// define stage variables
		originMCStage.setInternalName("internal name test");
		originMCStage.setExternalName("external name test");
		originMCStage.setTaskDescription("task description test");
		originMCStage.setSkipMessage("skip message test");

		defaultTransition = new StageTransition();
		defaultTransition.setStageExpression(new EvaluatorExpression("expression code of default transition"));
		originMCStage.setDefaultTransition(defaultTransition);
		originMCStage.setWeight(5);
		originMCStage.setOrderIndex(3);
		originMCStage.setAllowSkip(true);

		skipTransition1 = new StageTransition();
		skipTransition2 = new StageTransition();
		skipTransition1.setStageExpression(new EvaluatorExpression("expression code of skip transition 1"));
		skipTransition2.setStageExpression(new EvaluatorExpression("expression code of skip transition 2"));
		originMCStage.addSkipTransition(skipTransition1);
		originMCStage.addSkipTransition(skipTransition2);

		stageTransition1 = new StageTransition();
		stageTransition2 = new StageTransition();
		stageTransition1.setStageExpression(new EvaluatorExpression("expression code of stage transition 1"));
		stageTransition2.setStageExpression(new EvaluatorExpression("expression code of stage transition 2"));
		originMCStage.addStageTransition(stageTransition1);
		originMCStage.addStageTransition(stageTransition2);

		stageHint1 = new StageHint();
		stageHint1.setText("text of hint 1");
		originMCStage.addHint(stageHint1);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable before check 1"));
		variableUpdate2
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable before check 2"));
		originMCStage.addVariableUpdateBeforeCheck(variableUpdate1);
		originMCStage.addVariableUpdateBeforeCheck(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable after check 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable after check 2"));
		originMCStage.addVariableUpdateAfterCheck(variableUpdate1);
		originMCStage.addVariableUpdateAfterCheck(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on normal exit 1"));
		variableUpdate2
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on normal exit 2"));
		originMCStage.addVariableUpdateOnNormalExit(variableUpdate1);
		originMCStage.addVariableUpdateOnNormalExit(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on repeat 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on repeat 2"));
		originMCStage.addVariableUpdateOnRepeat(variableUpdate1);
		originMCStage.addVariableUpdateOnRepeat(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on skip 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on skip 2"));
		originMCStage.addVariableUpdateOnSkip(variableUpdate1);
		originMCStage.addVariableUpdateOnSkip(variableUpdate2);

		resource1 = new StageResource();
		resource2 = new StageResource();
		resource1.setDescription("description of resource 1");
		resource2.setDescription("description of resource 2");
		originMCStage.addStageResource(resource1);
		originMCStage.addStageResource(resource2);

		deepCopyOfMCStage = originMCStage.deepCopy();

		// get private field "stage" of deepcopied stage
		try {
			StageHint tempDeepCopyStageHint = new StageHint();

			if (!deepCopyOfMCStage.getHints().isEmpty()) {
				tempDeepCopyStageHint = deepCopyOfMCStage.getHints().get(0);
			}

			Field fdStage = tempDeepCopyStageHint.getClass().getDeclaredField("stage");
			fdStage.setAccessible(true);
			stageOfDeepCopiedHintStage = (MCStage) fdStage.get(tempDeepCopyStageHint);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new AssertionError("Fiueld could not be found.");
		}

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Stagevariables Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");

		// check stage variables
		assertEquals("internal name test", deepCopyOfMCStage.getInternalName(),
				"The stage variable 'internal name' is different.");
		assertEquals("external name test", deepCopyOfMCStage.getExternalName(),
				"The stage variable 'external name' is different.");
		assertEquals("task description test", deepCopyOfMCStage.getTaskDescription(),
				"The stage variable 'task description' is different.");
		assertEquals("skip message test", deepCopyOfMCStage.getSkipMessage(),
				"The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals("expression code of default transition",
				deepCopyOfMCStage.getDefaultTransition().getStageExpression().getCode(),
				"The default transition is different.");
		assertEquals(5, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(3, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertTrue(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertFalse(deepCopyOfMCStage.getSkipTransitions().isEmpty(),
				"The stage variable 'skip transitions' is not set.");
		assertEquals(2, deepCopyOfMCStage.getSkipTransitions().size(),
				"The stage variable 'skip transitions' is to small.");
		assertEquals("expression code of skip transition 1",
				deepCopyOfMCStage.getSkipTransitions().get(0).getStageExpression().getCode(),
				"The first skip transition is different.");
		assertEquals("expression code of skip transition 2",
				deepCopyOfMCStage.getSkipTransitions().get(1).getStageExpression().getCode(),
				"The second skip transition is different.");
		assertFalse(deepCopyOfMCStage.getStageTransitions().isEmpty(),
				"The stage variable 'stage transitions' is not set.");
		assertEquals(2, deepCopyOfMCStage.getStageTransitions().size(),
				"The stage variable 'stage transitions' is to small.");
		assertEquals("expression code of stage transition 1",
				deepCopyOfMCStage.getStageTransitions().get(0).getStageExpression().getCode(),
				"The first stage transition is different.");
		assertEquals("expression code of stage transition 2",
				deepCopyOfMCStage.getStageTransitions().get(1).getStageExpression().getCode(),
				"The second stage transition is different.");
		assertFalse(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is not set.");
		assertEquals(1, deepCopyOfMCStage.getHints().size(), "The stage variable 'hints' greater than 1.");
		assertEquals("text of hint 1", deepCopyOfMCStage.getHints().get(0).getText(), "The hint is different.");
		/*
		 * For proving the internal stage of hint stage only the internal name
		 * of the origin mc stage will checked. All other entities, e. g.
		 * internal transitions, are proofed in deepCopy tests of the
		 * specific entities.
		 */
		assertEquals("internal name test", stageOfDeepCopiedHintStage.getInternalName(),
				"The stage of hints are different.");
		assertFalse(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is not set.");
		assertEquals(2, deepCopyOfMCStage.getVariableUpdatesBeforeCheck().size(),
				"The stage variable 'VariableUpdateBeforeCheck' is to small.");
		assertEquals("expression code of update variable before check 1",
				deepCopyOfMCStage.getVariableUpdatesBeforeCheck().get(0).getUpdateCode().getCode(),
				"The first update variable before check is different.");
		assertEquals("expression code of update variable before check 2",
				deepCopyOfMCStage.getVariableUpdatesBeforeCheck().get(1).getUpdateCode().getCode(),
				"The second update variable before check is different.");
		assertFalse(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is not set.");
		assertEquals(2, deepCopyOfMCStage.getVariableUpdatesAfterCheck().size(),
				"The stage variable 'VariableUpdateAfterCheck' is to small.");
		assertEquals("expression code of update variable after check 1",
				deepCopyOfMCStage.getVariableUpdatesAfterCheck().get(0).getUpdateCode().getCode(),
				"The first update variable after check is different.");
		assertEquals("expression code of update variable after check 2",
				deepCopyOfMCStage.getVariableUpdatesAfterCheck().get(1).getUpdateCode().getCode(),
				"The second update variable after check is different.");
		assertFalse(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is not set.");
		assertEquals(2, deepCopyOfMCStage.getVariableUpdatesOnNormalExit().size(),
				"The stage variable 'variable updates on normal exit' is to small.");
		assertEquals("expression code of update variable on normal exit 1",
				deepCopyOfMCStage.getVariableUpdatesOnNormalExit().get(0).getUpdateCode().getCode(),
				"The first update variable on normal exit is different.");
		assertEquals("expression code of update variable on normal exit 2",
				deepCopyOfMCStage.getVariableUpdatesOnNormalExit().get(1).getUpdateCode().getCode(),
				"The second update variable normal exit is different.");
		assertFalse(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is not set.");
		assertEquals(2, deepCopyOfMCStage.getVariableUpdatesOnRepeat().size(),
				"The stage variable 'variable update on repeat' is to small.");
		assertEquals("expression code of update variable on repeat 1",
				deepCopyOfMCStage.getVariableUpdatesOnRepeat().get(0).getUpdateCode().getCode(),
				"The first update variable on repeat is different.");
		assertEquals("expression code of update variable on repeat 2",
				deepCopyOfMCStage.getVariableUpdatesOnRepeat().get(1).getUpdateCode().getCode(),
				"The second update variable on repeat is different.");
		assertFalse(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is not set.");
		assertEquals(2, deepCopyOfMCStage.getVariableUpdatesOnSkip().size(),
				"The stage variable 'variable update on Skip' is to small.");
		assertEquals("expression code of update variable on skip 1",
				deepCopyOfMCStage.getVariableUpdatesOnSkip().get(0).getUpdateCode().getCode(),
				"The first update variable on skip is different.");
		assertEquals("expression code of update variable on skip 2",
				deepCopyOfMCStage.getVariableUpdatesOnSkip().get(1).getUpdateCode().getCode(),
				"The second update variable on skip is different.");
		assertFalse(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
		assertEquals(2, deepCopyOfMCStage.getStageResources().size(),
				"The stage variable 'stage resources' is to small.");
		assertEquals("description of resource 1", deepCopyOfMCStage.getStageResources().get(0).getDescription(),
				"The first resource is different.");
		assertEquals("description of resource 2", deepCopyOfMCStage.getStageResources().get(1).getDescription(),
				"The second resource is different.");
	}

	/**
	 * This test checks the deepCopy of a complete MCStage with following
	 * fields sets:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - randomize
	 * - answerOptions (one times)
	 * - correctAnswerFeedback
	 * - extraFeedbacks (one time)
	 */
	@Test
	void deepCopyOfFullMCStage() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;
		MCFeedback feedback;
		XStream xstream = new XStream();
		String originStageXML;
		String copiedStageXML;

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Complete Multiple Choice Test");
		originMCStage.setDefaultResult(42);
		originMCStage.setRandomize(true);
		originMCStage.setCorrectAnswerFeedback("correct answer for " + "complete mc test");

		feedback = new MCFeedback(new EvaluatorExpression("1+1"));
		feedback.setFeedbackText("extra feedback for complete mc test");
		originMCStage.getExtraFeedbacks().add(feedback);

		originMCStage.addAnswerOption("answer text");

		deepCopyOfMCStage = originMCStage.deepCopy();

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Complete Multiple Choice Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");
		assertTrue(deepCopyOfMCStage.isRandomize(), "The mc stage is not randomizeable.");
		assertEquals("correct answer for complete mc test", deepCopyOfMCStage.getCorrectAnswerFeedback(),
				"The correct anwser feedback is different.");
		assertEquals("extra feedback for complete mc test",
				deepCopyOfMCStage.getExtraFeedbacks().get(0).getFeedbackText(), "The extra feedback is different.");
		assertFalse(deepCopyOfMCStage.getAnswerOptions().isEmpty(), "The mc answer is not set.");
		assertEquals(1, deepCopyOfMCStage.getAnswerOptions().size(), "The mc answer is greater than 1.");
		assertEquals("answer text", deepCopyOfMCStage.getAnswerOptions().get(0).getText(),
				"The mc answer is different.");
		// check mc stage (deepcopied)
		originStageXML = xstream.toXML(originMCStage);
		copiedStageXML = xstream.toXML(deepCopyOfMCStage);
		/*
		 * Cut the first tags "...Stage", "jackId" & "updateTimeStamp"
		 * from XML serialized stages to compare the content. The start
		 * tag in the serialized XML file is the
		 * "listOfStageEntitiesToRemoveBySaving" tag.
		 */
		assertTrue(
				originStageXML.substring(originStageXML.indexOf("<listOfStageEntitiesToRemoveBySaving")).contentEquals(
						copiedStageXML.substring(copiedStageXML.indexOf("<listOfStageEntitiesToRemoveBySaving"))),
				"The deepcopied mc stage of the answer is different.");

		// check stage variables
		assertNull(deepCopyOfMCStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfMCStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfMCStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfMCStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfMCStage.getSkipTransitions().isEmpty(), "The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfMCStage.getStageTransitions().isEmpty(), "The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is set.");

		assertTrue(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a complete MCStage with multiple
	 * answers. The following fields set:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - randomize
	 * - answerOptions (two times)
	 * - correctAnswerFeedback
	 * - extraFeedbacks (one time)
	 * - ...
	 */
	@Test
	void deepCopyOfMCStageWithMultipleAnswers() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;
		MCFeedback feedback;
		XStream xstream = new XStream();
		String originAnswerStageXML;
		String copiedAnswerStageXML_1;
		String copiedAnswerStageXML_2;

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Multiple AnswerOptions MC Test");
		originMCStage.setDefaultResult(42);
		originMCStage.setRandomize(true);
		originMCStage.setCorrectAnswerFeedback("correct answer for " + "complete mc test");
		originMCStage.addAnswerOption("answer text of first answer");
		originMCStage.addAnswerOption("answer text of second answer");

		feedback = new MCFeedback(new EvaluatorExpression("1+1"));
		feedback.setFeedbackText("extra feedback for complete mc test");
		originMCStage.getExtraFeedbacks().add(feedback);

		deepCopyOfMCStage = originMCStage.deepCopy();

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Multiple AnswerOptions MC Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");
		assertTrue(deepCopyOfMCStage.isRandomize(), "The mc stage is not randomizeable.");
		assertEquals("correct answer for complete mc test", deepCopyOfMCStage.getCorrectAnswerFeedback(),
				"The correct anwser feedback is different.");
		assertEquals("extra feedback for complete mc test",
				deepCopyOfMCStage.getExtraFeedbacks().get(0).getFeedbackText(), "The extra feedback is different.");
		assertFalse(deepCopyOfMCStage.getAnswerOptions().isEmpty(), "The mc answer is not set.");
		assertEquals(2, deepCopyOfMCStage.getAnswerOptions().size(), "The mc answer amount isn't 2.");
		assertEquals("answer text of first answer", deepCopyOfMCStage.getAnswerOptions().get(0).getText(),
				"The mc answer is different.");
		assertEquals("answer text of second answer", deepCopyOfMCStage.getAnswerOptions().get(1).getText(),
				"The mc answer is different.");
		// check mc stage (deepcopied)
		originAnswerStageXML = xstream.toXML(originMCStage);
		copiedAnswerStageXML_1 = xstream.toXML(deepCopyOfMCStage);
		copiedAnswerStageXML_2 = xstream.toXML(deepCopyOfMCStage);
		/*
		 * Cut the first tags "...Stage", "jackId" & "updateTimeStamp"
		 * from XML serialized stages to compare the content. The start
		 * tag in the serialized XML file is the
		 * "listOfStageEntitiesToRemoveBySaving" tag.
		 */
		assertTrue(
				originAnswerStageXML.substring(originAnswerStageXML.indexOf("<listOfStageEntitiesToRemoveBySaving"))
						.contentEquals(copiedAnswerStageXML_1
								.substring(copiedAnswerStageXML_1.indexOf("<listOfStageEntitiesToRemoveBySaving"))),
				"The deepcopied mc stage of the first answer is different.");
		assertTrue(
				originAnswerStageXML.substring(originAnswerStageXML.indexOf("<listOfStageEntitiesToRemoveBySaving"))
						.contentEquals(copiedAnswerStageXML_2
								.substring(copiedAnswerStageXML_2.indexOf("<listOfStageEntitiesToRemoveBySaving"))),
				"The deepcopied mc stage of the second answer is different.");

		// check stage variables
		assertNull(deepCopyOfMCStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfMCStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfMCStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfMCStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfMCStage.getSkipTransitions().isEmpty(), "The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfMCStage.getStageTransitions().isEmpty(), "The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a MCStage with multiple
	 * available extra Feedbacks. The following fields set:
	 * 
	 * - defaultFeedback
	 * - defaulResult
	 * - extraFeedbacks (two times)
	 */
	@Test
	void deepCopyOfMCStageWithMultipleExtraFeedbacks() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;
		MCFeedback feedback1;
		MCFeedback feedback2;

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Multiple Extra Feedbacks");
		originMCStage.setDefaultResult(42);

		feedback1 = new MCFeedback(new EvaluatorExpression("1+1"));
		feedback1.setFeedbackText("first extra feedback for extra feedbacks test");
		originMCStage.getExtraFeedbacks().add(feedback1);
		feedback2 = new MCFeedback(new EvaluatorExpression("1*1"));
		feedback2.setFeedbackText("second extra feedback for extra feedbacks test");
		originMCStage.getExtraFeedbacks().add(feedback2);

		deepCopyOfMCStage = originMCStage.deepCopy();

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Multiple Extra Feedbacks", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");

		assertFalse(deepCopyOfMCStage.getExtraFeedbacks().isEmpty(), "The extra feedback is not set.");
		assertEquals(2, deepCopyOfMCStage.getExtraFeedbacks().size(), "The size of extra feedback is not equal to 2.");
		assertEquals("first extra feedback for extra feedbacks test",
				deepCopyOfMCStage.getExtraFeedbacks().get(0).getFeedbackText(),
				"The first extra feedback is different.");
		assertEquals("second extra feedback for extra feedbacks test",
				deepCopyOfMCStage.getExtraFeedbacks().get(1).getFeedbackText(),
				"The second extra feedback is different.");

		// check stage variables
		assertNull(deepCopyOfMCStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfMCStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfMCStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfMCStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfMCStage.getDefaultTransition(), "The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfMCStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfMCStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfMCStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfMCStage.getSkipTransitions().isEmpty(), "The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfMCStage.getStageTransitions().isEmpty(), "The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is set.");

		assertTrue(deepCopyOfMCStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfMCStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfMCStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a MCStage with multiple
	 * available hints. The following fields set:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - internal name
	 * - hints (two times)
	 */
	@Test
	void deepCopyOfMCStageWithMultipleHints() {
		MCStage originMCStage;
		MCStage deepCopyOfMCStage;
		StageHint stageHint1;
		StageHint stageHint2;
		MCStage stageOfDeepCopiedHintStage1 = new MCStage();
		MCStage stageOfDeepCopiedHintStage2 = new MCStage();

		originMCStage = new MCStage();
		originMCStage.setDefaultFeedback("Multiple Hints Test");
		originMCStage.setDefaultResult(42);

		// define stage variables
		originMCStage.setInternalName("internal name of multiple hints");

		stageHint1 = new StageHint();
		stageHint2 = new StageHint();
		stageHint1.setText("text of hint 1");
		stageHint2.setText("text of hint 2");
		originMCStage.addHint(stageHint1);
		originMCStage.addHint(stageHint2);

		deepCopyOfMCStage = originMCStage.deepCopy();

		// get private field "stage" of deepcopied stage
		try {
			StageHint tempDeepCopyStageHint1 = new StageHint();
			StageHint tempDeepCopyStageHint2 = new StageHint();
			Field fdStage1;
			Field fdStage2;

			if (!deepCopyOfMCStage.getHints().isEmpty() && deepCopyOfMCStage.getHints().size() == 2) {
				tempDeepCopyStageHint1 = deepCopyOfMCStage.getHints().get(0);
				tempDeepCopyStageHint2 = deepCopyOfMCStage.getHints().get(1);
			}

			fdStage1 = tempDeepCopyStageHint1.getClass().getDeclaredField("stage");
			fdStage2 = tempDeepCopyStageHint2.getClass().getDeclaredField("stage");
			fdStage1.setAccessible(true);
			fdStage2.setAccessible(true);

			stageOfDeepCopiedHintStage1 = (MCStage) fdStage1.get(tempDeepCopyStageHint1);
			stageOfDeepCopiedHintStage2 = (MCStage) fdStage2.get(tempDeepCopyStageHint2);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
			throw new AssertionError("Field could not be found.");
		}

		assertNotEquals(originMCStage, deepCopyOfMCStage, "The mc stage is the origin itself.");
		assertFalse(deepCopyOfMCStage.isSingleChoice(), "The mc stage is not single choice.");
		assertEquals("Multiple Hints Test", deepCopyOfMCStage.getDefaultFeedback(),
				"The default feedbacks of mc stage are different");
		assertEquals(42, deepCopyOfMCStage.getDefaultResult(), "The default results are different");

		// check hints
		assertFalse(deepCopyOfMCStage.getHints().isEmpty(), "The stage variable 'hints' is not set.");
		assertEquals(2, deepCopyOfMCStage.getHints().size(), "The size of stage variable 'hints' is not equal to 2.");
		assertEquals("text of hint 1", deepCopyOfMCStage.getHints().get(0).getText(), "The hint is different.");
		assertEquals("text of hint 2", deepCopyOfMCStage.getHints().get(1).getText(), "The hint is different.");
		/*
		 * For proving the internal stage of hint stage only the internal name
		 * of the origin mc stage will checked. All other entities, e. g.
		 * internal transitions, are proofed in deepCopy tests of the
		 * specific entities.
		 */
		assertEquals("internal name of multiple hints", stageOfDeepCopiedHintStage1.getInternalName(),
				"The stage of hints are different.");
		assertEquals("internal name of multiple hints", stageOfDeepCopiedHintStage2.getInternalName(),
				"The stage of hints are different.");
	}

}
