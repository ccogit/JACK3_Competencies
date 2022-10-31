package de.uni_due.s3.jack3.tests.business;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.CourseBuilder;
import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.CoursePlayerBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Tests for CourseBusiness (courses, course offers and course records)
 *
 * @author lukas.glaser
 *
 */
class CourseBusinessTest extends AbstractBusinessTest {

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private CoursePlayerBusiness coursePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	private User user;
	private ContentFolder folder;
	private PresentationFolder presFolder;
	private Course course1;

	// We need a user counter to get unique user names for persisting new test users in this test.
	private static int userCount = 0;

	/**
	 * Prepare tests: Create user/folder (if necessary) and an empty course
	 * @throws ActionNotAllowedException
	 */
	@BeforeEach
	void prepareTest() throws ActionNotAllowedException {

		// Persist a default user if neccessary
		user = getAdmin("user");

		folder = folderBusiness.getContentFolderWithLazyData(user.getPersonalFolder());
		presFolder = folderBusiness.getPresentationRoot();
		course1 = courseBusiness.createCourse("Course", user, folder);
	}

	/**
	 * Creates a new test user with a unique name.
	 */
	private User createNewTestUser() {
		return getStudent("testuser" + ++userCount);
	}

	/**
	 * Takes the abstractCourse course and creates a FrozenRevision of it. If Necessary the ContentProvider will be set
	 * to a FixedListExerciseProvider.
	 *
	 * @return returns the created FrozenCourse
	 */
	private FrozenCourse createFrozenCourse() {
		// create a Frozen Course
		if ((course1.getContentProvider() == null)
				|| !(course1.getContentProvider() instanceof FixedListExerciseProvider)) {
			course1.setContentProvider(new FixedListExerciseProvider());
		}
		course1 = (Course) courseBusiness.updateCourse(course1);
		courseBusiness.createFrozenCourse(course1, courseBusiness.getRevisionNumbersFor(course1)
				.get(courseBusiness.getRevisionNumbersFor(course1).size() - 1));
		List<FrozenCourse> revisions = courseBusiness.getFrozenRevisionsForCourse(course1);
		return revisions.get(revisions.size() - 1);
	}

	private Exercise addSampleStageToExercise(Exercise exercise) {
		exercise = new ExerciseBuilder(exercise).withSampleMCStage().create();
		exercise = (Exercise) exerciseBusiness.updateExercise(exercise);
		return exerciseBusiness.getExerciseWithLazyDataByExerciseId(exercise.getId());
	}

	/**
	 * Tests moving a course to another folder.
	 *
	 * The course should be removed from the original folder and inserted into the new one.
	 * @throws ActionNotAllowedException
	 */
	@Test
	void moveCourse() throws ActionNotAllowedException {
		ContentFolder otherFolder = folderBusiness.createContentFolder(user, "Folder", folder);
		assertDoesNotThrow(() -> {courseBusiness.moveCourse(course1, otherFolder, user);});
		ContentFolder updatedOtherFolder = folderBusiness.getContentFolderWithLazyData(otherFolder);
		Course updatedCourse = courseBusiness.getCourseWithLazyDataByCourseID(course1.getId());
		assertTrue(updatedOtherFolder.getChildrenCourses().contains(updatedCourse));
		assertFalse(folder.getChildrenCourses().contains(updatedCourse));
		assertEquals(updatedOtherFolder, updatedCourse.getFolder());
	}

	/**
	 * Tests deleting a course.
	 */
	@Test
	void deleteCourse() {
		// Delete Frozen Course
		FrozenCourse frozenCourse = createFrozenCourse();
		assertEquals(1, courseBusiness.getFrozenRevisionsForCourse(course1).size());

		// Delete Course
		assertDoesNotThrow(() ->courseBusiness.deleteCourse(course1, user));
		assertFalse(folder.getChildrenCourses().contains(course1));
		assertFalse(courseBusiness.getAllCoursesForUser(user).contains(course1));
	}

	/**
	 * Tests creating a course record belonging to a course offer.
	 *
	 * The new course record should be found by getting all course records.
	 */
	@Test
	void testCourseRecord() {
		CourseOffer offer = courseBusiness.createCourseOffer("Course Offer", course1, presFolder, user);
		// NOTE: We bypass courseBusiness.drawExercisesFromCourse here because we don't need the exercise list!
		CourseRecord record = new CourseRecord(user, offer, course1);
		baseService.persist(record);

		var found = courseBusiness.getCourseRecordById(record.getId());
		assertEquals(record, found);
		assertTrue(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record));
		assertTrue(courseBusiness.getAllCourseRecords(offer).contains(record));

		// create a Frozen Course
		FrozenCourse frozenCourse = createFrozenCourse();

		offer = courseBusiness.createCourseOffer("Course Offer2", frozenCourse, presFolder, user);
		record = new CourseRecord(user, offer, frozenCourse);
		baseService.persist(record);

		found = courseBusiness.getCourseRecordById(record.getId());
		assertEquals(record, found);
		assertTrue(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record));
		assertTrue(courseBusiness.getAllCourseRecords(offer).contains(record));
	}

	@Test
	void testCourseRecordWithSpecifiedNumberOfExercisesFromFolder() throws ActionNotAllowedException {
		//create some exercises and a Course with FolderExerciseProvider
		for (int i = 1; i <= 3; i++) {
			exerciseBusiness.createExercise("exercise" + i, user, folder, "de");
		}
		AbstractCourse course2 = courseBusiness.updateCourse(
				new CourseBuilder("Course2")
				.withFolderExerciseProvider(Collections.singletonList(folder)).build());

		//Create CourseRecord of the new course
		CourseRecord record = courseBusiness.createTestCourseRecord(user, course2);

		//all exercises of the course should be used by default
		assertEquals(3, record.getExercises().size());

		//Only one exercise of the folder should be used
		((FolderExerciseProvider) course2.getContentProvider()).removeFolder(folder);
		((FolderExerciseProvider) course2.getContentProvider()).addFolder(folder, 1);

		courseBusiness.updateCourse(course2);

		CourseRecord record2 = courseBusiness.createTestCourseRecord(user, course2);
		assertEquals(1, record2.getExercises().size());
	}

	/**
	 * Tests creating course offers.
	 *
	 * The new course offer should be found by getting all course offers and by its Id.
	 */
	@Test
	void testCourseOffer() throws ActionNotAllowedException, PasswordRequiredException, MessagingException {
		final User student = createNewTestUser();

		// The user must have rights for deleting the course offer
		folderBusiness.updateFolderRightsForUser(presFolder, user, AccessRight.getFull());

		// Prepare exercise
		Exercise exercise = exerciseBusiness.createExercise("Exercise", user, user.getPersonalFolder(), "de");
		exercise = addSampleStageToExercise(exercise); // Append a demo-stage to the exercise to prevent empty exercise

		// Prepare course offer
		CourseOffer offer1 = courseBusiness.createCourseOffer("Course Offer", course1, presFolder, user);
		offer1.setExplicitSubmission(true);
		offer1.setExplicitEnrollment(true);
		offer1.setCanBeVisible(true);
		offer1 = baseService.merge(offer1);

		// We first enroll the user and then start a new submission
		enrollmentBusiness.enrollUser(student, offer1);
		CourseRecord record1 = enrollmentBusiness.startSubmission(student, offer1, null);

		// Then we create a submission for the exercise and pretend that the student is going to start a submission
		Submission submission1 = exerciseBusiness.createSubmissionForCourseRecord(exercise, student, record1, false,
				false);

		// We also do the preparation steps with a second course offer
		CourseOffer offer2 = courseBusiness.createCourseOffer("Course Offer not for deletion", course1, presFolder,
				user);
		offer2.setExplicitSubmission(true);
		offer2.setExplicitEnrollment(true);
		offer2.setCanBeVisible(true);
		offer2 = baseService.merge(offer2);
		enrollmentBusiness.enrollUser(student, offer2);
		CourseRecord record2 = enrollmentBusiness.startSubmission(student, offer2, null);
		Submission submission2 = exerciseBusiness.createSubmissionForCourseRecord(exercise, student, record2, false,
				false);

		// assert Test preparation
		assertTrue(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record1));
		assertTrue(
				exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)
				.contains(submission1));
		assertTrue(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record2));
		assertTrue(
				exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)
				.contains(submission2));

		// We should have some enrollments for the course offers
		assertEquals(2, baseService.findAll(Enrollment.class).size());

		// Test findability
		assertTrue(courseBusiness.getAllCourseOffers().contains(offer1));
		assertEquals(offer1, courseBusiness.getCourseOfferById(offer1.getId()).orElseThrow(AssertionError::new));
		assertEquals(offer1, courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(offer1.getId())
				.orElseThrow(AssertionError::new));

		// Test CourseOffer deletion
		courseBusiness.deleteCourseOffer(user, offer1);
		assertFalse(courseBusiness.getAllCourseOffers().contains(offer1));
		assertFalse(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record1));
		assertFalse(
				exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)
				.contains(submission1));
		assertTrue(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record2));
		assertTrue(
				exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)
				.contains(submission2));
		assertEquals(1, baseService.findAll(Enrollment.class).size());
		courseBusiness.deleteCourseOffer(user, offer2);
		assertFalse(courseBusiness.getAllCourseRecordsIncludingFrozenCourses(course1).contains(record2));
		assertFalse(
				exerciseBusiness.getAllSubmissionsForExerciseAndFrozenVersions(exercise)
				.contains(submission2));

		// There shouldn't exist any enrollments for the user since we deleted the course offer.
		assertTrue(baseService.findAll(Enrollment.class).isEmpty());

		// create a Frozen Course
		FrozenCourse frozenCourse = createFrozenCourse();

		offer1 = courseBusiness.createCourseOffer("Course Offer2", frozenCourse, presFolder, user);

		assertTrue(courseBusiness.getAllCourseOffers().contains(offer1));
		assertEquals(offer1, courseBusiness.getCourseOfferById(offer1.getId()).orElseThrow(AssertionError::new));
		assertEquals(offer1, courseBusiness.getCourseOfferWithLazyDataByCourseOfferID(offer1.getId())
				.orElseThrow(AssertionError::new));

		courseBusiness.deleteCourseOffer(user, offer1);
		assertFalse(courseBusiness.getAllCourseOffers().contains(offer1));

		exerciseBusiness.deleteExercise(exercise, user);

	}

	/**
	 * Tests submissions for a course record.
	 *
	 * The new submissions should be found by their course record after initializing.
	 */
	@Test
	void testCourseRecordSubmissions() throws Exception {

		// We need a course offer and an exercise to create test submissions.
		CourseOffer offer = courseBusiness.createCourseOffer("Course Offer", course1, presFolder, user);
		CourseRecord record = new CourseRecord(user, offer, course1);
		baseService.persist(record);
		Exercise exercise = exerciseBusiness.createExercise("Exercise", user, user.getPersonalFolder(), "de");
		exercise = addSampleStageToExercise(exercise); // Append a demo-stage to the exercise to prevent empty exercise
		record = coursePlayerBusiness.updateCourseRecord(record);

		// Check if there is no latest submission for course record
		assertFalse(courseBusiness.getLatestSubmissionForCourseRecordAndExercise(record, exercise).isPresent());

		Submission submission1 = exerciseBusiness.createSubmissionForCourseRecord(exercise, user, record, false, false);

		Submission submission2 = exerciseBusiness.createSubmissionForCourseRecord(exercise, user, record, false, false);

		// The created submissions should be found by getting all submissions.
		assertTrue(courseBusiness.getAllSubmissionsForCourseRecord(record).contains(submission1));
		assertTrue(courseBusiness.getAllSubmissionsForCourseRecord(record).contains(submission2));

		// The latest submission for this course record should be submission2,
		// submission1 should be found by getting old submissions.
		assertTrue(courseBusiness.getLatestSubmissionForCourseRecordAndExercise(record, exercise).isPresent());
		assertEquals(submission2, courseBusiness.getLatestSubmissionForCourseRecordAndExercise(record, exercise).get());
		assertFalse(courseBusiness.getOldSubmissionsForCourseRecord(record).contains(submission2));
		assertNotEquals(submission1,
				courseBusiness.getLatestSubmissionForCourseRecordAndExercise(record, exercise).get());
		assertTrue(courseBusiness.getOldSubmissionsForCourseRecord(record).contains(submission1));
	}

	/**
	 * Test course resources.
	 *
	 * It should be possible to create and delete course resources.
	 */
	@Test
	void testCourseResources() {
		CourseResource resource = new CourseResource("File.txt", "Content...".getBytes(), course1, user);
		CourseResource noResource = new CourseResource("WrongFile.txt", "Content...".getBytes(), course1, user);
		course1.addCourseResource(resource);
		course1 = (Course) courseBusiness.updateCourse(course1);

		Set<CourseResource> resourcesFromDB = courseBusiness.getCourseWithLazyDataByCourseID(course1.getId())
				.getCourseResources();
		assertEquals(1, resourcesFromDB.size());
		resource = resourcesFromDB.iterator().next();
		// Course resource should be found.
		assertTrue(courseBusiness.getAllCourseResourcesForCourse(course1).contains(resource));
		assertFalse(courseBusiness.getAllCourseResourcesForCourse(course1).contains(noResource));

		assertTrue(courseBusiness.isCourseResourceFilenameAlreadyExisting("File.txt", course1));
		assertFalse(courseBusiness.isCourseResourceFilenameAlreadyExisting("File_.txt", course1));
		assertFalse(courseBusiness.isCourseResourceFilenameAlreadyExisting("File.pdf", course1));
		assertFalse(courseBusiness.isCourseResourceFilenameAlreadyExisting("WrongFile.txt", course1));

		long resourceId = resource.getId();

		// When a course resource is removed from its course and the course is saved, the resource should be deleted.
		course1.removeCourseResource(resource);
		course1 = (Course) courseBusiness.updateCourse(course1);

		assertTrue(courseBusiness.getAllCourseResourcesForCourse(course1).isEmpty());
		assertTrue(courseBusiness.getCourseWithLazyDataByCourseID(course1.getId()).getCourseResources().isEmpty());
		assertFalse(baseService.findById(CourseResource.class, resourceId, false).isPresent());
		assertTrue(baseService.findAll(CourseResource.class).isEmpty());
	}

	/**
	 * Tests basic revision features (get a specific revision, reset to revision)
	 */
	@Test
	void testCourseRevision() {
		// Give a new name and save it to get a new revision
		course1.setName("Course 2.0");
		course1 = (Course) courseBusiness.updateCourse(course1);
		assertEquals("Course 2.0", course1.getName());

		List<Integer> revisions = courseBusiness.getRevisionNumbersFor(course1);

		// Check specific revisions
		assertEquals("Course", courseBusiness.getRevisionOfCourseWithLazyData(course1, revisions.get(FIRST_REVISION))
				.orElseThrow(AssertionError::new).getName());

		assertEquals("Course 2.0",
				courseBusiness.getRevisionOfCourseWithLazyData(course1, revisions.get(SECOND_REVISION))
				.orElseThrow(AssertionError::new).getName());

		// Reset to first revision
		course1 = (Course) courseBusiness.resetToRevision(course1, revisions.get(FIRST_REVISION), user);
		assertEquals("Course", course1.getName());
	}

	/**
	 * Tests correct filtering of course revisions
	 */
	@Test
	void testLazyCourseRevision() {
		for (int i = 1; i < 50; i++) {
			course1.setName("Course, Revision " + i);
			courseBusiness.updateCourse(course1);
		}
		// we now have 50 course revisions
		course1 = courseBusiness.getCourseWithLazyDataByCourseID(course1.getId());

		List<Course> allRevisions = courseBusiness.getAllRevisionsForCourse(course1);
		assertEquals(50, allRevisions.size());

		// Get a list of all revisions
		assertEquals(allRevisions, courseBusiness.getFilteredRevisionsForCourse(course1, 0, 50, "", "ASC"));

		// Get lists of revisions, splittet into pages with 20 entries
		List<Course> page1 = courseBusiness.getFilteredRevisionsForCourse(course1, 0, 20, "", "ASC");
		List<Course> page2 = courseBusiness.getFilteredRevisionsForCourse(course1, 20, 20, "", "ASC");
		List<Course> page3 = courseBusiness.getFilteredRevisionsForCourse(course1, 40, 20, "", "ASC");

		// Check the size of the lists
		assertEquals(20, page1.size());
		assertEquals(20, page2.size());
		assertEquals(10, page3.size());

		// Check if the lists contains the expected revisions
		int revisionNumber = 1;
		assertEquals("Course", page1.get(0).getName());
		for (int i = 1; i < 20; i++, revisionNumber++) {
			assertEquals("Course, Revision " + revisionNumber, page1.get(i).getName());
		}
		for (int i = 0; i < 20; i++, revisionNumber++) {
			assertEquals("Course, Revision " + revisionNumber, page2.get(i).getName());
		}
		for (int i = 0; i < 10; i++, revisionNumber++) {
			assertEquals("Course, Revision " + revisionNumber, page3.get(i).getName());
		}
	}

	/*-********************************************************
	 *  Ab hier Tests für einzeilige "Proxy"-Methoden         *
	 **********************************************************/

	/**
	 * Tests
	 * {@linkplain CourseBusiness#getBestSubmissionForCourseRecordAndExercise(CourseRecord, de.uni_due.s3.jack3.entities.tenant.AbstractExercise)}
	 * @throws ActionNotAllowedException
	 */
	@Test
	void getBestSubmissionForCourseRecordAndExercise() throws ActionNotAllowedException {

		// Create needed content
		CourseOffer offer = courseBusiness.createCourseOffer("offer", course1, presFolder, user);
		CourseRecord record = new CourseRecord(user, offer, course1);
		baseService.persist(record);
		Exercise exercise = exerciseBusiness.createExercise("exercise", user, folder, "de_DE");

		// Persist a good and a bad submission
		Submission bestSubmission = new Submission(user, exercise, record, false);
		bestSubmission.setResultPoints(75);
		baseService.persist(bestSubmission);
		Submission worstSubmission = new Submission(user, exercise, record, false);
		worstSubmission.setResultPoints(25);
		baseService.persist(worstSubmission);

		Optional<Submission> result = courseBusiness.getBestSubmissionForCourseRecordAndExercise(record, exercise);
		assertTrue(result.isPresent());
		assertEquals(bestSubmission, result.get());
		assertEquals(75, result.get().getResultPoints());

	}

	/**
	 * Tests {@linkplain CourseBusiness#getAllCoursesForContentFolderList(List)}
	 * @throws ActionNotAllowedException
	 */
	@Test
	void getAllCoursesForContentFolderList() throws ActionNotAllowedException {

		//assertDoesNotThrow(() -> {setContentFolder(folderBusiness.createContentFolder(user, "Folder", folder),otherFolder);});

		ContentFolder folder1 = folderBusiness.createContentFolder(user, "Folder1", folder);
		ContentFolder folder2 = folderBusiness.createContentFolder(user, "Folder2", folder);
		ContentFolder folder3 = folderBusiness.createContentFolder(user, "Folder2", folder);

		Course course11 = courseBusiness.createCourse("course1", user, folder1);
		Course course12 = courseBusiness.createCourse("course2", user, folder1);
		Course course21 = courseBusiness.createCourse("course3", user, folder2);
		Course course22 = courseBusiness.createCourse("course4", user, folder2);

		Set<Course> coursesInFolder1 = new HashSet<>(Arrays.asList(course11, course12));
		Set<Course> coursesInFolder2 = new HashSet<>(Arrays.asList(course21, course22));
		Set<Course> coursesInFolder3 = Collections.emptySet();
		Set<Course> coursesInAllFolders = new HashSet<>(Arrays.asList(course11, course12, course21, course22));

		Function<List<ContentFolder>, HashSet<Course>> testQuery = //
				folders -> new HashSet<>(courseBusiness.getAllCoursesForContentFolderList(folders));

				// Assertions
				assertEquals(Collections.emptySet(), testQuery.apply(Collections.emptyList()));

				assertEquals(coursesInFolder1, testQuery.apply(Arrays.asList(folder1)));
				assertEquals(coursesInFolder2, testQuery.apply(Arrays.asList(folder2)));
				assertEquals(coursesInFolder3, testQuery.apply(Arrays.asList(folder3)));

				assertEquals(coursesInAllFolders, testQuery.apply(Arrays.asList(folder1, folder2)));
				assertEquals(coursesInFolder1, testQuery.apply(Arrays.asList(folder1, folder3)));
				assertEquals(coursesInFolder2, testQuery.apply(Arrays.asList(folder2, folder3)));
				assertEquals(coursesInAllFolders, testQuery.apply(Arrays.asList(folder1, folder2, folder3)));
	}

}
