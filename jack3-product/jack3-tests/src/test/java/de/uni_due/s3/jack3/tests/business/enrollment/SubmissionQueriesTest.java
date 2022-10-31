package de.uni_due.s3.jack3.tests.business.enrollment;

import static de.uni_due.s3.jack3.tests.utils.Assert.assertEqualsEntityListUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;

class SubmissionQueriesTest extends AbstractEnrollmentBusinessTest {

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();

		offer.setCourse(getCourse("Course"));
		offer = baseService.merge(offer);
	}

	@Test
	void hasAlreadyParticipated() throws Exception {
		final User student = getStudent("stud");
		assertFalse(enrollmentBusiness.hasAlreadyParticipated(student, offer));

		enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.hasAlreadyParticipated(student, offer));
	}

	@Test
	void oneOpenCourseRecord() throws Exception {
		final User student = getStudent("stud");
		assertFalse(enrollmentBusiness.getOpenCourseRecord(student, offer).isPresent());

		final CourseRecord expected = enrollmentBusiness.startSubmission(student, offer);
		final Optional<CourseRecord> actual = enrollmentBusiness.getOpenCourseRecord(student, offer);
		assertTrue(actual.isPresent());
		assertEquals(expected, actual.get());
	}

	@Test
	void openCourseRecordsForUser() throws Exception {
		final User student = getStudent("stud");
		assertTrue(enrollmentBusiness.getOpenCourseRecords(student).isEmpty());

		final CourseRecord expected = enrollmentBusiness.startSubmission(student, offer);
		assertEqualsEntityListUnordered(Arrays.asList(expected), enrollmentBusiness.getOpenCourseRecords(student));
	}

	@Test
	void visibleCourseRecords() throws Exception {
		final User student = getStudent("stud");
		assertTrue(enrollmentBusiness.getOpenCourseRecords(student).isEmpty());

		final CourseRecord expected = enrollmentBusiness.startSubmission(student, offer);
		assertTrue(enrollmentBusiness.getVisibleOldCourseRecords(student, offer).isEmpty());

		expected.closeManually();
		baseService.merge(expected);

		// Only visible after ending
		assertEqualsEntityListUnordered(Arrays.asList(expected),
				enrollmentBusiness.getVisibleOldCourseRecords(student, offer));
	}

}
