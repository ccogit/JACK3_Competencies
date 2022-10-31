package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.I18nHelper;
import de.uni_due.s3.jack3.uitests.utils.pages.CourseEditPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.exerciseproviders.FixedListExerciseProviderPage;
import de.uni_due.s3.jack3.uitests.utils.pages.exerciseproviders.FolderExerciseProviderPage;

/**
 * 
 * @author kilian.kraus
 *
 */
class CreateCourseTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private CourseBusiness courseBusiness;
	private final String EXTERNAL_COURSE_DESCRIPTION = "Dieser Kurs dient dem Testen!";
	private final String INTERNAL_COURSE_DESCRIPTION = "Kurs muss noch angepasst werden.";

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);

		assertDoesNotThrow(() -> {
			ContentFolder folder = folderBusiness.createContentFolder(lecturer, "Meine Aufgaben",
					lecturer.getPersonalFolder());
			exerciseBusiness.createExercise("Aufgabe 1", lecturer, folder, "de");
		});
	}

	@Test
	@Order(1)
	@RunAsClient
	void createCourse() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer", "secret");
		MyWorkspacePage.navigateToPage();

		// Create Course
		MyWorkspacePage.createCourse(MyWorkspacePage.getPersonalFolder(), "Mein Kurs");

		// Enter the Course
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("Mein Kurs"));

		//Change Title and descriptions of the Course
		CourseEditPage.setCourseTitle("Course in Jack3");
		CourseEditPage.setInternalNotes(INTERNAL_COURSE_DESCRIPTION);
		CourseEditPage.setExternalDescription(EXTERNAL_COURSE_DESCRIPTION);

		//Set the CourseScoringtype to BEST
		CourseEditPage.setScoringType(ECourseScoring.BEST);

		//Save the Course
		CourseEditPage.saveCourse();

		//Set the FixedListExerciseProvider for this Course
		CourseEditPage.setContentType(FixedListExerciseProvider.class);

		//Add an Exercise
		FixedListExerciseProviderPage.addExerciseWithName("lecturer", "Meine Aufgaben", "Aufgabe 1");

		//Save the Course again
		CourseEditPage.saveCourse();
		logout();
	}

	@Test
	@Order(2)
	void verifyCourse() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionError::new);
		AbstractExercise lazyExercise = exerciseBusiness.getAllExercisesForUser(lecturer).get(0);
		Exercise exercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(lazyExercise.getId());

		assertEquals(1, courseBusiness.getAllCoursesForUser(lecturer).size());
		Course course = courseBusiness.getAllCoursesForUser(lecturer).get(0);

		assertEquals(lecturer.getLoginName(), course.getUpdatedBy());
		assertEquals(I18nHelper.LANGUAGE_GERMAN, course.getLanguage());
		// The description might not exactly be the original because the CKEditor adds HTML tags.
		assertTrue(course.getExternalDescription().contains(EXTERNAL_COURSE_DESCRIPTION));

		assertEquals(INTERNAL_COURSE_DESCRIPTION, course.getInternalDescription());
		assertEquals(ECourseScoring.BEST, course.getScoringMode());
		assertEquals(FixedListExerciseProvider.class, course.getContentProvider().getClass());
		assertEquals(1, course.getContentProvider().getCourseEntries().size());
		assertEquals(exercise, course.getContentProvider().getCourseEntries().get(0).getExercise());
	}

	@Test
	@RunAsClient
	@Order(3)
	void duplicateCourse() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer", "secret");
		// navigate to the Course
		MyWorkspacePage.navigateToPage();
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());

		// Duplicate the Course
		MyWorkspacePage.duplicateCourse(MyWorkspacePage.getCourse("Course in Jack3"), "duplikat",
				"lecturer");
	}

	@Test
	@Order(4)
	void verifyDuplicatedCourse() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionError::new);
		List<Course> courses = courseBusiness.getAllCoursesForUser(lecturer);

		assertEquals(2, courses.size());
		AbstractCourse originalCourse = courses.stream().filter(course -> course.getName().equals("Course in Jack3"))
				.findAny().orElseThrow(() -> new AssertionFailedError(
						"Der Kurs mit dem Namen 'Course in Jack3' konnte nicht gefunden werden."));
		AbstractCourse duplicateCourse = courses.stream().filter(course -> course.getName().equals("duplikat"))
				.findAny().orElseThrow(
						() -> new AssertionFailedError(
								"Der Kurs mit dem Namen 'duplikat' konnte nicht gefunden werden."));

		assertEquals(lecturer.getLoginName(), duplicateCourse.getUpdatedBy());
		assertEquals(duplicateCourse.getLanguage(), originalCourse.getLanguage());
		assertEquals(duplicateCourse.getExternalDescription(), originalCourse.getExternalDescription());
		assertEquals(duplicateCourse.getInternalDescription(), originalCourse.getInternalDescription());
		assertEquals(duplicateCourse.getScoringMode(), originalCourse.getScoringMode());
		assertEquals(duplicateCourse.getContentProvider().getClass(), originalCourse.getContentProvider().getClass());
		assertEquals(duplicateCourse.getContentProvider().getCourseEntries().size(),
				originalCourse.getContentProvider().getCourseEntries().size());
		assertEquals(duplicateCourse.getContentProvider().getCourseEntries().get(0).getExercise(),
				originalCourse.getContentProvider().getCourseEntries().get(0).getExercise());
	}

	@Test
	@RunAsClient
	@Order(5)
	void changeContentProvider() { // NOSONAR Only runs ins the UI, no assertions
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());
		MyWorkspacePage.openCourse(MyWorkspacePage.getCourse("Course in Jack3"));

		//Change the ContentProvider to FolderExerciseProvider
		CourseEditPage.changeContentType(FolderExerciseProvider.class);

		//Add all exercises from the folder 'Meine Aufgaben' to the Course
		FolderExerciseProviderPage.addFolderWithName("lecturer", "Meine Aufgaben");

		CourseEditPage.saveCourse();

		logout();
	}

	@Test
	@Order(6)
	void verifyCourseWithNewContentProvider() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionError::new);
		Course course = courseBusiness.getAllCoursesForUser(lecturer).get(0);
		course = courseBusiness.getCourseWithLazyDataByCourseID(course.getId());

		assertEquals(FolderExerciseProvider.class, course.getContentProvider().getClass());
		assertEquals(1, ((FolderExerciseProvider) course.getContentProvider()).getFolders().size());
		assertEquals("Meine Aufgaben",
				((FolderExerciseProvider) course.getContentProvider()).getFolders().get(0).getName());
	}

}
