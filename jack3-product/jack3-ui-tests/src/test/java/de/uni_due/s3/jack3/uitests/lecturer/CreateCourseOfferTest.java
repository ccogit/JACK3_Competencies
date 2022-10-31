package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Misc;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseOfferEditPage;

class CreateCourseOfferTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);
		userBusiness.createUser("student", "secret", "student@foobar.com", false, false);

		// Add a course
		assertDoesNotThrow(() -> {courseBusiness.createCourse("Kurs", lecturer, lecturer.getPersonalFolder());});

		// Initialize presentation folder
		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote", root);
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());
	}

	@Test
	@Order(1)
	@RunAsClient
	void createCourseOffer() { // NOSONAR no assertions here
		login("lecturer", "secret");
		AvailableCoursesPage.navigateToPage();

		// Create CourseOffer
		AvailableCoursesPage.createCourseOffer("Kursangebote", "Mein Kursangebot");

		// Enter the CourseOffer
		AvailableCoursesPage.openCourseOffer("Mein Kursangebot");
	}

	@Test
	@Order(2)
	void verifyDefaultSettings() {
		CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		assertNotNull(courseOffer);
		assertNull(courseOffer.getCourse());
		assertFalse(courseOffer.isCanBeVisible());

		// Submission settings
		assertTrue(courseOffer.isAllowStageRestart());
		assertTrue(courseOffer.isAllowHints());
		assertTrue(courseOffer.isAllowPauses());
		assertTrue(courseOffer.isAllowStudentComments());
		assertTrue(courseOffer.isAllowExerciseRestart());
		assertEquals(0, courseOffer.getMaxSubmissionsPerExercise());

		// Explicit submission settings
		assertFalse(courseOffer.isExplicitSubmission());
		assertFalse(courseOffer.isOnlyOneParticipation());
		assertEquals(0, courseOffer.getTimeLimitInMinutes());
		assertFalse(courseOffer.isEnablePersonalPasswords());

		// Explicit enrollment settings
		assertTrue(courseOffer.isExplicitEnrollment());
		assertNull(courseOffer.getGlobalPassword());
		assertFalse(courseOffer.isEnableWaitingList());
		assertFalse(courseOffer.getFolder().isContainsLinkedCourses());

		// Open course record settings
		assertTrue(courseOffer.isShowDifficulty());
		assertTrue(courseOffer.isShowResultImmediately());
		assertTrue(courseOffer.isShowFeedbackImmediately());

		// Review settings
		assertEquals(ECourseResultDisplay.BOTH, courseOffer.getCourseResultDisplay());
		assertEquals(ECourseOfferReviewMode.ALWAYS, courseOffer.getReviewMode());
		assertTrue(courseOffer.isShowExerciseAndSubmissionInCourseResults());
		assertTrue(courseOffer.isShowResultInCourseResults());
		assertTrue(courseOffer.isShowFeedbackInCourseResults());
	}

	@Test
	@Order(3)
	@RunAsClient
	void changeSettings() {
		assumeLogin();

		// Change name of the courseOffer
		assertEquals("Mein Kursangebot", CourseOfferEditPage.getNameOfCourseOffer());
		CourseOfferEditPage.setNameOfCourseOffer("Mein tolles Kursangebot");

		// Choose Course
		assertEquals("Kein Kurs", CourseOfferEditPage.getNameOfSelectedCourse());
		CourseOfferEditPage.setCourse("Kurs");
		assertEquals("Kurs", CourseOfferEditPage.getNameOfSelectedCourse());

		// Disable all submission settings
		CourseOfferEditPage.setAllowStageRestart(false);
		CourseOfferEditPage.setAllowHints(false);
		CourseOfferEditPage.setAllowPause(false);
		CourseOfferEditPage.setAllowStudentsComments(false);

		// Change the repetition limit
		CourseOfferEditPage.limitExericseRepetitions(10);

		// driver.findElement(By.id("courseOfferEdit:solvingExerciseAgain")).click();

		// Enable explicit submission settings -> Submission panel should be visible
		CourseOfferEditPage.setExplicitSubmissionRequired(true);
		CourseOfferEditPage.setOnlyAllowAllowSingularParticipation(true);
		CourseOfferEditPage.setTimeLimit(15);
		CourseOfferEditPage.setHasPersonalPasswords(true);

		// Disable explicit enrollment settings -> Enrollment panel should not be visible
		CourseOfferEditPage.setExplicitEnrollment(false);
		
		// Enable explicit enrollment settings -> Enrollment panel should be visible
		CourseOfferEditPage.setExplicitEnrollment(true);
		CourseOfferEditPage.setGlobalPassword("globalPassword");
		CourseOfferEditPage.setMaxAllowedParticpants(25);
		CourseOfferEditPage.setHasWaitingList(true);

		// Display settings in open course record
		CourseOfferEditPage.setShowDifficulty(false);
		CourseOfferEditPage.setShowResultImmediately(false);
		CourseOfferEditPage.setShowFeedbackImmediately(false);

		// Display settings in closed course records
		CourseOfferEditPage.setCourseResultDisplay(ECourseResultDisplay.NONE);
		CourseOfferEditPage.setReviewMode(ECourseOfferReviewMode.AFTER_EXIT);
		CourseOfferEditPage.setShowFeedbackInCourseResults(false);
		CourseOfferEditPage.setShowResultInCourseResults(false);
		CourseOfferEditPage.setShowExerciseAndSubmissionInCourseResults(false);

		CourseOfferEditPage.saveCourseOffer();
	}

	@Test
	@Order(4)
	void verifyChangedSettings() {
		CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		assumeTrue(Objects.nonNull(courseOffer));

		// Submission settings
		assertFalse(courseOffer.isAllowStageRestart());
		assertFalse(courseOffer.isAllowHints());
		assertFalse(courseOffer.isAllowPauses());
		assertFalse(courseOffer.isAllowStudentComments());
		assertTrue(courseOffer.isAllowExerciseRestart());
		assertEquals(11, courseOffer.getMaxSubmissionsPerExercise()); // max. 10 repetitions

		// Explicit submission settings
		assertTrue(courseOffer.isExplicitSubmission());
		assertTrue(courseOffer.isOnlyOneParticipation());
		assertEquals(15, courseOffer.getTimeLimitInMinutes());
		assertTrue(courseOffer.isEnablePersonalPasswords());

		// Explicit enrollment settings
		assertTrue(courseOffer.isExplicitEnrollment());
		assertEquals("globalPassword", courseOffer.getGlobalPassword());
		assertEquals(25, courseOffer.getMaxAllowedParticipants());
		assertTrue(courseOffer.isEnableWaitingList());

		// Open course record settings
		assertFalse(courseOffer.isShowDifficulty());
		assertFalse(courseOffer.isShowResultImmediately());
		assertFalse(courseOffer.isShowFeedbackImmediately());

		// Review settings
		assertEquals(ECourseResultDisplay.NONE, courseOffer.getCourseResultDisplay());
		assertEquals(ECourseOfferReviewMode.AFTER_EXIT, courseOffer.getReviewMode());
		assertFalse(courseOffer.isShowExerciseAndSubmissionInCourseResults());
		assertFalse(courseOffer.isShowResultInCourseResults());
		assertFalse(courseOffer.isShowFeedbackInCourseResults());
	}

	@Test
	@Order(5)
	@RunAsClient
	void changeTimeSettings() { // NOSONAR
		assumeLogin();

		LocalDateTime[] dates = {
				LocalDateTime.of(2020, 12, 25, 11, 0), // 0 - Visibility start "25.12.2020 11:00"
				LocalDateTime.of(2020, 12, 25, 12, 0), // 1 - Enrollment start "25.12.2020 12:00"
				LocalDateTime.of(2020, 12, 25, 13, 0), // 2 - Enrollment deadline "25.12.2020 13:00"
				LocalDateTime.of(2020, 12, 25, 14, 0), // 3 - Disenrollment deadline "25.12.2020 14:00"
				LocalDateTime.of(2020, 12, 25, 15, 0), // 4 - Submission start "25.12.2020 15:00"
				LocalDateTime.of(2020, 12, 25, 16, 0), // 5 - Submission deadline "25.12.2020 16:00"
				LocalDateTime.of(2020, 12, 25, 17, 0), // 6 - Visibility end "25.12.2020 17:00"
		};

		// make the CourseOffer visible according to the access restrictions.
		CourseOfferEditPage.setVisibilityOfCourseOffer(true);

		// we have to do this to avoid a strange selenium bug. 
		CourseOfferEditPage.saveCourseOffer();
		Misc.reloadPage();

		// Visibility start
		CourseOfferEditPage.setVisibilityStart(dates[0]);
		
		// Enrollment start
		CourseOfferEditPage.setEnrollmentStart(dates[1]);

		// Enrollment deadline
		CourseOfferEditPage.setEnrollmentDeadline(dates[2]);

		// Disenrollment deadline
		CourseOfferEditPage.setDisenrollmentDeadline(dates[3]);

		// Submission start
		CourseOfferEditPage.setSubmissionStart(dates[4]);

		// Submission deadline
		CourseOfferEditPage.setSubmissionDeadline(dates[5]);

		// Visibility end
		CourseOfferEditPage.setVisibilityEnd(dates[6]);

		CourseOfferEditPage.saveCourseOffer();
	}

	@Test
	@Order(6)
	void verifyChangedTimeSettings() {
		CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		assumeTrue(Objects.nonNull(courseOffer));

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
		assertNotNull(courseOffer.getVisibilityStartTime());
		assertNotNull(courseOffer.getEnrollmentStart());
		assertNotNull(courseOffer.getEnrollmentDeadline());
		assertNotNull(courseOffer.getDisenrollmentDeadline());
		assertNotNull(courseOffer.getSubmissionStart());
		assertNotNull(courseOffer.getSubmissionDeadline());
		assertNotNull(courseOffer.getVisibilityEndTime());

		assertEquals("25.12.2020 11:00", formatter.format(courseOffer.getVisibilityStartTime()));
		assertEquals("25.12.2020 12:00", formatter.format(courseOffer.getEnrollmentStart()));
		assertEquals("25.12.2020 13:00", formatter.format(courseOffer.getEnrollmentDeadline()));
		assertEquals("25.12.2020 14:00", formatter.format(courseOffer.getDisenrollmentDeadline()));
		assertEquals("25.12.2020 15:00", formatter.format(courseOffer.getSubmissionStart()));
		assertEquals("25.12.2020 16:00", formatter.format(courseOffer.getSubmissionDeadline()));
		assertEquals("25.12.2020 17:00", formatter.format(courseOffer.getVisibilityEndTime()));
	}

}
