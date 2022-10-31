package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Tests course revision as an example for other RevisionServices.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CourseRevisionTest extends AbstractTest {

	private static User user = TestDataFactory.getUser("User");
	private static ContentFolder folder = TestDataFactory.getContentFolder("Folder", null);
	private static AbstractCourse course = new Course("Course");

	@Inject
	private FolderService folderService;

	@Inject
	private CourseService courseService;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private DevelopmentService devService;

	@Inject
	private RevisionService revisionService;

	private void initializeTest() {
		// We explicitly don't call "getAdmin()" because we don't want to have the automatic personal folder.
		baseService.persist(user);
		folderService.persistFolder(folder);

		folder.addChildCourse(course);

		courseService.persistCourse(course);
		folder = folderService.mergeContentFolder(folder);

	}

	private void clearDatabase() {
		devService.deleteTenantDatabase(EDatabaseType.H2);
	}

	/**
	 * Tests if new created course has revisionID 0
	 */
	@Test
	@Order(0)
	void getNewRevisionID() {

		// only do initialization once
		initializeTest();
		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		assertEquals(revisions.get(FIRST_REVISION).intValue(),
				revisionService.getProxiedOrLastPersistedRevisionId(course));
	}

	/**
	 * Tests if merging increases revisionID of course
	 */
	@Test
	@Order(1)
	void getRevisionID() {

		course = courseService.mergeCourse(course);
		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		assertEquals(revisions.get(SECOND_REVISION).intValue(),
				revisionService.getProxiedOrLastPersistedRevisionId(course));
	}

	/**
	 * Tests if earlier revisions were found after merging
	 */
	@Test
	@Order(2)
	void getAllRevisions() {
		course.setName("Course 2.0");
		course = courseService.mergeCourse(course);

		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);

		assertEquals(revisions.get(THIRD_REVISION).intValue(),
				revisionService.getProxiedOrLastPersistedRevisionId(course));

		assertEquals(3, courseBusiness.getAllRevisionsForCourse((Course) course).size());

		assertEquals("Course",
				courseService.getRevisionOfCourse(course, revisions.get(SECOND_REVISION)).get().getName());

		assertEquals("Course 2.0",
				courseService.getRevisionOfCourse(course, revisions.get(THIRD_REVISION)).get().getName());
	}

	/**
	 * Tests resetting course to revision
	 */
	@Test
	@Order(3)
	void resetToRevision() {
		List<Integer> revisions = revisionService.getRevisionNumbersFor(course);
		course = courseBusiness.resetToRevision(course, revisions.get(SECOND_REVISION), user);
		assertEquals("Course", course.getName());
	}

	/**
	 * Delete one entity
	 */
	@Test
	@Order(4)
	void deleteEntity() {

		// Remove course from folder and delete folder
		folder.removeChildCourse(course);
		folder = folderService.mergeContentFolder(folder);
		baseService.deleteEntity(course);

		// No course should be in database
		assertTrue(baseService.findAll(Course.class).isEmpty());
	}

	/**
	 * Delete entity list
	 */
	@Test
	@Order(5)
	void deleteEntities() {

		// Add many courses
		for (int i = 0; i < 4; i++) {
			Course course = new Course("Course " + i);
			folder = folderService.getContentFolderWithLazyData(folder);
			folder.addChildCourse(course);
			courseService.persistCourse(course);
			folder = folderService.mergeContentFolder(folder);
		}

		// Assert if the courses are found
		List<Course> allCourses = baseService.findAll(Course.class);
		assertEquals(4, allCourses.size());

		// Remove courses
		folder = folderService.getContentFolderWithLazyData(folder);
		for (Course course : allCourses) {
			folder.removeChildCourse(course);
		}
		folder = folderService.mergeContentFolder(folder);
		allCourses.forEach(baseService::deleteEntity);

		assertTrue(baseService.findAll(Course.class).isEmpty());

		// clear database after testing
		clearDatabase();
	}

}
