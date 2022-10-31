package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.XStream;

import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.tests.utils.AbstractStageTest;

class FillInStageTest extends AbstractStageTest<FillInStage> {

	@Override
	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	@Override
	protected String getExpectedType() {
		return "fillIn";
	}

	@Test
	void getAddAndRemoveFeedbackRules() {

		// Creating Rules for the test
		Rule singleRule = new Rule("myFirstRule", 0);
		singleRule.setFeedbackText("text1");

		List<Rule> rules = new ArrayList<>(3);
		rules.add(new Rule("rule1", 1));
		Rule willBeRemoved = new Rule("rule2", 2);
		willBeRemoved.setFeedbackText("text2");
		rules.add(willBeRemoved);
		rules.add(new Rule("rule3", 3));
		rules.get(0).setFeedbackText("text3");
		rules.get(2).setFeedbackText("text4");


		assertEquals(0, stage.getFeedbackRulesAsList().size());
		stage.addFeedbackRule(singleRule);

		saveExercise();

		assertEquals(1, stage.getFeedbackRulesAsList().size());

		stage.addFeedbackRules(rules);

		saveExercise();

		assertEquals(3, stage.getFeedbackRulesAsList().size());
		assertTrue(stage.getFeedbackRulesAsList().stream().anyMatch(x -> x.getFeedbackText().equals("text2")));

		stage.removeFeedbackRule(2);
		saveExercise();

		assertEquals(2, stage.getFeedbackRulesAsList().size());
		assertFalse(stage.getFeedbackRulesAsList().stream().anyMatch(x -> x.getFeedbackText().equals("text2")));
	}


	@Test
	void getAddAndRemoveFillInFields() {
		assertEquals(0, stage.getFillInFields().size());
		FillInField field1 = new FillInField("field0", 0);
		stage.addFillInField(field1);

		saveExercise();

		assertEquals(1, stage.getFillInFields().size());
		assertEquals("field0", stage.getFillInFields().stream().findFirst().get().getName());
		FillInField field2 = new FillInField("field1", 1);
		stage.addFillInField(field2);

		saveExercise();

		assertEquals(2, stage.getFillInFields().size());

		// remove
		stage.removeFillInField(
				stage.getFillInFields().stream().filter(x -> x.getName().equals("field0")).findFirst().get());
		saveExercise();

		assertEquals(1, stage.getFillInFields().size());
	}

	@Test
	void getAddAndRemoveDropDownFields() {
		assertEquals(0, stage.getDropDownFields().size());
		DropDownField field1 = new DropDownField("field0", 0);
		stage.addDropDownField(field1);

		saveExercise();

		assertEquals(1, stage.getDropDownFields().size());
		assertEquals("field0", stage.getDropDownFields().stream().findFirst().get().getName());
		DropDownField field2 = new DropDownField("field1", 1);
		stage.addDropDownField(field2);

		saveExercise();

		assertEquals(2, stage.getDropDownFields().size());

		// remove
		stage.removeDropDownField(
				stage.getDropDownFields().stream().filter(x -> x.getName().equals("field0")).findFirst().get());
		saveExercise();

		assertEquals(1, stage.getDropDownFields().size());
	}

	@Test
	void getAndSetFormularEditorPalette() {
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE.toString(),
				stage.getFormularEditorPalette());

		stage.setFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC.toString());

		saveExercise();

		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC, stage.getFormularEditorPaletteEnum());
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC.toString(),
				stage.getFormularEditorPalette());
	}

	/**
	 * This test checks the deepCopy of a base fill in stage with following
	 * fields set:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 */
	@Test
	void deepCopyOfBaseFillInStage() {
		FillInStage originFillInStage;
		FillInStage deepCopyOfFillInStage;

		originFillInStage = new FillInStage();
		originFillInStage.setDefaultFeedback("Deep copy test of basic fill in stage.");
		originFillInStage.setDefaultResult(42);

		deepCopyOfFillInStage = originFillInStage.deepCopy();

		assertNotEquals(originFillInStage, deepCopyOfFillInStage, "The fill in stage is the origin itself.");
		assertEquals("Deep copy test of basic fill in stage.", deepCopyOfFillInStage.getDefaultFeedback(),
				"The default feedbacks of fill in stages are different");
		assertEquals(42, deepCopyOfFillInStage.getDefaultResult(),
				"The default results of fill in stages are different");
		assertTrue(deepCopyOfFillInStage.getFeedbackRulesAsList().isEmpty(), "Rules for the fill in stage are set.");
		assertTrue(deepCopyOfFillInStage.getFillInFields().isEmpty(), "Fill in fields for the fill in stage are set.");
		assertTrue(deepCopyOfFillInStage.getDropDownFields().isEmpty(),
				"Drop down fields for the fill in stage are set.");
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE,
				deepCopyOfFillInStage.getFormularEditorPaletteEnum(),
				"The formular editor for the fill in stage is not the default 'no palette'.");

		// check stage variables
		assertNull(deepCopyOfFillInStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfFillInStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfFillInStage.getTaskDescription(), "The stage variable 'task description' is different.");
		assertNull(deepCopyOfFillInStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfFillInStage.getDefaultTransition(),
				"The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfFillInStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfFillInStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfFillInStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfFillInStage.getSkipTransitions().isEmpty(),
				"The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfFillInStage.getStageTransitions().isEmpty(),
				"The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfFillInStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfFillInStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a complete fill in stage with following
	 * fields set:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - formularEditorPalette
	 * - rules (2 times)
	 * - fillInFields (2 times)
	 * - dropDownFields (2 times)
	 */
	@Test
	void deepCopyOfFullFillInStage() {
		FillInStage originFillInStage;
		FillInStage deepCopyOfFillInStage;
		Rule ruleOne;
		Rule ruleTwo;
		FillInField fillInFieldOne;
		FillInField fillInFieldTwo;
		DropDownField dropDownFieldOne;
		DropDownField dropDownFieldTwo;
		Iterator<FillInField> iteratorFillInField;
		Iterator<DropDownField> iteratorDropDownField;
		XStream xstream = new XStream();
		String firstOriginRuleXML;
		String secondOriginRuleXML;
		String firstCopiedRuleXML;
		String secondCopiedRuleXML;
		String firstOriginFillInFieldXML;
		String secondOriginFillInFieldXML;
		String firstCopiedFillInFieldXML;
		String secondCopiedFillInFieldXML;
		String firstOriginDropDownFieldXML;
		String secondOriginDropDownFieldXML;
		String firstCopiedDropDownFieldXML;
		String secondCopiedDropDownFieldXML;
		Boolean isFillInFieldEqual = false;
		Boolean isDropDownFieldEqual = false;

		// initialize and define fields of origin fill in stage
		originFillInStage = new FillInStage();
		originFillInStage.setDefaultFeedback("Deep copy test of full " + "fill in stage.");
		originFillInStage.setDefaultResult(42);

		originFillInStage.setFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC.toString());

		ruleOne = new Rule("First rule of complete fill in stage test.", 1);
		firstOriginRuleXML = xstream.toXML(ruleOne);
		ruleTwo = new Rule("Second rule of complete fill in stage test.", 2);
		secondOriginRuleXML = xstream.toXML(ruleTwo);
		originFillInStage.addFeedbackRule(ruleOne);
		originFillInStage.addFeedbackRule(ruleTwo);

		fillInFieldOne = new FillInField("First fill in field of complete " + "fill in stage test.", 1);
		firstOriginFillInFieldXML = xstream.toXML(fillInFieldOne);
		fillInFieldTwo = new FillInField("Second fill in field of complete " + "fill in stage test.", 2);
		secondOriginFillInFieldXML = xstream.toXML(fillInFieldTwo);
		originFillInStage.addFillInField(fillInFieldOne);
		originFillInStage.addFillInField(fillInFieldTwo);

		dropDownFieldOne = new DropDownField("First drop down field of " + "complete fill in stage test.", 1);
		firstOriginDropDownFieldXML = xstream.toXML(dropDownFieldOne);
		dropDownFieldTwo = new DropDownField("Second drop down field of " + "complete fill in stage test.", 2);
		secondOriginDropDownFieldXML = xstream.toXML(dropDownFieldTwo);
		originFillInStage.addDropDownField(dropDownFieldOne);
		originFillInStage.addDropDownField(dropDownFieldTwo);

		deepCopyOfFillInStage = originFillInStage.deepCopy();

		// check copied fields of fill in stage
		assertNotEquals(originFillInStage, deepCopyOfFillInStage, "The mc stage is the origin itself.");
		assertEquals("Deep copy test of full fill in stage.", deepCopyOfFillInStage.getDefaultFeedback(),
				"The default feedback of fill in stage are different");
		assertEquals(42, deepCopyOfFillInStage.getDefaultResult(), "The default results are different");
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC.toString(),
				deepCopyOfFillInStage.getFormularEditorPalette(),
				"The formular editor palette of fill in stage is different.");

		// === Feedback Rules ===
		assertFalse(deepCopyOfFillInStage.getFeedbackRulesAsList().isEmpty(),
				"No feedback rules are defined for full fill in stage.");
		assertEquals(2, deepCopyOfFillInStage.getFeedbackRulesAsList().size(),
				"The defined amount of feedback rules is not equal to two.");
		firstCopiedRuleXML = xstream.toXML(deepCopyOfFillInStage.getFeedbackRulesAsList().get(0));
		assertTrue(
				firstOriginRuleXML.substring(firstOriginRuleXML.indexOf("</jackId>"))
						.contentEquals(firstCopiedRuleXML.substring(firstCopiedRuleXML.indexOf("</jackId>"))),
				"The first feedback of full fill in stage is different.");

		secondCopiedRuleXML = xstream.toXML(deepCopyOfFillInStage.getFeedbackRulesAsList().get(1));
		assertTrue(
				secondOriginRuleXML.substring(secondOriginRuleXML.indexOf("</jackId>"))
						.contentEquals(secondCopiedRuleXML.substring(secondCopiedRuleXML.indexOf("</jackId>"))),
				"The second feedback of full fill in stage is different.");

		// === Fill In Fields ===
		assertFalse(deepCopyOfFillInStage.getFillInFields().isEmpty(),
				"No fill in fields are defined for full fill in stage.");
		assertEquals(2, deepCopyOfFillInStage.getFillInFields().size(),
				"The defined amount of fill in fields is not equal to two.");
		iteratorFillInField = deepCopyOfFillInStage.getFillInFields().iterator();
		firstCopiedFillInFieldXML = xstream.toXML(iteratorFillInField.next());
		secondCopiedFillInFieldXML = xstream.toXML(iteratorFillInField.next());
		/*
		 * Check the copied fill in fields without considering the
		 * order inside the set.
		 */
		if (firstOriginFillInFieldXML.substring(firstOriginFillInFieldXML.indexOf("</jackId>"))
				.contentEquals(firstCopiedFillInFieldXML.substring(firstCopiedFillInFieldXML.indexOf("</jackId>")))) {

			if (secondOriginFillInFieldXML.substring(secondOriginFillInFieldXML.indexOf("</jackId>")).contentEquals(
					secondCopiedFillInFieldXML.substring(secondCopiedFillInFieldXML.indexOf("</jackId>")))) {

				isFillInFieldEqual = true;
			}
		} else if (secondOriginFillInFieldXML.substring(secondOriginFillInFieldXML.indexOf("</jackId>"))
				.contentEquals(firstCopiedFillInFieldXML.substring(firstCopiedFillInFieldXML.indexOf("</jackId>")))) {

			if (firstOriginFillInFieldXML.substring(firstOriginFillInFieldXML.indexOf("</jackId>")).contentEquals(
					secondCopiedFillInFieldXML.substring(secondCopiedFillInFieldXML.indexOf("</jackId>")))) {

				isFillInFieldEqual = true;
			}
		} else {

			isFillInFieldEqual = false;
		}
		assertTrue(isFillInFieldEqual, "The copy of the set of fill in fields is different.");

		// === Drop Down Fields ===
		assertFalse(deepCopyOfFillInStage.getDropDownFields().isEmpty(),
				"No drop down fields are defined for full fill in stage.");
		assertEquals(2, deepCopyOfFillInStage.getDropDownFields().size(),
				"The defined amount of drop down fields is not equal to two.");
		iteratorDropDownField = deepCopyOfFillInStage.getDropDownFields().iterator();
		firstCopiedDropDownFieldXML = xstream.toXML(iteratorDropDownField.next());
		secondCopiedDropDownFieldXML = xstream.toXML(iteratorDropDownField.next());
		/*
		 * Check the copied drop down fields without considering the
		 * order inside the set.
		 */
		if (firstOriginDropDownFieldXML.substring(firstOriginDropDownFieldXML.indexOf("</jackId>")).contentEquals(
				firstCopiedDropDownFieldXML.substring(firstCopiedDropDownFieldXML.indexOf("</jackId>")))) {

			if (secondOriginDropDownFieldXML.substring(secondOriginDropDownFieldXML.indexOf("</jackId>")).contentEquals(
					secondCopiedDropDownFieldXML.substring(secondCopiedDropDownFieldXML.indexOf("</jackId>")))) {

				isDropDownFieldEqual = true;
			}
		} else if (secondOriginDropDownFieldXML.substring(secondOriginDropDownFieldXML.indexOf("</jackId>"))
				.contentEquals(
						firstCopiedDropDownFieldXML.substring(firstCopiedDropDownFieldXML.indexOf("</jackId>")))) {

			if (firstOriginDropDownFieldXML.substring(firstOriginDropDownFieldXML.indexOf("</jackId>")).contentEquals(
					secondCopiedDropDownFieldXML.substring(secondCopiedDropDownFieldXML.indexOf("</jackId>")))) {

				isDropDownFieldEqual = true;
			}
		} else {

			isDropDownFieldEqual = false;
		}
		assertTrue(isDropDownFieldEqual, "The copie of the set of drop down fields is different.");

		// check stage variables
		assertNull(deepCopyOfFillInStage.getInternalName(), "The stage variable 'internal name' is different.");
		assertNull(deepCopyOfFillInStage.getExternalName(), "The stage variable 'external name' is different.");
		assertNull(deepCopyOfFillInStage.getTaskDescription(),
				"The stage variable 'task description' is different.");
		assertNull(deepCopyOfFillInStage.getSkipMessage(), "The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfFillInStage.getDefaultTransition(),
				"The stage variable 'default transition' is not set.");
		assertEquals(1, deepCopyOfFillInStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(0, deepCopyOfFillInStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertFalse(deepCopyOfFillInStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertTrue(deepCopyOfFillInStage.getSkipTransitions().isEmpty(),
				"The stage variable 'skip transitions' is set.");
		assertTrue(deepCopyOfFillInStage.getStageTransitions().isEmpty(),
				"The stage variable 'stage transitions' is set.");
		assertTrue(deepCopyOfFillInStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertTrue(deepCopyOfFillInStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertTrue(deepCopyOfFillInStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
	}

	/**
	 * This test checks the deepCopy of a fill in stage with following
	 * fields set:
	 *
	 * - defaultFeedback
	 * - defaulResult
	 * - all stage variables
	 */
	@Test
	void deepCopyOfFillInStageWithStageVariables() {
		FillInStage originFillInStage;
		FillInStage deepCopyOfFillInStage;
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
		FillInStage stageOfDeepCopiedHintStage = new FillInStage();

		originFillInStage = new FillInStage();
		originFillInStage
				.setDefaultFeedback("Deep copy test of fill in " + "stage with all super " + "stage variables.");
		originFillInStage.setDefaultResult(42);

		// define stage variables
		originFillInStage.setInternalName("internal name test");
		originFillInStage.setExternalName("external name test");
		originFillInStage.setTaskDescription("task description test");
		originFillInStage.setSkipMessage("skip message test");

		defaultTransition = new StageTransition();
		defaultTransition.setStageExpression(new EvaluatorExpression("expression code of default transition"));
		originFillInStage.setDefaultTransition(defaultTransition);
		originFillInStage.setWeight(5);
		originFillInStage.setOrderIndex(3);
		originFillInStage.setAllowSkip(true);

		skipTransition1 = new StageTransition();
		skipTransition2 = new StageTransition();
		skipTransition1.setStageExpression(new EvaluatorExpression("expression code of skip transition 1"));
		skipTransition2.setStageExpression(new EvaluatorExpression("expression code of skip transition 2"));
		originFillInStage.addSkipTransition(skipTransition1);
		originFillInStage.addSkipTransition(skipTransition2);

		stageTransition1 = new StageTransition();
		stageTransition2 = new StageTransition();
		stageTransition1.setStageExpression(new EvaluatorExpression("expression code of stage transition 1"));
		stageTransition2.setStageExpression(new EvaluatorExpression("expression code of stage transition 2"));
		originFillInStage.addStageTransition(stageTransition1);
		originFillInStage.addStageTransition(stageTransition2);

		stageHint1 = new StageHint();
		stageHint1.setText("text of hint 1");
		originFillInStage.addHint(stageHint1);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable before check 1"));
		variableUpdate2
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable before check 2"));
		originFillInStage.addVariableUpdateBeforeCheck(variableUpdate1);
		originFillInStage.addVariableUpdateBeforeCheck(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable after check 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable after check 2"));
		originFillInStage.addVariableUpdateAfterCheck(variableUpdate1);
		originFillInStage.addVariableUpdateAfterCheck(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on normal exit 1"));
		variableUpdate2
				.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on normal exit 2"));
		originFillInStage.addVariableUpdateOnNormalExit(variableUpdate1);
		originFillInStage.addVariableUpdateOnNormalExit(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on repeat 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on repeat 2"));
		originFillInStage.addVariableUpdateOnRepeat(variableUpdate1);
		originFillInStage.addVariableUpdateOnRepeat(variableUpdate2);

		variableUpdate1 = new VariableUpdate();
		variableUpdate2 = new VariableUpdate();
		variableUpdate1.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on skip 1"));
		variableUpdate2.setUpdateCode(new EvaluatorExpression("expression code of update " + "variable on skip 2"));
		originFillInStage.addVariableUpdateOnSkip(variableUpdate1);
		originFillInStage.addVariableUpdateOnSkip(variableUpdate2);

		resource1 = new StageResource();
		resource2 = new StageResource();
		resource1.setDescription("description of resource 1");
		resource2.setDescription("description of resource 2");
		originFillInStage.addStageResource(resource1);
		originFillInStage.addStageResource(resource2);

		deepCopyOfFillInStage = originFillInStage.deepCopy();

		// get private field "stage" of deepcopied stage
		try {
			StageHint tempDeepCopyStageHint = new StageHint();

			if (!deepCopyOfFillInStage.getHints().isEmpty()) {
				tempDeepCopyStageHint = deepCopyOfFillInStage.getHints().get(0);
			}

			Field fdStage = tempDeepCopyStageHint.getClass().getDeclaredField("stage");
			fdStage.setAccessible(true);
			stageOfDeepCopiedHintStage = (FillInStage) fdStage.get(tempDeepCopyStageHint);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new AssertionError("Field could not be found.");
		}

		assertNotEquals(originFillInStage, deepCopyOfFillInStage, "The fill in stage is the origin itself.");
		assertEquals("Deep copy test of fill in stage with all super stage variables.",
				deepCopyOfFillInStage.getDefaultFeedback(), "The default feedbacks of fill in stages are different");
		assertEquals(42, deepCopyOfFillInStage.getDefaultResult(),
				"The default results of fill in stages are different");
		assertTrue(deepCopyOfFillInStage.getFeedbackRulesAsList().isEmpty(), "Rules for the fill in stage are set.");
		assertTrue(deepCopyOfFillInStage.getFillInFields().isEmpty(), "Fill in fields for the fill in stage are set.");
		assertTrue(deepCopyOfFillInStage.getDropDownFields().isEmpty(),
				"Drop down fields for the fill in stage are set.");
		assertEquals(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE,
				deepCopyOfFillInStage.getFormularEditorPaletteEnum(),
				"The formular editor for the fill in stage is not the default 'no palette'.");

		// check stage variables
		assertEquals("internal name test", deepCopyOfFillInStage.getInternalName(),
				"The stage variable 'internal name' is different.");
		assertEquals("external name test", deepCopyOfFillInStage.getExternalName(),
				"The stage variable 'external name' is different.");
		assertEquals("task description test", deepCopyOfFillInStage.getTaskDescription(),
				"The stage variable 'task description' is different.");
		assertEquals("skip message test", deepCopyOfFillInStage.getSkipMessage(),
				"The stage variable 'skip message' is different.");
		assertNotNull(deepCopyOfFillInStage.getDefaultTransition(),
				"The stage variable 'default transition' is not set.");
		assertEquals("expression code of default transition",
				deepCopyOfFillInStage.getDefaultTransition().getStageExpression().getCode(),
				"The stage variable 'default transition' is different.");
		assertEquals(5, deepCopyOfFillInStage.getWeight(), "The stage variable 'weight' is not the default.");
		assertEquals(3, deepCopyOfFillInStage.getOrderIndex(), "The stage variable 'order index' is not the default.");
		assertTrue(deepCopyOfFillInStage.getAllowSkip(), "The stage variable 'allow skip' is the default.");
		assertFalse(deepCopyOfFillInStage.getSkipTransitions().isEmpty(),
				"The stage variable 'skip transitions' is set.");
		assertEquals("expression code of skip transition 1",
				deepCopyOfFillInStage.getSkipTransitions().get(0).getStageExpression().getCode(),
				"The first skip transition is different.");
		assertEquals("expression code of skip transition 2",
				deepCopyOfFillInStage.getSkipTransitions().get(1).getStageExpression().getCode(),
				"The second skip transition is different.");
		assertFalse(deepCopyOfFillInStage.getStageTransitions().isEmpty(),
				"The stage variable 'stage transitions' is set.");
		assertEquals(2, deepCopyOfFillInStage.getStageTransitions().size(),
				"The stage variable 'stage transitions' is to small.");
		assertEquals("expression code of stage transition 1",
				deepCopyOfFillInStage.getStageTransitions().get(0).getStageExpression().getCode(),
				"The first stage transition is different.");
		assertEquals("expression code of stage transition 2",
				deepCopyOfFillInStage.getStageTransitions().get(1).getStageExpression().getCode(),
				"The second stage transition is different.");
		assertFalse(deepCopyOfFillInStage.getHints().isEmpty(), "The stage variable 'hints' is set.");
		assertEquals(1, deepCopyOfFillInStage.getHints().size(), "The stage variable 'hints' greater than 1.");
		assertEquals("text of hint 1", deepCopyOfFillInStage.getHints().get(0).getText(), "The hint is different.");
		/*
		 * For proving the internal stage of hint stage only the internal name
		 * of the origin fill in stage will checked. All other entities, e. g.
		 * internal transitions, are proofed in deepCopy tests of the
		 * specific entities.
		 */
		assertEquals("internal name test", stageOfDeepCopiedHintStage.getInternalName(),
				"The stage of hints are different.");
		assertFalse(deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().isEmpty(),
				"The stage variable 'variable updates before check' is set.");
		assertEquals(2, deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().size(),
				"The stage variable 'VariableUpdateBeforeCheck' is to small.");
		assertEquals("expression code of update variable before check 1",
				deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().get(0).getUpdateCode().getCode(),
				"The first update variable before check is different.");
		assertEquals("expression code of update variable before check 2",
				deepCopyOfFillInStage.getVariableUpdatesBeforeCheck().get(1).getUpdateCode().getCode(),
				"The second update variable before check is different.");
		assertFalse(deepCopyOfFillInStage.getVariableUpdatesAfterCheck().isEmpty(),
				"The stage variable 'variable updates after check' is set.");
		assertEquals(2, deepCopyOfFillInStage.getVariableUpdatesAfterCheck().size(),
				"The stage variable 'VariableUpdateAfterCheck' is to small.");
		assertEquals("expression code of update variable after check 1",
				deepCopyOfFillInStage.getVariableUpdatesAfterCheck().get(0).getUpdateCode().getCode(),
				"The first update variable after check is different.");
		assertEquals("expression code of update variable after check 2",
				deepCopyOfFillInStage.getVariableUpdatesAfterCheck().get(1).getUpdateCode().getCode(),
				"The second update variable after check is different.");
		assertFalse(deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().isEmpty(),
				"The stage variable 'variable updates on normal exit' is set.");
		assertEquals(2, deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().size(),
				"The stage variable 'variable updates on normal exit' is to small.");
		assertEquals("expression code of update variable on normal exit 1",
				deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().get(0).getUpdateCode().getCode(),
				"The first update variable on normal exit is different.");
		assertEquals("expression code of update variable on normal exit 2",
				deepCopyOfFillInStage.getVariableUpdatesOnNormalExit().get(1).getUpdateCode().getCode(),
				"The second update variable normal exit is different.");
		assertFalse(deepCopyOfFillInStage.getVariableUpdatesOnRepeat().isEmpty(),
				"The stage variable 'variable updates on repeat' is set.");
		assertEquals(2, deepCopyOfFillInStage.getVariableUpdatesOnRepeat().size(),
				"The stage variable 'variable update on repeat' is to small.");
		assertEquals("expression code of update variable on repeat 1",
				deepCopyOfFillInStage.getVariableUpdatesOnRepeat().get(0).getUpdateCode().getCode(),
				"The first update variable on repeat is different.");
		assertEquals("expression code of update variable on repeat 2",
				deepCopyOfFillInStage.getVariableUpdatesOnRepeat().get(1).getUpdateCode().getCode(),
				"The second update variable on repeat is different.");
		assertFalse(deepCopyOfFillInStage.getVariableUpdatesOnSkip().isEmpty(),
				"The stage variable 'variable updates on skip' is set.");
		assertEquals(2, deepCopyOfFillInStage.getVariableUpdatesOnSkip().size(),
				"The stage variable 'variable update on Skip' is to small.");
		assertEquals("expression code of update variable on skip 1",
				deepCopyOfFillInStage.getVariableUpdatesOnSkip().get(0).getUpdateCode().getCode(),
				"The first update variable on skip is different.");
		assertEquals("expression code of update variable on skip 2",
				deepCopyOfFillInStage.getVariableUpdatesOnSkip().get(1).getUpdateCode().getCode(),
				"The second update variable on skip is different.");
		assertFalse(deepCopyOfFillInStage.getStageResources().isEmpty(), "The stage variable 'resources' is set.");
		assertEquals(2, deepCopyOfFillInStage.getStageResources().size(),
				"The stage variable 'stage resources' is to small.");
		assertEquals("description of resource 1", deepCopyOfFillInStage.getStageResources().get(0).getDescription(),
				"The first resource is different.");
		assertEquals("description of resource 2", deepCopyOfFillInStage.getStageResources().get(1).getDescription(),
				"The second resource is different.");
	}

}
