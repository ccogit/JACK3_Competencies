package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class EnrollmentTest extends AbstractBasicTest {

	private User lecturer;
	private User student;
	private CourseOffer offer;

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();

		student = getStudent("student");
		lecturer = getLecturer("lecturer");
		PresentationFolder folder = TestDataFactory.getPresentationFolder("Folder", null);
		offer = new CourseOffer("Offer", null);
		folder.addChildCourseOffer(offer);

		baseService.persist(folder);
		offer = baseService.merge(offer);
	}

	/**
	 * @return Whether there are less than 5 seconds between the Temporals.
	 */
	private boolean almostEquals(Temporal t1, Temporal t2) {
		return Duration.between(t1, t2).abs().getSeconds() < 5;
	}

	/**
	 * @return Whether there are less than 5 seconds between the passed Temporal and now.
	 */
	private boolean almostNow(Temporal t) {
		return Duration.between(t, LocalDateTime.now()).abs().getSeconds() < 5;
	}

	@Test
	void testConstructor1() {
		// Simple enrollment, no changed-by user and no explanation
		final Enrollment e = new Enrollment(student, offer, EEnrollmentStatus.ENROLLED, null, null);
		assertEquals(student, e.getUser());
		assertEquals(offer, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertNull(e.getLastChangedBy());
		assertNull(e.getExplanation());
	}

	@Test
	void testConstructor2() {
		// Simple enrollment with changed-by user
		final Enrollment e = new Enrollment(student, offer, EEnrollmentStatus.ENROLLED, student, null);
		assertEquals(student, e.getUser());
		assertEquals(offer, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertEquals(student, e.getLastChangedBy());
		assertNull(e.getExplanation());
	}

	@Test
	void testConstructor3() {
		// Simple enrollment with changed-by user and explanation
		final Enrollment e = new Enrollment(student, offer, EEnrollmentStatus.ENROLLED, lecturer,
				"You are registered now!");
		assertEquals(student, e.getUser());
		assertEquals(offer, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertTrue(almostNow(e.getLastChange()));
		assertEquals(lecturer, e.getLastChangedBy());
		assertEquals("You are registered now!", e.getExplanation());
	}

	@Test
	void testConstructorInvalidParams1() {
		assertThrows(NullPointerException.class, () -> {
			new Enrollment(null, offer, EEnrollmentStatus.ENROLLED, null, null);
		});
	}

	@Test
	void testConstructorInvalidParams2() {
		assertThrows(NullPointerException.class, () -> {
			new Enrollment(student, null, EEnrollmentStatus.ENROLLED, null, null);
		});
	}

	@Test
	void testConstructorInvalidParams3() {
		assertThrows(NullPointerException.class, () -> {
			new Enrollment(student, offer, null, null, null);
		});
	}

	@Test
	void testStatusUpdate1() {
		// Simple enrollment, no changed-by user and no explanation
		final Enrollment e = new Enrollment(student, offer, EEnrollmentStatus.ENROLLED, student, "I register you.");
		final LocalDateTime oldLastChange = e.getLastChange();
		e.updateStatus(EEnrollmentStatus.DISENROLLED, null, null);

		// Check if values have changed
		assertNotEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertNotEquals(student, e.getLastChangedBy());
		assertNotEquals("I register you.", e.getExplanation());

		// Check if new values are correct
		assertEquals(student, e.getUser());
		assertEquals(offer, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.DISENROLLED, e.getStatus());
		assertTrue(almostEquals(e.getLastChange(), oldLastChange));
		assertNull(e.getLastChangedBy());
		assertNull(e.getExplanation());
	}

	@Test
	void testStatusUpdate2() {
		// Simple enrollment, no changed-by user and no explanation
		final Enrollment e = new Enrollment(student, offer, EEnrollmentStatus.ENROLLED, null, null);
		final LocalDateTime oldLastChange = e.getLastChange();
		e.updateStatus(EEnrollmentStatus.ON_WAITINGLIST, student, "I wait now.");

		// Check if values have changed
		assertNotEquals(EEnrollmentStatus.ENROLLED, e.getStatus());
		assertNotNull(e.getLastChangedBy());
		assertNotNull(e.getExplanation());

		// Check if new values are correct
		assertEquals(student, e.getUser());
		assertEquals(offer, e.getCourseOffer());
		assertEquals(EEnrollmentStatus.ON_WAITINGLIST, e.getStatus());
		assertTrue(almostEquals(e.getLastChange(), oldLastChange));
		assertEquals(student, e.getLastChangedBy());
		assertEquals("I wait now.", e.getExplanation());
	}

}
