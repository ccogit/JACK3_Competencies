package de.uni_due.s3.jack3.uitests.student;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertNotPresent;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Student.leaveCourse;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotVisible;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.builders.CourseBuilder;
import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Click;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Find;
import de.uni_due.s3.jack3.uitests.utils.I18nHelper;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.uitests.utils.Misc;
import de.uni_due.s3.jack3.uitests.utils.Student;

/**
 * This test class simulates an exam and tests the following features:
 * <ul>
 * <li>Access mode: personal password</li>
 * <li>Only one submission per exercise</li>
 * <li>No hints</li>
 * <li>Restart disabled</li>
 * <li>Pause disabled</li>
 * <li>No results shown at all</li>
 * <li>A course that does not allow a pause</li>
 * </ul>
 */
class ExamTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Override
	protected void initializeTest() {
		// Clear database, do first time setup
		super.initializeTest();

		// Create users
		User lecturer = userBusiness.createUser("lecturer", "lecturerPassword", "lecturer@foobar.com", false, true);
		User student = userBusiness.createUser("student", "studentPassword", "student@foobar.com", false, false);

		// Generate an exercise with simple stages just for testing
		Exercise exercise = null;
		ArrayList<Exercise> tmpExerciseArrayList = new ArrayList<Exercise>();//Workaround to get the Exercise out of the Scope.
		tmpExerciseArrayList.add(null);
		assertDoesNotThrow(() -> {tmpExerciseArrayList.set(0, exerciseBusiness.createExercise("Aufgabe", lecturer, lecturer.getPersonalFolder(), "de"));});
		exercise = tmpExerciseArrayList.get(0);

		exercise = new ExerciseBuilder(exercise)

				// First MC stage
				.withMCStage().selectOne().withTitle("Stufe 1").allowSkip("Aufgabe übersprungen")
				.withAnswerOption("Korrekt", true).withAnswerOption("Falsch", false)
				.withDefaultFeedback("Ergebnis ist falsch.", 0).withCorrectFeedback("Ergebnis ist richtig.")
				.withHint("Hinweis")
				.and()

				// Second MC stage
				.withMCStage().selectOne().withTitle("Stufe 2").allowSkip("Aufgabe übersprungen")
				.withAnswerOption("Korrekt", true).withAnswerOption("Falsch", false)
				.withDefaultFeedback("Ergebnis ist falsch.", 0).withCorrectFeedback("Ergebnis ist richtig.")
				.withHint("Hinweis")
				.and().create();

		exercise = (Exercise) exerciseBusiness.updateExercise(exercise);

		// Generate a course with this exercise
		Course course = null;
		ArrayList<Course> tmpCourseArrayList = new ArrayList<Course>();//Workaround to get the Course out of the Scope.
		tmpCourseArrayList.add(null);
		assertDoesNotThrow(() -> {tmpCourseArrayList.set(0, courseBusiness.createCourse("Kurs", lecturer, lecturer.getPersonalFolder()));});
		course = tmpCourseArrayList.get(0);

		course = new CourseBuilder(course)
				.withScoringMode(ECourseScoring.LAST)
				.withFixedListExerciseProvider(Arrays.asList(new CourseEntry(exercise, 100)))
				.build();

		course = (Course) courseBusiness.updateCourse(course);

		// Initialize presentation folder
		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote", root);
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());

		// Add the course to an exam course offer
		CourseOffer offer = courseBusiness.createCourseOffer("Kursangebot", course, offerFolder, lecturer);
		offer.setAllowExerciseRestart(false);
		offer.setOnlyOneParticipation(true);
		offer.setAllowHints(false);
		offer.setAllowPauses(false);
		offer.setAllowStudentComments(false);
		offer.setShowResultImmediately(false);
		offer.setShowFeedbackImmediately(false);
		offer.setReviewMode(ECourseOfferReviewMode.NEVER);
		offer.setCourseResultDisplay(ECourseResultDisplay.NONE);
		offer.setShowExerciseAndSubmissionInCourseResults(false);
		offer.setShowResultInCourseResults(false);
		offer.setShowFeedbackInCourseResults(false);
		offer.setExplicitSubmission(true);
		offer.setEnablePersonalPasswords(true);
		offer.addPersonalPassword(student, "personalPassword");
		offer.setAllowStageRestart(false);
		offer.setExplicitEnrollment(false);
		offer.setCanBeVisible(true);
		courseBusiness.updateCourseOffer(offer);
	}

	@Test
	@RunAsClient
	@Order(1)
	void startCourseWithPassword() {
		Misc.login("student", "studentPassword");

		// Open course
		Student.openCourseOffer(0, 0);
		assertFalse(driver.findElement(By.id("globalGrowl_container")).isDisplayed(), "No error message should be shown at the beginning.");

		// Try to start --> starting the course should not be possible because no password was given
		WebElement startButton = Driver.get().findElement(By.id("submissionForm:startAction"));
		Click.clickWithAjax(startButton);
		waitNotVisible(By.id("globalSpinner"));

		assertTrue(driver.findElement(By.id("globalGrowl_container")).isDisplayed(), "The password was empty, but no error message was shown.");

		// Give the wrong password --> starting the course should not be possible
		WebElement passwordField = driver.findElement(By.id("submissionForm:personalPasswordInput"));
		passwordField.sendKeys("asdf");
		Click.clickWithAjax(startButton);
		waitNotVisible(By.id("globalSpinner"));

		assertTrue(driver.findElement(By.id("globalGrowl_container")).isDisplayed(),
				"The password was empty, but no error message was shown.");

		// Give the correct password --> starting the course should be possible!
		passwordField.clear();
		passwordField.sendKeys("personalPassword");
		Click.clickWithRedirect(startButton);
		waitNotVisible(By.id("globalSpinner"));

		// The click should now have resulted in a redirect.
		// Now the student should see the course details
		assertVisible(By.id("showCourseRecordMainForm:menubarWrapperPanel"), "Course details page was not shown correctly.");
	}

	@Test
	@RunAsClient
	@Order(2)
	void testCoursePauseFilter() {
		assumeLogin();

		// We go to main page to test the course pause filter
		navigate(JackUrl.AVAILABLE_COURSES);
		// We should land in the course record page
		assertVisible(By.id("showCourseRecordMainForm:menubarWrapperPanel"),
				"The user was not redirected to the non-pausing course.");
		assertVisible(By.id("showCourseRecordMainForm:redirectInfo"), "The redirection info was not shown.");
	}

	@Test
	@RunAsClient
	@Order(3)
	void submitExercises() {
		assumeLogin();

		Student.openExercise(0);

		final WebElement stage1 = Student.getStages().get(0);

		// The 'hint' button should not (!) be shown --> Exception when we try to find the 'hint' button
		Misc.expectException("Hint button should not be shown, but was shown.",
				NoSuchElementException.class, () -> Find.findButtonByText(stage1, I18nHelper.HINT));

		// "Restart from here" should also not be shown
		Misc.expectException("Hint button should not be shown, but was shown.",
				NoSuchElementException.class, () -> Find.findButtonByText(stage1, I18nHelper.ERASE_SUBMISSION));

		// Select the correct answer and submit
		Student.mcSelectAnswer(stage1, "Korrekt");
		Student.submitAndWait(stage1);

		// Result and feedback should not (!) be shown
		assertFalse(stage1.getText().contains("Punkte"), "Result points should not be shown, but was shown.");
		assertFalse(stage1.getText().contains("Ergebnis ist"), "Feedback should not be shown, but was shown.");

		// Now the student should see the second stage

		final WebElement stage2 = Student.getStages().get(1);

		// The 'hint' button should not (!) be shown --> Exception when we try to find the 'hint' button
		Misc.expectException("Hint button should not be shown, but was shown.",
				NoSuchElementException.class, () -> Find.findButtonByText(stage2, I18nHelper.HINT));
		// "Restart from here" should also not be shown
		Misc.expectException("Hint button should not be shown, but was shown.",
				NoSuchElementException.class, () -> Find.findButtonByText(stage1, I18nHelper.ERASE_SUBMISSION));

		// Select the correct answer and submit
		Student.mcSelectAnswer(stage2, "Falsch");
		Student.submitAndWait(stage2);

		// Result and feedback should not (!) be shown
		assertFalse(stage2.getText().contains("Punkte"), "Result points should not be shown, but was shown.");
		assertFalse(stage2.getText().contains("Ergebnis ist"), "Feedback should not be shown, but was shown.");

		// Student leaves the course
		leaveCourse();
	}

	@Test
	@RunAsClient
	@Order(4)
	void checkPostconditions() {
		assumeLogin();

		waitVisible(By.id("submissionForm:submissionPanel"));
		// The student should see the course main menu again, but there is no 'start' button because
		// restarting is not allowed.
		WebElement submissionPanel = driver.findElement(By.id("submissionForm:submissionPanel"));
		try {
			// We assert both languages here because sometimes in the tests the wrong locale is selected by the testing
			// browser. This is the case if the language the user has chosen in the UI is different from the browser
			// language.
			assertTrue(submissionPanel.getText().contains("eine weitere Teilnahme ist nicht erlaubt")); // NOSONAR
		} catch (AssertionError e) {
			assertTrue(submissionPanel.getText().contains("further participation is not allowed"));
		}

		// There should be no buttons on this page.
		List<WebElement> submissionButtons = submissionPanel.findElements(By.tagName("button"));
		// No buttons should be shown!
		if (!submissionButtons.isEmpty()) {
			throw new AssertionError("The following buttons were shown: " + submissionButtons.stream()
					.map(element -> element.getAttribute("id")).collect(Collectors.joining(", ")));
		}

		// The student should not see the table with closed earlier participations because review is disabled
		assertNotPresent(By.id("reviewForm:reviewTable"), "Course record table should not be shown.");
	}

	@Test
	@Order(5)
	void checkDatabase() {
		// Get the course record
		List<CourseRecord> records = courseBusiness.getAllCourseRecords(courseBusiness.getAllCourseOffers().get(0));
		assumeFalse(records.isEmpty(), "No course record was present.");
		assertTrue(records.get(0).isManuallyClosed());
		assertEquals(50, records.get(0).getResultPoints());
		// TODO add more assertions
	}

}
