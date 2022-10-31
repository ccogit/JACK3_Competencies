package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.EnrollmentService;

/**
 * Tests the Enrollment actions that should be performed automatically if a user saves a Course Offer with changed
 * parameters.
 */
class EnrollmentActionsOnSavingTest extends AbstractEnrollmentBusinessTest {

	@Inject
	private EnrollmentService enrollmentService;

	// For the different cases, see the uploaded design in issue #1094

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();
		offer.setExplicitEnrollment(true);
		offer.setCanBeVisible(true);
		offer = baseService.merge(offer);
	}

	private void saveAndPerformActions() throws Exception {
		offer = baseService.merge(offer);
		try {
			enrollmentBusiness.moveUpUsersAfterSaving(offer, lecturer);
		} catch (MessagingException e) {
			// Ignore
		}
		try {
			enrollmentBusiness.disenrollUsersAfterSaving(offer, lecturer);
		} catch (MessagingException e) {
			// Ignore
		}
	}

	/**
	 * Sets 2 max. participants, enables waiting list, enrolls 2 users and lets 2 users wait.
	 */
	private void setupScenario() throws Exception {
		offer.setMaxAllowedParticipants(2);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);
		enrollmentBusiness.enrollUser(getStudent("st1"), offer);
		enrollmentBusiness.enrollUser(getStudent("st2"), offer);
		enrollmentBusiness.joinWaitingList(getStudent("st3"), offer);
		enrollmentBusiness.joinWaitingList(getStudent("st4"), offer);
	}

	/**
	 * Sets 2 max. participants, leaves waiting list disabled and enrolls 2 users.
	 */
	private void enroll2UsersAndSetLimit() throws Exception {
		offer.setMaxAllowedParticipants(2);
		offer = baseService.merge(offer);
		enrollmentBusiness.enrollUser(getStudent("st1"), offer);
		enrollmentBusiness.enrollUser(getStudent("st2"), offer);
	}

	/**
	 * Enrolls 2 users.
	 */
	private void enroll2Users() throws Exception {
		enrollmentBusiness.enrollUser(getStudent("st1"), offer);
		enrollmentBusiness.enrollUser(getStudent("st2"), offer);
	}

	private void expectStatus(EEnrollmentStatus status, Set<String> expectedUsernames) {
		final var enrollments = enrollmentService.getEnrollments(offer, status);
		final var actualUsernames = enrollments.stream()
				.map(Enrollment::getUser)
				.map(User::getLoginName)
				.collect(Collectors.toSet());
		assertEquals(expectedUsernames, actualUsernames);
	}

	@Test
	void noEnrolledUsers() throws Exception {
		assertEquals(0, enrollmentBusiness.countCurrentEnrollments(offer));
		assertEquals(0, enrollmentBusiness.countCurrentWaitinglist(offer));
		saveAndPerformActions();
		assertEquals(0, enrollmentBusiness.countCurrentEnrollments(offer));
		assertEquals(0, enrollmentBusiness.countCurrentWaitinglist(offer));
	}

	@Test
	void case1NoChange() throws Exception {
		setupScenario();
		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of("st3", "st4"));
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());

		saveAndPerformActions();
		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of("st3", "st4"));
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case2EnableWaitlist() throws Exception {
		enroll2UsersAndSetLimit();
		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());

		offer.setEnableWaitingList(true);
		saveAndPerformActions();
		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case3DisableWaitlist() throws Exception {
		setupScenario();
		offer.setEnableWaitingList(false);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of("st3", "st4"));
	}

	@Test
	void case4DecreaseMaxParticipants() throws Exception {
		setupScenario();
		offer.setMaxAllowedParticipants(1);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of("st3", "st4"));
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case5DecreaseMaxParticipantsAndEnableWaitlist() throws Exception {
		enroll2UsersAndSetLimit();
		offer.setEnableWaitingList(true);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case6DecreaseMaxParticipantsAndDisableWaitlist() throws Exception {
		setupScenario();
		offer.setEnableWaitingList(false);
		offer.setMaxAllowedParticipants(1);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of("st3", "st4"));
	}

	@Test
	void case7IncreaseMaxParticipants() throws Exception {
		setupScenario();
		offer.setMaxAllowedParticipants(3);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2", "st3"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of("st4"));
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case8IncreaseMaxParticipantsAndEnableWaitlist() throws Exception {
		enroll2UsersAndSetLimit();
		offer.setEnableWaitingList(true);
		offer.setMaxAllowedParticipants(3);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case9IncreaseMaxParticipantsAndDisableWaitlist() throws Exception {
		setupScenario();
		offer.setEnableWaitingList(false);
		offer.setMaxAllowedParticipants(3);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2", "st3"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of("st4"));
	}

	@Test
	void case10DisableParticipantLimit() throws Exception {
		setupScenario();
		offer.setEnableWaitingList(false);
		offer.setMaxAllowedParticipants(0);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2", "st3", "st4"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

	@Test
	void case11EnableParticipantLimit() throws Exception {
		enroll2Users();
		offer.setMaxAllowedParticipants(1);
		saveAndPerformActions();

		expectStatus(EEnrollmentStatus.ENROLLED, Set.of("st1", "st2"));
		expectStatus(EEnrollmentStatus.ON_WAITINGLIST, Set.of());
		expectStatus(EEnrollmentStatus.DISENROLLED, Set.of());
	}

}
