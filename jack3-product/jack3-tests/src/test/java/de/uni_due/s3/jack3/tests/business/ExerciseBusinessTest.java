package de.uni_due.s3.jack3.tests.business;

import static de.uni_due.s3.jack3.builders.FillInStageBuilder.FILLIN_FIELD_PREFIX;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmath.OMF;
import org.openmath.OMOBJ;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectFactory;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.entities.comparators.TimeComparator;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;
import de.uni_due.s3.openobject.OpenObject;

@NeedsEureka
class ExerciseBusinessTest extends AbstractBusinessTest {

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private TagService tagService;

	@Inject
	private SubmissionService submissionService;

	private User user;
	private ContentFolder folder;
	private AbstractExercise exercise;

	private static final OpenObject OO_42 = OpenObjectFactory.createOpenObjectForOpenMathInteger(42);
	private static final OpenObject OO_24 = OpenObjectFactory.createOpenObjectForOpenMathInteger(24);

	/**
	 * Prepare tests: Create user/folder (if necessary) and an empty exercise
	 *
	 * @throws ActionNotAllowedException
	 */
	@BeforeEach
	void prepareTest() throws ActionNotAllowedException {
		user = getAdmin("user");

		folder = folderBusiness.getContentFolderWithLazyData(user.getPersonalFolder());
		exercise = exerciseBusiness.createExercise("Exercise", user, folder, "de");
	}

	/**
	 * Returns all submissionlog entries that are a specific type, sorted by timestamp
	 */
	private List<SubmissionLogEntry> filterSubmissionlog(Submission submission, ESubmissionLogEntryType type) {
		return submission.getSubmissionLogAsSortedList().stream().filter(x -> x.getType() == type)
				.sorted(new TimeComparator<SubmissionLogEntry>()).collect(Collectors.toList());
	}

	/**
	 * Takes the abstractExercise exercise and creates a FrozenRevision of it.
	 *
	 * @return returns the created FrozenExercise
	 */
	private FrozenExercise createFrozenExercise(AbstractExercise exercise) {
		exercise = exerciseBusiness.updateExercise(exercise);
		exerciseBusiness.createFrozenExercise(exercise, exerciseBusiness.getRevisionNumbersFor(exercise)
				.get(exerciseBusiness.getRevisionNumbersFor(exercise).size() - 1));
		return exerciseBusiness
				.getFrozenExerciseWithLazyDataById(exerciseBusiness.getFrozenRevisionsForExercise(exercise)
						.get(exerciseBusiness.getFrozenRevisionsForExercise(exercise).size() - 1).getId());
	}

	/**
	 * Tests moving an exercise to a new folder
	 *
	 * @throws ActionNotAllowedException
	 */
	@Test
	void moveExercise() throws ActionNotAllowedException {
		final ContentFolder otherFolder = folderBusiness.createContentFolder(user, "Folder", folder);

		assertDoesNotThrow(() -> {
			exercise = exerciseBusiness.moveExercise((Exercise) exercise, otherFolder, user);
		});
		ContentFolder updatedOtherFolder = folderBusiness.getContentFolderWithLazyData(otherFolder);
		assertTrue(updatedOtherFolder.getChildrenExercises().contains(exercise));
		assertFalse(folder.getChildrenExercises().contains(exercise));
		assertEquals(updatedOtherFolder, ((Exercise) exercise).getFolder());
	}

	/**
	 * Tests deleting an exercise
	 */
	@Test
	void deleteExercise() {
		// TODO: revisit this when there is a supported way of deleting FrozenExercises
		//		FrozenExercise frozenExercise = createFrozenExercise(exercise);
		//		assertTrue(exerciseBusiness.getFrozenRevisionsForExercise(exercise).contains(frozenExercise));
		//		exerciseBusiness.deleteExercise(frozenExercise, user);
		//		assertFalse(exerciseBusiness.getFrozenRevisionsForExercise(exercise).contains(frozenExercise));

		assertDoesNotThrow(() -> exerciseBusiness.deleteExercise((Exercise) exercise, user));
		assertFalse(folder.getChildrenExercises().contains(exercise));
	}

	/**
	 * Tests registered stagetypes
	 */
	@Test
	void getRegisteredStagetypes() {
		assertTrue(exercisePlayerBusiness.getRegisteredStagetypes().contains(MCStage.class));
		assertTrue(exercisePlayerBusiness.getRegisteredStagetypes().contains(FillInStage.class));
	}

	/**
	 * Tests tags for an exercise
	 *
	 * @throws ActionNotAllowedException
	 */
	@Test
	void testTagsForExercise() throws ActionNotAllowedException {
		assertTrue(exercise.getTags().isEmpty());

		// Add a tag to the exercise
		// Expected: exercise -> tag1
		exercise.addTag(tagService.getOrCreateByName("tag1"));
		exercise = exerciseBusiness.updateExercise(exercise);
		assertTrue(exercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").contains(exercise));

		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag1"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(exercise, "tag2"));

		// Add a second tag to the exercise
		// Expected: exercise -> tag1, tag2
		exercise.addTag(tagService.getOrCreateByName("tag2"));
		exercise = exerciseBusiness.updateExercise(exercise);

		assertTrue(exercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(exercise.getTags().contains(tagService.getTagByName("tag2").get()));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").contains(exercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag2").contains(exercise));

		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag1"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag2"));

		// Add the first tag to another exercise
		// Expected: exercise -> tag1, tag2; otherExercise -> tag1
		AbstractExercise otherExercise = exerciseBusiness.createExercise("Exercise", user, user.getPersonalFolder(),
				"de");
		otherExercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(otherExercise.getId());
		exerciseBusiness.addTagToExercise(otherExercise, "tag1");
		otherExercise = exerciseBusiness.updateExercise(otherExercise);

		assertTrue(exercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(otherExercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").contains(exercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").contains(otherExercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag2").contains(exercise));

		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag1"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag2"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag1"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag2"));

		// Remove the first tag from the first exercise
		// Expected: exercise -> tag2; otherExercise -> tag1
		exerciseBusiness.removeTagFromExercise(exercise, "tag1");
		exercise = exerciseBusiness.updateExercise(exercise);

		assertFalse(exercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(otherExercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertFalse(exerciseBusiness.getAllExercisesForTagName("tag1").contains(exercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").contains(otherExercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag2").contains(exercise));

		assertFalse(exerciseBusiness.isExercisePointingToTagName(exercise, "tag1"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag2"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag1"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag2"));

		// Remove the first tag from the second exercise
		// Expected: exercise -> tag2; otherExercise -> no tag
		exerciseBusiness.removeTagFromExercise(otherExercise, "tag1");
		otherExercise = exerciseBusiness.updateExercise(otherExercise);

		assertFalse(exercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertFalse(otherExercise.getTags().contains(tagService.getTagByName("tag1").get()));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag1").isEmpty());

		assertFalse(exerciseBusiness.getAllExercisesForTagName("tag1").contains(exercise));
		assertFalse(exerciseBusiness.getAllExercisesForTagName("tag1").contains(otherExercise));
		assertTrue(exerciseBusiness.getAllExercisesForTagName("tag2").contains(exercise));
		assertFalse(exerciseBusiness.getAllExercisesForTagName("tag2").contains(otherExercise));

		assertFalse(exerciseBusiness.isExercisePointingToTagName(exercise, "tag1"));
		assertTrue(exerciseBusiness.isExercisePointingToTagName(exercise, "tag2"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(exercise, "tag3"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag1"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag2"));
		assertFalse(exerciseBusiness.isExercisePointingToTagName(otherExercise, "tag3"));

		// Check that FrozenExercise cant add or remove Tags
		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		assertTrue(exerciseBusiness.isExercisePointingToTagName(frozenExercise, "tag2"));
		assertEquals(1, frozenExercise.getTags().size());

		assertThrows(UnsupportedOperationException.class, () -> {
			exerciseBusiness.removeTagFromExercise(frozenExercise, "tag2");
		});
		assertThrows(UnsupportedOperationException.class, () -> {
			exerciseBusiness.addTagToExercise(frozenExercise, "tag1");
		});
	}

	/**
	 * Tests submitting a correct submission
	 */
	@Test
	void performStageSubmitTest1() {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create FrozenExercise
		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Check that the fresh submission gains 0 points
		assertEquals(0, submission.getResultPoints());

		// Make a correct submission
		// In particular, this is expected to trigger an update of the total result for the submission
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Check that the submission now gains 100 points
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, stageSubmission.getPoints());
		assertTrue(submission.isCompleted());

		// Repeat but with the FrozenExercise
		// Create a submission
		submission = exerciseBusiness.createSubmission(frozenExercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Check that the fresh submission gains 0 points
		assertEquals(0, submission.getResultPoints());

		// Make a correct submission
		// In particular, this is expected to trigger an update of the total result for the submission
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Check that the submission now gains 100 points
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, stageSubmission.getPoints());
		assertTrue(submission.isCompleted());
	}

	/**
	 * Tests submitting a correct submission (2 stages with weighting)
	 */
	@Test
	void performStageSubmitTest2() {
		Submission sol;
		MCSubmission subm1;
		MCSubmission subm2;

		// Build sample exercise
		exercise = new ExerciseBuilder((Exercise) exercise) //
				.withMCStage().withAnswerOption("Correct", true).withWeight(2).and() //
				.withMCStage().withAnswerOption("Correct", true).withWeight(1).and() //
				.create();
		exercise = exerciseBusiness.updateExercise(exercise);

		/*
		 ************* Stage 1 = 100% (weight=2) -> Stage 2 = 100% (weight=1) => Total=100%
		 */
		// Create FrozenExercise
		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		// Submit a correct submission for first stage
		sol = exerciseBusiness.createSubmission(exercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Although this submission is correct, it only should gain 67p due to different stage weights.
		assertEquals(67, sol.getResultPoints());

		// Submit a correct submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(100, subm1.getPoints());
		assertEquals(100, subm2.getPoints());
		assertEquals(100, sol.getResultPoints());
		assertTrue(sol.isCompleted());

		/*
		 ************* Stage 2 = 100% (weight=2) -> Stage 2 = 0% (weight=1) => Total=66,66...%
		 */

		// Submit a correct submission for first stage
		sol = exerciseBusiness.createSubmission(exercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Submit a wrong submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("0");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(100, subm1.getPoints());
		assertEquals(0, subm2.getPoints());
		assertEquals(67, sol.getResultPoints());
		assertTrue(sol.isCompleted());

		/*
		 ************* Stage 2 = 0% (weight=2) -> Stage 2 = 100% (weight=1) => Total=33,333...%
		 */

		// Submit a wrong submission for first stage
		sol = exerciseBusiness.createSubmission(exercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("0");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Submission should gain 0p at this point
		assertEquals(0, sol.getResultPoints());

		// Submit a correct submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, exercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(0, subm1.getPoints());
		assertEquals(100, subm2.getPoints());
		assertEquals(33, sol.getResultPoints());
		assertTrue(sol.isCompleted());

		// Repeat with FrozenExercise
		// Submit a correct submission for first stage
		sol = exerciseBusiness.createSubmission(frozenExercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Although this submission is correct, it only should gain 67p due to different stage weights.
		assertEquals(67, sol.getResultPoints());

		// Submit a correct submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(100, subm1.getPoints());
		assertEquals(100, subm2.getPoints());
		assertEquals(100, sol.getResultPoints());
		assertTrue(sol.isCompleted());

		/*
		 ************* Stage 1 = 100% (weight=2) -> Stage 2 = 0% (weight=1) => Total=66,66...%
		 */

		// Submit a correct submission for first stage
		sol = exerciseBusiness.createSubmission(frozenExercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Submit a wrong submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("0");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(100, subm1.getPoints());
		assertEquals(0, subm2.getPoints());
		assertEquals(67, sol.getResultPoints());
		assertTrue(sol.isCompleted());

		/*
		 ************* Stage 1 = 0% (weight=2) -> Stage 2 = 100% (weight=1) => Total=33,333...%
		 */

		// Submit a wrong submission for first stage
		sol = exerciseBusiness.createSubmission(frozenExercise, user, true);
		sol = exercisePlayerBusiness.initSubmissionForExercisePlayer(sol);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();
		subm1.setTickedPattern("0");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(0), subm1);
		subm1 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Submission should gain 0p at this point
		assertEquals(0, sol.getResultPoints());

		// Submit a correct submission for second stage
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		subm2.setTickedPattern("1");
		sol = exercisePlayerBusiness.performStageSubmit(sol, frozenExercise.getStagesAsList().get(1), subm2);
		subm2 = (MCSubmission) filterSubmissionlog(sol, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the submission now gains 100 points and is completed because no stage is ahead
		assertEquals(0, subm1.getPoints());
		assertEquals(100, subm2.getPoints());
		assertEquals(33, sol.getResultPoints());
		assertTrue(sol.isCompleted());
	}

	/**
	 * Tests submitting a correct submission (stage with optional follow-up stage)
	 */
	@Test
	void performStageSubmitTest3() {
		// Build sample exercise
		exercise = new ExerciseBuilder((Exercise) exercise) //
				.withMCStage().withAnswerOption("Correct", true).withWeight(2).and() //
				.withMCStage().withAnswerOption("Correct", true).withWeight(1).and() //
				.create();
		Stage startStage = exercise.getStartStage();
		Stage optionalStage = startStage.getDefaultTransition().getTarget();
		StageTransition optionalTransition = new StageTransition(optionalStage);
		optionalTransition.setConditionExpression(new EvaluatorExpression("[meta=stageCurrentResult]<50"));
		optionalTransition.setStageExpression(new EvaluatorExpression("true()"));
		startStage.addStageTransition(optionalTransition);
		startStage.setDefaultTransition(new StageTransition(null));
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Check that the fresh submission gains 0 points
		assertEquals(0, submission.getResultPoints());

		// Make a correct submission
		// In particular, this is expected to terminate the submission and trigger an update of the total result for the
		// submission, ignoring the points that can be gained by the optional stage.
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, startStage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Check that the submission now gains 100 points and is marked as completed
		assertEquals(100, stageSubmission.getPoints());
		assertFalse(submission.hasInternalErrors());
		assertTrue(submission.isCompleted());
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Tests skipping a stage that does not allow skipping
	 */
	@Test
	void performIllegalStageSkip() throws IllegalAccessException {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Dummies for lambda expression
		final Submission finalSubmission1 = submission;
		final StageSubmission finalstageSubmission1 = stageSubmission;

		// Skip current stage
		assertThrows(IllegalAccessException.class, () -> {
			exercisePlayerBusiness.performStageSkip(finalSubmission1, stage, finalstageSubmission1);
		});

		// Repeat with FrozenExercise
		// Create a submission
		submission = exerciseBusiness.createSubmission(frozenExercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Dummies for lambda expression
		final Submission finalSubmission2 = submission;
		final StageSubmission finalstageSubmission2 = stageSubmission;

		// Skip current stage
		assertThrows(IllegalAccessException.class, () -> {
			exercisePlayerBusiness.performStageSkip(finalSubmission2, stage, finalstageSubmission2);
		});
	}

	/**
	 * Tests skipping a stage that does allow skipping
	 */
	@Test
	void performStageSkip1() throws IllegalAccessException {

		// Add a normal mc stage to the exercise
		MCStage stage = new MCStage();
		stage.setAllowSkip(true);
		stage.addAnswerOption("Option");
		stage.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);
		exercise.addStage(stage);
		exercise.setStartStage(stage);
		exercise = exerciseBusiness.updateExercise(exercise);

		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Skip current stage
		submission = exercisePlayerBusiness.performStageSkip(submission, stage, stageSubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stageSubmission.getPoints());
		assertTrue(submission.isCompleted());

		// Repeat with FrozenExercise
		// Create a submission
		submission = exerciseBusiness.createSubmission(frozenExercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Skip current stage
		submission = exercisePlayerBusiness.performStageSkip(submission, stage, stageSubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stageSubmission.getPoints());
		assertTrue(submission.isCompleted());
	}

	/**
	 * Tests skipping a stage that has successors.
	 */
	@Test
	void performStageSkip2() throws IllegalAccessException {

		// Add a normal mc stage to the exercise
		MCStage stage1 = new MCStage();
		stage1.setAllowSkip(true);
		stage1.setExternalName("MCStage1");
		stage1.addAnswerOption("Option");
		stage1.setOrderIndex(0);
		stage1.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);

		MCStage stage2 = new MCStage();
		stage2.setAllowSkip(true);
		stage2.setExternalName("MCStage2");
		stage2.addAnswerOption("Option");
		stage2.setCorrectAnswerFeedback("1");
		stage2.setOrderIndex(1);
		stage2.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);

		exercise.addStage(stage1);
		exercise.addStage(stage2);
		exercise.setStartStage(stage1);
		stage1.setDefaultTransition(new StageTransition(stage2));
		exercise = exerciseBusiness.updateExercise(exercise);

		FrozenExercise frozenExercise = createFrozenExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();

		// Skip stage1 -> submission is not completed
		submission = exercisePlayerBusiness.performStageSkip(submission, stage1, stagesubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stagesubmission.getPoints());
		assertFalse(submission.isCompleted());

		// Skip stage2 -> submission is completed
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		submission = exercisePlayerBusiness.performStageSkip(submission, stage2, stagesubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stagesubmission.getPoints());
		assertTrue(submission.isCompleted());

		// Repeat wirh FrozenExercise
		// Create a submission
		submission = exerciseBusiness.createSubmission(frozenExercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// Skip stage1 -> submission is not completed
		submission = exercisePlayerBusiness.performStageSkip(submission, frozenExercise.getStagesAsList().get(0),
				stagesubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stagesubmission.getPoints());
		assertFalse(submission.isCompleted());

		// Skip stage2 -> submission is completed
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1).getSubmission();
		submission = exercisePlayerBusiness.performStageSkip(submission, frozenExercise.getStagesAsList().get(1),
				stagesubmission);
		assertEquals(0, submission.getResultPoints());
		assertEquals(0, stagesubmission.getPoints());
		assertTrue(submission.isCompleted());
	}

	/**
	 * Tests requesting a hint
	 */
	@Test
	void requestHint1() {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		// add hint
		StageHint hint = new StageHint();
		hint.setText("This is a hint.");
		stage.addHint(new StageHint());
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Request hint
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();
		submission = exercisePlayerBusiness.performStageHintRequest(submission, stage, stagesubmission);

		// Hint should be logged
		assertEquals(1, filterSubmissionlog(submission, ESubmissionLogEntryType.HINT).size());

		// Request a second hint should do nothing because there are no more hints
		submission = exercisePlayerBusiness.performStageHintRequest(submission, stage, stagesubmission);
		assertEquals(1, filterSubmissionlog(submission, ESubmissionLogEntryType.HINT).size());
	}

	/**
	 * Tests requesting a hint for a stage where no hints exist
	 */
	@Test
	void requestHint2() {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Request hint -> not possible
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();
		submission = exercisePlayerBusiness.performStageHintRequest(submission, stage, stagesubmission);

		// Hint should not be logged
		assertTrue(filterSubmissionlog(submission, ESubmissionLogEntryType.HINT).isEmpty());
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_MAXIMUM, 100p).
	 */
	@Test
	void requestHint3() {
		requestHintWithMalus(EStageHintMalus.CUT_MAXIMUM, true, false);
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_ACTUAL, 100p).
	 */
	@Test
	void requestHint4() {
		requestHintWithMalus(EStageHintMalus.CUT_ACTUAL, true, false);
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_MAXIMUM, 0p).
	 */
	@Test
	void requestHint5() {
		requestHintWithMalus(EStageHintMalus.CUT_MAXIMUM, false, false);
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_ACTUAL, 0p).
	 */
	@Test
	void requestHint6() {
		requestHintWithMalus(EStageHintMalus.CUT_ACTUAL, false, false);
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_MAXIMUM, 50p).
	 */
	@Test
	void requestHint7() {
		requestHintWithMalus(EStageHintMalus.CUT_MAXIMUM, true, true);
	}

	/**
	 * Tests if points are calculated correctly when hints are displayed (CUT_ACTUAL, 50p).
	 */
	@Test
	void requestHint8() {
		requestHintWithMalus(EStageHintMalus.CUT_ACTUAL, true, true);
	}

	/**
	 *
	 * @param stageHintMalus
	 * @param answer1ticked
	 * @param answer2ticked
	 * @return Expected
	 */
	void requestHintWithMalus(EStageHintMalus stageHintMalus, boolean answer1ticked, boolean answer2ticked) {
		String mc0 = PlaceholderPatternProducer.forMcInputVariable(0);
		String mc1 = PlaceholderPatternProducer.forMcInputVariable(1);
		exercise = new ExerciseBuilder((Exercise) exercise).withHintMalusType(stageHintMalus).withMCStage().selectOne()
				.withAnswerOption("Correct answer", true) // correct answer
				.withAnswerOption("Wrong answer", false) // wrong answer
				.withHint("Hint text", 40).withExtraFeedback("This is an incredible feedback.", mc0 + "&&" + mc1, 50)
				.and().create();
		exercise = exerciseBusiness.updateExercise(exercise);

		MCStage stage = (MCStage) exercise.getStartStage();

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Request hint
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();
		submission = exercisePlayerBusiness.performStageHintRequest(submission, stage, stagesubmission);

		// Submit the submission
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0).getSubmission();

		// We concatenate the ticked pattern from the two boolean parameters
		((MCSubmission) stagesubmission).setTickedPattern((answer1ticked ? "1" : "0") + (answer2ticked ? "1" : "0"));
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stagesubmission);

		/*-
		 * 40% malus ==> 60% of X
		 *
		 * result		CUT_MAXIMUM							CUT_ACTUAL						ticked pattern
		 * 100%/100		=> 60 (60% of (100% of 100))		=> 60 (60% of (100% of 100))	10
		 *  50%/100		=> 50 (min(60% of 100,50% of 100))	=> 30 (60% of (50% of 100))		11
		 *   0%/100		=> 0								=> 0							00
		 */

		// User input is not correct ==> 0 points
		if (!answer1ticked && !answer2ticked) {
			assertEquals(0, submission.getResultPoints());
		}

		// User input is correct ==> 60 points
		else if (answer1ticked && !answer2ticked) {
			assertEquals(60, submission.getResultPoints());
		} else if (stageHintMalus == EStageHintMalus.CUT_ACTUAL) {
			assertEquals(30, submission.getResultPoints());
		} else if (stageHintMalus == EStageHintMalus.CUT_MAXIMUM) {
			assertEquals(50, submission.getResultPoints());
		}
	}

	/**
	 * Tests performing a stage exit
	 */
	@Test
	void performExit() {
		prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Exit stage
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();
		submission = exercisePlayerBusiness.performExit(submission, stagesubmission);

		assertEquals(1, filterSubmissionlog(submission, ESubmissionLogEntryType.EXIT).size());
	}

	private void assertVariableEquals(VariableDeclaration varDecl, Submission submission, OpenObject expected)
			throws JAXBException {
		VariableValue varValue = null;
		for (Entry<String, VariableValue> entry : submission.getVariableValues().entrySet()) {
			if (entry.getKey().equals(varDecl.getName())) {
				varValue = entry.getValue();
			}
		}

		if (varValue == null) {
			fail("Variable Value for Declaration " + varDecl.getName() + " not found!");
		}
		OpenObject actual = OpenObjectConverter.fromXmlString(varValue.getContent());
		assertEquals(expected, actual);
	}

	/**
	 * Tests creating an exercise with a variable declaration
	 */
	@Test
	void assignVariableDeclaration() throws Exception {
		prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		String varName = exercise.getNextDefaultNameForVariables();
		// Add a variable to the exercise
		VariableDeclaration var = new VariableDeclaration(varName);
		var.getInitializationCode().setDomain(EDomain.MATH);
		var.getInitializationCode().setCode("42.42");
		exercise.addVariable(var);
		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		String firstSubmissionVarName = submission.getVariableValues().keySet().stream().findFirst().get();

		// Variable should have value "42.42" after initializing
		assertVariableEquals(var, submission, OpenObject.of(OMOBJ.of(OMF.of(42.42))));

		// Variable name should be registered
		assertTrue(exerciseBusiness.variableNameAlreadyExistsForThisExercise(exercise, firstSubmissionVarName));
		assertFalse(exerciseBusiness.variableNameAlreadyExistsForThisExercise(exercise,
				exercise.getNextDefaultNameForVariables()));
	}

	/**
	 * Tests calculating variable updates while submitting a stage but after checking
	 */
	@Test
	void calculateVariableUpdateAfterCheck() throws Exception {
		String varX = PlaceholderPatternProducer.forExerciseVariable("varX");

		// Creating FillInStage by using the ExerciseBuilder
		exercise = new ExerciseBuilder((Exercise) exercise).withFillInStage() //
				.withTitle("Title") //
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("varX sollte 42 sein", varX + "==42", "42 ist richtig", 100, false) //
				.and() //
				.create();
		FillInStage stage = (FillInStage) exercise.getStartStage();

		// Add a variable to the exercise
		VariableDeclaration varDecl = new VariableDeclaration("varX");
		varDecl.getInitializationCode().setDomain(EDomain.MATH);
		varDecl.getInitializationCode().setCode("24");
		exercise.addVariable(varDecl);

		// Add a variable update to the stage
		VariableUpdate varUpd = new VariableUpdate(varDecl);
		varUpd.getUpdateCode().setDomain(EDomain.MATH);
		varUpd.getUpdateCode().setCode("42");
		stage.addVariableUpdateAfterCheck(varUpd);

		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Variable should have value "24" after initializing
		assertVariableEquals(varDecl, submission, OO_24);

		// Submit stage
		FillInSubmission fillInStagesubmission = (FillInSubmission) submission.getSubmissionLogAsSortedList().get(0)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("2");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);

		// Submission should have 0 Points because 'varX' changed the value to 42 only after checking
		assertEquals(0, submission.getResultPoints());

		// Variable should have value "42" after submitting
		assertVariableEquals(varDecl, submission, OO_42);

		fillInStagesubmission.clearResults();
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 100 Points because 'varX' changed the value to 42 already
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Tests calculating variable updates while submitting a stage but before checking
	 */
	@Test
	void calculateVariableUpdateBeforeCheck() throws Exception {
		String varX = PlaceholderPatternProducer.forExerciseVariable("varX");

		// Creating FillInStage by using the ExerciseBuilder
		exercise = new ExerciseBuilder((Exercise) exercise).withFillInStage() //
				.withTitle("Title") //
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("x sollte 42 sein", varX + "==42", "42 ist richtig", 100, false) //
				.and() //
				.create();
		FillInStage stage = (FillInStage) exercise.getStartStage();

		// Add a variable to the exercise
		VariableDeclaration varDecl = new VariableDeclaration("varX");
		varDecl.getInitializationCode().setDomain(EDomain.MATH);
		varDecl.getInitializationCode().setCode("24");
		exercise.addVariable(varDecl);

		// Add a variable update to the stage
		VariableUpdate varUpd = new VariableUpdate(varDecl);
		varUpd.getUpdateCode().setDomain(EDomain.MATH);
		varUpd.getUpdateCode().setCode("42");
		stage.addVariableUpdateBeforeCheck(varUpd);

		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Variable should have value "24" after initializing
		assertVariableEquals(varDecl, submission, OO_24);

		// Submit stage
		FillInSubmission fillInStagesubmission = (FillInSubmission) submission.getSubmissionLogAsSortedList().get(0)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("2");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);

		// Submission should have 100 Points because 'x' changed the value to 42 before checking
		assertEquals(100, submission.getResultPoints());

		// Variable should have value "42" after submitting
		assertVariableEquals(varDecl, submission, OO_42);
	}

	/**
	 * Tests calculating variable updates while submitting a stage on a normal exit
	 */
	@Test
	void calculateVariableUpdateOnNormalExit() throws Exception {
		String varX = PlaceholderPatternProducer.forExerciseVariable("varX");
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");

		// Creating FillInStage by using the ExerciseBuilder
		exercise = new ExerciseBuilder((Exercise) exercise).withFillInStage() //
				.withTitle("Title") //
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("x sollte 42 sein", varX + "==42", "42 ist richtig", 100, false) //
				.withWeight(1).and() //
				.create();

		FillInStage stage = (FillInStage) exercise.getStartStage();

		StageTransition transition = new StageTransition(new RepeatStage());
		transition.setStageExpression(new EvaluatorExpression("equals(" + input1 + ", 'repeat')"));
		stage.addStageTransition(transition);

		// Add a variable to the exercise
		VariableDeclaration varDecl = new VariableDeclaration("varX");
		varDecl.getInitializationCode().setDomain(EDomain.MATH);
		varDecl.getInitializationCode().setCode("24");
		exercise.addVariable(varDecl);

		// Add a variable update to the stage
		VariableUpdate varUpd = new VariableUpdate(varDecl);
		varUpd.getUpdateCode().setDomain(EDomain.MATH);
		varUpd.getUpdateCode().setCode("42");
		stage.addVariableUpdateOnNormalExit(varUpd);

		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Variable should have value "24" after initializing
		assertVariableEquals(varDecl, submission, OO_24);

		// Submit stage
		FillInSubmission fillInStagesubmission = (FillInSubmission) submission.getSubmissionLogAsSortedList().get(0)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("repeat");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);

		// Variable should have value "24" because the Stage repeats and the variable update doesn't trigger
		assertVariableEquals(varDecl, submission, OO_24);

		assertEquals(0, submission.getResultPoints());

		fillInStagesubmission = (FillInSubmission) filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("2");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 0 Points
		assertEquals(0, submission.getResultPoints());

		// Variable should have value "42" because the Stage doens't repeat, so it's a normal exit
		assertVariableEquals(varDecl, submission, OO_42);

		fillInStagesubmission.clearResults();
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 100 Points
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Tests calculating variable updates while submitting a stage on repeat
	 */
	@Test
	void calculateVariableUpdateOnRepeat() throws Exception {
		String varX = PlaceholderPatternProducer.forExerciseVariable("varX");
		String input1 = PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + "1");

		// Creating FillInStage by using the ExerciseBuilder
		exercise = new ExerciseBuilder((Exercise) exercise).withFillInStage() //
				.withTitle("Title") //
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("x sollte 42 sein", varX + "==42", "42 ist richtig", 100, false) //
				.and() //
				.create();
		FillInStage stage = (FillInStage) exercise.getStartStage();
		StageTransition transition = new StageTransition(new RepeatStage());
		transition.setStageExpression(new EvaluatorExpression("equals(" + input1 + ", 'repeat')"));
		stage.addStageTransition(transition);

		// Add a variable to the exercise
		VariableDeclaration varDecl = new VariableDeclaration("varX");
		varDecl.getInitializationCode().setDomain(EDomain.MATH);
		varDecl.getInitializationCode().setCode("24");
		exercise.addVariable(varDecl);

		// Add a variable update to the stage
		VariableUpdate varUpd = new VariableUpdate(varDecl);
		varUpd.getUpdateCode().setDomain(EDomain.MATH);
		varUpd.getUpdateCode().setCode("42");
		stage.addVariableUpdateOnRepeat(varUpd);

		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Variable should have value "24" after initializing
		assertVariableEquals(varDecl, submission, OO_24);

		// Submit stage
		FillInSubmission fillInStagesubmission = (FillInSubmission) submission.getSubmissionLogAsSortedList().get(0)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("2");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);

		// Variable should have value "24" because the Stage doesn't repeat
		assertVariableEquals(varDecl, submission, OO_24);
		// Submission should have 0 Points because 'x' doesn't changed
		assertEquals(0, submission.getResultPoints());

		fillInStagesubmission.clearResults();
		submission.setIsCompleted(false);
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("repeat");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 0 Points
		assertEquals(0, submission.getResultPoints());

		// Variable should have value "42" because the Stage does repeat
		assertVariableEquals(varDecl, submission, OO_42);

		fillInStagesubmission.clearResults();
		submission.setIsCompleted(false);
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 100 Points
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Tests calculating variable updates while submitting a stage on skip
	 */
	@Test
	void calculateVariableUpdateOnSkip() throws Exception {
		String varX = PlaceholderPatternProducer.forExerciseVariable("varX");

		// Creating FillInStage by using the ExerciseBuilder
		exercise = new ExerciseBuilder((Exercise) exercise).withFillInStage() //
				.withTitle("Title") //
				.allowSkip()//
				.withDescription() //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.and() //
				.withFeedbackRule("x sollte 42 sein", varX + "==42", "42 ist richtig", 100, false) //
				.and() //
				.create();
		FillInStage stage = (FillInStage) exercise.getStartStage();

		// Add a variable to the exercise
		VariableDeclaration varDecl = new VariableDeclaration("varX");
		varDecl.getInitializationCode().setDomain(EDomain.MATH);
		varDecl.getInitializationCode().setCode("24");
		exercise.addVariable(varDecl);

		// Add a variable update to the stage
		VariableUpdate varUpd = new VariableUpdate(varDecl);
		varUpd.getUpdateCode().setDomain(EDomain.MATH);
		varUpd.getUpdateCode().setCode("42");
		stage.addVariableUpdateOnSkip(varUpd);

		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Variable should have value "24" after initializing
		assertVariableEquals(varDecl, submission, OO_24);

		// Submit stage
		FillInSubmission fillInStagesubmission = (FillInSubmission) submission.getSubmissionLogAsSortedList().get(0)
				.getSubmission();
		((FillInSubmissionField) fillInStagesubmission.getSubmissionFields().iterator().next()).setUserInput("123");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);

		// Variable should have value "24" because the Stage wouldn't skipped
		assertVariableEquals(varDecl, submission, OO_24);

		// Submission should have 0 Points because 'x' doesn't changed
		assertEquals(0, submission.getResultPoints());

		fillInStagesubmission.clearResults();
		// perform Skip
		submission = exercisePlayerBusiness.performStageSkip(submission, stage, fillInStagesubmission);
		// Submission should have 0 Points
		assertEquals(0, submission.getResultPoints());

		// Variable should have value "42" because the Stage skipped
		assertVariableEquals(varDecl, submission, OO_42);

		fillInStagesubmission.clearResults();
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, fillInStagesubmission);
		// Submission should have 100 Points
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Tests repeating a stage
	 */
	@Test
	void repeatStage() {
		MCStage stage = prepareSimpleMCStage();
		stage.setDefaultTransition(new StageTransition(new RepeatStage()));
		exercise = exerciseBusiness.updateExercise(exercise);

		// Init submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Submit current submission
		MCSubmission mcStagesubmission = (MCSubmission) filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER)
				.get(0).getSubmission();
		mcStagesubmission.setTickedPattern("1");
		// Save stage id to check if current stage and next stage are the same
		long stageId = mcStagesubmission.getStageId();
		submission = exercisePlayerBusiness.performStageSubmit(submission, exercise.getStartStage(), mcStagesubmission);

		// The submission should not be finished and the current stage should be repeated
		mcStagesubmission = (MCSubmission) filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1)
				.getSubmission();
		assertFalse(submission.isCompleted());
		assertEquals(stageId, mcStagesubmission.getStageId());
	}

	/**
	 * Tests erasing a submission after
	 */
	@Test
	void eraseSubmission() throws Exception {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		// Make a correct submission
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();

		// Correct submission should be completed with 100 points
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, stageSubmission.getPoints());
		assertTrue(submission.isCompleted());

		// Erase submission
		submission = exercisePlayerBusiness.eraseSubmission(submission, stage, stageSubmission);

		// Submission must be still correct, but submission must not be completed any more
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		assertEquals(0, submission.getResultPoints());
		assertEquals(100, stageSubmission.getPoints());
		assertFalse(submission.isCompleted());
	}

	/**
	 * Tests finding a skip transition
	 */
	@Test
	void findSkipTransition() throws IllegalAccessException {
		// Add a stage to the exercise that repeats on default. Also add a skip transition to the Stage that moves on to
		// the next Stage
		MCStage stage = new MCStage();
		stage.setExternalName("stage1");
		stage.addAnswerOption("Option");
		stage.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);
		stage.setAllowSkip(true);

		MCStage stage2 = new MCStage();
		stage2.setExternalName("stage2");

		// If the Stage is skipped the Exercise should go to the next stage
		StageTransition skipTransition = new StageTransition(stage2);
		skipTransition.setConditionExpression(new EvaluatorExpression("42==42"));
		stage.addSkipTransition(skipTransition);

		// If the Stage isn't skipped the Stage should repeat
		stage.setDefaultTransition(new StageTransition(new RepeatStage()));

		exercise.addStage(stage);
		exercise.addStage(stage2);
		exercise.setStartStage(stage);
		exercise = exerciseBusiness.updateExercise(exercise);

		// Create a submission and skip first stage -> the stage should not be repeated but move on to Stage2
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);

		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();
		submission = exercisePlayerBusiness.performStageSkip(submission, stage, stagesubmission);
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check if the stage really moves on
		assertFalse(submission.isCompleted());
		assertEquals(stage2.getId(), stagesubmission.getStageId());
	}

	/**
	 * Tests finding a specific transition
	 */
	@Test
	void findSpecificTransition() {

		// Add stages
		MCStage startStage = new MCStage();
		startStage.setOrderIndex(0);
		startStage.addAnswerOption("Option");
		startStage.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);
		exercise.addStage(startStage);
		exercise.setStartStage(startStage);

		MCStage targetStage = new MCStage();
		targetStage.setOrderIndex(1);
		targetStage.addAnswerOption("Option");
		targetStage.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);
		exercise.addStage(targetStage);

		MCStage notTargetStage = new MCStage();
		notTargetStage.setOrderIndex(2);
		notTargetStage.addAnswerOption("Option");
		notTargetStage.getAnswerOptions().get(0).setRule(EMCRuleType.CORRECT);
		exercise.addStage(notTargetStage);

		// Add transitions
		StageTransition transition = new StageTransition(targetStage);
		transition.setConditionExpression(new EvaluatorExpression("42==42"));
		transition.setStageExpression(new EvaluatorExpression("true()"));
		startStage.addStageTransition(transition);

		transition = new StageTransition(notTargetStage);
		transition.setConditionExpression(new EvaluatorExpression("42==43"));
		transition.setStageExpression(new EvaluatorExpression("true()"));
		startStage.addStageTransition(transition);

		// Save stages and ids to find it later
		exercise = exerciseBusiness.updateExercise(exercise);
		List<Stage> exerciseStages = exercise.getStagesAsList();
		startStage = (MCStage) exerciseStages.get(0);
		long targetStageId = exerciseStages.get(1).getId();
		long notTargetStageId = exerciseStages.get(2).getId();

		// Perform stage submission
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		StageSubmission stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(0)
				.getSubmission();

		((MCSubmission) stagesubmission).setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, startStage, stagesubmission);
		stagesubmission = filterSubmissionlog(submission, ESubmissionLogEntryType.ENTER).get(1).getSubmission();

		// Check that the next stage is the right stage
		assertNotEquals(notTargetStageId, stagesubmission.getStageId());
		assertEquals(targetStageId, stagesubmission.getStageId());
	}

	/**
	 * Tests if deleting a submission is possible
	 */
	@Test
	void deleteSubmission() {
		MCStage stage = prepareSimpleMCStage();
		exercise = exerciseBusiness.updateExercise(exercise);

		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		MCSubmission stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);

		submissionService.deleteSubmissionAndDependentEntities(submission);

		assertTrue(exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise).isEmpty());
	}

	private MCStage prepareSimpleMCStage() {
		// Adds a simple MC stage to the already existing test exercise
		exercise = new ExerciseBuilder((Exercise) exercise).withMCStage().withAnswerOption("Option", true).and()
				.create();
		return (MCStage) exercise.getStartStage();
	}

	//Helper to avoid scope-error
	private void setContentFolder(ContentFolder folder, ContentFolder folderToSet) {
		folderToSet = folder;
	}

}
