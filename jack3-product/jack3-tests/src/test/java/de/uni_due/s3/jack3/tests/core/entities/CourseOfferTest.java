package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.services.ProfileFieldService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Note: Some tests are skipped because not all features are implemented yet.
 */
@NeedsCourse
class CourseOfferTest extends AbstractContentTest {

	@Inject
	private CourseOfferService offerService;

	@Inject
	private ProfileFieldService profileFieldService;

	private CourseOffer offer = new CourseOffer("Course Offer", course);

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		persistPresentationFolder();
		presentationFolder.addChildCourseOffer(offer);
		offerService.persistCourseOffer(offer);

		presentationFolder = folderService.mergePresentationFolder(presentationFolder);
		offer = offerService.getCourseOfferById(offer.getId())
							.orElseThrow(AssertionError::new);
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * General settings
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeName() {
		assertEquals("Course Offer", offer.getName());

		offer.setName("Course Offer 2");
		offer = offerService.mergeCourseOffer(offer);

		assertEquals("Course Offer 2", offer.getName());
	}

	@Test
	void changeCourse() {
		assertEquals(course, offer.getCourse());

		Course newCourse = new Course("Course 2");
		baseService.persist(newCourse);
		offer.setCourse(newCourse);
		offer = offerService.mergeCourseOffer(offer);

		assertNotEquals(course, offer.getCourse());
		assertEquals(newCourse, offer.getCourse());
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Rights for students
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeAllowExerciseRestart() {
		assertTrue(offer.isAllowExerciseRestart());

		offer.setAllowExerciseRestart(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isAllowExerciseRestart());
	}

	@Test
	void changeOnlyOneParticipation() {
		assertFalse(offer.isOnlyOneParticipation());

		offer.setOnlyOneParticipation(true);
		offer = offerService.mergeCourseOffer(offer);

		assertTrue(offer.isOnlyOneParticipation());
	}

	@Test
	void changeAllowHints() {
		assertTrue(offer.isAllowHints());

		offer.setAllowHints(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isAllowHints());
	}

	@Test
	void changeAllowPauses() {
		assertTrue(offer.isAllowPauses());

		offer.setAllowPauses(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isAllowPauses());
	}

	@Test
	void changeAllowStudentComments() {
		assertTrue(offer.isAllowStudentComments());

		offer.setAllowStudentComments(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isAllowStudentComments());
	}

	@Test
	void changeMaxParticipationsPerUser() {
		assertEquals(0, offer.getMaxAllowedParticipants());

		offer.setMaxAllowedParticipants(42);
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(42, offer.getMaxAllowedParticipants());

		offer.setMaxAllowedParticipants(-1);
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			offer = offerService.mergeCourseOffer(offer);
		});
	}

	@Test
	void changeMaxSubmissionsPerExercise() {
		assertEquals(0, offer.getMaxSubmissionsPerExercise());

		offer.setMaxSubmissionsPerExercise(43);
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(43, offer.getMaxSubmissionsPerExercise());

		// TODO Hier wird aktuell noch keine Exception geworfen
		// offer.setMaxSubmissionsPerExercise(-1);
		// expectException(EJBTransactionRolledbackException.class, () -> offer = offerService.mergeCourseOffer(offer));
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Time constraints
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeAccessStartTime() {
		assertNull(offer.getEnrollmentStart());

		offer.setEnrollmentStart(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getEnrollmentStart());
	}

	@Test
	void changeRegistrationEndTime() {
		assertNull(offer.getDisenrollmentDeadline());

		offer.setDisenrollmentDeadline(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getDisenrollmentDeadline());
	}

	@Test
	void changeVisibilityEndTime() {
		assertNull(offer.getVisibilityEndTime());

		offer.setVisibilityEndTime(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getVisibilityEndTime());
	}

	@Test
	void changeProcessingStartTime() {
		assertNull(offer.getSubmissionStart());

		offer.setSubmissionStart(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getSubmissionStart());
	}

	@Test
	void changeAccessEndTime() {
		assertNull(offer.getEnrollmentDeadline());

		offer.setEnrollmentDeadline(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getEnrollmentDeadline());
	}

	@Test
	void changeSubmissionDeadline() {
		assertNull(offer.getSubmissionDeadline());

		offer.setSubmissionDeadline(TestDataFactory.getDateTime());
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(TestDataFactory.getDateTime(), offer.getSubmissionDeadline());
	}

	@Test
	void changeTimeLimit() {
		assertEquals(Duration.ZERO, offer.getTimeLimit());

		offer.setTimeLimit(Duration.ofMinutes(120));
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(Duration.ofMinutes(120), offer.getTimeLimit());
		assertEquals(120, offer.getTimeLimitInMinutes());

		// TODO Was passiert mit negativen Zeitspannen?
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Feedback settings (immediately)
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeShowResultImmediately() {
		assertTrue(offer.isShowResultImmediately());

		offer.setShowResultImmediately(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isShowResultImmediately());
	}

	@Test
	void changeShowFeedbackImmediately() {
		assertTrue(offer.isShowResultImmediately());

		offer.setShowFeedbackImmediately(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isShowFeedbackImmediately());
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Feedback settings (submission review)
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeReviewMode() {
		for (ECourseOfferReviewMode type : ECourseOfferReviewMode.values()) {
			offer.setReviewMode(type);
			offer = offerService.mergeCourseOffer(offer);

			assertEquals(type, offer.getReviewMode());
		}
	}

	@Test
	void changeCourseResultDiaplay() {
		for (ECourseResultDisplay type : ECourseResultDisplay.values()) {
			offer.setCourseResultDisplay(type);
			offer = offerService.mergeCourseOffer(offer);

			assertEquals(type, offer.getCourseResultDisplay());
		}
	}

	@Test
	void changeShowExerciseAndSubmissionInResults() {
		assertTrue(offer.isShowExerciseAndSubmissionInCourseResults());

		offer.setShowExerciseAndSubmissionInCourseResults(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isShowExerciseAndSubmissionInCourseResults());
	}

	@Test
	void changeShowResultInCourseResults() {
		assertTrue(offer.isShowResultInCourseResults());

		offer.setShowResultInCourseResults(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isShowResultInCourseResults());
	}

	@Test
	void changeShowFeedbackInResults() {
		assertTrue(offer.isShowFeedbackInCourseResults());

		offer.setShowFeedbackInCourseResults(false);
		offer = offerService.mergeCourseOffer(offer);

		assertFalse(offer.isShowFeedbackInCourseResults());
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Enrollment settings
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeMaxAllowedParticipants() {
		assertEquals(0, offer.getMaxAllowedParticipants());

		offer.setMaxAllowedParticipants(44);
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(44, offer.getMaxAllowedParticipants());
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Access mode specific stuff
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void changeGlobalPassword() {
		assertNull(offer.getGlobalPassword());

		offer.setGlobalPassword("abcdefg");
		offer = offerService.mergeCourseOffer(offer);

		assertEquals("abcdefg", offer.getGlobalPassword());
	}

	@Test
	void testPersonalPasswords() {
		assertTrue(offer.getPersonalPasswords().isEmpty());

		offer.addPersonalPassword(user, "12345");
		offer = offerService.mergeCourseOffer(offer);

		assertEquals(1, offer.getPersonalPasswords().size());
		assertEquals("12345", offer.getPersonalPasswords().get(user));

		offer.clearAllPersonalPasswords();
		offer = offerService.mergeCourseOffer(offer);
		assertTrue(offer.getPersonalPasswords().isEmpty());
	}

	@Test
	void testStudentFilter() {
		assertTrue(offer.getUserFilter().isEmpty());

		HashSet<String> filter = new HashSet<>();
		for (int i = 1; i < 3; i++) {
			User newUser = TestDataFactory.getUser("UserOnBlocklist" + i);
			userService.persistUser(newUser);
			filter.add(newUser.getLoginName());
		}

		offer.setUserFilter(filter);
		offer = offerService.mergeCourseOffer(offer);

		Collection<String> blocklistFromDB = offer.getUserFilter();
		assertEquals(2, blocklistFromDB.size());

		assertTrue(filter.containsAll(blocklistFromDB));
		assertTrue(blocklistFromDB.containsAll(filter));
	}

	@Test
	void testBlocklistWithProfileFieldFilter() {
		// This tests the error reported in https://s3gitlab.paluno.uni-due.de/JACK/jack3-core/-/issues/856

		// Create an identity profile field and add it to the field filter
		final ProfileField field = profileFieldService.getOrCreateIdentityField("role");
		final Set<ProfileField> fields = new HashSet<>();
		fields.add(field);
		offer.setProfileFieldFilter(fields);

		// Add a blocklisted user to the user filter
		final Set<String> userFilter = new HashSet<>();
		userFilter.add("blocklistedUser");
		offer.setUserFilter(userFilter);

		// Saving the course offer should not throws an exception
		offer = offerService.mergeCourseOffer(offer);

		assertTrue(offer.getProfileFieldFilter().contains(field));
		assertTrue(offer.getUserFilter().contains("blocklistedUser"));
	}

	/*-
	 * ------------------------------------------------------------------------------------------------------
	 * Miscellaneous
	 * ------------------------------------------------------------------------------------------------------
	 */

	@Test
	void getBreadcrumb() {
		Collection<Folder> breadcrumb = offer.getBreadcrumb();
		assertEquals(1, breadcrumb.size());
		assertTrue(breadcrumb.contains(offer.getFolder()));
	}
}
