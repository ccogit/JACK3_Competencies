package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

import javax.mail.MessagingException;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.LinkedCourseException;
import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Tests regarding enrollment / disenrollment / joining the waitlist for a student and a course offer.
 * 
 * @author lukas.glaser
 */
class EnrollmentActionsTest extends AbstractEnrollmentBusinessTest {

	// #########################################################################
	// Enrollment
	// #########################################################################

	@Test
	void enroll() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithMaxParticipants() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(2);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");
		baseService.persist(new Enrollment(getStudent("temp"), offer, EEnrollmentStatus.ENROLLED, null, null));

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithPassword() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer, "globalPassword123");
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithPasswordButNotEnabled() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer, "globalPassword123");
		// The user should be enrolled because no password is required. It does not matter that the user entered a
		// password
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithinVisibilityPeriod() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(2));
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithinRegistrationPeriod() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentStart(LocalDateTime.now().minusDays(2));
		offer.setEnrollmentDeadline(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	@Test
	void enrollWithinAllPeriods() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(4));
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(4));
		offer.setEnrollmentStart(LocalDateTime.now().minusDays(2));
		offer.setEnrollmentDeadline(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	// Fail enrollment because missing password
	@Test
	void enrolFailedPasswordRequired() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because password is wrong
	@Test
	void enrolFailedPasswordWrong() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer, "globalPassword1234");
		});
	}

	// Fail enrollment because course offer is not visible (start time after now)
	@Test
	void enrolFailedNotInteractable1() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because course offer is not visible (end time before now)
	@Test
	void enrolFailedNotInteractable2() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because student is not on allowlist
	@Test
	void enrolFailedNotAllowlisted() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setToggleAllowlist(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because student is on blocklist
	@Test
	void enrolFailedBlocklisted() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setToggleAllowlist(false);
		offer.setUserFilter(new HashSet<>(Arrays.asList("stud")));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because registration has not started yet
	@Test
	void enrolFailedOutsideRegistrationPeriod1() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentStart(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because registration is over
	@Test
	void enrolFailedOutsideRegistrationPeriod2() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentDeadline(LocalDateTime.now().minusDays(2));
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because already enrolled
	@Test
	void enrolFailedAlreadyEnrolled() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		// This should throw the exception!
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
	}

	// Fail enrollment because already enrolled in a sub folder
	@Test
	void enrolFailedAlreadyEnrolledIndirectLinkedCourse1() throws Exception {
		CourseOffer offer2 = getCourseOffer("Second Course Offer", getSubfolder("Sub Folder"));
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		offer2.setExplicitEnrollment(true);
		offer2 = baseService.merge(offer2);
		folder.setContainsLinkedCourses(true);
		folder = baseService.merge(folder);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer2);
		// This should throw the exception, even if the Course Offer where the user is registered is in another folder
		final EnrollmentException thrown = assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer);
		});
		assertTrue(thrown instanceof LinkedCourseException);
		assertEquals(offer2, ((LinkedCourseException) thrown).getLinkedCourse());
	}

	// Fail enrollment because already enrolled in a parent folder
	@Test
	void enrolFailedAlreadyEnrolledIndirectLinkedCourse2() throws Exception {
		CourseOffer offer2 = getCourseOffer("Second Course Offer", getSubfolder("Sub Folder"));
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		offer2.setExplicitEnrollment(true);
		offer2 = baseService.merge(offer2);
		folder.setContainsLinkedCourses(true);
		folder = baseService.merge(folder);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		final CourseOffer offer2Final = offer2;
		// This should throw the exception, even if the Course Offer where the user is registered is in another folder
		final EnrollmentException thrown = assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer2Final);
		});
		assertTrue(thrown instanceof LinkedCourseException);
		assertEquals(offer, ((LinkedCourseException) thrown).getLinkedCourse());
	}

	// Fail enrollment because already enrolled in a Folder beside
	@Test
	void enrolFailedAlreadyEnrolledIndirectLinkedCourse3() throws Exception {
		CourseOffer offer1 = getCourseOffer("First Course Offer", getSubfolder("Sub Folder 1"));
		CourseOffer offer2 = getCourseOffer("Second Course Offer", getSubfolder("Sub Folder 2"));
		offer1.setExplicitEnrollment(true);
		offer1 = baseService.merge(offer1);
		offer2.setExplicitEnrollment(true);
		offer2 = baseService.merge(offer2);
		folder.setContainsLinkedCourses(true);
		folder = baseService.merge(folder);
		final User student = getStudent("stud");
		final CourseOffer offer1Final = offer1;
		final CourseOffer offer2Final = offer2;

		enrollmentBusiness.enrollUser(student, offer1Final);
		// This should throw the exception, even if the Course Offer where the user is registered is in another folder
		final EnrollmentException thrown = assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student, offer2Final);
		});
		assertTrue(thrown instanceof LinkedCourseException);
		assertEquals(offer1Final, ((LinkedCourseException) thrown).getLinkedCourse());
	}

	// Fail enrollment because course is full
	@Test
	void enrolFailedCourseFull() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer = baseService.merge(offer);
		final User student1 = getStudent("stud1");
		final User student2 = getStudent("stud2");

		enrollmentBusiness.enrollUser(student1, offer);
		// This should throw the exception!
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUser(student2, offer);
		});
	}

	// Manual enrollment
	@Test
	void enrollManually() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		try {
			enrollmentBusiness.enrollUserManually(student, offer, lecturer, "You enter the course, now!");
		} catch (MessagingException e) {
			//this will cause a MessagingException because we won't have an e-mail session configured
			//for this test we will just ignore this.
		}
		assertTrue(enrollmentBusiness.isEnrolled(student, offer));
	}

	// Fail manual enrollment because no lecturer rights
	@Test
	void enrollManuallyFailedNoRights() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		final User student1 = getStudent("stud1");
		final User student2 = getStudent("stud2");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.enrollUserManually(student1, offer, student2, "I want to add my friend to this course.");
		});
	}

	// #########################################################################
	// Waiting list
	// #########################################################################

	@Test
	void joinWaitingList() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer);
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	@Test
	void joinWaitingListWithPassword() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer, "globalPassword123");
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	@Test
	void joinWaitingListWithPasswordButNotEnabled() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer, "globalPassword123");
		// It does not matter that the user entered a password
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	@Test
	void joinWaitingListWithinVisibilityPeriod() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(2));
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer);
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	@Test
	void joinWaitingListWithinRegistrationPeriod() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentStart(LocalDateTime.now().minusDays(2));
		offer.setEnrollmentDeadline(LocalDateTime.now().plusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer);
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	@Test
	void joinWaitingListWithinAllPeriods() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(4));
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(4));
		offer.setEnrollmentStart(LocalDateTime.now().minusDays(2));
		offer.setEnrollmentDeadline(LocalDateTime.now().plusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.joinWaitingList(student, offer);
		assertTrue(enrollmentBusiness.isOnWaitingList(student, offer));
	}

	// Fail enrollment for waiting list because missing password
	@Test
	void joinWaitingListFailedPasswordRequired() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(PasswordRequiredException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because password is wrong
	@Test
	void joinWaitingListFailedPasswordWrong() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("globalPassword123");
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer, "globalPassword1234");
		});
	}

	// Fail enrollment for waiting list because course offer is not visible (start time after now)
	@Test
	void joinWaitingListFailedNotInteractable1() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because course offer is not visible (end time before now)
	@Test
	void joinWaitingListFailedNotInteractable2() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because student is not on allowlist
	@Test
	void joinWaitingListFailedNotAllowlisted() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setToggleAllowlist(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because student is on blocklist
	@Test
	void joinWaitingListFailedBlocklisted() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setToggleAllowlist(false);
		offer.setUserFilter(new HashSet<>(Arrays.asList("stud")));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(NotInteractableException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because registration has not started yet
	@Test
	void joinWaitingListFailedOutsideRegistrationPeriod1() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentStart(LocalDateTime.now().plusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because registration is over
	@Test
	void joinWaitingListFailedOutsideRegistrationPeriod2() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setEnrollmentDeadline(LocalDateTime.now().minusDays(2));
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because already enrolled
	@Test
	void joinWaitingListFailedAlreadyEnrolled() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer);
		// This should throw the exception!
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because already enrolled
	@Test
	void joinWaitingListFailedAlreadyEnrolledInLinkedCourse() throws Exception {
		CourseOffer offer2 = getCourseOffer("Second Course Offer");
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer.setCanBeVisible(true);
		offer = baseService.merge(offer);
		offer2.setExplicitEnrollment(true);
		offer2.setMaxAllowedParticipants(1);
		offer2.setEnableWaitingList(true);
		offer2.setCanBeVisible(true);
		offer2 = baseService.merge(offer2);
		folder.setContainsLinkedCourses(true);
		folder = baseService.merge(folder);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		final User student = getStudent("stud");

		enrollmentBusiness.enrollUser(student, offer2);
		// This should throw the exception!
		assertThrows(LinkedCourseException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because course is not full
	@Test
	void joinWaitingListFailedCourseNotFull() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// Fail enrollment for waiting list because waiting list is not enabled
	@Test
	void joinWaitingListFailedListDisabled() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer = baseService.merge(offer);
		final User student = getStudent("stud");

		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.joinWaitingList(student, offer);
		});
	}

	// #########################################################################
	// Disenrollment
	// #########################################################################

	@Test
	void disenroll() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);

		final User student1 = getStudent("stud1");
		baseService.persist(new Enrollment(student1, offer, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentBusiness.disenrollUser(student1, offer);
		assertTrue(enrollmentBusiness.isDisenrolled(student1, offer));

		final User student2 = getStudent("stud2");
		baseService.persist(new Enrollment(student2, offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		enrollmentBusiness.disenrollUser(student2, offer);
		assertTrue(enrollmentBusiness.isDisenrolled(student2, offer));
	}

	@Test
	void disenrollWithinDisenrollmentDeadline() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setDisenrollmentDeadline(LocalDateTime.now().plusDays(2));
		offer = baseService.merge(offer);

		final User student1 = getStudent("stud1");
		baseService.persist(new Enrollment(student1, offer, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentBusiness.disenrollUser(student1, offer);
		assertTrue(enrollmentBusiness.isDisenrolled(student1, offer));

		final User student2 = getStudent("stud2");
		baseService.persist(new Enrollment(student2, offer, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentBusiness.disenrollUser(student2, offer);
		assertTrue(enrollmentBusiness.isDisenrolled(student2, offer));
	}

	@Test
	void disenrollFailedOutsideDeadline() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setDisenrollmentDeadline(LocalDateTime.now().minusDays(2));
		offer = baseService.merge(offer);

		final User student1 = getStudent("stud1");
		baseService.persist(new Enrollment(student1, offer, EEnrollmentStatus.ENROLLED, null, null));
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.disenrollUser(student1, offer);
		});

		final User student2 = getStudent("stud2");
		baseService.persist(new Enrollment(student2, offer, EEnrollmentStatus.ENROLLED, null, null));
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.disenrollUser(student2, offer);
		});
	}


	// Manual enrollment
	@Test
	void disenrollManually() throws Exception {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);

		final User student1 = getStudent("stud1");
		baseService.persist(new Enrollment(student1, offer, EEnrollmentStatus.ENROLLED, null, null));
		try {
			enrollmentBusiness.disenrollUserManually(student1, offer, lecturer, "You leave the course, now!");
		} catch (MessagingException e) {
			// this will cause a MessagingException because we won't have an e-mail session configured
			// for this test we will just ignore this.
		}
		assertTrue(enrollmentBusiness.isDisenrolled(student1, offer));

		final User student2 = getStudent("stud2");
		baseService.persist(new Enrollment(student2, offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		try {
			enrollmentBusiness.disenrollUserManually(student2, offer, lecturer, "You leave the course, now!");
		} catch (MessagingException e) {
			// this will cause a MessagingException because we won't have an e-mail session configured
			// for this test we will just ignore this.
		}
		assertTrue(enrollmentBusiness.isDisenrolled(student2, offer));
	}

	@Test
	void disenrollManuallyFailedNoRights() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setDisenrollmentDeadline(LocalDateTime.now().minusDays(2));
		offer = baseService.merge(offer);
		final User student1 = getStudent("stud1");
		final User student2 = getStudent("stud2");
		final User student3 = getStudent("stud3");

		baseService.persist(new Enrollment(student1, offer, EEnrollmentStatus.ENROLLED, null, null));
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.disenrollUserManually(student1, offer, student3, "I try to remove a friend from course");
		});

		baseService.persist(new Enrollment(student2, offer, EEnrollmentStatus.ENROLLED, null, null));
		assertThrows(EnrollmentException.class, () -> {
			enrollmentBusiness.disenrollUserManually(student2, offer, student3, "I try to remove a friend from course");
		});
	}

}
