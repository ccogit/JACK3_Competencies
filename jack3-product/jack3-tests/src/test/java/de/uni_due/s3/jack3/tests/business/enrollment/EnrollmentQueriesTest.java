package de.uni_due.s3.jack3.tests.business.enrollment;

import static de.uni_due.s3.jack3.tests.utils.Assert.assertEqualsListUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.User;

class EnrollmentQueriesTest extends AbstractEnrollmentBusinessTest {

	@Test
	void isRegistered() {
		baseService.persist(new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.DISENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		assertTrue(enrollmentBusiness.isEnrolled(getStudent("stud1"), offer));
		assertFalse(enrollmentBusiness.isEnrolled(getStudent("stud2"), offer));
		assertFalse(enrollmentBusiness.isEnrolled(getStudent("stud3"), offer));
		assertFalse(enrollmentBusiness.isEnrolled(getStudent("stud4"), offer));
	}

	@Test
	void isDeregistered() {
		baseService.persist(new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.DISENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		assertFalse(enrollmentBusiness.isDisenrolled(getStudent("stud1"), offer));
		assertTrue(enrollmentBusiness.isDisenrolled(getStudent("stud2"), offer));
		assertFalse(enrollmentBusiness.isDisenrolled(getStudent("stud3"), offer));
		assertTrue(enrollmentBusiness.isDisenrolled(getStudent("stud4"), offer));
	}

	@Test
	void isOnWaitingList() {
		baseService.persist(new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.DISENROLLED, null, null));
		baseService.persist(new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		assertFalse(enrollmentBusiness.isOnWaitingList(getStudent("stud1"), offer));
		assertFalse(enrollmentBusiness.isOnWaitingList(getStudent("stud2"), offer));
		assertTrue(enrollmentBusiness.isOnWaitingList(getStudent("stud3"), offer));
		assertFalse(enrollmentBusiness.isOnWaitingList(getStudent("stud4"), offer));
	}

	@Test
	void freePlaces() {
		// Unlimited
		assertTrue(enrollmentBusiness.hasFreePlaces(offer));
		assertFalse(enrollmentBusiness.getFreePlaces(offer).isPresent());

		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(2);
		offer = baseService.merge(offer);

		// noone registered -> 2 free places
		assertTrue(enrollmentBusiness.hasFreePlaces(offer));
		assertEquals(2, (long) enrollmentBusiness.getFreePlaces(offer).orElseThrow(AssertionError::new));

		// 1 student registered -> 1 free place
		baseService.persist(new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null));
		assertTrue(enrollmentBusiness.hasFreePlaces(offer));
		assertEquals(1, (long) enrollmentBusiness.getFreePlaces(offer).orElseThrow(AssertionError::new));

		// 2 students registered -> 0 free place
		baseService.persist(new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.ENROLLED, null, null));
		assertFalse(enrollmentBusiness.hasFreePlaces(offer));
		assertEquals(0, (long) enrollmentBusiness.getFreePlaces(offer).orElseThrow(AssertionError::new));

		// 3 students registered -> 0 free place (number of free places must NOT be negative!)
		baseService.persist(new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.ENROLLED, null, null));
		assertFalse(enrollmentBusiness.hasFreePlaces(offer));
		assertEquals(0, (long) enrollmentBusiness.getFreePlaces(offer).orElseThrow(AssertionError::new));
	}

	@Test
	void waitingList() throws Exception {
		offer.setExplicitEnrollment(true);
		offer.setMaxAllowedParticipants(1);
		offer.setEnableWaitingList(true);
		offer = baseService.merge(offer);

		// Dummy enrollment for waiting list activation
		baseService.persist(new Enrollment(getStudent("dummy"), offer, EEnrollmentStatus.ENROLLED, null, null));

		// Empty waiting list
		assertTrue(enrollmentBusiness.getWaitingList(offer).isEmpty());

		// 1 student on waiting list
		baseService.persist(new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		List<String> studentNames = enrollmentBusiness.getWaitingList(offer)
				.stream().map(Enrollment::getUser).map(User::getLoginName).collect(Collectors.toList());
		assertEquals(Arrays.asList("stud1"), studentNames);

		// 2 students on waiting list
		baseService.persist(new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null));
		studentNames = enrollmentBusiness.getWaitingList(offer)
				.stream().map(Enrollment::getUser).map(User::getLoginName).collect(Collectors.toList());
		// stud1 should have higher priority on waiting list
		assertEquals(Arrays.asList("stud1", "stud2"), studentNames);

		// Now stud1 leaves the waitinglist and joins again -> stud2 should have higher priority
		enrollmentBusiness.disenrollUser(getStudent("stud1"), offer);
		enrollmentBusiness.joinWaitingList(getStudent("stud1"), offer);
		studentNames = enrollmentBusiness.getWaitingList(offer)
				.stream().map(Enrollment::getUser).map(User::getLoginName).collect(Collectors.toList());
		assertEquals(Arrays.asList("stud2", "stud1"), studentNames);
	}

	@Test
	void allParticipations() {
		// No participations
		assertTrue(enrollmentBusiness.getAllParticipations(offer).isEmpty());

		// Create dummy enrollments
		Enrollment registered = new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null);
		Enrollment deregistered = new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.DISENROLLED, null,
				null);
		Enrollment waiting = new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null);
		getStudent("stud4");

		baseService.persist(registered);
		baseService.persist(deregistered);
		baseService.persist(waiting);

		// We check if each enrollment is included in "getAllParticipations"
		List<Enrollment> allParticipations = enrollmentBusiness.getAllParticipations(offer);
		// Sort list by student name for better checking
		allParticipations.sort(Comparator.comparing(e -> e.getUser().getLoginName()));

		assertEquals(3, allParticipations.size());

		Enrollment result = allParticipations.get(0);
		assertEquals(getStudent("stud1"), result.getUser());
		assertEquals(EEnrollmentStatus.ENROLLED, result.getStatus());

		result = allParticipations.get(1);
		assertEquals(getStudent("stud2"), result.getUser());
		assertEquals(EEnrollmentStatus.DISENROLLED, result.getStatus());

		result = allParticipations.get(2);
		assertEquals(getStudent("stud3"), result.getUser());
		assertEquals(EEnrollmentStatus.ON_WAITINGLIST, result.getStatus());
	}

	@Test
	void currentRegistrations() {
		// No registrations
		assertEquals(0, enrollmentBusiness.countCurrentEnrollments(offer));
		assertTrue(enrollmentBusiness.getAllParticipations(offer).isEmpty());

		// Create dummy enrollments
		Enrollment registered = new Enrollment(getStudent("stud1"), offer, EEnrollmentStatus.ENROLLED, null, null);
		Enrollment registered2 = new Enrollment(getStudent("stud2"), offer, EEnrollmentStatus.ENROLLED, null, null);
		Enrollment deregistered = new Enrollment(getStudent("stud3"), offer, EEnrollmentStatus.DISENROLLED, null,
				null);
		Enrollment waiting = new Enrollment(getStudent("stud4"), offer, EEnrollmentStatus.ON_WAITINGLIST, null, null);
		getStudent("stud5");

		baseService.persist(registered);
		baseService.persist(registered2);
		baseService.persist(deregistered);
		baseService.persist(waiting);

		// We check if stud1 and stud2 are included in the registration user list - This should be enough to determine
		// that the function is working correctly
		List<Enrollment> current = enrollmentBusiness.getCurrentEnrollments(offer);
		List<String> registeredUsernames = current.stream().map(Enrollment::getUser).map(User::getLoginName)
				.collect(Collectors.toList());

		assertEqualsListUnordered(Arrays.asList("stud1", "stud2"), registeredUsernames);
		assertEquals(2, enrollmentBusiness.countCurrentEnrollments(offer));
	}

	@Test
	void getOneEnrollment() {
		User stud1 = getStudent("student1");
		User stud2 = getStudent("student2");
		User stud3 = getStudent("student3");
		User stud4 = getStudent("student4");

		// Create dummy enrollments
		Enrollment registered = new Enrollment(stud1, offer, EEnrollmentStatus.ENROLLED, null, null);
		Enrollment deregistered = new Enrollment(stud2, offer, EEnrollmentStatus.DISENROLLED, null, null);
		Enrollment waiting = new Enrollment(stud3, offer, EEnrollmentStatus.ON_WAITINGLIST, null, null);

		baseService.persist(registered);
		baseService.persist(deregistered);
		baseService.persist(waiting);

		assertEquals(registered, enrollmentBusiness.getEnrollment(stud1, offer).orElseThrow(AssertionError::new));
		assertEquals(deregistered, enrollmentBusiness.getEnrollment(stud2, offer).orElseThrow(AssertionError::new));
		assertEquals(waiting, enrollmentBusiness.getEnrollment(stud3, offer).orElseThrow(AssertionError::new));
		assertFalse(enrollmentBusiness.getEnrollment(stud4, offer).isPresent());

		assertEquals(EEnrollmentStatus.ENROLLED, enrollmentBusiness.getStatus(stud1, offer));
		assertEquals(EEnrollmentStatus.DISENROLLED, enrollmentBusiness.getStatus(stud2, offer));
		assertEquals(EEnrollmentStatus.ON_WAITINGLIST, enrollmentBusiness.getStatus(stud3, offer));
		assertEquals(EEnrollmentStatus.DISENROLLED, enrollmentBusiness.getStatus(stud4, offer));
	}

}
