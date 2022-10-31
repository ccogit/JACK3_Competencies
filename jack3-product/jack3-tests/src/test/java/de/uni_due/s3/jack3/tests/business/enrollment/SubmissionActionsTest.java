package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Tests regarding (re)starting / finishing a {@link CourseRecord} submission for a course(offer) and a user.
 * 
 * @author lukas.glaser
 */
class SubmissionActionsTest extends AbstractEnrollmentBusinessTest {

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();

		final Course course = getCourse("Course");
		offer.setCourse(course);
		offer.setExplicitSubmission(true);
		offer = baseService.merge(offer);
	}

	// #########################################################################
	// Start
	// #########################################################################

	@Test
	void start() throws Exception {
		final User student = getStudent("stud");
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

		enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	@Test
	void startWithPassword() throws Exception {
		final User student = getStudent("stud");

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "sudo rm -rf /");
		offer = baseService.merge(offer);

		enrollmentBusiness.startSubmission(student, offer, "sudo rm -rf /");
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	@Test
	void startWithinSubmissionPeriod() throws Exception {
		final User student = getStudent("stud");

		offer.setSubmissionStart(LocalDateTime.now().minusDays(1));
		offer.setSubmissionDeadline(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	@Test
	void startAlreadyParticipated() throws Exception {
		final User student = getStudent("stud");

		CourseRecord oldParticipation = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(oldParticipation);

		enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because user already has an open course record
	@Test
	void startFailedAlreadyParticipating() throws Exception {
		final User student = getStudent("stud");

		CourseRecord oldParticipation = enrollmentBusiness.startSubmission(student, offer);
		baseService.merge(oldParticipation);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
	}

	// Submission start failed because course offer is not visible
	@Test
	void startFailedNotVisible() throws Exception {
		final User student = getStudent("stud");

		// not started yet

		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

		// Already finished

		offer.setVisibilityStartTime(null);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because user is not enrolled
	@Test
	void startFailedNotEnrolled() throws Exception {
		final User student = getStudent("stud");

		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

	}

	// Submission start failed because the course offer is not linked to a course
	@Test
	void startFailedNoCourse() throws Exception {
		final User student = getStudent("stud");

		offer.setCourse(null);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

	}

	// Submission start failed because the current timestamp is not within the submission period
	@Test
	void startFailedOutsideSubmissionPeriod() throws Exception {
		final User student = getStudent("stud");

		// Submission not started yet

		offer.setSubmissionStart(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

		// Deadline over

		offer.setSubmissionStart(null);
		offer.setSubmissionDeadline(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because the student has already participated and restart is not allowed
	@Test
	void startFailedOnlyOneParticipation() throws Exception {
		final User student = getStudent("stud");

		offer.setOnlyOneParticipation(true);
		offer = baseService.merge(offer);

		CourseRecord oldParticipation = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(oldParticipation);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because the student entered the wrong password
	@Test
	void startFailedWrongPassword() throws Exception {
		final User student = getStudent("stud");

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "myPersonalPassword123!");
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer, "myPass");
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because the student has no personal password entry
	@Test
	void startFailedNoPasswordForUser() throws Exception {
		final User student = getStudent("stud");

		offer.setEnablePersonalPasswords(true);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer, "myPass");
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Submission start failed because a password is required
	@Test
	void startFailedMissingPassword() throws Exception {
		final User student = getStudent("stud");

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "myPersonalPassword123!");
		offer = baseService.merge(offer);

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.startSubmission(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// #########################################################################
	// Restart
	// #########################################################################

	@Test
	void restart() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		enrollmentBusiness.restartCourse(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
		assertEquals(2, baseService.countAll(CourseRecord.class));
	}

	@Test
	void restartWithinSubmissionPeriod() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setSubmissionStart(LocalDateTime.now().minusDays(1));
		offer.setSubmissionDeadline(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		enrollmentBusiness.restartCourse(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
		assertEquals(2, baseService.countAll(CourseRecord.class));
	}

	@Test
	void restartWithPassword() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "sudo rm -rf /");
		offer = baseService.merge(offer);

		enrollmentBusiness.restartCourse(student, offer, "sudo rm -rf /");
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
		assertEquals(2, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because user has no open course record to restart
	@Test
	void restartFailedNoOpenCourseRecord() throws Exception {
		final User student = getStudent("stud");

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
		assertEquals(0, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because course offer is not visible
	@Test
	void restartFailedNotVisible() throws Exception {
		final User student = getStudent("stud");

		CourseRecord oldParticipation = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(oldParticipation);

		// not started yet

		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));

		// Already finished

		offer.setVisibilityStartTime(null);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because course offer is not linked to a course
	@Test
	void restartFailedNoCourse() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setCourse(null);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because current time is not within the submission period
	@Test
	void restartFailedOutsideSubmissionPeriod() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		// Submission not started yet

		offer.setSubmissionStart(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));

		// Deadline over

		offer.setSubmissionStart(null);
		offer.setSubmissionDeadline(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because only one participation is allowed an the user already has participated
	@Test
	void restartFailedOnlyOneParticipation() throws Exception {
		final User student = getStudent("stud");

		offer.setOnlyOneParticipation(true);
		offer = baseService.merge(offer);

		enrollmentBusiness.startSubmission(student, offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because the user entered a wrong password
	@Test
	void restartFailedWrongPassword() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "myPersonalPassword123!");
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer, "myPass");
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because user has no personal password entry
	@Test
	void restartFailedNoPasswordForUser() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setEnablePersonalPasswords(true);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer, "myPass");
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

	// Restart failed because a password is required
	@Test
	void restartFailedMissingPassword() throws Exception {
		final User student = getStudent("stud");

		enrollmentBusiness.startSubmission(student, offer);

		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "myPersonalPassword123!");
		offer = baseService.merge(offer);

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.restartCourse(student, offer);
		});
		assertEquals(1, baseService.countAll(CourseRecord.class));
	}

}
