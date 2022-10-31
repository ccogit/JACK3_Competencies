package de.uni_due.s3.jack3.uitests.student;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertPresent;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTag;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Student.fillFormulaEditorField;
import static de.uni_due.s3.jack3.uitests.utils.Student.fillInputField;
import static de.uni_due.s3.jack3.uitests.utils.Student.getStages;
import static de.uni_due.s3.jack3.uitests.utils.Student.leaveCourse;
import static de.uni_due.s3.jack3.uitests.utils.Student.mcSelectAnswer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openCourseOffer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openExercise;
import static de.uni_due.s3.jack3.uitests.utils.Student.requestHint;
import static de.uni_due.s3.jack3.uitests.utils.Student.scrollDown;
import static de.uni_due.s3.jack3.uitests.utils.Student.submitAndWait;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.DevelopmentBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Time;

class SubmitSingleExerciseTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private DevelopmentBusiness developmentBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@SuppressWarnings("unused")
	@Override
	protected void initializeTest() {
		// Clear database, do first time seup
		super.initializeTest();
		
		// Create users
		User lecturer = userBusiness.createUser("lecturer", "lecturerPassword", "lecturer@foobar.com", false, true);
		User student = userBusiness.createUser("student", "studentPassword", "student@foobar.com", false, false);
		
		// Initialize presentation folder
		PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote",
				folderBusiness.getPresentationRoot());
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());

		for (int i = 1; i <= developmentBusiness.getSampleExerciseCount(); i++) {
			// For each sample exercise create a course and a corresponding course offer
			AbstractExercise exercise = developmentBusiness.createSampleExercise(lecturer, i);
			Course course = null;
			ArrayList<Course> tmpArrayList = new ArrayList<Course>();//Workaround to get the Course out of the Scope.
			tmpArrayList.add(null);
			final int finalI = i;//Workaround to get the i into the Scope.
			assertDoesNotThrow(() -> {tmpArrayList.set(0, courseBusiness.createCourse("Course " + finalI, lecturer, lecturer.getPersonalFolder()));});
			course = tmpArrayList.get(0);
			
			FixedListExerciseProvider fep = new FixedListExerciseProvider();
			fep.addCourseEntry(new CourseEntry(exercise, 100));
			course.setContentProvider(fep);

			course.setScoringMode(ECourseScoring.LAST);

			course = (Course) courseBusiness.updateCourse(course);
			CourseOffer offer = courseBusiness.createCourseOffer("Offer " + i, course, offerFolder, lecturer);

			// Adjust course offer settings
			offer.setShowResultImmediately(true);
			offer.setShowFeedbackImmediately(true);
			offer.setCourseResultDisplay(ECourseResultDisplay.BOTH);
			offer.setReviewMode(ECourseOfferReviewMode.ALWAYS);
			offer.setShowExerciseAndSubmissionInCourseResults(true);
			offer.setShowResultInCourseResults(true);
			offer.setShowFeedbackInCourseResults(true);
			offer.setExplicitEnrollment(false);
			offer.setCanBeVisible(true);

			courseBusiness.updateCourseOffer(offer);
		}
	}

	/**
	 * Tests the exercise "JACK"
	 */
	@RunAsClient
	@Order(1)
	@Test
	void submitSampleExercise1() {
		login("student", "studentPassword");
		assumeLogin();

		// Skip main menu
		openCourseOffer(0, 0);
		openExercise(0);

		// Click the correct answer option ("Ein E-Assessment-System")

		// ----- First stage -----

		// Get first stage
		assertEquals(1, getStages().size());
		WebElement stage = getStages().get(0);

		// Select correct answer
		mcSelectAnswer(stage, "Ein E-Assessment-System");

		// Submit current stage
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 100%"));
		assertTrue(stage.getText().contains("Das ist korrekt"));

		// ----- Second stage -----

		// Get second stage
		assertEquals(2, getStages().size());
		stage = getStages().get(1);

		// Request a hint
		requestHint(stage);
		Time.waitVisible(By.id("showCourseRecordMainForm:hintBlock1"),
				"Hint for Stage 2 was requested but isn't visible");
		assertNotNull(findChildElementByTagAndText(stage, "h4", "Hinweise"));
		assertVisible(By.id("showCourseRecordMainForm:hintBlock1"));
		assertTrue(find(By.id("showCourseRecordMainForm:hintBlock1")).getText().contains("Ergebnis ist zweistellig"));

		// Get the description text and calculate correct result
		String addition = findChildElementByTag(stage, "label", elem -> elem.getText().contains("Berechnen Sie"))
				.getText().replace("Berechnen Sie ", "").replace(" =", "");
		String[] numbers = addition.split(" \\+ ");
		int result = Integer.parseInt(numbers[0]) + Integer.parseInt(numbers[1]);
		
		// Write correct result to input field
		fillInputField(stage, 0, Integer.toString(result));

		// Submit current stage
		submitAndWait(stage);

		// Check that hint is shown after submitting
		assertTrue(find(By.id("showCourseRecordMainForm:hintBlock1")).getText().contains("Ergebnis ist zweistellig"));

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 100%"));
		assertTrue(stage.getText().contains("Das ist korrekt"));

		// There should be an indicator that the exercise is finished
		assertTrue(find(By.id("showCourseRecordMainForm:exercise_options")).getText().contains("Aufgabe ist beendet"));

		// Quit course
		leaveCourse();

		// The student should see the table with participation
		assertPresent(By.id("reviewForm:reviewTable"), "Course record table should be shown.");
		assertEquals("100 %", find("reviewForm:reviewTable:0:score").getText());
	}

	/**
	 * Tests the created course records
	 */
	@Test
	@Order(2)
	void checkSampleExercise1Submission() {
		User student = userBusiness.getUserByName("student").get();
		PresentationFolder folder = folderBusiness
				.getPresentationFolderWithLazyData(folderBusiness.getPresentationRoot());
		folder = folderBusiness.getPresentationFolderWithLazyData(folder.getChildrenFolder().iterator().next());

		List<CourseOffer> offers = new LinkedList<CourseOffer>(folder.getChildrenCourseOffer());
		offers.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
		CourseOffer offer = offers.get(0);

		// ----- First exercise (100 points) -----

		List<CourseRecord> records = courseBusiness.getAllCourseRecords(offer);
		assumeFalse(records.isEmpty(), "No course record was present.");
		assertEquals(1, records.size());
		CourseRecord record = records.get(0);
		assertTrue(record.isClosed());
		assertFalse(record.isTestSubmission());
		assertEquals(100, record.getResultPoints());

		List<Submission> submissions = courseBusiness.getAllSubmissionsForCourseRecord(record);
		assertEquals(1, submissions.size());
		Submission submission = submissions.get(0);
		assertTrue(submission.isCompleted());
		assertFalse(submission.isTestSubmission());
		assertEquals(100, submission.getResultPoints());

		// Assert given hint
		submission = exerciseBusiness.getSubmissionWithLazyDataBySubmissionId(submission.getId()).get();
		assertTrue(submission.getSubmissionLog().stream().anyMatch(log -> log.getType() == ESubmissionLogEntryType.HINT));
	}

	/**
	 * Tests the exercise "Einfache Mengenlehre"
	 */
	@RunAsClient
	@Order(3)
	@Test
	void submitSampleExercise2() throws Exception {
		login("student", "studentPassword");
		assumeLogin();

		// Skip main menu
		openCourseOffer(0, 1);
		openExercise(0);
		
		// ----- First stage -----

		// Get first stage
		assertEquals(1, getStages().size());
		WebElement stage = getStages().get(0);

		// Select correct answers, submit and wait
		mcSelectAnswer(stage, "Leere Menge");
		mcSelectAnswer(stage, "Menge");
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 100%"));
		assertTrue(stage.getText().contains("Das ist korrekt"));
		
		// ----- Second stage -----
		
		assertEquals(2, getStages().size());
		stage = getStages().get(1);

		scrollDown();

		// Get stage description to evaluate the correct answer
		String text = findChildElementByTag(stage, "label", elem -> elem.getText().contains("Wir betrachten eine")).getText();
		int variable = Integer.parseInt(text.replace("Wir betrachten eine ", "").charAt(0) + "");

		// Send correct input to formula editor
		fillFormulaEditorField(stage, 0, "2^" + variable);
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 80%"));
		assertTrue(stage.getText().contains("aber die Zahl könnte noch weiter vereinfacht werden"));

		// ----- Third stage -----

		assertEquals(3, getStages().size());
		stage = getStages().get(2);

		scrollDown();

		// Input "{{},{1},{2},{1,2}}" as the correct input
		fillFormulaEditorField(stage, 0,
				"{"+ Keys.RIGHT + Keys.LEFT+"{" + Keys.RIGHT + ",{1" + Keys.RIGHT + ",{2" + Keys.RIGHT + ",{1,2");
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 100%"));
		assertTrue(stage.getText().contains("Das ist korrekt"));

		// There should be an indicator that the exercise is finished
		assertTrue(
				find(By.id("showCourseRecordMainForm:exercise_options")).getText().contains("Die Aufgabe ist beendet"));

		// Quit course
		leaveCourse();

		// The student should see the table with participation
		assertPresent(By.id("reviewForm:reviewTable"), "Course record table should be shown.");
		assertEquals("93 %", find("reviewForm:reviewTable:0:score").getText());
	}

	/**
	 * Tests the created course records
	 */
	@Test
	@Order(4)
	void checkSampleExercise2Submission() {
		User student = userBusiness.getUserByName("student").get();
		PresentationFolder folder = folderBusiness
				.getPresentationFolderWithLazyData(folderBusiness.getPresentationRoot());
		folder = folderBusiness.getPresentationFolderWithLazyData(folder.getChildrenFolder().iterator().next());

		List<CourseOffer> offers = new LinkedList<CourseOffer>(folder.getChildrenCourseOffer());
		offers.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
		CourseOffer offer = offers.get(1);

		// ----- Second exercise (93 points) -----
		List<CourseRecord> records = courseBusiness.getAllCourseRecords(offer);
		assumeFalse(records.isEmpty(), "No course record was present.");
		assertEquals(1, records.size());
		CourseRecord record = records.get(0);
		assertTrue(record.isClosed());
		assertFalse(record.isTestSubmission());
		assertEquals(93, record.getResultPoints());

		List<Submission> submissions = courseBusiness.getAllSubmissionsForCourseRecord(record);
		assertEquals(1, submissions.size());
		Submission submission = submissions.get(0);
		assertTrue(submission.isCompleted());
		assertFalse(submission.isTestSubmission());
		assertEquals(93, submission.getResultPoints());
	}

	/**
	 * Tests the exercise "Städte & Länder"
	 */
	@RunAsClient
	@Order(5)
	@Test
	void submitSampleExercise4() {
		login("student", "studentPassword");
		assumeLogin();

		// Skip main menu
		openCourseOffer(0, 3);
		openExercise(0);

		// ----- First stage -----

		// Get first stage
		assertEquals(1, getStages().size());
		WebElement stage = getStages().get(0);

		// Select an answer and submit
		mcSelectAnswer(stage, "Nordrhein-Westfalen");
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 0%")); // Maybe this will be removed in the future

		// ----- Second stage -----
		assertEquals(2, getStages().size());
		stage = getStages().get(1);

		// Select correct answers and submit
		mcSelectAnswer(stage, "Essen");
		mcSelectAnswer(stage, "Dortmund");
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 100%"));

		// ----- Third stage -----
		assertEquals(3, getStages().size());
		stage = getStages().get(2);

		// Don't select any answer and submit
		submitAndWait(stage);

		// Check shown feedback
		assertTrue(stage.getText().contains("Ergebnis: 0%"));

		// End course
		assertTrue(find("showCourseRecordMainForm:exercise_options").getText().contains("Die Aufgabe ist beendet"));
		leaveCourse();

		// The student should see the table with participation
		assertPresent(By.id("reviewForm:reviewTable"), "Course record table should be shown.");
		assertEquals("50 %", find("reviewForm:reviewTable:0:score").getText());
	}

	/**
	 * Tests the created course records
	 */
	@Test
	@Order(6)
	void checkSampleExercise4Submission() {
		User student = userBusiness.getUserByName("student").get();
		PresentationFolder folder = folderBusiness
				.getPresentationFolderWithLazyData(folderBusiness.getPresentationRoot());
		folder = folderBusiness.getPresentationFolderWithLazyData(folder.getChildrenFolder().iterator().next());

		List<CourseOffer> offers = new LinkedList<CourseOffer>(folder.getChildrenCourseOffer());
		offers.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
		CourseOffer offer = offers.get(3);

		// ----- Second exercise (50 points) -----
		List<CourseRecord> records = courseBusiness.getAllCourseRecords(offer);
		assumeFalse(records.isEmpty(), "No course record was present.");
		assertEquals(1, records.size());
		CourseRecord record = records.get(0);
		assertTrue(record.isClosed());
		assertFalse(record.isTestSubmission());
		assertEquals(50, record.getResultPoints());

		List<Submission> submissions = courseBusiness.getAllSubmissionsForCourseRecord(record);
		assertEquals(1, submissions.size());
		Submission submission = submissions.get(0);
		assertTrue(submission.isCompleted());
		assertFalse(submission.isTestSubmission());
		assertEquals(50, submission.getResultPoints());
	}

}
