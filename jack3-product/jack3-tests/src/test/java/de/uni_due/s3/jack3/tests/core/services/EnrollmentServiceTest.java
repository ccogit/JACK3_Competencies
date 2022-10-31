package de.uni_due.s3.jack3.tests.core.services;

import static de.uni_due.s3.jack3.tests.utils.Assert.assertEqualsEntityListUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class EnrollmentServiceTest extends AbstractBasicTest {

	private User lecturer;
	private User student1;
	private User student2;
	private User student3;
	private CourseOffer offer1;
	private CourseOffer offer2;
	private CourseOffer offer3;

	@Inject
	private EnrollmentService enrollmentService;

	@Override
	@BeforeEach
	protected void doBeforeTest() { // Partially copied from EnrollmentTest
		super.doBeforeTest();

		student1 = getStudent("student1");
		student2 = getStudent("student2");
		student3 = getStudent("student3");
		lecturer = getLecturer("lecturer");
		PresentationFolder folder = TestDataFactory.getPresentationFolder("Folder", null);
		offer1 = new CourseOffer("Offer1", null);
		folder.addChildCourseOffer(offer1);
		offer2 = new CourseOffer("Offer2", null);
		folder.addChildCourseOffer(offer2);
		offer3 = new CourseOffer("Offer3", null);
		folder.addChildCourseOffer(offer3);

		baseService.persist(folder);
		offer1 = baseService.merge(offer1);
		offer2 = baseService.merge(offer2);
		offer3 = baseService.merge(offer3);
	}

	/**
	 * @return Whether there are less than 5 seconds between the passed Temporal and now.
	 */
	private boolean almostNow(Temporal t) {
		return Duration.between(t, LocalDateTime.now()).abs().getSeconds() < 5;
	}

	/**
	 * Tests persisting, merging and deletion
	 */
	void testEnrollmentLifecycle() {
		Enrollment e = new Enrollment(student1, offer1, EEnrollmentStatus.ON_WAITINGLIST, student1, null);
		enrollmentService.persistEnrollment(e);
		e = enrollmentService.getEnrollment(student1, offer1).orElseThrow(AssertionError::new);

		assertEquals(student1, e.getUser());
		assertEquals(offer1, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ON_WAITINGLIST, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertEquals(student1, e.getLastChangedBy());
		assertNull(e.getExplanation());

		e.updateStatus(EEnrollmentStatus.ENROLLED, null, "Automatically moved up");
		e = enrollmentService.mergeEnrollment(e);
		e = enrollmentService.getEnrollment(student1, offer1).orElseThrow(AssertionError::new);

		assertEquals(student1, e.getUser());
		assertEquals(offer1, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertNull(e.getLastChangedBy());
		assertEquals("Automatically moved up", e.getExplanation());

		e.updateStatus(EEnrollmentStatus.DISENROLLED, lecturer, "Manually removed");
		e = enrollmentService.mergeEnrollment(e);
		e = enrollmentService.getEnrollment(student1, offer1).orElseThrow(AssertionError::new);

		assertEquals(student1, e.getUser());
		assertEquals(offer1, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.DISENROLLED, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertEquals(lecturer, e.getLastChangedBy());
		assertEquals("Manually removed", e.getExplanation());

		enrollmentService.deleteEnrollment(e);
		assertFalse(enrollmentService.getEnrollment(student1, offer1).isPresent());
	}

	@Test
	void testUniqueConstraint1() {
		final Enrollment e1 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e2 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);

		enrollmentService.persistEnrollment(e1);
		// It should be impossible to persist a new enrollment for the same student and course offer
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			enrollmentService.persistEnrollment(e2);
		});
	}

	@Test
	void testUniqueConstraint2() {
		final Enrollment e1 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e2 = new Enrollment(student1, offer1, EEnrollmentStatus.DISENROLLED, lecturer, "Explanation");

		enrollmentService.persistEnrollment(e1);
		// It should be impossible to persist a new enrollment for the same student and course offer
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			enrollmentService.persistEnrollment(e2);
		});
	}

	@Test
	void testEmptyState() {
		assertTrue(enrollmentService.getEnrollments(student1).isEmpty());
		assertTrue(enrollmentService.getEnrollments(offer1).isEmpty());
		assertTrue(enrollmentService.getEnrollments(offer1, EEnrollmentStatus.ENROLLED).isEmpty());
		assertEquals(0, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.DISENROLLED));
		assertFalse(enrollmentService.getEnrollment(student1, offer1).isPresent());
	}

	@Test
	void testEnrollmentsForUser() {
		final Enrollment e1 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e2 = new Enrollment(student1, offer2, EEnrollmentStatus.DISENROLLED, null, null);
		final Enrollment e3 = new Enrollment(student2, offer3, EEnrollmentStatus.ON_WAITINGLIST, null, null);
		enrollmentService.persistEnrollment(e1);
		enrollmentService.persistEnrollment(e2);
		enrollmentService.persistEnrollment(e3);

		// test EnrollmentService.getEnrollments(User)
		assertEqualsEntityListUnordered(Arrays.asList(e1, e2), enrollmentService.getEnrollments(student1));
		assertEqualsEntityListUnordered(Arrays.asList(e3), enrollmentService.getEnrollments(student2));
		assertTrue(enrollmentService.getEnrollments(student3).isEmpty());
	}

	@Test
	void testEnrollmentsForCourseOffer() {
		final Enrollment e1 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e2 = new Enrollment(student2, offer1, EEnrollmentStatus.DISENROLLED, null, null);
		final Enrollment e3 = new Enrollment(student3, offer2, EEnrollmentStatus.ON_WAITINGLIST, null, null);
		enrollmentService.persistEnrollment(e1);
		enrollmentService.persistEnrollment(e2);
		enrollmentService.persistEnrollment(e3);

		// test EnrollmentService.getEnrollments(CourseOffer)
		assertEqualsEntityListUnordered(Arrays.asList(e1, e2), enrollmentService.getEnrollments(offer1));
		assertEqualsEntityListUnordered(Arrays.asList(e3), enrollmentService.getEnrollments(offer2));
		assertTrue(enrollmentService.getEnrollments(offer3).isEmpty());

		// test EnrollmentService.getEnrollments(User,EEnrollmentStatus)
		assertEqualsEntityListUnordered(Arrays.asList(e1),
				enrollmentService.getEnrollments(offer1, EEnrollmentStatus.ENROLLED));
		assertEqualsEntityListUnordered(Arrays.asList(e2),
				enrollmentService.getEnrollments(offer1, EEnrollmentStatus.DISENROLLED));
		assertEqualsEntityListUnordered(Arrays.asList(e3),
				enrollmentService.getEnrollments(offer2, EEnrollmentStatus.ON_WAITINGLIST));
		assertTrue(enrollmentService.getEnrollments(offer1, EEnrollmentStatus.ON_WAITINGLIST).isEmpty());
		assertTrue(enrollmentService.getEnrollments(offer2, EEnrollmentStatus.ENROLLED).isEmpty());
		assertTrue(enrollmentService.getEnrollments(offer3, EEnrollmentStatus.DISENROLLED).isEmpty());

		// test EnrollmentService.countEnrollments(User,EEnrollmentStatus)
		assertEquals(1, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.ENROLLED));
		assertEquals(1, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.DISENROLLED));
		assertEquals(1, enrollmentService.countEnrollments(offer2, EEnrollmentStatus.ON_WAITINGLIST));
		assertEquals(0, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.ON_WAITINGLIST));
		assertEquals(0, enrollmentService.countEnrollments(offer2, EEnrollmentStatus.DISENROLLED));
		assertEquals(0, enrollmentService.countEnrollments(offer3, EEnrollmentStatus.ENROLLED));
	}

	@Test
	void testEnrollmentCount() {
		enrollmentService.persistEnrollment(new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentService.persistEnrollment(new Enrollment(student2, offer1, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentService.persistEnrollment(new Enrollment(student3, offer1, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentService.persistEnrollment(new Enrollment(student1, offer2, EEnrollmentStatus.ENROLLED, null, null));
		enrollmentService.persistEnrollment(new Enrollment(student2, offer2, EEnrollmentStatus.ENROLLED, null, null));

		assertEquals(3, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.ENROLLED));
		assertEquals(0, enrollmentService.countEnrollments(offer1, EEnrollmentStatus.DISENROLLED));
		assertEquals(2, enrollmentService.countEnrollments(offer2, EEnrollmentStatus.ENROLLED));
		assertEquals(0, enrollmentService.countEnrollments(offer2, EEnrollmentStatus.ON_WAITINGLIST));
		assertEquals(0, enrollmentService.countEnrollments(offer3, EEnrollmentStatus.ENROLLED));
		assertEquals(0, enrollmentService.countEnrollments(offer3, EEnrollmentStatus.DISENROLLED));
		assertEquals(0, enrollmentService.countEnrollments(offer3, EEnrollmentStatus.ON_WAITINGLIST));
	}

	@Test
	void testGettingOneEnrollment() {
		final Enrollment e1 = new Enrollment(student1, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e2 = new Enrollment(student2, offer1, EEnrollmentStatus.ENROLLED, null, null);
		final Enrollment e3 = new Enrollment(student1, offer2, EEnrollmentStatus.ENROLLED, null, null);
		enrollmentService.persistEnrollment(e1);
		enrollmentService.persistEnrollment(e2);
		enrollmentService.persistEnrollment(e3);

		assertEquals(e1, enrollmentService.getEnrollment(student1, offer1).orElseThrow(AssertionError::new));
		assertEquals(e2, enrollmentService.getEnrollment(student2, offer1).orElseThrow(AssertionError::new));
		assertEquals(e3, enrollmentService.getEnrollment(student1, offer2).orElseThrow(AssertionError::new));
		assertFalse(enrollmentService.getEnrollment(student2, offer2).isPresent());
	}

}
