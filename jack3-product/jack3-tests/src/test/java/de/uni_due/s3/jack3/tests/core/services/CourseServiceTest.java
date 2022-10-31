package de.uni_due.s3.jack3.tests.core.services;

import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ejb.EJBException;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class CourseServiceTest extends AbstractContentTest {

	@Inject
	private RevisionService revisionService;

	@Inject
	private CourseService courseService;

	private User lecturer = TestDataFactory.getUser("Lecturer", true, true);
	private User student = TestDataFactory.getUser("Student", false, false);

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		// We explicitly don't call "getAdmin()" because we don't want to have the automatic personal folder.
		userService.persistUser(lecturer);
		userService.persistUser(student);
		persistFolder();
	}

	/**
	 * Add new course to folder
	 */
	private Course addNewCourse(String courseName) {
		Course course = new Course(courseName);
		folder.addChildCourse(course);
		courseService.persistCourse(course);
		folder = folderService.mergeContentFolder(folder);
		folder = folderService.getContentFolderWithLazyData(folder);
		return course;
	}

	/**
	 * Test empty database
	 */
	@Test
	void getEmptyCourseList() {

		// there should be no courses
		assertTrue(courseService.getAllCoursesForUser(lecturer).isEmpty());
		assertTrue(courseService.getAllCoursesForUser(student).isEmpty());
	}

	/**
	 * Delete course
	 */
	@Test
	void deleteCourse1() {

		// persist course
		Course course = addNewCourse("Course");

		// delete course
		folder.removeChildCourse(course);
		courseService.deleteCourse(course); // course should be merged and then deleted

		// course should be deleted from database & folder
		assertTrue(courseService.getAllCoursesForUser(lecturer).isEmpty());
		assertTrue(courseService.getAllCoursesForUser(student).isEmpty());
		assertTrue(courseService.getAllCoursesForContentFolderList(Arrays.asList(folder)).isEmpty());
	}

	/**
	 * Delete course
	 */
	@Test
	void deleteCourse2() {

		// persist course
		Course course = addNewCourse("Course");

		// delete course
		folder.removeChildCourse(course);
		courseService.deleteCourse(courseService.mergeCourse(course)); // course should be deleted directly

		// course should be deleted from database & folder
		assertTrue(courseService.getAllCoursesForUser(lecturer).isEmpty());
		assertTrue(courseService.getAllCoursesForUser(student).isEmpty());
		assertTrue(courseService.getAllCoursesForContentFolderList(Arrays.asList(folder)).isEmpty());
	}

	/**
	 * Get all courses for user
	 */
	@Test
	void getCourseForUser() {

		// add courses
		Course course1 = addNewCourse("Course 1");
		Course course2 = addNewCourse("Course 2");

		// set rights for Lecturer for folder
		folder.addUserRight(lecturer, AccessRight.getFull());
		folder = folderService.mergeContentFolder(folder);

		// there should be 2 courses for lecturer and 0 for student
		Collection<Course> getCoursesForLecturerFromDB = courseService.getAllCoursesForUser(lecturer);
		assertEquals(Arrays.asList(course1, course2), getCoursesForLecturerFromDB);
		assertEquals(0, courseService.getAllCoursesForUser(student).size());

		// adding Student to folder
		folder.addUserRight(student, AccessRight.getFromFlags(READ));
		folder = folderService.mergeContentFolder(folder);

		// course list for lecturer and students should be equal
		assertEquals(getCoursesForLecturerFromDB, courseService.getAllCoursesForUser(student));
	}

	/**
	 * Get all courses for content folder list
	 */
	@Test
	void getCourseForFolderList() {

		// add course to folder
		Course course1 = addNewCourse("Course 1");
		Course course2 = addNewCourse("Course 2");

		// set rights for Lecturer for folder to get content folder for user
		folder.addUserRight(lecturer, AccessRight.getFull());
		folder = folderService.mergeContentFolder(folder);

		// 2 courses should be found
		assertEquals(Arrays.asList(course1, course2),
				courseService.getAllCoursesForContentFolderList(folderService.getAllContentFoldersForUser(lecturer)));
	}

	/**
	 * Get non-existing course with lazy data
	 */
	@Test
	void getCourseWithLazyDataByIllegalId() {
		assertFalse(courseService.getCourseWithLazyDataByCourseID(0).isPresent());
	}

	/**
	 * Test if all lazy getters work
	 */
	@Test
	void getCourseWithLazyData() {

		// persist course
		Course course = addNewCourse("Course");

		// get course with lazy data
		long courseID = course.getId();
		Course courseWithLazyData = courseService	.getCourseWithLazyDataByCourseID(courseID)
													.orElseThrow(AssertionError::new);
		assertEquals(course, courseWithLazyData);
		assertEquals(course, courseService.getCourseWithLazyDataByCourseID(courseID)
													.orElseThrow(AssertionError::new));


		// get fields
		assertTrue(courseWithLazyData.getResultFeedbackMappings().isEmpty());
		assertTrue(courseWithLazyData.getCourseResources().isEmpty());
	}

	/**
	 * Get non-existing course by ID
	 */
	@Test
	void getCourseByIllegalId() {
		assertFalse(courseService.getCourseByCourseID(0).isPresent());
	}

	/**
	 * Test getting a course by ID
	 */
	@Test
	void getCourseById() {
		// Persist course
		Course course = addNewCourse("Course");
		long id = course.getId();

		// Get course
		Optional<Course> result = courseService.getCourseByCourseID(id);
		assertTrue(result.isPresent());
		assertEquals(course, result.get());
	}

	/**
	 * Get revision of course with lazy data
	 */
	@Test
	void getRevisionWithLazyData() {
		// persist course
		AbstractCourse course = addNewCourse("Course 1.0");

		// give a new name and merge to test revision
		course.setName("Course 2.0");
		course = courseService.mergeCourse(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);
		// get revision with lazy data
		course = courseService	.getRevisionOfCourseWithLazyData(course, revisions.get(FIRST_REVISION))
								.orElseThrow(AssertionError::new);

		// test new revision
		assertEquals("Course 1.0", course.getName());

		try {
			revisionService.getProxiedOrLastPersistedRevisionId(course);
			fail("We should not get to here!");
		} catch (EJBException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
			assertTrue(e.getCause().getMessage()
					.startsWith("Getting the latest revision of a Course from the audit table is not well defined!"));
		}

		// test if all lazy collections were fetched
		assertTrue(course.getResultFeedbackMappings().isEmpty());
		assertTrue(course.getCourseResources().isEmpty());
	}

}
