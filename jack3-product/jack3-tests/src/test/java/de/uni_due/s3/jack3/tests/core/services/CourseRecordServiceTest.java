package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsCourse
class CourseRecordServiceTest extends AbstractContentTest {

	@Inject
	private CourseOfferService offerService;

	@Inject
	private CourseRecordService recordService;

	private CourseOffer offer = new CourseOffer("Course Offer", course);

	/**
	 *
	 * @param number
	 *            the number of records to create
	 */
	private List<CourseRecord> createRecords(int number) {
		offerService.persistCourseOffer(offer);

		List<CourseRecord> records = new ArrayList<>(number);
		for (int i = 0; i < number; i++) {
			CourseRecord tmp = new CourseRecord(user, offer, course);
			records.add(tmp);
			recordService.persistCourseRecord(tmp);
		}

		return records;
	}

	private void setStartTimeViaReflection(CourseRecord cr, LocalDateTime ldt) throws Exception {
		Field field = CourseRecord.class.getDeclaredField("startTime");
		field.setAccessible(true);
		field.set(cr, ldt);
	}

	private void setEndTimeViaReflection(CourseRecord cr, LocalDateTime ldt) throws Exception {
		Field field = CourseRecord.class.getDeclaredField("endTime");
		field.setAccessible(true);
		field.set(cr, ldt);
	}

	private void setDeadlineViaReflection(CourseRecord cr, LocalDateTime ldt) throws Exception {
		Field field = CourseRecord.class.getDeclaredField("individualDeadline");
		field.setAccessible(true);
		field.set(cr, ldt);
	}

	@SuppressWarnings("unused") // For completeness
	private void setExitedViaReflection(CourseRecord cr, boolean exited) throws Exception {
		Field field = CourseRecord.class.getDeclaredField("manuallyExited");
		field.setAccessible(true);
		field.set(cr, exited);
		field = CourseRecord.class.getDeclaredField("automaticallyExited");
		field.setAccessible(true);
		field.set(cr, exited);
	}

	/**
	 * Tests getting empty list of course records
	 */
	@Test
	void getEmptyRecordList() {
		assertTrue(recordService.getAllCourseRecordsForCourse(course).isEmpty());
	}

	/**
	 * Tests if records are found by course
	 */
	@Test
	void getRecordsByCourse() {

		Collection<CourseRecord> records = createRecords(2);
		Collection<CourseRecord> getRecordsFromDB = recordService.getAllCourseRecordsForCourse(course);

		// lists should contain the same items
		assertTrue(getRecordsFromDB.containsAll(records));
		assertTrue(records.containsAll(getRecordsFromDB));
	}

	/**
	 * Tests if records which are found by course are ordered by startTime
	 *
	 * (Uses Reflection)
	 */
	@Test
	void getAllCourseRecordsForCourseOrderedByStarttime() throws Exception {
		List<CourseRecord> records = createRecords(3);

		// Set startTime of the records via Reflection to an invervall of 15 seconds
		// so they can be ordered correctly
		Field startTimeField = CourseRecord.class.getDeclaredField("startTime");
		startTimeField.setAccessible(true);
		startTimeField.set(records.get(0), LocalDateTime.now().minusSeconds(45));
		startTimeField.set(records.get(1), LocalDateTime.now().minusSeconds(30));
		startTimeField.set(records.get(2), LocalDateTime.now().minusSeconds(15));

		for (CourseRecord courseRecord : records) {
			recordService.mergeCourseRecord(courseRecord);
		}

		List<CourseRecord> getRecordsFromDB = recordService.getAllCourseRecordsForCourse(course);

		for (int i = 1; i < getRecordsFromDB.size(); i++) {
			assertTrue(
					getRecordsFromDB.get(i).getStartTime().isBefore(getRecordsFromDB.get(i - 1).getStartTime()));
		}
	}

	/**
	 * Tests if records which are found by course offer are ordered by startTime
	 *
	 * (Uses Reflection)
	 */
	@Test
	void getAllCourseRecordsForCourseOfferOrderedByStarttime() throws Exception {
		List<CourseRecord> records = createRecords(3);

		// Set startTime of the records via Reflection to an invervall of 15 seconds
		// so they can be ordered correctly
		Field startTimeField = CourseRecord.class.getDeclaredField("startTime");
		startTimeField.setAccessible(true);
		startTimeField.set(records.get(0), LocalDateTime.now().minusSeconds(45));
		startTimeField.set(records.get(1), LocalDateTime.now().minusSeconds(30));
		startTimeField.set(records.get(2), LocalDateTime.now().minusSeconds(15));

		for (CourseRecord courseRecord : records) {
			recordService.mergeCourseRecord(courseRecord);
		}

		List<CourseRecord> getRecordsFromDB = recordService
				.getAllCourseRecordsForCourseOfferOrderedByStarttime(offer);

		for (int i = 1; i < getRecordsFromDB.size(); i++) {
			assertTrue(
					getRecordsFromDB.get(i).getStartTime().isBefore(getRecordsFromDB.get(i - 1).getStartTime()));
		}
	}

	/**
	 * Remove all records for course
	 */
	@Test
	void removeAllCourseRecordsForCourse() {

		createRecords(2);
		recordService.removeAllCourseRecordsForCourse(course);

		// no records should be found
		assertTrue(recordService.getAllCourseRecordsForCourse(course).isEmpty());
	}

	/**
	 * Tests if records are found by Id
	 */
	@Test
	void getRecordById() {

		offerService.persistCourseOffer(offer);
		CourseRecord record = new CourseRecord(user, offer, course);
		recordService.persistCourseRecord(record);

		assertEquals(record, recordService.getCourseRecordById(record.getId())
													.orElseThrow(AssertionError::new));
	}

	/**
	 * Tests {@linkplain CourseRecordService#getOpenCourseRecordFor(User, CourseOffer)}
	 */
	@Test
	void getOpenCourseRecordForUser() throws Exception {
		offerService.persistCourseOffer(offer);

		// User has no course record at all
		assertFalse(recordService.getOpenCourseRecordFor(user, offer).isPresent());

		CourseRecord record = new CourseRecord(user, offer, course);
		setStartTimeViaReflection(record, LocalDateTime.now().minusMinutes(1));
		recordService.persistCourseRecord(record);

		// User has an open course record
		assertTrue(recordService.getOpenCourseRecordFor(user, offer).isPresent());
		assertEquals(record, recordService.getOpenCourseRecordFor(user, offer).get());

		// User has a manually closed course record
		record.closeManually();
		record = recordService.mergeCourseRecord(record);
		assertFalse(recordService.getOpenCourseRecordFor(user, offer).isPresent());

		// User starts the course again
		record = new CourseRecord(user, offer, course);
		setStartTimeViaReflection(record, LocalDateTime.now().minusMinutes(1));
		recordService.persistCourseRecord(record);

		// User has an open course record
		assertTrue(recordService.getOpenCourseRecordFor(user, offer).isPresent());
		assertEquals(record, recordService.getOpenCourseRecordFor(user, offer).get());

		// Close course record via deadline
		setDeadlineViaReflection(record, LocalDateTime.now().minusMinutes(1));
		record = recordService.mergeCourseRecord(record);

		// User should not have an open course record
		assertFalse(recordService.getOpenCourseRecordFor(user, offer).isPresent());
	}

}
