package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Student;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CoursePlayerPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseRecordSubmissionPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseStatisticsPage;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseSubmissionsPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.SubmissionDetailsPage;

/**
 * Tests statistics for exercises, courses and course offers
 * @author kilian.kraus
 *
 */
class LecturerStatisticsTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private CourseBusiness courseBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);
		userBusiness.createUser("student", "secret", "student@foobar.com", false, false);

		// Generate an exercise with simple stages just for testing
		Exercise exercise = new ExerciseBuilder("Aufgabe 1").withRandomVariableDeclaration("x", 0, 9)

				// First MC stage
				.withMCStage().selectOne().withTitle("Stufe 1").allowSkip("Aufgabe übersprungen")
				.withAnswerOption("Korrekt", true).withAnswerOption("Falsch", false)
				.withDefaultFeedback("Ergebnis ist falsch.", 0).withCorrectFeedback("Ergebnis ist richtig.")
				.withHint("Hinweis").and()

				// Second MC stage
				.withMCStage().selectOne().withTitle("Stufe 2").allowSkip("Aufgabe übersprungen")
				.withAnswerOption("Korrekt", true).withAnswerOption("Falsch", false)
				.withDefaultFeedback("Ergebnis ist falsch.", 0).withCorrectFeedback("Ergebnis ist richtig.")
				.withHint("Hinweis").and().create();

		// Persist the exercise
		ContentFolder folder = lecturer.getPersonalFolder();
		folder.addChildExercise(exercise);
		exerciseService.persistExercise(exercise);
		folderBusiness.updateFolder(folder);

		// Add the exercise to a course
		Course course = null;
		ArrayList<Course> tmpArrayList = new ArrayList<Course>();//Workaround to get the Course out of the Scope.
		tmpArrayList.add(null);
		assertDoesNotThrow(() -> {tmpArrayList.set(0, courseBusiness.createCourse("Kurs 1", lecturer, lecturer.getPersonalFolder()));});
		course = tmpArrayList.get(0);
		
		FixedListExerciseProvider fep = new FixedListExerciseProvider();
		fep.addCourseEntry(new CourseEntry(exercise, 100));
		course.setContentProvider(fep);
		course.setScoringMode(ECourseScoring.LAST);
		course = (Course) courseBusiness.updateCourse(course);

		// Initialize presentation folder
		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote", root);
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());

		// Add the course to an exam course offer
		CourseOffer offer = courseBusiness.createCourseOffer("Mein Kursangebot", course, offerFolder, lecturer);
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
		offer.setExplicitEnrollment(false);
		offer.setCanBeVisible(true);
		courseBusiness.updateCourseOffer(offer);
	}

	@Test
	@Order(1)
	@RunAsClient
	void testExerciseAndCheckStatistics() {
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();

		// Expand PersonalFolder
		MyWorkspacePage.getPersonalFolder().click();
		// Open Exercise
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));

		// There must not be a Submission yet
		assertEquals(0, ExerciseEditPage.getNumberOfTestSubmissions());
		assertEquals(0, ExerciseEditPage.getNumberOfStudentSubmissions());

		// Test Exercise
		ExerciseEditPage.testExercise();

		// Check that all Buttons are Clickable
		waitClickable(By.id("exerciseTest:skip0"));
		waitClickable(By.id("exerciseTest:hint0"));
		waitClickable(By.id("exerciseTest:submit0"));

		find("exerciseTest:panelStageSubmission_0_PreVars_toggler").click();
		find("exerciseTest:panelStageSubmission_0_Vars_toggler").click();

		waitClickable(By.id("exerciseTest:submit0"));
		driver.findElement(By.id("exerciseTest:submit0")).click();
		waitClickable(By.id("exerciseTest:submit1"));
		driver.findElement(By.id("exerciseTest:backToExercise")).click();

		// After Testing one Submission should now be displayed
		assertEquals(1, ExerciseEditPage.getNumberOfTestSubmissions());
		assertEquals(0, ExerciseEditPage.getNumberOfStudentSubmissions());

		ExerciseEditPage.openSubmissions();

		assertEquals(1, ExerciseSubmissionsPage.getNumberOfTestSubmissions());
		assertEquals(0, ExerciseSubmissionsPage.getNumberOfStudentSubmissions());
		ExerciseSubmissionsPage.openExercisesSubmissionDetails(ExerciseSubmissionsPage.getFirstEntryOfUser("lecturer"));

		// The Submission should be from the lecturer
		assertEquals("lecturer", SubmissionDetailsPage.getAuthor());
		//There should be 4 logEntries
		assertEquals(4, SubmissionDetailsPage.getNumberOfLogEntries());

		// The User should get 0 Points for the submission
		assertEquals(0, SubmissionDetailsPage.getResultPoints());
	}

	@Test
	@Order(2)
	@RunAsClient
	void testCourseAndCheckStatistics() {
		assumeLogin();
		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("Kurs 1"));

		// Open statistics
		CourseEditPage.openStatistics();

		//There shouldn't be any data available
		assertEquals("Für den gewählten Kurs sind noch keine Daten über Bearbeitungen verfügbar.", driver
				.findElement(By.cssSelector("#courseStatisticsMainForm\\:headerPart > p:nth-child(1)")).getText());

		// Go Back to the Course
		CourseStatisticsPage.goBackToCourse();

		// Test the Course
		CourseEditPage.testCourse();

		// Enter the Exercise
		CoursePlayerPage.openExercise("Aufgabe 1");

		// The Buttons to Submit, Skip and get a Hint should be Visible and Clickable
		waitClickable(By.id("courseTestForm:submit0"));
		waitClickable(By.id("courseTestForm:skip0"));
		waitClickable(By.id("courseTestForm:hint0"));

		// Choose the Correct Answer and Submit the the first Stage
		Student.mcSelectAnswer(find("courseTestForm:panelIDStage_0_content"), "Korrekt");
		driver.findElement(By.id("courseTestForm:submit0")).click();
		waitClickable(By.id("courseTestForm:submit1"));

		// Go Back to the Course
		CoursePlayerPage.goBack();

		// Check the Statistics
		CourseEditPage.openStatistics();

		//After Testing there should be 1 Submission with 50 Points.
		assertEquals(1, CourseStatisticsPage.getNumberOfTestSubmissions());
		assertEquals(0, CourseStatisticsPage.getNumberOfStudentSubmissions());
		assertEquals(50, CourseStatisticsPage.getPointsOfEntry(CourseStatisticsPage.getFirstEntryOfUser("lecturer")));
		// The Number of Participants should still be 0 because we only have tested the Course and no student participated
		assertEquals(0, CourseStatisticsPage.getNumberOfParticipants());

		// Look inside the CourseRecord
		CourseStatisticsPage.openCourseRecordSubmission(CourseStatisticsPage.getFirstEntryOfUser("lecturer"));

		assertEquals("lecturer", CourseRecordSubmissionPage.getUserName());
		assertEquals(50, CourseRecordSubmissionPage.getResultPoints());

		// Look inside the Exercise Submission
		CourseRecordSubmissionPage.openSubmission("Aufgabe 1");

		// The Logentries and the General informations must be visible
		SubmissionDetailsPage.getLogEntryRows();
		assertEquals("lecturer", SubmissionDetailsPage.getAuthor());
		assertEquals(50, SubmissionDetailsPage.getResultPoints());

		logout();
	}

	@Test
	@Order(3)
	@RunAsClient
	void studentCreatesASubmission() { // NOSONAR no assertions here
		login("student", "secret");
		assumeLogin();

		Student.openCourseOffer(0, 0);

		// Navigate to the Exercise
		CoursePlayerPage.openExercise("Aufgabe 1");

		// Choose the Correct Answer and Submit the the first Stage
		Student.mcSelectAnswer(find("showCourseRecordMainForm:panelIDStage_0_content"), "Korrekt");
		driver.findElement(By.id("showCourseRecordMainForm:submit0")).click();
		waitClickable(By.id("showCourseRecordMainForm:submit1"));

		// Leave CourseOffer
		CoursePlayerPage.quiteCourse();

		logout();
	}

	@Test
	@Order(4)
	@RunAsClient
	void testCourseOfferStatistics() {
		login("lecturer", "secret");
		assumeLogin();

		//Open the CourseOffer
		AvailableCoursesPage.navigateToPage();
		AvailableCoursesPage.expandFolder("Kursangebote");
		AvailableCoursesPage.openCourseOffer("Mein Kursangebot");

		waitClickable(By.id("courseOfferEdit:toMoreCourseOfferStatistics"));
		driver.findElement(By.id("courseOfferEdit:toMoreCourseOfferStatistics")).click();


		waitVisible(By.id("courseOfferParticipantsMainForm:submissions"));
		// there should be 1 submission
		assertEquals("1", find(By.id("courseOfferParticipantsMainForm:submissions")).getText());

		// Open the CourseRecord Submission
		driver.findElement(By.id(
				"courseOfferParticipantsMainForm:participantsDetails:courseRecordTable:0:actions"))
		.click();
		driver.findElement(By.id(
				"courseOfferParticipantsMainForm:participantsDetails:courseRecordTable:0:showAction"))
		.click();

		// Check that the correct informations are displayed
		assertEquals("student", CourseRecordSubmissionPage.getUserName());
		assertEquals(50, CourseRecordSubmissionPage.getPointsForSubmission("Aufgabe 1"));
		// Open the Submission for "Aufgabe 1"
		CourseRecordSubmissionPage.openSubmission("Aufgabe 1");

		// Check that the correct informations are displayed
		SubmissionDetailsPage.getLogEntryRows();
		assertEquals("student", SubmissionDetailsPage.getAuthor());
		assertEquals(50, SubmissionDetailsPage.getResultPoints());

		logout();
	}

	@Test
	@Order(5)
	@RunAsClient
	void testSubmissionView() {
		login("lecturer", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();

		// Open Course
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("Kurs 1"));

		// Open Statistics
		CourseEditPage.openStatistics();

		// Now there should be 1 Submissions and 1 Participant
		assertEquals(1, CourseStatisticsPage.getNumberOfStudentSubmissions());
		assertEquals(1, CourseStatisticsPage.getNumberOfTestSubmissions());
		assertEquals(1, CourseStatisticsPage.getNumberOfParticipants());

		// Go Back to the Main Menu
		MyWorkspacePage.navigateToPage();

		// Open Exercise
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));

		// Now there must be 3 Submissions
		assertEquals(1, ExerciseEditPage.getNumberOfStudentSubmissions());
		assertEquals(2, ExerciseEditPage.getNumberOfTestSubmissions());

		// Open Submissions
		ExerciseEditPage.openSubmissions();

		// Delete one Submission
		ExerciseSubmissionsPage.removeSingleSubmission(ExerciseSubmissionsPage.getAllEntriesOfUser("lecturer").get(1));

		// Go Back to the Exercise
		ExerciseSubmissionsPage.goBackToExercise();

		// Now there must be 2 Submissions
		assertEquals(1, ExerciseEditPage.getNumberOfStudentSubmissions());
		assertEquals(1, ExerciseEditPage.getNumberOfTestSubmissions());
		// Go back to the Submissions
		ExerciseEditPage.openSubmissions();

		// Delete All TestSubmissions
		ExerciseSubmissionsPage.removeAllTestSubmissions();

		// Go Back to the Exercise
		ExerciseSubmissionsPage.goBackToExercise();
		// There should be two Submissions
		assertEquals(1, ExerciseEditPage.getNumberOfStudentSubmissions());
		assertEquals(1, ExerciseEditPage.getNumberOfTestSubmissions());
		logout();
	}

}

