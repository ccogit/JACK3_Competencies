package de.uni_due.s3.jack3.tests.core.entities;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsCourse
class CourseRecordTest extends AbstractContentTest {

	@Inject
	private CourseRecordService recordService;

	@Inject
	private RevisionService revisionService;

	private CourseRecord record;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		if (record == null) {
			record = new CourseRecord(user, course);
		}

		recordService.persistCourseRecord(record);
		record = recordService	.getCourseRecordById(record.getId())
								.orElseThrow(AssertionError::new);


	}

	/**
	 * Create course record for a course with a time limit
	 * 
	 * @param timeLimit
	 *            Time limit in minutes
	 */
	private void persistRecord(long timeLimit) throws Exception {
		course = baseService.merge(course);

		record = new CourseRecord(user, course);

		if (timeLimit != 0) {
			// Set deadline of submission via Reflection to now plus timeLimit
			Field deadlineField = CourseRecord.class.getDeclaredField("individualDeadline");
			deadlineField.setAccessible(true);

			if (timeLimit > 0) {
				deadlineField.set(record, LocalDateTime.now().plusMinutes(timeLimit));
			} else if (timeLimit < 0) {
				deadlineField.set(record, LocalDateTime.now().minusMinutes(timeLimit));
			}
		}

		recordService.persistCourseRecord(record);
		record = recordService	.getCourseRecordById(record.getId())
								.orElseThrow(AssertionError::new);
	}

	@Disabled("Currently without function. Comments are implemented at Submission.")
	@Test
	void addComment() {
		assertTrue(record.getStudentComments().isEmpty());

		Comment comment = new Comment(user, "Comment", false);
		record.addStudentComment(comment);
		record = recordService.mergeCourseRecord(record);

		assertEquals(1, record.getStudentComments().size());
	}

	@Test
	void changeLastVisit() {
		assertNull(record.getLastVisit());

		record.setLastVisit(TestDataFactory.getDateTime());
		record = recordService.mergeCourseRecord(record);

		assertEquals(TestDataFactory.getDateTime(), record.getLastVisit());
	}

	@Test
	void changeResultPoints() {
		assertEquals(0, record.getResultPoints());

		record.setResultPoints(100);
		record = recordService.mergeCourseRecord(record);

		assertEquals(100, record.getResultPoints());
	}

	/**
	 * Test if the user can exit the record manually
	 */
	@Test
	void exitManually() {
		assertFalse(record.isClosed());

		record.closeManually();
		record = recordService.mergeCourseRecord(record);

		assertTrue(record.isClosed());
		assertTrue(record.isManuallyClosed());
	}

	/**
	 * Test if the record is exited if deadline is up
	 *
	 * (Uses Reflection)
	 */
	@Test
	void exitFromDeadline() throws Exception {
		// Persist a course record with a deadline in the past (now minus 10 minutes)
		persistRecord(-10);

		// Set startTime of course record via Reflection to now minus 30 minutes
		// so the course record should be exited by deadline
		Field startTimeField = CourseRecord.class.getDeclaredField("individualDeadline");
		startTimeField.setAccessible(true);
		startTimeField.set(record, LocalDateTime.now().minusMinutes(30));

		assertFalse(record.isManuallyClosed());
		assertFalse(record.isAutomaticallyClosed());
		assertTrue(record.isClosed());
	}

	@Test
	void exitManuallyWithDeadline() throws Exception {
		// Persist a course record with a deadline in the future
		persistRecord(30);
		assertFalse(record.isClosed());
		assertFalse(record.isManuallyClosed());

		// Exit manually
		record.closeManually();
		record = recordService.mergeCourseRecord(record);

		assertTrue(record.isClosed());
		assertTrue(record.isManuallyClosed());
		assertFalse(record.isAutomaticallyClosed());
		assertTrue(record.getEndTime().isBefore(record.getDeadline()));
	}

	@Test
	void getCourse() {
		assertEquals(course, record.getCourse());
		assertEquals(revisionService.getProxiedOrLastPersistedRevisionId(course), record.getCourseRevisionId());
	}

	@Test
	void getUser() {
		assertEquals(user, record.getUser());
	}

}
