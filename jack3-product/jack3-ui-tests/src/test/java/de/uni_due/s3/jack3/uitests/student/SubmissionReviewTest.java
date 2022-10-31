package de.uni_due.s3.jack3.uitests.student;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithRedirect;
import static de.uni_due.s3.jack3.uitests.utils.Find.ALL;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTag;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.formatDate;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Student.fillInputField;
import static de.uni_due.s3.jack3.uitests.utils.Student.getStages;
import static de.uni_due.s3.jack3.uitests.utils.Student.getSubmissionStages;
import static de.uni_due.s3.jack3.uitests.utils.Student.leaveCourse;
import static de.uni_due.s3.jack3.uitests.utils.Student.mcSelectAnswer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openCourseOffer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openExercise;
import static de.uni_due.s3.jack3.uitests.utils.Student.submitAndWait;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitPresent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * This test first simulates a student submit and then tests the submission view.
 */
class SubmissionReviewTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private DevelopmentBusiness developmentBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();

		// Initialize users
		final User lecturer = userBusiness.createUser("lecturer", "secret", null, false, true);
		userBusiness.createUser("student", "secret", null, false, false);

		// Initialize folders
		final PresentationFolder preRoot = folderBusiness.getPresentationRoot();
		final PresentationFolder preFolder = folderBusiness.createPresentationFolder("Kurse", preRoot);
		folderBusiness.updateFolderRightsForUser(preFolder, lecturer, AccessRight.getFull());
		final ContentFolder conFolder = lecturer.getPersonalFolder();

		// Initialize exercise "JACK"
		final AbstractExercise exercise = developmentBusiness.createSampleExerciseJACK(lecturer);

		// Initialize course
		Course course = null;
		ArrayList<Course> tmpArrayList = new ArrayList<Course>();//Workaround to get the Course out of the Scope.
		tmpArrayList.add(null);
		assertDoesNotThrow(() -> {tmpArrayList.set(0, courseBusiness.createCourse("Kurs", lecturer, conFolder));});
		course = tmpArrayList.get(0);

		FixedListExerciseProvider fep = new FixedListExerciseProvider();
		fep.addCourseEntry(new CourseEntry(exercise, 100));
		course.setContentProvider(fep);
		course.setScoringMode(ECourseScoring.BEST);
		course = (Course) courseBusiness.updateCourse(course);

		// Initialize course offer
		CourseOffer offer = courseBusiness.createCourseOffer("Kursangebot", course, preFolder, lecturer);
		offer.setExplicitEnrollment(false);
		offer.setCanBeVisible(true);
		courseBusiness.updateCourseOffer(offer);
	}

	/**
	 * Tests the exercise "JACK"
	 */
	@RunAsClient
	@Order(1)
	@Test
	void submitExercise() { // Copied from SubmitSingleExerciseTest#submitSampleExercise1
		login("student", "secret");
		assumeLogin();
		openCourseOffer(0, 0);
		openExercise(0);

		// ----- Stage 1 ("Was ist JACK?") -----
		assertEquals(1, getStages().size());
		WebElement currentStage = getStages().get(0);
		mcSelectAnswer(currentStage, "Ein E-Assessment-System");
		submitAndWait(currentStage);

		// ----- Test navigation via the tab "My Courses" -----

		navigate(JackUrl.MY_PARTICIPATIONS);
		// There should be a panel with the current course record
		assertVisible(By.id("myParticipationsForm:courseRecordGrid:0:courseRecord"),
				"Course Record was not visible in 'My Courses' page.");
		clickWithRedirect(By.id("myParticipationsForm:courseRecordGrid:0:dgLinkShowCourseRecord"));
		// Now we should be in the submission again
		assertVisible(By.id("showCourseRecordMainForm:exercise_content"),
				"Exercise content was not shown after resuming the current submission via the 'My Courses' page");

		// ----- Stage 2 ("Berechnen Sie ...") -----
		assertEquals(2, getStages().size());
		currentStage = getStages().get(1);
		final String[] numbers =
				findChildElementByTag(currentStage, "label", elem -> elem.getText().contains("Berechnen Sie"))
				.getText()
				.replace("Berechnen Sie ", "")
				.replace(" =", "")
				.split(" \\+ ");
		final int result = Integer.parseInt(numbers[0]) + Integer.parseInt(numbers[1]);
		fillInputField(currentStage, 0, Integer.toString(result));
		submitAndWait(currentStage);

		// Finish course
		assertTrue(find(By.id("showCourseRecordMainForm:exercise_options")).getText().contains("Aufgabe ist beendet"));
		leaveCourse();

		assertVisible(By.id("reviewForm:reviewTable"), "Course record table should be shown.");
		assertEquals("100 %", find("reviewForm:reviewTable:0:score").getText());

		// Switch to record details
		clickWithRedirect(By.id("reviewForm:reviewTable:0:courseRecordReviewButton"));
	}

	/**
	 * Tests the course record details page
	 */
	@RunAsClient
	@Order(2)
	@Test
	void testCourseRecordDetails() {
		assumeLogin();
		waitPresent(By.id("courseRecordSubmissionsMainForm"), "Course record details page did not load.");

		// Check general information
		assertVisible(By.id("courseRecordSubmissionsMainForm:pgCourseRecordSubmissionsGeneral"),
				"Course record details table was not shown.");
		assertEquals("student", find("courseRecordSubmissionsMainForm:student").getText());
		assertEquals("100 %", find("courseRecordSubmissionsMainForm:result").getText());
		final LocalDate today = LocalDate.now();
		assertTrue(find("courseRecordSubmissionsMainForm:startTime").getText().contains(formatDate(today)));
		assertTrue(find("courseRecordSubmissionsMainForm:startTime").getText().contains(formatDate(today)));

		// Check submission details
		assertVisible(By.id("courseRecordSubmissionsMainForm:availableSubmissions"),
				"Submission details table was not shown.");
		assertEquals("JACK", find("courseRecordSubmissionsMainForm:availableSubmissions")
				.findElement(By.cssSelector("tbody > tr > td:nth-child(1)")).getText());
		assertEquals("100 %", find("courseRecordSubmissionsMainForm:availableSubmissions")
				.findElement(By.cssSelector("tbody > tr > td:nth-child(3)")).getText());

		// Switch to submission details
		clickWithRedirect(By.id("courseRecordSubmissionsMainForm:availableSubmissions:0:btnViewSubmissionId"));
	}

	/**
	 * Tests the submission details page
	 */
	@RunAsClient
	@Order(3)
	@Test
	void testSubmissionDetails() {
		assumeLogin();
		waitPresent(By.id("submissionDetails"), "Course record details page did not load.");

		// Check general information
		assertVisible(By.id("submissionDetails:pgSubmissionDetailsGeneral"), "Submission details table was not shown.");
		assertEquals("student", find("submissionDetails:student").getText());
		assertTrue(find("submissionDetails:startTime").getText().contains(formatDate(LocalDate.now())));
		assertEquals("100 %", find("submissionDetails:result").getText());

		// Check stages
		List<WebElement> stages = getSubmissionStages();

		assertTrue(stages.get(0).getText().contains("Was ist JACK"));
		assertTrue(stages.get(0).getText().contains("Raumstation"));
		assertTrue(stages.get(0).getText().contains("E-Assessment-System"));
		assertTrue(stages.get(0).getText().contains("Bier"));
		assertTrue(stages.get(0).getText().contains("Ergebnis: 100%"));
		assertTrue(stages.get(0).getText().contains("Das ist korrekt"));

		assertTrue(stages.get(1).getText().contains("Berechnen Sie"));
		assertTrue(stages.get(1).getText().contains("Ergebnis: 100%"));

		assertTrue(find("submissionDetails:exercise_options_content").getText().contains("Die Aufgabe ist beendet"));
	}

	/**
	 * Tests that lecturer infos are not shown for students
	 */
	@RunAsClient
	@Order(4)
	@Test
	void checkLecturerInfos() {
		assumeLogin();

		// Note: We don't call "assertNotPresent..." here because this method is slower than just check. Instead we
		// collect all IDs.
		Set<String> allElementIDs = find("submissionDetails")
				.findElements(ALL)
				.stream()
				.map(webElement -> webElement.getAttribute("id"))
				.filter(JackStringUtils::isNotBlank)
				.collect(Collectors.toSet());

		assertFalse(allElementIDs.contains("submissionDetails:panelStageSubmission0Vars"));
		assertFalse(allElementIDs.contains("submissionDetails:panelStageSubmission1Vars"));
		assertFalse(allElementIDs.contains("submissionDetails:panelStageSubmission0Log"));
		assertFalse(allElementIDs.contains("submissionDetails:panelStageSubmission1Log"));
		assertFalse(allElementIDs.contains("submissionDetails:logView:dtSubmissionLogEntries"));
		assertTrue(allElementIDs.contains("submissionDetails:exercise_options_content"));
	}

}
