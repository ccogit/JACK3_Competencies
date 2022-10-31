package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionAttribute;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsExercise
@NeedsCourse
class SubmissionTest extends AbstractContentTest {

	@Inject
	private RevisionService revisionService;

	private CourseRecord courseRecord;
	private Submission submission;

	/**
	 * Persist user, folder, exercise, course, course record, submission
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		courseRecord = new CourseRecord(user, course);
		baseService.persist(courseRecord);

		folder = folderService.getContentFolderWithLazyData(folder);

		submission = new Submission(user, exercise, courseRecord, false);
		baseService.persist(submission);

	}

	/**
	 * Tests if creating new submissions with illegal arguments is not possible
	 */
	@Test
	void createSubmissionWithIllegalArguments() {
		final Exercise exercise = new Exercise();
		assertThrows(NullPointerException.class, () -> {
			new Submission(null, exercise);
		});
	}

	/**
	 * Get variable value, for further tests see VariableValue(Service)Test
	 */
	@Test
	void testVariableValues() {
		assertTrue(submission.getVariableValues().isEmpty());
	}

	/**
	 * Get attributes
	 */
	@Test
	void testAttributes() {
		assertTrue(submission.getAttributes().isEmpty());

		submission.addAttribute(new SubmissionAttribute());
		submission = baseService.merge(submission);

		assertEquals(1, submission.getAttributes().size());
	}

	/**
	 * Change author
	 */
	@Test
	void changeAuthor() {
		assertEquals(user, submission.getAuthor());

		// add a new author and change course author
		User newAuthor = TestDataFactory.getUser("NewAuthor");
		userService.persistUser(newAuthor);

		submission.setAuthor(newAuthor);
		submission = baseService.merge(submission);

		assertNotEquals(user, submission.getAuthor());
		assertEquals(newAuthor, submission.getAuthor());
	}

	/**
	 * Change exercise revision ids
	 */
	@Test
	void changeExerciseRevisionId() {
		List<Integer> revisions = revisionService.getRevisionNumbersFor(exercise);
		assertEquals(revisions.get(FIRST_REVISION).intValue(), submission.getShownExerciseRevisionId());
		assertEquals(0, submission.getCheckedExerciseRevisionId());

		// change exercise revision IDs
		submission.setShownExerciseRevisionId(1);
		submission.setCheckedExerciseRevision(2);
		submission = baseService.merge(submission);

		assertEquals(1, submission.getShownExerciseRevisionId());
		assertEquals(2, submission.getCheckedExerciseRevisionId());
	}

	/**
	 * Get and add comments
	 */
	@Test
	void TestComments() {
		assertTrue(submission.getComments().isEmpty());

		submission.addComment(new Comment(user, "Test", false));
		submission = baseService.merge(submission);

		assertEquals(1, submission.getComments().size());
	}

	/**
	 * Get and add comments with email address of the user
	 */
	@Test
	void TestCommentsWithEmail() {
		assertTrue(submission.getComments().isEmpty());

		submission.addComment(new Comment(user, "Test", true));
		submission = baseService.merge(submission);

		assertEquals(1, submission.getComments().size());
		Comment c = submission.getComments().iterator().next();

		assertTrue(c.isShowEmail());
		assertEquals(user.getEmail(), c.getCommentAuthor().getEmail());
	}

	/**
	 * Get timestamp
	 */
	@Test
	void getCreationTimestamp() {
		assertTrue(submission.getCreationTimestamp().isBefore(LocalDateTime.now()));
	}

	/**
	 * Change exercise
	 */
	@Test
	void changeExercise() {
		assertEquals(exercise, submission.getExercise());

		// add new exercise and change exercise
		Exercise newExercise = new Exercise("New Exercise", TestDataFactory.getDefaultLanguage());
		folder.addChildExercise(newExercise);
		baseService.persist(newExercise);
		folder = folderService.mergeContentFolder(folder);

		submission.setExercise(newExercise);
		submission = baseService.merge(submission);

		assertNotEquals(exercise, submission.getExercise());
		assertEquals(newExercise, submission.getExercise());
	}

	/**
	 * Change result points
	 */
	@Test
	void changeResultPoints() {
		assertEquals(0, submission.getResultPoints());

		submission.setResultPoints(100);
		submission = baseService.merge(submission);

		assertEquals(100, submission.getResultPoints());
	}

	/**
	 * Add SubmissionResources
	 */
	@Disabled("Not implemented yet")
	@Test
	void addSubmissionResources() {

		assertTrue(submission.getSubmissionResources().isEmpty());

		SubmissionResource submissionResource = new SubmissionResource();
		submissionResource.setFilename("fileName");
		submission.addSubmissionResource(submissionResource);
		submission = baseService.merge(submission);

		assertEquals(1, submission.getSubmissionResources().size());
	}

	/**
	 * Get submission resources
	 */
	@Test
	void getSubmissionResources() {
		assertTrue(submission.getSubmissionResources().isEmpty());
	}

	/**
	 * Change has internal errors
	 */
	@Test
	void changeHasInternalErrors() {
		assertFalse(submission.hasInternalErrors());

		submission.setHasInternalErrors(true);
		submission = baseService.merge(submission);

		assertTrue(submission.hasInternalErrors());
	}

	/**
	 * Change has pending stage checks
	 */
	@Test
	void changeHasPendingStageChecks() {
		assertFalse(submission.hasPendingStageChecks());

		submission.setHasPendingStageChecks(true);
		submission = baseService.merge(submission);

		assertTrue(submission.hasPendingStageChecks());
	}

	/**
	 * Change is completed
	 */
	@Test
	void changeIsCompleted() {
		assertFalse(submission.isCompleted());

		submission.setIsCompleted(true);
		submission = baseService.merge(submission);

		assertTrue(submission.isCompleted());
	}

	/**
	 * Change is reviewed manually
	 */
	@Test
	void changeIsReviewedManually() {
		assertFalse(submission.isReviewed());

		submission.setReviewed(true);
		submission = baseService.merge(submission);

		assertTrue(submission.isReviewed());
	}

	/**
	 * Change is test submission
	 */
	@Test
	void changeIsTestSubmission() {
		assertFalse(submission.isTestSubmission());

		submission.setIsTestSubmission(true);
		submission = baseService.merge(submission);

		assertTrue(submission.isTestSubmission());
	}

}
