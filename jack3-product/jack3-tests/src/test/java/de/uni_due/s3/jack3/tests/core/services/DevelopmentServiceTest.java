package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import javax.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Tests if developmentService returns correct results and deletes all entities.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DevelopmentServiceTest extends AbstractTest {

	@Inject
	private DevelopmentService developmentService;

	@Inject
	private FolderService folderService;

	private static final int COUNT_OBJECTS = 3;

	private static User[] users = new User[COUNT_OBJECTS];
	private static UserGroup[] groups = new UserGroup[COUNT_OBJECTS];

	private static ContentFolder[] contentFolders = new ContentFolder[COUNT_OBJECTS];
	private static PresentationFolder[] presentationFolders = new PresentationFolder[COUNT_OBJECTS];

	private static Exercise[] exercises = new Exercise[COUNT_OBJECTS];

	// x offers/records/submissions per course
	private static AbstractCourse[] courses = new AbstractCourse[COUNT_OBJECTS];
	private static CourseResource[] resources = new CourseResource[COUNT_OBJECTS];
	private static CourseOffer[] offers = new CourseOffer[COUNT_OBJECTS];
	private static CourseRecord[] records = new CourseRecord[COUNT_OBJECTS];
	private static Submission[] submissions = new Submission[COUNT_OBJECTS];
	private static CourseEntry[] courseEntries = new CourseEntry[COUNT_OBJECTS];
	private static AbstractExerciseProvider[] exerciseProviders = new AbstractExerciseProvider[COUNT_OBJECTS];

	/**
	 * Creates all needed objects
	 */
	private void initializeTest() {
		// Delete tenant database because this test does not need root folders
		developmentService.deleteTenantDatabase(EDatabaseType.H2);

		for (int i = 0; i < COUNT_OBJECTS; i++) {

			// create users, log entries and groups
			users[i] = TestDataFactory.getUser("User" + i);
			groups[i] = TestDataFactory.getUserGroup("Group" + i, users[i]);

			// create contentFolders and presentationFolders:
			// Folder 1 --> Folder 2 --> ...
			if (i == 0) {
				contentFolders[i] = TestDataFactory.getContentFolder("Content Folder " + i, null,
						users[i]);
				presentationFolders[i] = TestDataFactory.getPresentationFolder("Presentation Folder " + i, null);
			} else {
				contentFolders[i] = TestDataFactory.getContentFolder("Content Folder " + i,
						contentFolders[i - 1], users[i]);
				presentationFolders[i] = TestDataFactory.getPresentationFolder("Presentation Folder " + i,
						presentationFolders[i - 1]);
			}

			// create exercises
			exercises[i] = new Exercise("Exercise " + i, TestDataFactory.getDefaultLanguage());

			// create courses with resource, offer, record
			courses[i] = new Course("Course " + i);

			resources[i] = new CourseResource("Resource " + i + ".txt", "Content".getBytes(), courses[i], users[i]);

			offers[i] = new CourseOffer("Course Offer " + i, courses[i]);
			records[i] = new CourseRecord(users[i], offers[i], courses[i]);

			// create submission
			submissions[i] = new Submission(users[i], exercises[i], records[i], false);

			courseEntries[i] = new CourseEntry(exercises[i], i * 10);

			exerciseProviders[i] = new FolderExerciseProvider(new HashMap<ContentFolder, Integer>());
			((FolderExerciseProvider) exerciseProviders[i]).addFolder(contentFolders[i]);
		}
	}

	/**
	 * Tests if all users were found
	 */
	@Test
	@Order(0)
	void getAllUsers() {

		initializeTest();

		for (User user : users) {
			baseService.persist(user);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllUsers().size());
		assertTrue(developmentService.getAllUsers().containsAll(Arrays.asList(users)));
	}

	/**
	 * Tests if all user groups were found
	 */
	@Test
	@Order(2)
	void getAllUserGroups() {

		for (UserGroup group : groups) {
			baseService.persist(group);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllUserGroups().size());
		assertTrue(developmentService.getAllUserGroups().containsAll(Arrays.asList(groups)));
	}

	/**
	 * Tests if all content folders were found
	 */
	@Test
	@Order(3)
	void getAllContentFolders() {

		for (int i = 0; i < COUNT_OBJECTS; i++) {
			baseService.persist(contentFolders[i]);

			users[i].setPersonalFolder(contentFolders[i]);
			baseService.merge(users[i]);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllContentFolders().size());
		assertTrue(developmentService.getAllContentFolders().containsAll(Arrays.asList(contentFolders)));
	}

	/**
	 * Tests if all presentation folders were found
	 */
	@Test
	@Order(4)
	void getAllPresentationFolders() {

		for (PresentationFolder presentationFolder : presentationFolders) {
			baseService.persist(presentationFolder);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllPresentationFolders().size());
		assertTrue(
				developmentService.getAllPresentationFolders().containsAll(Arrays.asList(presentationFolders)));
	}

	/**
	 * Tests if all exercises were found
	 */
	@Test
	@Order(5)
	void getAllExercises() {

		for (int i = 0; i < COUNT_OBJECTS; i++) {
			contentFolders[i].addChildExercise(exercises[i]);
			baseService.persist(exercises[i]);
			contentFolders[i] = baseService.merge(contentFolders[i]);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllExercises().size());
		assertTrue(developmentService.getAllExercises().containsAll(Arrays.asList(exercises)));
	}

	/**
	 * Tests if all courses were found
	 */
	@Test
	@Order(6)
	void getAllCourses() {

		for (int i = 0; i < COUNT_OBJECTS; i++) {
			contentFolders[i] = folderService.getContentFolderWithLazyData(contentFolders[i]);
			contentFolders[i].addChildCourse(courses[i]);
			baseService.persist(courses[i]);
			contentFolders[i] = baseService.merge(contentFolders[i]);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllCourses().size());
		assertTrue(developmentService.getAllCourses().containsAll(Arrays.asList(courses)));
	}

	/**
	 * Tests if all course resources were found
	 */
	@Test
	@Order(7)
	void getAllCourseResources() {

		for (int i = 0; i < COUNT_OBJECTS; i++) {
			baseService.persist(resources[i]);
			courses[i].addCourseResource(resources[i]);
			courses[i] = baseService.merge(courses[i]);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllCourseResources().size());
		assertTrue(developmentService.getAllCourseResources().containsAll(Arrays.asList(resources)));
	}

	/**
	 * Tests if all course offers were found
	 */
	@Test
	@Order(8)
	void getAllCourseOffers() {

		for (CourseOffer courseOffer : offers) {
			baseService.persist(courseOffer);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllCourseOffers().size());
		assertTrue(developmentService.getAllCourseOffers().containsAll(Arrays.asList(offers)));
	}

	/**
	 * Tests if all course records were found
	 */
	@Test
	@Order(9)
	void getAllCourseRecords() {

		for (CourseRecord courseRecord : records) {
			baseService.persist(courseRecord);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllCourseRecords().size());
		assertTrue(developmentService.getAllCourseRecords().containsAll(Arrays.asList(records)));
	}

	/**
	 * Tests if all submissions were found
	 */
	@Test
	@Order(10)
	void getAllSubmissions() {

		for (Submission submission : submissions) {
			baseService.persist(submission);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllSubmissions().size());
		assertTrue(developmentService.getAllSubmissions().containsAll(Arrays.asList(submissions)));
	}

	@Test
	@Order(11)
	void getAllExerciseProviders() {

		for (AbstractExerciseProvider exerciseProvider : exerciseProviders) {
			baseService.persist(exerciseProvider);
		}

		assertEquals(COUNT_OBJECTS, developmentService.getAllExerciseProviders().size());
		assertTrue(developmentService.getAllExerciseProviders().containsAll(Arrays.asList(exerciseProviders)));
	}

	@Test
	@Order(12)
	void getAllCourseEntries() {
		courses[0].setContentProvider(new FixedListExerciseProvider());

		for (CourseEntry courseEntry : courseEntries) {
			((FixedListExerciseProvider) courses[0].getContentProvider()).addCourseEntry(courseEntry);
		}

		baseService.merge(courses[0]);

		assertEquals(COUNT_OBJECTS, developmentService.getAllCourseEntries().size());
	}

	/**
	 * Tests if all entities were deleted
	 */
	@Test
	@Order(13)
	void deleteEntities() {

		developmentService.deleteTenantDatabase(EDatabaseType.H2);

		assertTrue(developmentService.getAllUsers().isEmpty());
		assertTrue(developmentService.getAllUserGroups().isEmpty());

		assertTrue(developmentService.getAllContentFolders().isEmpty());
		assertTrue(developmentService.getAllPresentationFolders().isEmpty());

		assertTrue(developmentService.getAllExercises().isEmpty());
		assertTrue(developmentService.getAllCourses().isEmpty());
		assertTrue(developmentService.getAllCourseResources().isEmpty());
		assertTrue(developmentService.getAllCourseOffers().isEmpty());
		assertTrue(developmentService.getAllCourseRecords().isEmpty());
		assertTrue(developmentService.getAllSubmissions().isEmpty());
		assertTrue(developmentService.getAllCourseEntries().isEmpty());
		assertTrue(developmentService.getAllExerciseProviders().isEmpty());
	}
}
