package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

@NeedsCourse
class CourseTest extends AbstractContentTest {

	/**
	 * Persist course
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		folder = folderService.getContentFolderWithLazyData(folder);
	}

	/**
	 * Get, add, remove resources
	 */
	@Test
	void testResources() {
		assertTrue(course.getCourseResources().isEmpty());

		// add resources
		for (int i = 1; i <= 4; i++) {
			course.addCourseResource(
					new CourseResource("Resource " + i, ("Content of resource " + i).getBytes(), course, null));
			course = baseService.merge(course);
		}

		// resources should be found
		assertEquals(4, course.getCourseResources().size());

		// remove resources
		List<CourseResource> resources = new ArrayList<>(course.getCourseResources());
		for (CourseResource item : resources) {
			course.removeCourseResource(item);
		}
		course = baseService.merge(course);

		// no resources should be found
		assertTrue(course.getCourseResources().isEmpty());
	}

	/**
	 * Get, add result feedback mappings
	 */
	@Test
	void testResultFeedbackMappings() {
		assertTrue(course.getResultFeedbackMappings().isEmpty());

		// add result feedback mappings
		for (int i = 1; i <= 3; i++) {
			course.addResultFeedbackMapping(new ResultFeedbackMapping());
			course = baseService.merge(course);
		}

		// result feedback mappings should be found
		assertEquals(3, course.getResultFeedbackMappings().size());
	}

	/**
	 * Change content povider
	 */
	@Test
	void changeContentProvider() {
		assertNull(course.getContentProvider());

		course.setContentProvider(new FolderExerciseProvider());
		course = baseService.merge(course);

		assertNotNull(course.getContentProvider());
	}

	/**
	 * Change descriptions
	 */
	@Test
	void changeDescriptions() {
		assertNull(course.getInternalDescription());
		assertNull(course.getExternalDescription());

		course.setInternalDescription("Internal description");
		course.setExternalDescription("External description");
		course = baseService.merge(course);

		assertEquals("Internal description", course.getInternalDescription());
		assertEquals("External description", course.getExternalDescription());
	}

	/**
	 * Move course to other folder
	 */
	@Test
	void moveCourse() {
		assertEquals(1, folder.getChildrenCourses().size());
		assertTrue(folder.getChildrenCourses().contains(course));
		assertEquals(folder, ((Course) course).getFolder());

		// create a new folder
		ContentFolder newFolder = TestDataFactory.getContentFolder("New Content Folder", null);
		baseService.persist(newFolder);

		// move course to the new folder
		folder.removeChildCourse(course);
		newFolder.addChildCourse(course);

		// merge
		course = baseService.merge(course);
		folder = folderService.mergeContentFolder(folder);
		newFolder = folderService.mergeContentFolder(newFolder);

		// get with lazy data
		folder = folderService.getContentFolderWithLazyData(folder);
		newFolder = folderService.getContentFolderWithLazyData(newFolder);

		// course should be in new folder
		assertEquals(0, folder.getChildrenCourses().size());
		assertFalse(folder.getChildrenCourses().contains(course));
		assertNotEquals(folder, ((Course) course).getFolder());

		assertEquals(1, newFolder.getChildrenCourses().size());
		assertTrue(newFolder.getChildrenCourses().contains(course));
		assertEquals(newFolder, ((Course) course).getFolder());
	}

	/**
	 * Change name
	 */
	@Test
	void changeName() {
		assertEquals("Course", course.getName());

		course.setName("Course 2.0");
		course = baseService.merge(course);

		assertEquals("Course 2.0", course.getName());
	}

	/**
	 * Change validity
	 */
	@Test
	void changeValidity() {
		assertFalse(course.isValid());

		course.setValid(true);
		course = baseService.merge(course);

		assertTrue(course.isValid());
	}
}
