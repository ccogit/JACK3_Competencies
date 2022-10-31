package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.CourseBuilder;
import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ManualResult;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * This test class contains test cases for the manual feedback feature.
 */
@NeedsEureka
class ManualFeedbackTest extends AbstractBusinessTest {

	private User student;
	private User lecturer;
	private User secondLecturer;

	private ContentFolder folder;
	private Exercise exercise;
	private MCStage stage;

	private Course course;
	private CourseOffer offer;

	private MCSubmission stageSubmission;
	private Submission submission;
	private CourseRecord record;

	@Inject
	private BaseService baseService;

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();

		student = getStudent("student");
		lecturer = getLecturer("lecturer");
		secondLecturer = getLecturer("secondLecturer");
	}

	private void createTestSubmission() throws ActionNotAllowedException {
		// Create exercise
		exercise = exerciseBusiness.createExercise("Exercise", lecturer, lecturer.getPersonalFolder(), "de");
		exercise = new ExerciseBuilder(exercise).withMCStage().withAnswerOption("Correct", true).and().create();
		exercise = (Exercise) exerciseBusiness.updateExercise(exercise);
		folder = exercise.getFolder();
		stage = (MCStage) exercise.getStagesAsList().get(0);

		// Simulate a submission
		submission = exerciseBusiness.createSubmission(exercise, student, true);
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
	}

	private void createRealSubmission() throws ActionNotAllowedException {
		// Create exercise
		exercise = exerciseBusiness.createExercise("Exercise", lecturer, lecturer.getPersonalFolder(), "de");
		exercise = new ExerciseBuilder(exercise).withMCStage().withAnswerOption("Correct", true).and().create();
		exercise = (Exercise) exerciseBusiness.updateExercise(exercise);
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId()).orElseThrow();
		folder = exercise.getFolder();
		stage = (MCStage) exercise.getStagesAsList().get(0);


		// Create course
		course = courseBusiness.createCourse("Course", lecturer, lecturer.getPersonalFolder());
		course = new CourseBuilder(course)
				.withFolderExerciseProvider(Arrays.asList(folder))
				.withResultFeedbackMappingExactPoints(100, "Perfect", "Perfect text")
				.withResultFeedbackMappingExactPoints(0, "Bad", "Bad text")
				.withResultFeedbackMappingExactPoints(42, "Hmm", "Hmm text")
				.withResultFeedbackMappingExactPoints(85, "Well done", "Well done text")
				.withScoringMode(ECourseScoring.LAST)
				.build();
		course = (Course) courseBusiness.updateCourse(course);

		// Create course offer
		folderBusiness.updateFolderRightsForUser(folderBusiness.getPresentationRoot(), lecturer, AccessRight.getFull());
		offer = courseBusiness.createCourseOffer("Offer", course, folderBusiness.getPresentationRoot(), lecturer);

		// Simulate a real submission by a student
		record = courseBusiness.createCourseRecord(student, offer);
		try {
			submission = exerciseBusiness.createSubmissionForCourseRecord(exercise, student, record, false, false);
		} catch (SubmissionException e) {
			throw new Error("Could not initialize submission for course record.");
		}
		submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		stageSubmission.setTickedPattern("1");
		submission = exercisePlayerBusiness.performStageSubmit(submission, stage, stageSubmission);
		stageSubmission = (MCSubmission) submission.getSubmissionLogAsSortedList().get(0).getSubmission();
		record = baseService.findById(CourseRecord.class, record.getId(), false).orElseThrow(AssertionError::new);
	}

	private void updateCache() {
		stageSubmission = baseService.findById(MCSubmission.class, stageSubmission.getId(), false)
				.orElseThrow(AssertionError::new);
		submission = baseService.findById(Submission.class, submission.getId(), false).orElseThrow(AssertionError::new);

		if (course != null) {
			// real mode
			record = baseService.findById(CourseRecord.class, record.getId(), false).orElseThrow(AssertionError::new);
		}
	}

	/**
	 * Test case: A lecturer gives a manual feedback to a submission that overrides the automatic score (100) to an
	 * other value.
	 */
	@Test
	void giveManualFeedback() throws Exception {
		createTestSubmission();
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(100, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, submission.getResultPoints());

		// Give a manual result
		ManualResult manualResult = new ManualResult(lecturer);
		manualResult.setInternalComment("Internal comment ...");
		manualResult.setPublicComment("Feedback ...");
		manualResult.setPoints(85);
		manualResult.setShowAutomaticResult(true);

		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, manualResult, offer);
		updateCache();

		// Assertions
		assertTrue(stageSubmission.isHasManualResult());
		assertTrue(stageSubmission.getManualResult().isPresent());
		assertEquals("Internal comment ...", stageSubmission.getManualResult().get().getInternalComment());
		assertEquals("Feedback ...", stageSubmission.getManualResult().get().getPublicComment());
		assertTrue(stageSubmission.getManualResult().get().isShowAutomaticResult());

		assertEquals(85, stageSubmission.getManualResult().get().getPoints());
		assertEquals(85, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(85, submission.getResultPoints());
	}

	/**
	 * Test case: A lecturer deletes a manual feedback, the submission score should be reset to the result of the
	 * automatic checks.
	 */
	@Test
	void deleteManualFeedback() throws Exception {
		createTestSubmission();
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(100, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, submission.getResultPoints());

		// Give a manual result (similar to previous test case)
		ManualResult manualResult = new ManualResult(lecturer);
		manualResult.setInternalComment("Internal comment ...");
		manualResult.setPublicComment("Feedback ...");
		manualResult.setPoints(85);
		manualResult.setShowAutomaticResult(true);

		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, manualResult, offer);
		updateCache();

		// Delete manual feedback
		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, null, offer);
		updateCache();

		// Assertions
		assertFalse(stageSubmission.isHasManualResult());
		assertFalse(stageSubmission.getManualResult().isPresent());

		assertEquals(100, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Test case: A lecturer gives a manual feedback to a submission that overrides the automatic score (100) to an
	 * other value. The submission was during a real course participation.
	 */
	@Test
	void giveManualFeedbackInCourse() throws Exception {
		createRealSubmission();
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(100, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, record.getResultPoints());

		// Give a manual result
		ManualResult manualResult = new ManualResult(lecturer);
		manualResult.setInternalComment("Internal comment ...");
		manualResult.setPublicComment("Feedback ...");
		manualResult.setPoints(42);
		manualResult.setShowAutomaticResult(true);

		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, manualResult, offer);
		updateCache();

		// Assertions for course record
		assertEquals(42, submission.getResultPoints());
		assertEquals(42, record.getResultPoints());
		assertEquals("<p><b>Hmm</b></p><p>Hmm text</p>", record.getCourseFeedback());
	}

	/**
	 * Test case: A lecturer deletes a manual feedback, the submission score should be reset to the result of the
	 * automatic checks. The submission was during a real course participation.
	 */
	@Test
	void deleteManualFeedbackInCourse() throws Exception {
		createRealSubmission();
		assertEquals(100, stageSubmission.getPoints());
		assertEquals(100, exercisePlayerBusiness.getManualOrAutomaticPoints(stageSubmission, exercise));
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, record.getResultPoints());

		// Give a manual result
		ManualResult manualResult = new ManualResult(lecturer);
		manualResult.setInternalComment("Internal comment ...");
		manualResult.setPublicComment("Feedback ...");
		manualResult.setPoints(42);
		manualResult.setShowAutomaticResult(true);

		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, manualResult, offer);
		updateCache();

		// Delete manual feedback
		exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, null, offer);
		updateCache();

		// Assertions for course record
		assertEquals(100, submission.getResultPoints());
		assertEquals(100, record.getResultPoints());
		assertEquals("<p><b>Perfect</b></p><p>Perfect text</p>", record.getCourseFeedback());
	}

	/**
	 * Tests giving a manual feedback where the user was not allowed to.
	 * @throws ActionNotAllowedException
	 */
	@Test
	@Disabled("The check if a user is allowed to call this method moved to the view, due to AuthorizationBusiness"
			+ " being @RequestScoped and exercisePlayerBusiness being @ApplicationScoped. Since we are currently"
			+ " not doing integration testing on view methods this can't be moved (yet?). Maybe it is useful"
			+ " to also make the AuthorizationBusiness @ApplicationScoped, but that requires all other referenced"
			+ " business to be @Applicationscoped as well.")
	void failManualFeedbackOnMissingRights() throws ActionNotAllowedException {
		createRealSubmission();
		folderBusiness.updateFolderRightsForUser(folderBusiness.getPresentationRoot(), secondLecturer,
				AccessRight.getFromFlags(AccessRight.READ, AccessRight.EXTENDED_READ, AccessRight.WRITE));

		ManualResult manualResult = new ManualResult(lecturer);
		manualResult.setInternalComment("Internal comment ...");
		manualResult.setPublicComment("Feedback ...");
		manualResult.setPoints(42);
		manualResult.setShowAutomaticResult(true);

		// The second lecturer must NOT give manual feedback ...
		assertThrows(ActionNotAllowedException.class, () -> {
			exercisePlayerBusiness.updateManualResult(secondLecturer, stageSubmission, submission, manualResult, offer);
		}, "It should not be allowed for the second lecturer to give manual feedback as he has no GRADE rights.");
		// ... but the first lecturer is allowed to.
		assertDoesNotThrow(() -> {
			exercisePlayerBusiness.updateManualResult(lecturer, stageSubmission, submission, manualResult, offer);
		}, "It should be allowed for the first lecturer to give manual feedback.");
	}

}
