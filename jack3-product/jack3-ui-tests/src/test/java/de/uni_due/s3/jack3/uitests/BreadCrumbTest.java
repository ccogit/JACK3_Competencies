package de.uni_due.s3.jack3.uitests;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertNotVisible;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithRedirect;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static de.uni_due.s3.jack3.uitests.utils.Student.getStages;
import static de.uni_due.s3.jack3.uitests.utils.Student.leaveCourse;
import static de.uni_due.s3.jack3.uitests.utils.Student.mcSelectAnswer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openCourseOffer;
import static de.uni_due.s3.jack3.uitests.utils.Student.openExercise;
import static de.uni_due.s3.jack3.uitests.utils.Student.submitAndWait;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseOfferEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseOfferParticipantsPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseRecordSubmissionPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseStatisticsPage;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.SubmissionDetailsPage;

/**
 * Simple Test checking the Path as String and Breadcrumb (incl. the kinking) 
 *
 */
class BreadCrumbTest extends AbstractSeleniumTest {
	
	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private FolderBusiness folderBusiness;
	
	@Inject
	private CourseBusiness courseBusiness;
	
	@Inject
	private ExerciseBusiness exerciseBusiness;
	
	@Inject
	private FolderService folderService;
	
	@Inject
	private ExerciseService exerciseService;
	
	private static final String PRESENTATIONFOLDER = "presFolder" ;
	
	private static final String FOLDER1 = "folderOne";
	private static final String FOLDER2 = "folderTwo";
	
	private static final String COURSEOFFER = "courseOffer";
	
	private static final String COURSE = "courseOne";
	
	private static final String EXERCISE = "exerciseOne";
	
	private static final String FEEDBACK_CORRECT = "Das ist korrekt.";
	private static final String FEEDBACK_NOT_CORRECT = "Das ist leider <u>nicht</u> korrekt.";
	
	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturerCreating = userBusiness.createUser("lecturerCreating", "secret", "lecturer@foobar.com", false, true);
		User lecturerAllFolders = userBusiness.createUser("lecturerAllFolders", "secret", "lecturer@foobar.com", false,
				true);
		User lecturerOneFolders = userBusiness.createUser("lecturerOneFolder", "secret", "lecturer@foobar.com", false, true);
		User student1 = userBusiness.createUser("student1", "secret", "lecturer@foobar.com", false, false);
		try {
			
			PresentationFolder preRoot = folderBusiness.getPresentationRoot();
			PresentationFolder preFolder = folderBusiness.createPresentationFolder(PRESENTATIONFOLDER, preRoot);
			
			folderBusiness.updateFolderRightsForUser(preFolder, lecturerCreating, AccessRight.getFull());
			
			ContentFolder folderOne = folderBusiness.createContentFolder(lecturerCreating, FOLDER1, lecturerCreating.getPersonalFolder());
			ContentFolder folderTwo = folderBusiness.createContentFolder(lecturerCreating, FOLDER2, folderOne);
			
			folderBusiness.updateFolderRightsForUser(folderOne, lecturerAllFolders, AccessRight.getFull());
			folderBusiness.updateFolderRightsForUser(folderTwo, lecturerOneFolders, AccessRight.getFull());
			
			Exercise exercise = createSampleExercise(EXERCISE, lecturerCreating, folderTwo);
			exerciseBusiness.moveExercise(exercise, folderTwo, lecturerCreating);

			Course course = createSimpleCourseForExercises(COURSE, lecturerCreating, folderTwo);
			
			CourseOffer offer = courseBusiness.createCourseOffer(COURSEOFFER, course, preFolder, lecturerCreating);
			offer.setCourse(course);
			offer.setExplicitEnrollment(false);
			offer.setExplicitSubmission(false);
			offer.setCanBeVisible(true);
			offer = courseBusiness.updateCourseOffer(offer);
			
		} catch (ActionNotAllowedException e) {
			e.printStackTrace();
		}
	}
		
	@Test
	@Order(0)
	@RunAsClient
	void submitAsStudent() {
		login("student1", "secret");
		assumeLogin();

		openCourseOffer(0, 0);
		//assertYouAreHere
		checkBreadCrumbForContainingStrings(find("showCourseRecordMainForm:breadCrumbCourseRecordView"), "presFolder",
				"courseOffer", "Aktuelle Kursbearbeitung");
		checkBreadCrumbForNotContainingStrings(find("showCourseRecordMainForm:breadCrumbCourseRecordView"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");

		openExercise(0);
		//assertYouAreHere
		checkBreadCrumbForContainingStrings(find("showCourseRecordMainForm:breadCrumbCourseRecordView"), "presFolder",
				"courseOffer", "Aktuelle Kursbearbeitung");
		checkBreadCrumbForNotContainingStrings(find("showCourseRecordMainForm:breadCrumbCourseRecordView"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");


		// ----- Stage 1 ("Was ist JACK?") -----
		assertEquals(1, getStages().size());
		WebElement currentStage = getStages().get(0);
		mcSelectAnswer(currentStage, "korrekt");
		submitAndWait(currentStage);

		// Finish course
		assertTrue(find(By.id("showCourseRecordMainForm:exercise_options")).getText().contains("Aufgabe ist beendet"));
		leaveCourse();

		assertVisible(By.id("reviewForm:reviewTable"), "Course record table should be shown.");
		assertEquals("100 %", find("reviewForm:reviewTable:0:score").getText());
		
		// Switch to record details
		clickWithRedirect(By.id("reviewForm:reviewTable:0:courseRecordReviewButton"));

		assertNotVisible(By.id("courseRecordSubmissionsMainForm:courseOffer"), "BreadCrumb of a CourseOffer should not be displayed to a Student.");
		assertNotVisible(By.id("courseRecordSubmissionsMainForm:course"), "BreadCrumb of a Course should not be displayed to a Student.");

		assertFalse(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseIsShown(),
				"BreadCrumb of a Course should not be displayed to a Student.");
		assertFalse(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseOfferIsShown(),
				"BreadCrumb of a CourseOffer should not be displayed to a Student.");

		//assertYouAreHere
		checkBreadCrumbForContainingStrings(find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"),
				"presFolder", "courseOffer", "Details zur Kursbearbeitung");
		checkBreadCrumbForNotContainingStrings(
				find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"), "persönlicher Ordner",
				"Geteilte Inhalte", "personal folder");
		
		logout();
	}
	
	@Test
	@Order(1)
	@RunAsClient
	void verifyVisibilityOfPathForLecFullRights() {
		login("lecturerCreating", "secret");
		assumeLogin();
		AvailableCoursesPage.navigateToPage();
		AvailableCoursesPage.expandFolder(PRESENTATIONFOLDER);
		AvailableCoursesPage.openCourseOffer("courseOffer");
		CourseOfferEditPage.openCourseOfferStatistics();

		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseOfferParticipantsMainForm:breadCrumb"), "presFolder",
					"courseOffer", "Teilnehmerübersicht");
		checkBreadCrumbForNotContainingStrings(find("courseOfferParticipantsMainForm:breadCrumb"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");

		CourseOfferParticipantsPage.openFirstCourseRecordDetails();

		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseIsShown(),
				"BreadCrumb of a Course should be displayed to a Lecturer.");
		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseOfferIsShown(),
				"BreadCrumb of a CourseOffer should be displayed to a Lecturer.");

		CourseRecordSubmissionPage.checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings("presFolder", "courseOffer");
		CourseRecordSubmissionPage.checkThatCourseBreadCrumbOnlyContainsGivenStrings("lecturercreating",
				"folderOne", "folderTwo", "courseOne");

		CourseRecordSubmissionPage.openCourse();

		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseEditMainForm:breadCrumbMainForm"), "lecturercreating",
				"folderOne", "folderTwo", "courseOne");
		checkBreadCrumbForNotContainingStrings(find("courseEditMainForm:breadCrumbMainForm"), "persönlicher Ordner",
				"Geteilte Inhalte", "personal folder");

		CourseEditPage.openStatistics();

		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseStatisticsMainForm:breadCrumbCourseStatistics"),
				"lecturercreating", "folderOne", "folderTwo", "courseOne", "Bearbeitungsübersicht");
		checkBreadCrumbForNotContainingStrings(find("courseStatisticsMainForm:breadCrumbCourseStatistics"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");

		//open the course record submission
		CourseStatisticsPage.openCourseRecordSubmission(CourseStatisticsPage.getFirstEntryOfUser("student1"));

		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"),
				"lecturercreating", "folderOne", "folderTwo", "courseOne", "Bearbeitungsübersicht",
				"Details zur Kursbearbeitung");
		checkBreadCrumbForNotContainingStrings(
				find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"), "persönlicher Ordner",
				"Geteilte Inhalte", "personal folder");

		//check that the breadcrumbs to the course and courseOffer are displayed
		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseIsShown(),
				"BreadCrumb of a Course should be displayed to a Lecturer.");
		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseOfferIsShown(),
				"BreadCrumb of a CourseOffer should be displayed to a Lecturer.");

		//check that the breadCrumbs to the course and courseOffer are correctly
		CourseRecordSubmissionPage.checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings("presFolder", "courseOffer");
		CourseRecordSubmissionPage.checkThatCourseBreadCrumbOnlyContainsGivenStrings("lecturercreating", "folderOne",
				"folderTwo", "courseOne");

		CourseRecordSubmissionPage.openSubmission(EXERCISE);

		//check you are here model
		checkBreadCrumbForContainingStrings(find("submissionDetails:breadCrumbSubmissionDetails"), "lecturercreating",
				"folderOne", "folderTwo", "courseOne", "Bearbeitungsübersicht", "Details zur Kursbearbeitung",
				"Bearbeitungsdetails");
		checkBreadCrumbForNotContainingStrings(find("submissionDetails:breadCrumbSubmissionDetails"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");

		//check that the breadCrumbs to the course and courseOffer are correctlyt
		SubmissionDetailsPage.checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings("presFolder", "courseOffer");
		SubmissionDetailsPage.checkThatCourseBreadCrumbOnlyContainsGivenStrings("lecturercreating", "folderOne",
				"folderTwo", "courseOne");


		//Seite des Kursangebot
		//	Einreichung Kurs
		//		Pfad Kursangebot
		//		Pfad Kurs
		//		Pfad YouAreHere
		//Seite des Kurses
		//	Pfad der Aufgabe
		//	Pfad YouAreHere
		//Seite der Aufgabe
		//	Einreichung Aufgabe
		//		Pfad Kursangebot
		//		Pfad Kurs
		//		Pfad YouAreHere
		logout();
	}
	
	@Test
	@Order(2)
	@RunAsClient
	void verifyVisibilityOfPathForLecPartRights() {
		//Überprüfe Anzeige von Pfad von Lehrendem mit teilw. Rechten
		login("lecturerOneFolder", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolders("lecturercreating","folderTwo");
		
		//open course
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("courseOne"));
		
		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseEditMainForm:breadCrumbMainForm"), "lecturercreating",
				"folderTwo", "courseOne");
		checkBreadCrumbForNotContainingStrings(find("courseEditMainForm:breadCrumbMainForm"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true); //we have rights on this folder
		map.put("courseOne", false); // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("courseEditMainForm:breadCrumbMainForm"), map);
		
		//open statistics
		CourseEditPage.openStatistics();
		
		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseStatisticsMainForm:breadCrumbCourseStatistics"),
				"lecturercreating", "folderTwo", "courseOne", "Bearbeitungsübersicht");
		checkBreadCrumbForNotContainingStrings(find("courseStatisticsMainForm:breadCrumbCourseStatistics"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("courseOne", true);  //we have rights on this course
		map.put("Bearbeitungsübersicht", false); // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("courseStatisticsMainForm:breadCrumbCourseStatistics"), map);
		
		//open the course record submission
		CourseStatisticsPage.openCourseRecordSubmission(CourseStatisticsPage.getFirstEntryOfUser("student1"));

		//check you are here model
		checkBreadCrumbForContainingStrings(find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"),
				"lecturercreating", "folderTwo", "courseOne", "Bearbeitungsübersicht",
				"Details zur Kursbearbeitung");
		checkBreadCrumbForNotContainingStrings(
				find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"), "persönlicher Ordner",
				"Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("courseOne", true);  //we have rights on this course
		map.put("Bearbeitungsübersicht", true); // we can always go back from where we come
		map.put("Details zur Kursbearbeitung", false); // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("courseRecordSubmissionsMainForm:breadCrumbCourseRecordSubmissions"), map);
		
		//check that the breadcrumbs to the course and courseOffer are displayed
		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseIsShown(),
				"BreadCrumb of a Course should be displayed to a Lecturer.");
		assertTrue(CourseRecordSubmissionPage.checkIfBreadCrumbToCourseOfferIsShown(),
				"BreadCrumb of a CourseOffer should be displayed to a Lecturer.");

		//check that the breadCrumbs to the course and courseOffer are correctly
		CourseRecordSubmissionPage.checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings("presFolder", "courseOffer");
		CourseRecordSubmissionPage.checkThatCourseBreadCrumbOnlyContainsGivenStrings("lecturercreating",
				"folderTwo", "courseOne");
		
		//check that the correct elements of the course breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("courseOne", true);  //we have rights on this course
		CourseRecordSubmissionPage.checkThatCourseBreadCrumbHasTheCorrectElementsEnabled(map);
		
		//check that the correct elements of the courseOffer breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("presFolder", true); //this works as root an we can always navigate to it.
		map.put("courseOffer", false);  //we don't have on this courseOffer
		CourseRecordSubmissionPage.checkThatCourseOfferBreadCrumbHasTheCorrectElementsEnabled(map);
		
		CourseRecordSubmissionPage.openSubmission(EXERCISE);
		
		//check you are here model
		checkBreadCrumbForContainingStrings(find("submissionDetails:breadCrumbSubmissionDetails"), "lecturercreating",
				"folderTwo", "courseOne", "Bearbeitungsübersicht", "Details zur Kursbearbeitung",
				"Bearbeitungsdetails");
		checkBreadCrumbForNotContainingStrings(find("submissionDetails:breadCrumbSubmissionDetails"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("courseOne", true);  //we have rights on this course
		map.put("Bearbeitungsübersicht", true); // we can always go back from where we come
		map.put("Details zur Kursbearbeitung", true); // we can always go back from where we come
		map.put("Bearbeitungsdetails", false); // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("submissionDetails:breadCrumbSubmissionDetails"), map);
				
		
		//check that the breadCrumbs to the course and courseOffer are correctly
		SubmissionDetailsPage.checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings("presFolder", "courseOffer");
		SubmissionDetailsPage.checkThatCourseBreadCrumbOnlyContainsGivenStrings("lecturercreating",
				"folderTwo", "courseOne");
		SubmissionDetailsPage.checkThatExerciseBreadCrumbOnlyContainsGivenStrings("lecturercreating",
				"folderTwo", "exerciseOne");
		
		//check that the correct elements of the course breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("courseOne", true);  //we have rights on this course
		SubmissionDetailsPage.checkThatCourseBreadCrumbHasTheCorrectElementsEnabled(map);
		
		//check that the correct elements of the courseOffer breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("presFolder", true); //this works as root an we can always navigate to it.
		map.put("courseOffer", false);  //we don't have rights on this courseOffer
		SubmissionDetailsPage.checkThatCourseOfferBreadCrumbHasTheCorrectElementsEnabled(map);
		
		//navigate to the exercise and the exercise statistics to check their breadcrumbs aswell.
		
		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise(EXERCISE));
		
		//check you are here model
		checkBreadCrumbForContainingStrings(find("exerciseEdit:breadCrumbModel"), "lecturercreating",
				"folderTwo", "exerciseOne");
		checkBreadCrumbForNotContainingStrings(find("exerciseEdit:breadCrumbModel"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("exerciseOne", false);  // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("exerciseEdit:breadCrumbModel"), map);
		
		ExerciseEditPage.openSubmissions();
		
		//check you are here model
		checkBreadCrumbForContainingStrings(find("exerciseSubmissions:breadCrumbExerciseSubmissions"), "lecturercreating",
				"folderTwo", "exerciseOne", "Bearbeitungen");
		checkBreadCrumbForNotContainingStrings(find("exerciseSubmissions:breadCrumbExerciseSubmissions"),
				"persönlicher Ordner", "Geteilte Inhalte", "personal folder");
		
		//check that the correct elements of the breadCrumb are enabled/disabled
		map = new HashMap<String, Boolean>();
		map.put("lecturercreating", false); //we don't have rights on this folder
		map.put("folderTwo", true);  //we have rights on this folder
		map.put("exerciseOne", true);  // we are already here so no reason for a link
		map.put("Bearbeitungen", false);  // we are already here so no reason for a link
		checkBreadCrumbForEnabledAndDisabledComponents(find("exerciseSubmissions:breadCrumbExerciseSubmissions"), map);
		
		//Seite des Kursangebot
		//	Einreichung Kurs
		//		Pfad Kursangebot
		//		Pfad Kurs
		//		Pfad YouAreHere
		//Seite des Kurses
		//	Pfad der Aufgabe
		//Seite der Aufgabe
		//	Einreichung Aufgabe
		//		Pfad Kursangebot
		//		Pfad Kurs
		//		Pfad YouAreHere
		logout();
	}
	
	/**
	 * Creates a simple MC-Exercise
	 * 
	 * @return Persisted exercise
	 */
	public Exercise createSampleExercise(String exerciseName, User author, ContentFolder contentFolder) {
		Exercise exercise = new ExerciseBuilder(exerciseName) //
				.withPublicDescription("generic Description") //
				.withDifficulty(0) //

				.withMCStage() //
				.withTitle("Stufe 1")//
				.withDescription("Waehle korrekt:") //
				.withAnswerOption("korrekt", true) //
				.withAnswerOption("falsch", false) //

				.withCorrectFeedback(FEEDBACK_CORRECT) //
				.withDefaultFeedback(FEEDBACK_NOT_CORRECT, 0) //
				.withRandomizedAnswerOrder().selectOne().and().create();

		return createExerciseFromBuilder(exercise, author, contentFolder);
	}
	
	/**
	 * Inserts a created exercise from an {@link ExerciseBuilder} to the content folder of the exercise's author and
	 * persist/merge it to the database.
	 *
	 * @param exercise
	 *            that was retrieved from an {@link ExerciseBuilder}
	 * @return Persisted exercise entity
	 */
	private Exercise createExerciseFromBuilder(Exercise exercise, User author, ContentFolder contentFolder) {
		ContentFolder folder = author.getPersonalFolder();
		folder.addChildExercise(exercise);
		folderService.mergeContentFolder(folder);
		exerciseService.persistExercise(exercise);
		return exercise;
	}
	
	private Course createSimpleCourseForExercises(String courseName, User author, ContentFolder contentFolder) throws ActionNotAllowedException {
		Course course = courseBusiness.createCourse(courseName, author, contentFolder);
		FolderExerciseProvider fep = new FolderExerciseProvider();
		fep.addFolder(contentFolder);
		course.setContentProvider(fep);
		course.setExerciseOrder(ECourseExercisesOrder.ALPHABETIC_ASCENDING);
		course.setExternalDescription("generic Description");
		course.setScoringMode(ECourseScoring.LAST);
		course.addResultFeedbackMapping(new ResultFeedbackMapping("[meta=currentResult]==100", "generic positive Title",
				"generic positive Feedback"));
		course.addResultFeedbackMapping(
				new ResultFeedbackMapping("[meta=currentResult]==0", "generic negative Title", "generic negative Feedback"));
		course = (Course) courseBusiness.updateCourse(course);
		return course;
	}

	private void checkBreadCrumbForContainingStrings(WebElement breadCrumb, String... strings) {
		waitClickable(breadCrumb);
		List<WebElement> listElements = breadCrumb.findElements(By.tagName("li"));
		List<String> elementTexts = listElements.stream().map(ele -> ele.getText().strip().toLowerCase())
				.collect(Collectors.toList());

		for (String string : strings) {
			assertTrue(elementTexts.contains(string.toLowerCase().strip()),
					"the Element '" + string + "' couldn't be found in the breadcrumb.");
		}
	}
	
	/**
	 * 
	 * @param breadCrumb the breadCrumb containing multiple <li> elements.
	 * @param stringMap a Map containing the Strings which should be part of the breadCrumb.
	 * if a String is mapped to true the component should be clickable/enabled.
	 * If mapped to false the component should be disabled and no href should be there
	 */
	private void checkBreadCrumbForEnabledAndDisabledComponents(WebElement breadCrumb, Map <String,Boolean> stringMap) {
		waitClickable(breadCrumb);
		List<WebElement> listElements = breadCrumb.findElements(By.tagName("li"));
		List<String> elementTexts = listElements.stream().map(ele -> ele.getText().strip().toLowerCase())
				.collect(Collectors.toList());

		for (String string : stringMap.keySet()) {
			assertTrue(elementTexts.contains(string.toLowerCase().strip()),
					"the Element '" + string + "' couldn't be found in the breadcrumb.");
			
			if(stringMap.get(string)) {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					throw new AssertionError("the element '"+string+"' should be enabled in the breadcrumb but wasn't");
				}
			}else {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					//we shouldn't find the link element.
					continue;
				}
				throw new AssertionError("the element '"+string+"' should be disabled in the breadcrumb but wasn't");
								
			}
			
		}
	}

	private void checkBreadCrumbForNotContainingStrings(WebElement breadCrumb, String... strings) {
		waitClickable(breadCrumb);
		List<WebElement> listElements = breadCrumb.findElements(By.tagName("li"));
		List<String> elementTexts = listElements.stream().map(ele -> ele.getText().strip().toLowerCase())
				.collect(Collectors.toList());

		for (String string : strings) {
			assertFalse(elementTexts.contains(string.toLowerCase().strip()),
					"the Element '" + string + "' was found in the breadcrumb. This should not happen.");
		}
	}

}
