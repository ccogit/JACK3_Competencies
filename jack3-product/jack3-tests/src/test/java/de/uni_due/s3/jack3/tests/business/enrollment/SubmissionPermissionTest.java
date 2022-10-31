package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Tests the different processing permissions (start, restart, continue). For other permission tests, see
 * {@link SubmissionActionsTest}.
 * 
 * @author lukas.glaser
 */
class SubmissionPermissionTest extends AbstractEnrollmentBusinessTest {

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();

		final Course course = getCourse("Course");
		offer.setCourse(course);
		offer.setExplicitSubmission(true);
		offer = baseService.merge(offer);
	}

	// NOTE: The other EnrollmentBusiness.check*-methods are testet in SubmissionActionsTest.

	@Test
	void continueCourse() throws Exception {
		final User student = getStudent("stud");
		enrollmentBusiness.startSubmission(student, offer);

		enrollmentBusiness.checkContinueCoursePermission(student, offer);
	}

	@Test
	void continueCoursePermittedNoOpenCourseRecord() throws Exception {
		final User student = getStudent("stud");
		final CourseRecord oldRecord = enrollmentBusiness.startSubmission(student, offer);
		oldRecord.closeManually();
		baseService.merge(oldRecord);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});
	}

	@Test
	void continueCoursePermittedNotVisible() throws Exception {
		final User student = getStudent("stud");
		enrollmentBusiness.startSubmission(student, offer);

		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});

		offer.setVisibilityStartTime(null);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		// This is a special case: After the visibility end time, all course records are closed so we get a
		// SubmissionException here with the reason "no open course record"
		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});
	}

	@Test
	void continueCoursePermittedNoCourse() throws Exception {
		final User student = getStudent("stud");
		enrollmentBusiness.startSubmission(student, offer);

		offer.setCourse(null);
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});
	}

	@Test
	void continueCoursePermittedOutsideSubmissionPeriod() throws Exception {
		final User student = getStudent("stud");
		enrollmentBusiness.startSubmission(student, offer);

		offer.setSubmissionStart(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});

		offer.setSubmissionStart(null);
		offer.setSubmissionDeadline(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		assertThrows(SubmissionException.class, () -> {
			enrollmentBusiness.checkContinueCoursePermission(student, offer);
		});
	}

	@Test
	void personalPasswordRequired() {
		offer.setExplicitSubmission(true);
		offer.setEnablePersonalPasswords(true);
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isPersonalPasswordRequired(offer));
	}

	@Test
	void noPersonalPasswordSet() {
		offer.setExplicitSubmission(true);
		offer.setEnablePersonalPasswords(false);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isPersonalPasswordRequired(offer));
	}

}
