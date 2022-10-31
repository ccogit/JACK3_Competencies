package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import javax.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Tests all actions of the course registration, which happen without any action of the user. These are e.g. the
 * automatic redirect to a course record, the automatic enrollment and the automatic closing of course records after
 * deregistering.
 * 
 * @author lukas.glaser
 */
class AutomationTest extends AbstractEnrollmentBusinessTest {

	private User student;

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();
		student = getStudent("student");
	}

	private void linkOfferWithCourse() {
		final Course course = getCourse("Course");
		offer.setCourse(course);
		offer.setExplicitSubmission(true);
		offer = baseService.merge(offer);
	}

	private void setExplicit(boolean enrollment, boolean submission) {
		offer.setExplicitEnrollment(enrollment);
		offer.setExplicitSubmission(submission);
		offer = baseService.merge(offer);
	}

	// #########################################################################
	// Automatic submission start / close
	// #########################################################################

	// Start a new submission automatically on enrollment
	@Test
	void submissionStartOnEnrollment1() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, false);

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Start a new submission automatically on enrollment
	@Test
	void submissionStartOnEnrollment2() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, false);

		try {
			enrollmentBusiness.enrollUserManually(student, offer, lecturer, null);
		} catch (MessagingException e) {
			//this will cause a MessagingException because we won't have an e-mail session configured
			//for this test we will just ignore this.
		}
		assertTrue(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Close an existing submission automatically on disenrollment
	@Test
	void submissionCloseOnDisenrollment1() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, false);

		enrollmentBusiness.enrollUser(student, offer);
		enrollmentBusiness.disenrollUser(student, offer);
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Close an existing submission automatically on disenrollment
	@Test
	void submissionCloseOnDisenrollment2() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, false);

		enrollmentBusiness.enrollUser(student, offer);
		try {
			enrollmentBusiness.disenrollUserManually(student, offer, lecturer, null);
		} catch (MessagingException e) {
			//this will cause a MessagingException because we won't have an e-mail session configured
			//for this test we will just ignore this.
		}
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Don't start a new submission on enrollment because explicit submission start is enabled
	@Test
	void noSubmissionStartOnEnrollment1() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, true);

		enrollmentBusiness.enrollUser(student, offer);
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// Don't start a new submission on enrollment because course offer is not linked to a course
	@Test
	void noSubmissionStartOnEnrollment2() throws Exception {
		setExplicit(true, false);

		enrollmentBusiness.enrollUser(student, offer);
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());
	}

	// #########################################################################
	// Automatic enrollment / disenrollment
	// #########################################################################

	// Enroll a student automatically on submission start
	@Test
	void enrollmentOnStartSubmission() throws Exception {
		linkOfferWithCourse();
		setExplicit(false, true);

		enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	// Disenroll a student automatically on closing a submission
	@Test
	void disenrollmentOnCloseSubmission1() throws Exception {
		linkOfferWithCourse();
		setExplicit(false, true);

		final CourseRecord record = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(record);
		assertTrue(enrollmentBusiness.isDisenrolled(student, offer));
	}

	// Disenroll a student automatically on closing a submission
	@Test
	void disenrollmentOnCloseSubmission2() throws Exception {
		linkOfferWithCourse();
		setExplicit(false, true);

		final CourseRecord record = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(record, lecturer, null);
		assertTrue(enrollmentBusiness.isDisenrolled(student, offer));
	}

	// Don't disenroll a student on closing a submission because explicit enrollment is enabled
	@Test
	void noDisenrollmentOnCloseSubmission1() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, true);

		enrollmentBusiness.enrollUser(student, offer);
		final CourseRecord record = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(record);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	// Don't disenroll a student on closing a submission because explicit enrollment is enabled
	@Test
	void noDisenrollmentOnCloseSubmission2() throws Exception {
		linkOfferWithCourse();
		setExplicit(true, true);

		enrollmentBusiness.enrollUser(student, offer);
		final CourseRecord record = enrollmentBusiness.startSubmission(student, offer);
		enrollmentBusiness.closeSubmissionManually(record, lecturer, null);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	// #########################################################################
	// Move up from waiting list
	// #########################################################################

	// Move up from waiting list if someone disenrolls
	@Test
	void moveUp() throws Exception {
		setExplicit(true, false);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);

		final User movingUp = getStudent("student2");
		enrollmentBusiness.enrollUser(student, offer);

		// student2 joins the waiting list
		enrollmentBusiness.joinWaitingList(movingUp, offer);

		// Only one student is enrolled, the other is on the waitinglist
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
		assertFalse(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(movingUp, offer));

		// Now the first student disenrolls himself => the second student should move up!
		enrollmentBusiness.disenrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertFalse(enrollmentBusiness.isOnWaitingList(movingUp, offer));
	}

	// Don't move up from waiting list if a lecturer manually enrolled more students than permitted
	@Test
	void noMoveUpCourseFull() throws Exception {
		setExplicit(true, false);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);

		final User movingUp = getStudent("student2");
		enrollmentBusiness.enrollUser(student, offer);

		// student2 joins the waiting list
		enrollmentBusiness.joinWaitingList(movingUp, offer);

		// student3 is manually enrolled by a lecturer
		final User manuallyEnrolled = getStudent("student3");
		try {
			enrollmentBusiness.enrollUserManually(manuallyEnrolled, offer, lecturer, null);
		} catch (MessagingException e) {
			//this will cause a MessagingException because we won't have an e-mail session configured
			//for this test we will just ignore this.
		}

		// student and student3 both should be enrolled, student2 must wait
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
		assertTrue(enrollmentBusiness.isEnrolled(manuallyEnrolled, offer));
		assertFalse(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(movingUp, offer));

		// Now the first student disenrolls himself => the second student should not move up because course is still
		// full!
		enrollmentBusiness.disenrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(manuallyEnrolled, offer));
		assertFalse(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(movingUp, offer));
	}

	// Don't move up from waiting list after enrollment deadline
	@Test
	void noMoveUpAfterEnrollmentDeadline() throws Exception {
		setExplicit(true, false);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);

		final User movingUp = getStudent("student2");
		enrollmentBusiness.enrollUser(student, offer);

		// student2 joins the waiting list
		enrollmentBusiness.joinWaitingList(movingUp, offer);

		// student should be enrolled, student2 must wait
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
		assertFalse(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(movingUp, offer));

		// The enrollment deadline is reached
		offer.setEnrollmentDeadline(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);

		// Now the first student disenrolls himself => the second student should not move up because the enrollment
		// deadline is over!
		enrollmentBusiness.disenrollUser(student, offer);
		assertFalse(enrollmentBusiness.isEnrolled(movingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(movingUp, offer));
	}

	// Moving up after participants limit was subsequently incresed
	void moveUpAllUsers() throws Exception {
		setExplicit(true, false);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);

		final User movingUp1 = getStudent("student2");
		final User movingUp2 = getStudent("student3");
		final User notMovingUp = getStudent("student4");
		enrollmentBusiness.enrollUser(student, offer);

		// Students join the waiting list
		enrollmentBusiness.joinWaitingList(movingUp1, offer);
		enrollmentBusiness.joinWaitingList(movingUp2, offer);
		enrollmentBusiness.joinWaitingList(notMovingUp, offer);

		// Increase max. participants
		offer.setMaxAllowedParticipants(3);
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.hasFreePlaces(offer));

		enrollmentBusiness.moveUpUsersAfterSaving(offer, lecturer);

		// Three students should be enrolled, one remains on the waiting list
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
		assertTrue(enrollmentBusiness.isEnrolled(movingUp1, offer));
		assertTrue(enrollmentBusiness.isEnrolled(movingUp2, offer));
		assertFalse(enrollmentBusiness.isEnrolled(notMovingUp, offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(notMovingUp, offer));
	}

}
