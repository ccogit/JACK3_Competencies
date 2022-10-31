package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsExercise
@NeedsCourse
class SubmissionServiceTest extends AbstractContentTest {

	@Inject
	private SubmissionService submissionService;

	/**
	 * Create 3 users with 1 submission per user
	 */
	private Submission[] createTestSubmissions(boolean createCourseRecord) throws Exception {
		return createTestSubmissions(createCourseRecord, 0);
	}

	/**
	 * Create 3 users with 1 submission per user, the submissions' starttime ("creationTimestamp") is setted to an
	 * intervall
	 * of the specified time (in seconds)
	 */
	private Submission[] createTestSubmissions(boolean createCourseRecord, int intervall) throws Exception {

		// Array for users
		User[] users = new User[] {
				TestDataFactory.getUser("Student1", false, false),
				TestDataFactory.getUser("Student2", false, false),
				TestDataFactory.getUser("Student3", false, false),
		};

		Submission submissions[] = new Submission[users.length];

		if (createCourseRecord) {

			// Persist users, create courseRecords and submissions
			for (int i = 0; i < users.length; i++) {
				userService.persistUser(users[i]);

				CourseRecord record = new CourseRecord(users[i], course);
				baseService.persist(record);

				submissions[i] = new Submission(users[i], exercise, record, false);

				if (intervall != 0) {
					Field creationTimestampField = Submission.class.getDeclaredField("creationTimestamp");
					creationTimestampField.setAccessible(true);
					creationTimestampField.set(submissions[i],
							LocalDateTime.now().minusSeconds((users.length - i) * intervall));
				}

				submissionService.persistSubmission(submissions[i]);
			}
		} else {
			// Persist users, create courseRecords and submissions
			for (int i = 0; i < users.length; i++) {
				userService.persistUser(users[i]);

				submissions[i] = new Submission(users[i], exercise);

				if (intervall != 0) {
					Field creationTimestampField = Submission.class.getDeclaredField("creationTimestamp");
					creationTimestampField.setAccessible(true);
					creationTimestampField.set(submissions[i],
							LocalDateTime.now().minusSeconds((users.length - i) * intervall));
				}

				submissionService.persistSubmission(submissions[i]);
			}
		}
		return submissions;
	}

	/**
	 * Tests empty database
	 */
	@Test
	void getEmptySubmissionList() {

		// no submission should be found
		assertTrue(submissionService.getAllSubmissionsForExerciseAndFrozenVersions(exercise).isEmpty());
		assertEquals(0, submissionService.countAllSubmissionsForExercise((Exercise) exercise));
	}

	/**
	 * Count all submission for exercise
	 */
	@Test
	void countAllSubmissionsForExercise() throws Exception {

		// there should be 3 submissions for Exercise
		createTestSubmissions(false);
		assertEquals(3, submissionService.countAllSubmissionsForExercise((Exercise) exercise));
	}

	/**
	 * Get all submissions for exercise ordered by time
	 */
	@Test
	void getAllSubmissionsOrderedByTime() throws Exception {

		Submission[] submissions = createTestSubmissions(false, 100);

		// 3 submissions should be found, ordered by time
		List<Submission> getSubmissionsFromDB = submissionService
				.getAllSubmissionsForExerciseAndFrozenVersions(exercise);
		assertEquals(3, getSubmissionsFromDB.size());
		assertEquals(submissions[2], getSubmissionsFromDB.get(0));
		assertEquals(submissions[1], getSubmissionsFromDB.get(1));
		assertEquals(submissions[0], getSubmissionsFromDB.get(2));
	}

	/**
	 * Load submission with lazy data
	 */
	@Test
	void getSubmissionWithLazyData() {

		Submission submission = new Submission(user, exercise);
		submissionService.persistSubmission(submission);
		long submissionID = submission.getId();

		Submission submissionFromDB = submissionService.getSubmissionnWithLazyDataBySubmissionId(submissionID)
													.orElseThrow(AssertionError::new);
		assertEquals(submission, submissionFromDB);
		assertTrue(submissionFromDB.getAttributes().isEmpty());
		assertTrue(submissionFromDB.getSubmissionResources().isEmpty());
		assertTrue(submissionFromDB.getComments().isEmpty());
		assertTrue(submissionFromDB.getSubmissionLog().isEmpty());
	}

	/**
	 * get latest submission for course record and exercise
	 */
	@Test
	void getLatestSubmissionForCourseRecordAndExercise() {

		CourseRecord record = new CourseRecord(user, course);
		baseService.persist(record);

		// submission with courseRecord should be found
		Submission submission = new Submission(user, exercise, record, false);
		submissionService.persistSubmission(submission);
		assertEquals(submission,
				submissionService.getLatestSubmissionForCourseRecordAndExercise(record, exercise).get());

		baseService.deleteEntity(submission);
		// submission with courseRecord should not be found
		assertFalse(submissionService.getLatestSubmissionForCourseRecordAndExercise(record, exercise).isPresent());
	}

	/**
	 * Get all submissions for course record
	 */
	@Test
	void getAllSubmissionsForCourseRecord() throws Exception {

		Submission[] submissions = createTestSubmissions(true);

		// add all submissions to the same courseRecord
		CourseRecord record = baseService.findById(CourseRecord.class, submissions[0].getCourseRecord().getId(), false)
				.orElseThrow(AssertionError::new);
		for (Submission submission : submissions) {
			submission.setCourseRecord(record);
			submissionService.mergeSubmission(submission);
		}

		// 3 submissions should be found
		Collection<Submission> getAllSubmissionsFromDB = submissionService
				.getAllSubmissionsForCourseRecord(submissions[0].getCourseRecord());
		assertEquals(3, getAllSubmissionsFromDB.size());
		assertTrue(getAllSubmissionsFromDB.containsAll(Arrays.asList(submissions)));
	}

	/**
	 * Tests if 1 user can submit 2 submissions
	 */
	@Test
	void createManySubmissionsForTheSameUser() {

		Submission submission = new Submission(user, exercise);
		submissionService.persistSubmission(submission);

		submission = new Submission(user, exercise);
		submissionService.persistSubmission(submission);

		// 2 submissions should be found
		assertEquals(2, submissionService.countAllSubmissionsForExercise((Exercise) exercise));
	}

	/**
	 * Get not available submission by incorrect id
	 */
	@Test
	void getNotAvailableSubmission() {
		assertFalse(submissionService.getSubmissionnWithLazyDataBySubmissionId(-1).isPresent());
	}

	@Test
	void countAllSubmissionsForRecordAndExercise() throws Exception {
		Submission[] submissions = createTestSubmissions(true);

		// add all submissions to the same courseRecord and exercises
		CourseRecord record = baseService.findById(CourseRecord.class, submissions[0].getCourseRecord().getId(), false)
				.orElseThrow(AssertionError::new);
		for (Submission submission : submissions) {
			submission.setCourseRecord(record);
			submission.setExercise(exercise);
			submissionService.mergeSubmission(submission);
		}

		// submissions should be found by course record and exercise
		long countSubmissions = submissionService.countAllSubmissionsForCourseRecordAndExercise(record, exercise);
		assertEquals(submissions.length, countSubmissions);
	}
}
