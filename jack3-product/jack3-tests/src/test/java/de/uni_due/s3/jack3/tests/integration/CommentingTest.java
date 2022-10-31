package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
@NeedsCourse
class CommentingTest extends AbstractContentTest {

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	private long countCommentsForExercise(AbstractExercise ae) {
		final String query = String.format(
				"SELECT COUNT(c) FROM Submission s LEFT JOIN s.comments as c WHERE s.exercise.id = %s", ae.getId());
		return querySingleResult(query, Long.class).orElseThrow(IllegalStateException::new);
	}

	@Test
	void addCommentToExercise() {
		Submission submission = exerciseBusiness.createSubmission(exercise, user, true);

		assertFalse(submission.hasComments());

		submission = exerciseBusiness.addCommentToSubmission(submission, user, "Comment", false);
		assertTrue(submission.hasComments());
		assertTrue(submission.hasUnreadComments());
		assertEquals(1, submission.getComments().size());
		assertEquals(1, countCommentsForExercise(exercise));

		Comment comment = submission.getComments().stream().findAny().get(); // NOSONAR
		assertEquals("Comment", comment.getText());
		assertFalse(comment.isRead());

		// Mark the comment as read
		comment.setRead(true);
		comment = exerciseBusiness.updateComment(comment);

		assertEquals("Comment", comment.getText());
		assertTrue(comment.isRead());

		assertTrue(submission.hasComments());
		assertFalse(submission.hasUnreadComments());
		assertEquals(1, submission.getComments().size());
		assertEquals(1, countCommentsForExercise(exercise));
	}

	@Test
	void addCommentToCourseRecord() throws Exception {
		CourseRecord record = courseBusiness.createTestCourseRecord(user, course);
		Submission submission = exerciseBusiness.createSubmissionForCourseRecord(exercise, user, record, true, false);
		submission = exerciseBusiness.addCommentToSubmission(submission, user, "Comment", false);
		Comment comment = submission.getComments().stream().findAny().get(); // NOSONAR

		assertEquals(1, courseBusiness.countCommentsForCourseRecord(record));
		assertEquals(1, courseBusiness.countUnreadCommentsForCourseRecord(record));

		// Mark the comment as read
		comment.setRead(true);
		comment = exerciseBusiness.updateComment(comment);

		assertEquals(1, courseBusiness.countCommentsForCourseRecord(record));
		assertEquals(0, courseBusiness.countUnreadCommentsForCourseRecord(record));
	}

}
