package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

class ContentFolderTest extends AbstractContentTest {

	/**
	 * Persist user and content folder
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		folder = folderService.getContentFolderWithLazyData(folder);
	}

	/**
	 * Adds a course to a content folder and returns the merged folder
	 */
	private ContentFolder addContentToFolder(ContentFolder folder, AbstractCourse course) {
		folder.addChildCourse(course);
		baseService.persist(course);
		return folderService.mergeContentFolder(folder);
	}

	/**
	 * Adds an exercise to a content folder and returns the merged folder
	 */
    private ContentFolder addContentToFolder(ContentFolder folder, AbstractExercise exercise) {
		folder.addChildExercise(exercise);
		baseService.persist(exercise);
		return folderService.mergeContentFolder(folder);
	}

	/**
	 * Tests inserting a course to a contentFolder
	 */
	@Test
	void insertCourse() {
		folder = addContentToFolder(folder, course);

		assertTrue(folder.getChildrenCourses().contains(course));
	}

	/**
	 * Tests removing a course to a contentFolder
	 */
	@Test
	void removeCourse() {
		folder = addContentToFolder(folder, course);

		// remove course from folder and merge folder
		folder.removeChildCourse(course);
		folder = folderService.mergeContentFolder(folder);

		// delete course
		baseService.deleteEntity(course);

		folder = folderService.getContentFolderWithLazyData(folder);
		assertFalse(folder.getChildrenCourses().contains(course));
	}

	/**
	 * Tests inserting an exercise to a contentFolder
	 */
	@Test
	void insertExercise() {
		folder = addContentToFolder(folder, exercise);

		assertTrue(folder.getChildrenExercises().contains(exercise));
	}

	/**
	 * Tests removing an exercise to a contentFolder
	 */
	@Test
	void removeExercise() {
		folder = addContentToFolder(folder, exercise);

		// remove exercise from folder and merge folder
		folder.removeChildExercise(exercise);
		folder = folderService.mergeContentFolder(folder);

		// delete exercise
		baseService.deleteEntity(exercise);

		folder = folderService.getContentFolderWithLazyData(folder);
		assertFalse(folder.getChildrenExercises().contains(exercise));
	}

}
