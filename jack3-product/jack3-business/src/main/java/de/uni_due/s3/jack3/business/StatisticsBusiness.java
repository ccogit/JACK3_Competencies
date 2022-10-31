package de.uni_due.s3.jack3.business;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.services.CommentService;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.services.UserService;

/**
 * Contains methods that collect various statistic data about exercises, courses and course offers. Unless otherwise
 * specified, all queries refering to submissions / course records ignore testing submissions / course records.
 */
@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class StatisticsBusiness extends AbstractBusiness {

	@Inject
	private EnrollmentService enrollmentService;

	@Inject
	private CourseRecordService courseRecordService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private CommentService commentService;

	@Inject
	private UserService userService;

	/**
	 * Counts all participants who have ever interacted with the course offer.
	 */
	public long countParticipants(final CourseOffer courseOffer) {
		// We can simply count all enrollments because enrollments are unique for course offers and users.
		return enrollmentService.countEnrollments(courseOffer);
	}

	/**
	 * Counts all course records for a course offer.
	 */
	public long countCourseRecords(final CourseOffer courseOffer) {
		return courseRecordService.countCourseRecordsForCourseOffer(courseOffer);
	}

	/**
	 * Returns the average score of all course records for a course offer.
	 */
	public double getAverageScore(final CourseOffer courseOffer) {
		return courseRecordService.getAverageScoreForCourseOffer(courseOffer);
	}


	/**
	 * Returns the highest score of all course records for a course offer.
	 */
	public int getHighestScore(final CourseOffer courseOffer) {
		return courseRecordService.getHighestScoreForCourseOffer(courseOffer);
	}

	/**
	 * Returns the lowest score of all course records for a course offer.
	 */
	public int getLowestScore(final CourseOffer courseOffer) {
		return courseRecordService.getLowestScoreForCourseOffer(courseOffer);
	}

	/**
	 * Counts all exercise submissions for a course offer.
	 */
	public long countSubmissions(final CourseOffer courseOffer) {
		return submissionService.countSubmissionsForCourseOffer(courseOffer);
	}

	/**
	 * Counts all unread comments for course offers.
	 */
	public long countUnreadComments(final CourseOffer courseOffer) {
		return commentService.countUnreadComments(courseOffer);
	}

	/**
	 * Counts all participants who have ever interacted with the course including participations in frozen courses.
	 */
	public long countParticipants(final Course course) {
		return userService.countAllParticipantsForCourse(course);
	}

	/**
	 * Counts all course records for a course including course records for frozen courses.
	 */
	public long countCourseRecords(final Course course) {
		return courseRecordService.countCourseRecordsForCourse(course);
	}

	/**
	 * Counts all <strong>test</strong> course records for a course including course records for frozen courses.
	 */
	public long countTestCourseRecords(final Course course) {
		return courseRecordService.countTestCourseRecordsForCourse(course);
	}

	/**
	 * Returns the average score of all course records for a course offer including course records for frozen courses.
	 */
	public double getAverageScore(final Course course) {
		return courseRecordService.getAverageScoreForCourse(course);
	}

	/**
	 * Returns the highest score of all course records for a course offer including course records for frozen courses.
	 */
	public int getHighestScore(final Course course) {
		return courseRecordService.getHighestScoreForCourse(course);
	}

	/**
	 * Returns the lowest score of all course records for a course offer including course records for frozen courses.
	 */
	public int getLowestScore(final Course course) {
		return courseRecordService.getLowestScoreForCourse(course);
	}

	/**
	 * Counts all exercise submissions for a course offer including submissions for frozen courses.
	 */
	public long countSubmissions(final Course course) {
		return submissionService.countSubmissionsForCourse(course);
	}

	/**
	 * Counts all unread comments for course offers including unread comments in course records for frozen courses.
	 */
	public long countUnreadComments(final Course course) {
		return commentService.countNontestingUnreadComments(course);
	}

	/**
	 * Counts all exercise submissions including submissions for frozen exercises.
	 */
	public long countSubmissions(final Exercise exercise) {
		return submissionService.countSubmissionsForExercise(exercise);
	}

	/**
	 * Counts all exercise submissions including submissions for frozen exercises and testing submissions.
	 */
	public long countAllSubmissions(final Exercise exercise) {
		return submissionService.countAllSubmissionsForExercise(exercise);
	}

	/**
	 * Counts all unread comments for exercises including unread comments for frozen exercises.
	 */
	public long countUnreadComments(final Exercise exercise) {
		return commentService.countNontestingUnreadComments(exercise);
	}

}
