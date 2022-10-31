package de.uni_due.s3.jack3.tests.business.stagetypes.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmath.OMS;

import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.exceptions.WrongStageBusinessException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.business.stagetypes.MCStageBusiness;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.SubmissionLogEntryService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

@NeedsEureka
class MCStageBusinessTest extends AbstractBusinessTest {

	@Inject
	private MCStageBusiness mcStageBusiness;

	@Inject
	private SubmissionLogEntryService submissionLogEntryService;

	@Inject
	private DevelopmentBusiness developmentBusiness;

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	private ContentFolder folder;

	private MCStage stage;

	private MCSubmission mcStagesubmission;

	private User user;

	private AbstractExercise exercise;

	private CourseRecord courseRecord;
	private Submission submission;

	@BeforeEach
	void prepareTest() {
		user = getAdmin("user");
		folder = new ContentFolder("Content Folder");

		baseService.persist(folder);

		user.setPersonalFolder(folder);

		developmentBusiness.createSampleExercise(user, 1);
		exercise = exerciseService.getAllExercisesForContentFolderEagerly(folder).get(0);

		stage = (MCStage) exercise.getStartStage();
		stage.setRandomize(false);
		exercise = exerciseService.mergeExercise(exercise);
		submission = new Submission(user, exercise, null, false);
		submission = baseService.merge(submission);

		mcStagesubmission = new MCSubmission();
		mcStagesubmission.setStageId(stage.getId());
		baseService.persist(mcStagesubmission);
	}

	@Test
	void incorrectClassUsageTest() {
		final StageSubmission stagesubmission = new FillInSubmission();
		assertThrows(WrongStageBusinessException.class, () -> {
			mcStageBusiness.prepareSubmission(submission, stage, stagesubmission);
		});
	}

	@Test
	void prepareSubmissionTest() {
		assertTrue(mcStagesubmission.getOptionsOrder().isEmpty());
		mcStagesubmission = (MCSubmission) mcStageBusiness.prepareSubmission(submission, stage, mcStagesubmission);
		List<Integer> list = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			list.add(i);
		}

		assertEquals(list, mcStagesubmission.getOptionsOrder());

		stage.setRandomize(true);
		mcStagesubmission = (MCSubmission) mcStageBusiness.prepareSubmission(submission, stage, mcStagesubmission);
		assertEquals(3, mcStagesubmission.getOptionsOrder().size());
	}

	@Test
	void startGradingTest() {
		// Testing correct submission
		mcStagesubmission.setTickedPattern("100");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		SubmissionLogEntry submissionLogEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, mcStagesubmission);
		submission.addSubmissionLogEntry(submissionLogEntry);
		submission = baseService.merge(submission);

		mcStagesubmission = (MCSubmission) mcStageBusiness.startGrading(submission, stage, mcStagesubmission,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false));

		assertEquals(100, mcStagesubmission.getPoints());
		assertEquals("Das ist korrekt.", mcStagesubmission.getResults().iterator().next().getPublicComment());
		assertFalse(mcStagesubmission.hasPendingChecks());
		assertFalse(mcStagesubmission.hasInternalErrors());

		// Testing wrong Submission
		submission = new Submission(user, exercise, courseRecord, false);
		submission = baseService.merge(submission);
		mcStagesubmission = new MCSubmission();
		mcStagesubmission.setStageId(stage.getId());
		baseService.persist(mcStagesubmission);

		mcStagesubmission.setTickedPattern("010");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		submission = baseService.merge(submission);
		submissionLogEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, mcStagesubmission);
		submission.addSubmissionLogEntry(submissionLogEntry);
		submission = baseService.merge(submission);
		baseService.merge(submission);

		mcStagesubmission = (MCSubmission) mcStageBusiness.startGrading(submission, stage, mcStagesubmission,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false));

		assertEquals(0, mcStagesubmission.getPoints());
		assertEquals("Das ist leider <u>nicht</u> korrekt.",
				mcStagesubmission.getResults().iterator().next().getPublicComment());

	}

	@Test
	void evaluateTransitionTest() throws CalculatorException, InternalErrorEvaluatorException {
		// create Transition: If the submission isn't correctly the Stage restarts
		StageTransition t = new StageTransition(stage);
		String mc0 = PlaceholderPatternProducer.forMcInputVariable(0);
		String mc1 = PlaceholderPatternProducer.forMcInputVariable(1);
		String mc2 = PlaceholderPatternProducer.forMcInputVariable(2);

		t.setStageExpression(new EvaluatorExpression("!(" + mc0 + " &&! " + mc1 + " &&! " + mc2 + ")"));
		stage.addStageTransition(t);
		exercise = exerciseService.mergeExercise(exercise);
		stage = (MCStage) exercise.getStartStage();

		// Test with correct answer: Stage should not start again
		mcStagesubmission.setTickedPattern("100");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		submission = baseService.merge(submission);
		assertFalse(mcStageBusiness.evaluateTransition(submission, stage, mcStagesubmission, t,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false)));

		// Test with incorrect answer: Stage should start again
		mcStagesubmission.setTickedPattern("010");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		submission = baseService.merge(submission);
		assertTrue(mcStageBusiness.evaluateTransition(submission, stage, mcStagesubmission, t,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false)));
	}

	@Test
	void updateStatusTest() {
		mcStagesubmission = (MCSubmission) mcStageBusiness.updateStatus(mcStagesubmission, null);
		assertTrue(mcStagesubmission.hasPendingChecks());
		assertFalse(mcStagesubmission.hasInternalErrors());
		assertEquals(0, mcStagesubmission.getPoints());

		Result r1 = new Result();
		r1.setPoints(10);
		r1.setSubmission(mcStagesubmission);
		Result r2 = new Result();
		r2.setPoints(98);
		r2.setSubmission(mcStagesubmission);
		baseService.persist(r1);
		baseService.persist(r2);

		mcStagesubmission.addResult(r1);
		mcStagesubmission.addResult(r2);

		mcStagesubmission = baseService.merge(mcStagesubmission);
		mcStagesubmission = (MCSubmission) mcStageBusiness.updateStatus(mcStagesubmission, null);
		assertFalse(mcStagesubmission.hasPendingChecks());
		assertTrue(mcStagesubmission.hasInternalErrors());

		mcStagesubmission.clearResults();

		Result r3 = new Result();
		r3.setPoints(33);
		r3.setSubmission(mcStagesubmission);
		baseService.persist(r3);

		mcStagesubmission.addResult(r3);
		mcStagesubmission = baseService.merge(mcStagesubmission);

		assertEquals(0, mcStagesubmission.getPoints());

		mcStagesubmission = (MCSubmission) mcStageBusiness.updateStatus(mcStagesubmission, null);
		assertFalse(mcStagesubmission.hasPendingChecks());
		assertFalse(mcStagesubmission.hasInternalErrors());
		assertEquals(33, mcStagesubmission.getPoints());
	}

	private static void assertIsOMOBJTrue(String xmlString) {
		OMS oms = OpenObjectConverter.fromXmlString(xmlString).getOMOBJ().getOMS();
		assertEquals("logic1", oms.getCd());
		assertEquals("true", oms.getName());
	}

	private static void assertIsOMOBJFalse(String xmlString) {
		OMS oms = OpenObjectConverter.fromXmlString(xmlString).getOMOBJ().getOMS();
		assertEquals("logic1", oms.getCd());
		assertEquals("false", oms.getName());
	}

	@Test
	void getInputVariablesTest() throws Exception {
		mcStagesubmission.setTickedPattern("100");
		mcStagesubmission = baseService.merge(mcStagesubmission);
		Map<String, VariableValue> map = mcStageBusiness.getInputVariables(mcStagesubmission);
		assertEquals(3, map.size());
		assertIsOMOBJTrue(map.get(PlaceholderConstants.MC + 0).getContent());
		assertIsOMOBJFalse(map.get(PlaceholderConstants.MC + 1).getContent());
		assertIsOMOBJFalse(map.get(PlaceholderConstants.MC + 2).getContent());
	}

	@Test
	void getMetaVariablesTest() {
		mcStagesubmission.setTickedPattern("010");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		//grade the submission and generate meta variables
		SubmissionLogEntry submissionLogEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, mcStagesubmission);
		submission.addSubmissionLogEntry(submissionLogEntry);
		submission = baseService.merge(submission);
		mcStagesubmission = (MCSubmission) mcStageBusiness.startGrading(submission, stage, mcStagesubmission,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false));

		//there should be 4 meta variables
		Map<String, VariableValue> map = mcStageBusiness.getMetaVariables(mcStagesubmission);
		assertEquals(4, map.size());

		//there should be 0 correct ticks
		assertEquals("0", OpenObjectConverter.fromXmlString(map.get("mcStageCorrectTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be 1 incorrect tick
		assertEquals("1", OpenObjectConverter.fromXmlString(map.get("mcStageIncorrectTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be in total 1 tick
		assertEquals("1", OpenObjectConverter.fromXmlString(map.get("mcStageTotalTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be 3 possible answer options
		assertEquals("3", OpenObjectConverter.fromXmlString(map.get("mcStageNumberOfAnswerOptions").getContent())
				.getOMOBJ().getOMI().getValue());

		// make a new submission with other ticketPattern and check that we get other values for the meta variables
		submission = new Submission(user, exercise, null, false);
		submission = baseService.merge(submission);

		mcStagesubmission = new MCSubmission();
		mcStagesubmission.setStageId(stage.getId());
		baseService.persist(mcStagesubmission);

		mcStagesubmission.setTickedPattern("101");
		mcStagesubmission = baseService.merge(mcStagesubmission);

		//grade the submission and generate meta variables
		submissionLogEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, mcStagesubmission);
		submission.addSubmissionLogEntry(submissionLogEntry);
		submission = baseService.merge(submission);
		mcStagesubmission = (MCSubmission) mcStageBusiness.startGrading(submission, stage, mcStagesubmission,
				exercisePlayerBusiness.prepareEvaluatorMaps(submission, mcStagesubmission, stage, false));

		//there should be 4 meta variables
		map = mcStageBusiness.getMetaVariables(mcStagesubmission);
		assertEquals(4, map.size());

		//there should be 1 correct ticks
		assertEquals("1", OpenObjectConverter.fromXmlString(map.get("mcStageCorrectTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be 1 incorrect tick
		assertEquals("1", OpenObjectConverter.fromXmlString(map.get("mcStageIncorrectTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be in total 1 tick
		assertEquals("2", OpenObjectConverter.fromXmlString(map.get("mcStageTotalTicks").getContent()).getOMOBJ()
				.getOMI().getValue());
		//there should be 3 possible answer options
		assertEquals("3", OpenObjectConverter.fromXmlString(map.get("mcStageNumberOfAnswerOptions").getContent())
				.getOMOBJ().getOMI().getValue());
	}

}
