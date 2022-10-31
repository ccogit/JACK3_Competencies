package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.builders.CourseBuilder;
import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Misc;
import de.uni_due.s3.jack3.uitests.utils.Time;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.CoursePlayerPage;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.ExerciseEditPage.RevisionDialogue;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.exerciseproviders.FixedListExerciseProviderPage;

class RevisionTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private ExerciseBusiness exerciseBusiness;

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
	}

	@Test
	@Order(1)
	@RunAsClient
	void modifyExerciseAndCreateFrozenExercise() {
		login("lecturer", "secret");

		MyWorkspacePage.navigateToPage();

		// expand PersonalFolder
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());

		// open Exercise
		MyWorkspacePage.openExercise(MyWorkspacePage.getExercise("Aufgabe 1"));

		// change Some Settings
		ExerciseEditPage.changeDifficulty(10);
		ExerciseEditPage.setInternalNotes("my description!");
		ExerciseEditPage.changeTitle("my example Exercise");

		// Save the Exercise
		ExerciseEditPage.saveExercise();
		// After saving the Exercise, there should now be 2 Revisions.
		assertEquals(2, ExerciseEditPage.getNumberOfRevisions());

		// Open Revisions
		RevisionDialogue revisionDialogue = ExerciseEditPage.openRevisions();

		// Freeze Revision
		revisionDialogue.freezeRevision(1);
		// It's hard to explain the UI-Test how long it has to wait here until freezing the Revision is done.
		// Because of that we simply reload the page, so that the Test doesn't have to wait
		Misc.reloadPage();

		// open the Revisions again
		revisionDialogue = ExerciseEditPage.openRevisions();
		// Take a look into the old Revision
		ExerciseEditPage.RevisionPage revisionPage = revisionDialogue.openRevision(0);

		assertEquals("Aufgabe 1", revisionPage.getExerciseName());
		// Most of the UI shouldn't be interactable because this is only a Revision and not the current Exercise
		revisionPage.checkThatUiIsDisabled();

		// Go Back to the Exercise
		revisionPage.jumpToNewestRevision();

		// Take a look at the Frozen Exercise
		revisionPage = ExerciseEditPage.selectFrozenRevision(1);

		assertEquals("my example Exercise", revisionPage.getExerciseName());

		// Here should the UI again be mostly deactivated
		revisionPage.checkThatUiIsDisabled();
		// Give the FrozenExercise an extra name
		
		revisionPage.changeFrozenTitle("this is frozen");
		
		// accepting the Revision as new
		revisionPage.acceptThisRevisionAsNew();

		// after accepting the Revision as new there should be 3 Revisions over all
		assertEquals(3, ExerciseEditPage.getNumberOfRevisions());

		// Do some changes in the Exercise, so that the frozenRevision has other settings
		ExerciseEditPage.changeTitle("changed Name");

		// Save the Exercise
		ExerciseEditPage.saveExercise();
		// After saving the Exercise, there should now be 4 Revisions.
		assertEquals(4, ExerciseEditPage.getNumberOfRevisions());
	}

	@Test
	@Order(2)
	void verifyFrozenExercise() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionFailedError::new);
		Exercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		List<FrozenExercise> frozenExercises = exerciseService.getFrozenRevisionsForExercise(exercise);

		// There should be 1 Frozen Revision
		assertEquals(1, frozenExercises.size());
		FrozenExercise frozenExercise = frozenExercises.get(0);

		// Check that the FrozenExercise and the Exercise have the same settings (with exception of the name)
		assertEquals(exercise.getDifficulty(), frozenExercise.getDifficulty());
		assertEquals(exercise.getInternalNotes(), frozenExercise.getInternalNotes());

		// check the extra name for the frozenExercise
		assertEquals("this is frozen", frozenExercise.getFrozenTitle());

		// Over all there should be 3 Revisions for this exercise
		assertEquals(4, exerciseBusiness.getAllRevisionsForExercise(exercise).size());
	}

	@Test
	@Order(3)
	void createCourse() throws ActionNotAllowedException { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		User lecturer = userBusiness.getUserByName("lecturer").get();
		Course course = courseBusiness.createCourse("Kurs 1", lecturer, lecturer.getPersonalFolder());
		course = new CourseBuilder(course).withScoringMode(ECourseScoring.LAST)
				.withFixedListExerciseProvider(Arrays.asList()).build();

		course = (Course) courseBusiness.updateCourse(course);
	}

	@Test
	@Order(4)
	@RunAsClient
	void useFrozenRevisionInACourse() {
		assumeLogin();

		// navigate to the Course
		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("Kurs 1"));

		// add the Exercise to the Course
		FixedListExerciseProviderPage.addExerciseWithName("lecturer", "changed Name");

		// choose the frozen Revision
		FixedListExerciseProviderPage.chooseFrozenRevisionForSelectedExercise("changed Name", 1);

		// Save the Course and test it
		CourseEditPage.testCourse();

		final List<String> allExerciseNamesInTheCourse = CoursePlayerPage.getShownExerciseNames();
		// It should only show 1 exercise
		assertEquals(1, allExerciseNamesInTheCourse.size());
		// Check that the frozen Revision is used and not the "normal" exercise
		assertEquals("my example Exercise", allExerciseNamesInTheCourse.get(0));
	}

	// Disabled, because this is buggy in the pipeline, see #721
	@Test
	@Order(5)
	@RunAsClient
	void createFrozenCourse() throws InterruptedException {
		assumeLogin();

		CoursePlayerPage.goBack();
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse1");

		Misc.waitUntilPageHasLoaded();
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse2");
		//open the Revisions
		Time.wait(ExpectedConditions.textToBe(By.id("courseEditMainForm:rev"), "3"),
				"The Revision Count doesn't show the right number");
		assertEquals(3, CourseEditPage.getRevisionNumber());
		CourseEditPage.openRevisions();

		//freeze the current Revision
		CourseEditPage.RevisionTable.freezeRevision(2);
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse3");
		Time.wait(ExpectedConditions.textToBe(By.id("courseEditMainForm:rev"), "3"),
				"The Revision Count doesn't show the right number");

		// Take a look into an old revision
		CourseEditPage.openRevisions();
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse4");
		CourseEditPage.RevisionTable.openRevision(1);
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse5");

		//most of the UI should be disabled
		CourseEditPage.RevisionPage.checkThatUiIsDisabled();

		//Choose this Revision as the new Revision
		CourseEditPage.RevisionPage.acceptAsNew();
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse6");

		//now there should be 4 Revisions
		waitClickable(By.id("courseEditMainForm:courseName_display"));
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse7");

		Time.wait(ExpectedConditions.textToBe(By.id("courseEditMainForm:rev"), "4"),
				"The Revision Count doesn't show the right number");
		assertEquals(4, CourseEditPage.getRevisionNumber());
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse8");

		//open the frozen Revision
		CourseEditPage.openFrozenRevision(1);
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse9");

		// Again most of the UI should be disabled
		CourseEditPage.RevisionPage.checkThatUiIsDisabled();

		// Give the frozen Revision an extra Name
		CourseEditPage.RevisionPage.setFrozenTitle("My Frozen Course");
//		de.uni_due.s3.jack3.uitests.utils.Misc.makeScreenshot("createFrozenCourse10");

		logout();
	}

	// Disabled because this depends on test 5
	@Test
	@Order(6)
	void verifyFrozenCourse() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionFailedError::new);
		Course course = courseBusiness.getAllCoursesForUser(lecturer).get(0);
		List<FrozenCourse> frozenCourses = courseBusiness.getFrozenRevisionsForCourse(course);

		// Over all there should be 4 Revisions for this exercise
		assertEquals(4, courseBusiness.getAllRevisionsForCourse(course).size());

		// There should be 1 Frozen Revision
		assertEquals(1, frozenCourses.size());
		FrozenCourse frozencourse = frozenCourses.get(0);

		// Check that the FrozenCourse and the Course have the same settings (with exception of the exercises)
		assertEquals(course.getName(), frozencourse.getName());
		assertEquals(course.getInternalDescription() == null || course.getInternalDescription().isEmpty(),
				frozencourse.getInternalDescription() == null || frozencourse.getInternalDescription().isEmpty());
		assertEquals(course.getExternalDescription() == null || course.getExternalDescription().isEmpty(),
				frozencourse.getExternalDescription() == null || frozencourse.getExternalDescription().isEmpty());
		assertEquals(course.getScoringMode(), frozencourse.getScoringMode());
		assertEquals(course.getContentProvider().getClass(), frozencourse.getContentProvider().getClass());
		assertEquals(0, ((FixedListExerciseProvider) course.getContentProvider()).getCourseEntries().size());
		assertEquals(1, ((FixedListExerciseProvider) frozencourse.getContentProvider()).getCourseEntries().size());

		// Check that the frozen exercise is used and not the "normal" exercise
		AbstractExercise exercise = exerciseService.getAllExercisesForUser(lecturer).get(0);
		FrozenExercise frozenExercise = exerciseService.getFrozenRevisionsForExercise(exercise).get(0);
		assertEquals(frozenExercise, ((FixedListExerciseProvider) frozencourse.getContentProvider()).getCourseEntries()
				.get(0).getFrozenExercise());

		// Check that the Frozen Course has the given name
		assertEquals("My Frozen Course", frozencourse.getFrozenTitle());

	}
}