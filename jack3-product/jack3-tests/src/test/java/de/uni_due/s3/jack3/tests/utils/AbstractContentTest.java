package de.uni_due.s3.jack3.tests.utils;

import org.junit.jupiter.api.BeforeEach;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;

/**
 * Test class for tests that depend on folders and exercises and/or courses. By default, it only persists a user with a
 * content folder, sub classes can specify if they need a course and/or an exercise.
 * 
 * @author lukas.glaser
 */
public abstract class AbstractContentTest extends AbstractBasicTest {

	// default entities
	protected AbstractExercise exercise = new Exercise("Exercise", TestDataFactory.getDefaultLanguage());

	protected AbstractCourse course = new Course("Course");

	/**
	 * Persists User, Folder and Exercise/Course if needed before each test.
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		userService.persistUser(user);
		folderService.persistFolder(folder);

		if (getClass().isAnnotationPresent(NeedsExercise.class)) {
			folder.addChildExercise(exercise);
			baseService.persist(exercise);
		}
		if (getClass().isAnnotationPresent(NeedsCourse.class)) {
			folder.addChildCourse(course);
			baseService.persist(course);
		}

		folder = folderService.mergeContentFolder(folder);
	}
}
