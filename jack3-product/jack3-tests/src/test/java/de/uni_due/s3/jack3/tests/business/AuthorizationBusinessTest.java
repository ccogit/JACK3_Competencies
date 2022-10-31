package de.uni_due.s3.jack3.tests.business;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.helpers.ECourseOfferAccess;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Tests for authorizationBusiness
 *
 * @author kilian.kraus
 *
 */
class AuthorizationBusinessTest extends AbstractBusinessTest {

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	private User admin;
	private User lecturerWithFullRights;
	private User lecturerWithReadWriteRights;
	private User lecturerWithReadRights;
	private User lecturerWithExtendedReadRights;
	private User lecturerWithoutRights;
	private User student1;
	private User student2;
	private ContentFolder contentFolder;
	private PresentationFolder presFolder;
	private AbstractCourse course;
	private AbstractExercise exercise;
	private CourseOffer courseOffer;

	@BeforeEach
	void prepareTest() throws ActionNotAllowedException {
		admin = getAdmin("admin");
		lecturerWithFullRights = getLecturer("lecturer1");
		lecturerWithReadWriteRights = getLecturer("lecturer2");
		lecturerWithReadRights = getLecturer("lecturer3");
		lecturerWithExtendedReadRights = getLecturer("lecturer4");
		lecturerWithoutRights = getLecturer("lecturer5");
		student1 = getStudent("student1");
		student2 = getStudent("student2");

		contentFolder = folderBusiness.getContentFolderWithLazyData(lecturerWithFullRights.getPersonalFolder());
		folderBusiness.updateFolderRightsForUser(contentFolder, lecturerWithReadWriteRights,
				AccessRight.getFromFlags(READ, WRITE));
		folderBusiness.updateFolderRightsForUser(contentFolder, lecturerWithReadRights, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(contentFolder, lecturerWithExtendedReadRights,
				AccessRight.getFromFlags(EXTENDED_READ));

		presFolder = folderBusiness.getPresentationRoot();
		folderBusiness.updateFolderRightsForUser(presFolder, lecturerWithFullRights, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUser(presFolder, lecturerWithReadWriteRights,
				AccessRight.getFromFlags(READ, WRITE));
		folderBusiness.updateFolderRightsForUser(presFolder, lecturerWithReadRights,
				AccessRight.getFromFlags(AccessRight.READ));
		folderBusiness.updateFolderRightsForUser(presFolder, lecturerWithExtendedReadRights,
				AccessRight.getFromFlags(EXTENDED_READ));

		course = courseBusiness.createCourse("Course", lecturerWithFullRights, contentFolder);
		exercise = exerciseBusiness.createExercise("Exercise", lecturerWithFullRights, contentFolder, "DE");
		courseOffer = courseBusiness.createCourseOffer("CourseOffer", course, presFolder, lecturerWithFullRights);
		courseOffer.setCanBeVisible(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
	}

	@Test
	void isAllowedToDeleteCourseOfferTest() {
		assertTrue(authorizationBusiness.isAllowedToDeleteCourseOffer(lecturerWithFullRights, courseOffer));
		assertTrue(authorizationBusiness.isAllowedToDeleteCourseOffer(lecturerWithReadWriteRights, courseOffer));

		assertFalse(authorizationBusiness.isAllowedToDeleteCourseOffer(lecturerWithReadRights, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseOffer(lecturerWithExtendedReadRights, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseOffer(lecturerWithoutRights, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseOffer(student1, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseOffer(admin, courseOffer));
	}

	@Test
	void isAllowedToDeleteCourseRecordTest() {
		final CourseRecord standardCourseRecord = courseBusiness.createCourseRecord(student1, courseOffer);
		assertTrue(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithFullRights, standardCourseRecord,
				courseOffer));
		assertFalse(
				authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithReadWriteRights, standardCourseRecord,
						courseOffer));

		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithReadRights, standardCourseRecord,
				courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithExtendedReadRights,
				standardCourseRecord, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithoutRights, standardCourseRecord,
				courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(student1, standardCourseRecord, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(admin, standardCourseRecord, courseOffer));

		final CourseRecord testCourseRecord = courseBusiness.createTestCourseRecord(lecturerWithReadWriteRights,
				course);
		assertTrue(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithFullRights, testCourseRecord,
				courseOffer));
		assertTrue(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithReadWriteRights, testCourseRecord,
				courseOffer));

		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithReadRights, standardCourseRecord,
				courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithExtendedReadRights,
				standardCourseRecord, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(lecturerWithoutRights, standardCourseRecord,
				courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(student1, testCourseRecord, courseOffer));
		assertFalse(authorizationBusiness.isAllowedToDeleteCourseRecord(admin, testCourseRecord, courseOffer));
	}

	@Test
	void isAllowedToDeleteTestSubmissionsInCourseTest() {
		assertTrue(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(lecturerWithFullRights, course));
		assertTrue(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(lecturerWithReadWriteRights, course));

		assertTrue(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(lecturerWithReadRights, course));
		assertTrue(
				authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(lecturerWithExtendedReadRights, course));
		assertFalse(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(lecturerWithoutRights, course));
		assertFalse(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(student1, course));
		assertFalse(authorizationBusiness.isAllowedToDeleteTestSubmissionsInCourse(admin, course));
	}

	@Test
	void isStudentAllowedToSeeCourseRecordSubmissionsTest() {
		CourseRecord courseRecordForStudent1 = courseBusiness.createCourseRecord(student1, courseOffer);
		courseOffer.setReviewMode(ECourseOfferReviewMode.ALWAYS);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);

		assertTrue(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student1, courseRecordForStudent1));
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student2, courseRecordForStudent1));

		courseOffer.setReviewMode(ECourseOfferReviewMode.NEVER);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		courseRecordForStudent1 = courseBusiness.getCourseRecordById(courseRecordForStudent1.getId());
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student1, courseRecordForStudent1));
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student2, courseRecordForStudent1));

		courseOffer.setReviewMode(ECourseOfferReviewMode.AFTER_EXIT);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		courseRecordForStudent1 = courseBusiness.getCourseRecordById(courseRecordForStudent1.getId());
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student1, courseRecordForStudent1));
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student2, courseRecordForStudent1));

		courseOffer.setReviewMode(ECourseOfferReviewMode.AFTER_END);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		courseRecordForStudent1 = courseBusiness.getCourseRecordById(courseRecordForStudent1.getId());
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student1, courseRecordForStudent1));
		assertFalse(
				authorizationBusiness.isStudentAllowedToSeeCourseRecordSubmissions(student2, courseRecordForStudent1));
	}

	@Test
	void isStudentAllowedToSeeFeedbackForSubmissionTest() {
		CourseRecord courseRecord = courseBusiness.createCourseRecord(student1, courseOffer);
		Submission submission = new Submission(student1, exercise, courseRecord, false);
		baseService.persist(submission);

		courseOffer.setShowFeedbackInCourseResults(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertTrue(authorizationBusiness.isStudentAllowedToSeeFeedbackForSubmission(student1, submission));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeFeedbackForSubmission(student2, submission));

		courseOffer.setShowFeedbackInCourseResults(false);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertFalse(authorizationBusiness.isStudentAllowedToSeeFeedbackForSubmission(student1, submission));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeFeedbackForSubmission(student2, submission));
	}

	@Test
	void isStudentAllowedToSeeResultForSubmissionTest() {
		CourseRecord courseRecord = courseBusiness.createCourseRecord(student1, courseOffer);
		Submission submission = new Submission(student1, exercise, courseRecord, false);
		baseService.persist(submission);

		courseOffer.setShowResultInCourseResults(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertTrue(authorizationBusiness.isStudentAllowedToSeeResultForSubmission(student1, submission));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForSubmission(student2, submission));

		courseOffer.setShowResultInCourseResults(false);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForSubmission(student1, submission));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForSubmission(student2, submission));
	}

	@Test
	void isStudentAllowedToSeeResultForCourseRecordTest() {
		CourseRecord courseRecord = courseBusiness.createCourseRecord(student1, courseOffer);

		courseOffer.setShowResultInCourseResults(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertTrue(authorizationBusiness.isStudentAllowedToSeeResultForCourseRecord(student1, courseRecord));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForCourseRecord(student2, courseRecord));

		courseOffer.setShowResultInCourseResults(false);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForCourseRecord(student1, courseRecord));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeResultForCourseRecord(student2, courseRecord));
	}

	@Test
	void isStudentAllowedToSeeDetailsForCourseRecordTest() {
		CourseRecord courseRecord = courseBusiness.createCourseRecord(student1, courseOffer);

		courseOffer.setShowExerciseAndSubmissionInCourseResults(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertTrue(authorizationBusiness.isStudentAllowedToSeeDetailsForCourseRecord(student1, courseRecord));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeDetailsForCourseRecord(student2, courseRecord));

		courseOffer.setShowExerciseAndSubmissionInCourseResults(false);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		assertFalse(authorizationBusiness.isStudentAllowedToSeeDetailsForCourseRecord(student1, courseRecord));
		assertFalse(authorizationBusiness.isStudentAllowedToSeeDetailsForCourseRecord(student2, courseRecord));
	}

	@Test
	void isAllowedToEditFolderTest() {
		//PresentationFolder
		assertTrue(authorizationBusiness.isAllowedToEditFolder(lecturerWithFullRights, presFolder));
		assertTrue(authorizationBusiness.isAllowedToEditFolder(lecturerWithReadWriteRights, presFolder));

		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithReadRights, presFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithExtendedReadRights, presFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithoutRights, presFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(student1, presFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(admin, presFolder));

		//ContentFolder
		assertTrue(authorizationBusiness.isAllowedToEditFolder(lecturerWithFullRights, contentFolder));
		assertTrue(authorizationBusiness.isAllowedToEditFolder(lecturerWithReadWriteRights, contentFolder));

		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithReadRights, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithExtendedReadRights, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(lecturerWithoutRights, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(student1, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToEditFolder(admin, contentFolder));
	}

	@Test
	void isAllowedToReadFromFolderTest() {
		//PresentationFolder
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithFullRights, presFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithReadWriteRights, presFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithReadRights, presFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithExtendedReadRights, presFolder));

		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithoutRights, presFolder));
		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(student1, presFolder));
		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(admin, presFolder));

		//ContentFolder
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithFullRights, contentFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithReadWriteRights, contentFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithReadRights, contentFolder));
		assertTrue(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithExtendedReadRights, contentFolder));

		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(lecturerWithoutRights, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(student1, contentFolder));
		assertFalse(authorizationBusiness.isAllowedToReadFromFolder(admin, contentFolder));
	}

	@Test
	void hasExtendedReadOnFolderTest() {
		//PresentationFolder
		assertTrue(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithFullRights, presFolder));
		assertTrue(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithExtendedReadRights, presFolder));

		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithReadWriteRights, presFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithReadRights, presFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithoutRights, presFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(student1, presFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(admin, presFolder));

		//ContentFolder
		assertTrue(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithFullRights, contentFolder));
		assertTrue(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithExtendedReadRights, contentFolder));

		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithReadWriteRights, contentFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithReadRights, contentFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(lecturerWithoutRights, contentFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(student1, contentFolder));
		assertFalse(authorizationBusiness.hasExtendedReadOnFolder(admin, contentFolder));
	}

	@Test
	void getCourseOfferVisibilityForUserTest() {
		assertEquals(ECourseOfferAccess.EDIT,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithFullRights, courseOffer));
		assertEquals(ECourseOfferAccess.EDIT,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithReadWriteRights, courseOffer));
		assertEquals(ECourseOfferAccess.READ,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithExtendedReadRights, courseOffer));
		assertEquals(ECourseOfferAccess.READ,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithReadRights, courseOffer));

		assertEquals(ECourseOfferAccess.SEE_AS_STUDENT,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithoutRights, courseOffer));
		assertEquals(ECourseOfferAccess.SEE_AS_STUDENT,
				authorizationBusiness.getCourseOfferVisibilityForUser(admin, courseOffer));
		assertEquals(ECourseOfferAccess.SEE_AS_STUDENT,
				authorizationBusiness.getCourseOfferVisibilityForUser(student1, courseOffer));

		//the courseOffer is not visible anymore. Because of this students shoudln't see the courseOffer anymore.
		courseOffer.setCanBeVisible(false);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);

		assertEquals(ECourseOfferAccess.EDIT,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithFullRights, courseOffer));
		assertEquals(ECourseOfferAccess.EDIT,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithReadWriteRights, courseOffer));
		assertEquals(ECourseOfferAccess.READ,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithExtendedReadRights, courseOffer));
		assertEquals(ECourseOfferAccess.READ,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithReadRights, courseOffer));

		assertEquals(ECourseOfferAccess.NONE,
				authorizationBusiness.getCourseOfferVisibilityForUser(lecturerWithoutRights, courseOffer));
		assertEquals(ECourseOfferAccess.NONE,
				authorizationBusiness.getCourseOfferVisibilityForUser(admin, courseOffer));
		assertEquals(ECourseOfferAccess.NONE,
				authorizationBusiness.getCourseOfferVisibilityForUser(student1, courseOffer));
	}
	
	@Test
	void isAllowedToSeeFolders() {
		List<Folder> foldersToTest = new ArrayList<Folder>();
		foldersToTest.add(contentFolder);
		foldersToTest.add(presFolder);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForAdmin = authorizationBusiness.isAllowedToSeeFolders(admin, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForLecFull = authorizationBusiness.isAllowedToSeeFolders(lecturerWithFullRights, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForLecRW = authorizationBusiness.isAllowedToSeeFolders(lecturerWithReadWriteRights, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForLecRead = authorizationBusiness.isAllowedToSeeFolders(lecturerWithReadRights, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForLecExtRead = authorizationBusiness.isAllowedToSeeFolders(lecturerWithExtendedReadRights, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForLecWoRight = authorizationBusiness.isAllowedToSeeFolders(lecturerWithoutRights, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForStud1 = authorizationBusiness.isAllowedToSeeFolders(student1, foldersToTest);
		Map<Folder, Boolean> folderIsAllowedToSeeMapForStud2 = authorizationBusiness.isAllowedToSeeFolders(student2, foldersToTest);
		
		//presFolder
		assertTrue(folderIsAllowedToSeeMapForLecFull.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForLecRW.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForLecRead.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForLecExtRead.get(presFolder));
		
		assertTrue(folderIsAllowedToSeeMapForAdmin.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForLecWoRight.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForStud1.get(presFolder));
		assertTrue(folderIsAllowedToSeeMapForStud2.get(presFolder));

		
		//contentFolder
		assertTrue(folderIsAllowedToSeeMapForLecFull.get(contentFolder));
		assertTrue(folderIsAllowedToSeeMapForLecRW.get(contentFolder));
		assertTrue(folderIsAllowedToSeeMapForLecRead.get(contentFolder));
		assertTrue(folderIsAllowedToSeeMapForLecExtRead.get(contentFolder));
		
		assertFalse(folderIsAllowedToSeeMapForAdmin.get(contentFolder));
		assertFalse(folderIsAllowedToSeeMapForLecWoRight.get(contentFolder));
		assertFalse(folderIsAllowedToSeeMapForStud1.get(contentFolder));
		assertFalse(folderIsAllowedToSeeMapForStud2.get(contentFolder));
		

	}

}
