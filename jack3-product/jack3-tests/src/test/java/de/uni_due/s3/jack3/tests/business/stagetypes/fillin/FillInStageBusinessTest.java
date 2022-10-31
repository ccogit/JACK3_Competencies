package de.uni_due.s3.jack3.tests.business.stagetypes.fillin;

import static de.uni_due.s3.jack3.builders.FillInStageBuilder.FILLIN_FIELD_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmath.OMA;
import org.openmath.OMF;
import org.openmath.OMI;
import org.openmath.OMOBJ;
import org.openmath.OMS;
import org.openmath.OMSTR;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.exceptions.WrongStageBusinessException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.business.stagetypes.FillInStageBusiness;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.SubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.SubmissionLogEntryService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;
import de.uni_due.s3.openobject.OpenObject;

@NeedsEureka
class FillInStageBusinessTest extends AbstractBusinessTest {

	@Inject
	private FillInStageBusiness stageBusiness;

	@Inject
	private SubmissionLogEntryService submissionLogEntryService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	private ContentFolder folder;

	private FillInStage stage;

	private FillInSubmission fillInStagesubmission;

	private User user;

	private AbstractExercise exercise;

	private Submission submission;

	@BeforeEach
	void prepareTest() {
		user = getAdmin("user");
		folder = new ContentFolder("Content Folder");

		baseService.persist(folder);

		user.setPersonalFolder(folder);

		exercise = createExercise();

		mergeExercise();
		submission = new Submission(user, exercise, null, false);
		submission = baseService.merge(submission);

		fillInStagesubmission = new FillInSubmission();
		fillInStagesubmission.setStageId(stage.getId());
		baseService.persist(fillInStagesubmission);
	}

	private Exercise createExercise() {
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");
		String input2 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "2");
		String input3 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "3");
		String input4 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "4");

		Exercise result = new ExerciseBuilder("Test").withFillInStage() //
				.withTitle("Multiplikation") //
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("Feld 1 richtig", input1 + " == 1", "Das erste Feld ist richtig.", 25, false) //
				.withFeedbackRule("Feld 1 falsch", input1 + " != 1", "Das erste Feld ist falsch.", 0, false) //
				.withFeedbackRule("Feld 2 richtig", input2 + " == 2", "Das zweite Feld ist richtig.", 25, false) //
				.withFeedbackRule("Feld 2 falsch", input2 + " != 2", "Das zweite Feld ist falsch.", 0, false) //
				.withFeedbackRule("Feld 3 richtig", input3 + " == 3", "Das dritte Feld ist richtig.", 25, false) //
				.withFeedbackRule("Feld 3 falsch", input3 + " != 3", "Das dritte Feld ist falsch.", 0, false) //
				.withFeedbackRule("Feld 4 richtig", input4 + " == 4", "Das vierte Feld ist richtig.", 25, false) //
				.withFeedbackRule("Feld 4 falsch", input4 + " != 4", "Das vierte Feld ist falsch.", 0, false) //
				.withFeedbackRule("Terminale Regel", "1==1", "Immer da!", 0, true)
				.withFeedbackRule("Das Ende", "1==1", "Wird nie getriggert!", -100, false).and() //
				.create();
		folder.addChildExercise(result);
		baseService.merge(folder);
		baseService.persist(result);
		return result;
	}

	private void mergeExercise() {
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStartStage();
	}

	@Test
	void incorrectClassUsageTest() {
		final MCSubmission mcsubmission = new MCSubmission();
		assertThrows(WrongStageBusinessException.class, () -> {
			stageBusiness.prepareSubmission(submission, stage, mcsubmission);
		});
	}

	@Test
	void prepareSubmissionTest() {
		assertEquals(0, fillInStagesubmission.getSubmissionFields().size());
		stageBusiness.prepareSubmission(submission, stage, fillInStagesubmission);
		assertEquals(4, fillInStagesubmission.getSubmissionFields().size());
	}

	@Test
	void startGradingTest() {
		stageBusiness.prepareSubmission(submission, stage, fillInStagesubmission);

		for (SubmissionField element : fillInStagesubmission.getOrderedSubmissionFields()) {
			if (element instanceof FillInSubmissionField) {
				FillInSubmissionField field = (FillInSubmissionField) element;
				field.setUserInput(field.getFieldName().replaceFirst(FILLIN_FIELD_PREFIX, ""));
			}
		}
		fillInStagesubmission = baseService.merge(fillInStagesubmission);

		submission = baseService.merge(submission);
		SubmissionLogEntry submissionLogEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, fillInStagesubmission);
		submission.addSubmissionLogEntry(submissionLogEntry);
		submission = baseService.merge(submission);
		baseService.merge(submission);

		assertEquals(0, fillInStagesubmission.getPoints());
		stageBusiness.startGrading(submission, stage, fillInStagesubmission,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, fillInStagesubmission, stage, false));
		assertEquals(100, fillInStagesubmission.getPoints());

		final String feedback = fillInStagesubmission.getResults().iterator().next().getPublicComment();
		assertFalse(feedback.contains("Wird nie getriggert!"));
		assertTrue(feedback.contains("Immer da!"));
	}

	@Test
	void evaluateTransitionTest() throws CalculatorException, InternalErrorEvaluatorException {
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");
		String input2 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "2");
		prepareSubmissionTest();
		StageTransition t = new StageTransition(stage);
		EvaluatorExpression expr = new EvaluatorExpression();
		expr.setCode(input1 + " == 1 && " + input2 + " == 2");
		t.setStageExpression(expr);
		stage.addStageTransition(t);
		mergeExercise();

		// Test with correct answer:
		for (SubmissionField element : fillInStagesubmission.getOrderedSubmissionFields()) {
			if (element instanceof FillInSubmissionField) {
				FillInSubmissionField field = (FillInSubmissionField) element;
				field.setUserInput(field.getFieldName().replaceFirst(FILLIN_FIELD_PREFIX, ""));

			}
		}
		fillInStagesubmission = baseService.merge(fillInStagesubmission);

		submission = baseService.merge(submission);
		assertTrue(stageBusiness.evaluateTransition(submission, stage, fillInStagesubmission, t,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, fillInStagesubmission, stage, false)));

		// Test with incorrect answer:
		for (SubmissionField element : fillInStagesubmission.getOrderedSubmissionFields()) {
			if (element instanceof FillInSubmissionField) {
				FillInSubmissionField field = (FillInSubmissionField) element;
				field.setUserInput("2");
			}
		}
		fillInStagesubmission = baseService.merge(fillInStagesubmission);

		submission = baseService.merge(submission);
		assertFalse(stageBusiness.evaluateTransition(submission, stage, fillInStagesubmission, t,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, fillInStagesubmission, stage, false)));
	}

	@Test
	void updateStatusTest() {
		fillInStagesubmission = (FillInSubmission) stageBusiness.updateStatus(fillInStagesubmission, null);
		assertTrue(fillInStagesubmission.hasPendingChecks());
		assertFalse(fillInStagesubmission.hasInternalErrors());
		assertEquals(0, fillInStagesubmission.getPoints());

		Result r1 = new Result();
		r1.setPoints(10);
		r1.setSubmission(fillInStagesubmission);
		Result r2 = new Result();
		r2.setPoints(98);
		r2.setSubmission(fillInStagesubmission);
		baseService.persist(r1);
		baseService.persist(r2);

		fillInStagesubmission.addResult(r1);
		fillInStagesubmission.addResult(r2);

		fillInStagesubmission = baseService.merge(fillInStagesubmission);
		fillInStagesubmission = (FillInSubmission) stageBusiness.updateStatus(fillInStagesubmission, null);
		assertFalse(fillInStagesubmission.hasPendingChecks());
		assertTrue(fillInStagesubmission.hasInternalErrors());

		fillInStagesubmission.clearResults();

		Result r3 = new Result();
		r3.setPoints(33);
		r3.setSubmission(fillInStagesubmission);
		baseService.persist(r3);

		fillInStagesubmission.addResult(r3);
		fillInStagesubmission = baseService.merge(fillInStagesubmission);

		assertEquals(0, fillInStagesubmission.getPoints());

		fillInStagesubmission = (FillInSubmission) stageBusiness.updateStatus(fillInStagesubmission, null);
		assertFalse(fillInStagesubmission.hasPendingChecks());
		assertFalse(fillInStagesubmission.hasInternalErrors());
		assertEquals(33, fillInStagesubmission.getPoints());
	}

	@Test
	void removeFeedbackFromStageTest() {

		// remove every Feedbackrule from the Stage
		for (int i = stage.getFeedbackRulesAsList().size(); i > 0; i--) {
			assertEquals(i, stage.getFeedbackRulesAsList().size());
			stageBusiness.removeFeedbackFromStage(i - 1, stage);
			mergeExercise();
			assertEquals(i - 1, stage.getFeedbackRulesAsList().size());
		}

		// adding 3 Rules
		Rule r1 = new Rule("rule1", 0);
		r1.setFeedbackText("ExampleText");
		Rule r2 = new Rule("rule2", 1);
		r2.setFeedbackText("another ExampleText");
		Rule r3 = new Rule("rule3", 2);
		r3.setFeedbackText("text");
		stage.addFeedbackRule(r1);
		stage.addFeedbackRule(r2);
		stage.addFeedbackRule(r3);

		mergeExercise();

		assertEquals(3, stage.getFeedbackRulesAsList().size());
		// remove "rule2"
		stageBusiness.removeFeedbackFromStage(1, stage);

		mergeExercise();

		assertTrue(stage.getFeedbackRulesAsList().stream().map(Rule::getName).anyMatch("rule1"::equals));
		assertTrue(stage.getFeedbackRulesAsList().stream().map(Rule::getName).anyMatch("rule3"::equals));
		assertFalse(stage.getFeedbackRulesAsList().stream().map(Rule::getName).anyMatch("rule2"::equals));
		// checking Order
		assertEquals(0, stage.getFeedbackRulesAsList().get(0).getOrderIndex());
		assertEquals("rule1", stage.getFeedbackRulesAsList().get(0).getName());
		assertEquals(1, stage.getFeedbackRulesAsList().get(1).getOrderIndex());
		assertEquals("rule3", stage.getFeedbackRulesAsList().get(1).getName());
	}

	@Test
	void reorderFeedbackRulesTest() {
		assertEquals("Feld 1 richtig", stage.getFeedbackRulesAsList().get(0).getName());
		assertEquals(0, stage.getFeedbackRulesAsList().get(0).getOrderIndex());
		assertEquals("Feld 4 falsch", stage.getFeedbackRulesAsList().get(7).getName());
		assertEquals(7, stage.getFeedbackRulesAsList().get(7).getOrderIndex());

		stageBusiness.reorderFeedbackRules(stage, 7, 0);
		mergeExercise();

		assertEquals("Feld 1 richtig", stage.getFeedbackRulesAsList().get(1).getName());
		assertEquals(1, stage.getFeedbackRulesAsList().get(1).getOrderIndex());
		assertEquals("Feld 4 falsch", stage.getFeedbackRulesAsList().get(0).getName());
		assertEquals(0, stage.getFeedbackRulesAsList().get(0).getOrderIndex());
	}

	@Test
	void getInputVariablesTest() throws Exception {
		stageBusiness.prepareSubmission(submission, stage, fillInStagesubmission);
		for (SubmissionField element : fillInStagesubmission.getOrderedSubmissionFields()) {
			if (element instanceof FillInSubmissionField) {
				FillInSubmissionField field = (FillInSubmissionField) element;
				field.setUserInput(field.getFieldName().replaceFirst(FILLIN_FIELD_PREFIX, ""));
			}
		}

		fillInStagesubmission = baseService.merge(fillInStagesubmission);
		Map<String, VariableValue> map = stageBusiness.getInputVariables(fillInStagesubmission);
		assertEquals(4, map.size());

		assertEquals("1", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "1").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("2", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "2").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("3", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "3").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("4", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "4").getContent()).getOMOBJ()
				.getOMI().getValue());

		// Test with other input
		for (int i = 0; i < fillInStagesubmission.getOrderedSubmissionFields().size(); i++) {
			if (fillInStagesubmission.getOrderedSubmissionFields().get(i) instanceof FillInSubmissionField) {
				FillInSubmissionField field = (FillInSubmissionField) fillInStagesubmission.getOrderedSubmissionFields()
						.get(i);
				field.setUserInput((i * 5) + "");
			}
		}
		fillInStagesubmission = baseService.merge(fillInStagesubmission);
		map = stageBusiness.getInputVariables(fillInStagesubmission);
		assertEquals(4, map.size());

		assertEquals("0", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "1").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("5", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "2").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("10", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "3").getContent()).getOMOBJ()
				.getOMI().getValue());
		assertEquals("15", OpenObjectConverter.fromXmlString(map.get(FILLIN_FIELD_PREFIX + "4").getContent()).getOMOBJ()
				.getOMI().getValue());
	}

	@Test
	void getItemPositionBasedOnOriginalItemListTest() {
		DropDownField field = new DropDownField("field1", 9);
		field.setRandomize(true);
		field.addItem("A");
		field.addItem("B");
		field.addItem("C");

		List<String> itemsWithoutRandomizedOrder = new ArrayList<>(field.getItems());
		List<String> itemsRandomized = new ArrayList<>(field.getItems());
		Collections.shuffle(itemsRandomized);

		DropDownSubmissionField submissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD, itemsRandomized, field.getRandomize(),
				itemsWithoutRandomizedOrder);

		for (int i = 0; i < field.getItems().size(); i++) {
			submissionField.setUserInput(submissionField.getItems().get(i));
			assertEquals(itemsWithoutRandomizedOrder.indexOf(submissionField.getItems().get(i)),
					stageBusiness.getSelectedDropDownPosition(submissionField));
		}
	}

	@Test
	void getFillInFieldUserInputAsOpenObjectTest() throws Exception {
		FillInField testField = new FillInField("field1", 1);
		testField.setFormularEditorType(EFillInEditorType.NUMBER.toString());

		FillInSubmissionField submissionField = new FillInSubmissionField(testField.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD, testField.getFormularEditorEnumType(),
				testField.getSize());

		// test Integer
		submissionField.setUserInput("123");
		OpenObject expectedInteger = OpenObject.of(OMOBJ.of(OMI.of(123)));
		assertEquals(expectedInteger,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test Double
		submissionField.setUserInput("0,42");
		OpenObject expectedDouble = OpenObject.of(OMOBJ.of(OMF.of(0.42)));
		assertEquals(expectedDouble,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));
		submissionField.setUserInput("0.42");
		assertEquals(expectedDouble,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));
		submissionField.setUserInput(".42");
		assertEquals(expectedDouble,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test String
		submissionField.setUserInput("my Answer");
		OpenObject expectedString = OpenObject.of(OMOBJ.of(OMSTR.of("my Answer")));
		assertEquals(expectedString,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test Empty
		submissionField.setUserInput("");
		OpenObject expectedEmpty = OpenObject.of(OMOBJ.of(OMSTR.of("")));
		assertEquals(expectedEmpty,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test Trim
		submissionField.setUserInput(" 1234 ");
		OpenObject expectedTrim = OpenObject.of(OMOBJ.of(OMI.of(1234)));
		assertEquals(expectedTrim,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test unary plus +
		submissionField.setUserInput("+4.4");
		OpenObject expectedUnaryPlus = OpenObject
				.of(OMOBJ.of(OMA.of(OMS.of("arith1", "unary_plus"), Arrays.asList(OMF.of(4.4)))));
		assertEquals(expectedUnaryPlus,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));

		// test unary minus -
		submissionField.setUserInput("-3");
		OpenObject expectedUnaryMinus = OpenObject
				.of(OMOBJ.of(OMA.of(OMS.of("arith1", "unary_minus"), Arrays.asList(OMI.of(3)))));
		assertEquals(expectedUnaryMinus,
				OpenObjectConverter.fromXmlString(stageBusiness.getFillInFieldUserInput(submissionField)));
	}

	@Test
	void getSelectedDropDownPositionAsOpenObjectTest() {
		DropDownField field = new DropDownField("field1", 9);
		field.setRandomize(true);
		field.addItem("A");
		field.addItem("B");
		field.addItem("C");

		List<String> itemsWithoutRandomizedOrder = new ArrayList<>(field.getItems());
		List<String> itemsRandomized = new ArrayList<>(field.getItems());
		Collections.shuffle(itemsRandomized);

		DropDownSubmissionField submissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD, itemsRandomized, field.getRandomize(),
				itemsWithoutRandomizedOrder);

		for (int i = 0; i < field.getItems().size(); i++) {
			submissionField.setUserInput(submissionField.getItems().get(i));
			OpenObject openObject = stageBusiness.getSelectedDropDownPositionAsOpenObject(submissionField);
			assertEquals(itemsWithoutRandomizedOrder.indexOf(submissionField.getItems().get(i)),
					Integer.parseInt(openObject.getOMOBJ().getOMI().getValue()));
		}
	}
}
