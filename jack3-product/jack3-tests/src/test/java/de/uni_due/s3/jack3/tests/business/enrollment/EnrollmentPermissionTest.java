package de.uni_due.s3.jack3.tests.business.enrollment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;

/**
 * Tests the different enrollment permissions (visibility and password check). See {@link EnrollmentActionsTest} for
 * tests (de)register permission checks.
 * 
 * @author lukas.glaser
 */
class EnrollmentPermissionTest extends AbstractEnrollmentBusinessTest {

	// NOTE: The other EnrollmentBusiness.check*-methods are testet in EnrollmentActionsTest.

	@Test
	void visibleNoSettings() {
		// Always visible without any settings
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void visibleWithinVisibilityPeriod() {
		// Between start and end time
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(1));
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));

		// After start time
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(1));
		offer.setVisibilityEndTime(null);
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));

		// Before end time
		offer.setVisibilityStartTime(null);
		offer.setVisibilityEndTime(LocalDateTime.now().plusDays(1));
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void visibleAllowlisted() {
		offer.setUserFilter(new HashSet<>(Arrays.asList("stud")));
		offer.setToggleAllowlist(true);
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void visibleNotBlocklisted() {
		offer.setToggleAllowlist(false);
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void visibleNotAllowlistedRegistered() {
		offer.setToggleAllowlist(true);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("tempStud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		// Not on allowlist but registered -> visible
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void visibleBlocklistedRegistered() {
		offer.setUserFilter(new HashSet<>(Arrays.asList("stud")));
		offer.setToggleAllowlist(false);
		offer = baseService.merge(offer);
		baseService.persist(new Enrollment(getStudent("stud"), offer, EEnrollmentStatus.ENROLLED, null, null));
		// On blocklist but registered -> visible
		assertTrue(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void notVisibleWithinVisibilityPeriod() {
		// Not between start and end time
		offer.setVisibilityStartTime(LocalDateTime.now().minusDays(2));
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));

		// Before start time
		offer.setVisibilityStartTime(LocalDateTime.now().plusDays(1));
		offer.setVisibilityEndTime(null);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));

		// After end time
		offer.setVisibilityStartTime(null);
		offer.setVisibilityEndTime(LocalDateTime.now().minusDays(1));
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void notVisibleNotAllowlisted() {
		offer.setToggleAllowlist(true);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void notVisibleBlocklisted() {
		offer.setUserFilter(new HashSet<>(Arrays.asList("stud")));
		offer.setToggleAllowlist(false);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isCourseOfferVisibleForStudent(getStudent("stud"), offer));
	}

	@Test
	void globalPasswordRequired() {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword("(°J°)");
		offer = baseService.merge(offer);
		assertTrue(enrollmentBusiness.isGlobalPasswordRequired(offer));
	}

	@Test
	void noGlobalPasswordSet1() {
		offer.setExplicitEnrollment(true);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isGlobalPasswordRequired(offer));
	}

	@Test
	void noGlobalPasswordSet2() {
		offer.setExplicitEnrollment(true);
		offer.setGlobalPassword(null);
		offer = baseService.merge(offer);
		assertFalse(enrollmentBusiness.isGlobalPasswordRequired(offer));
	}

}
