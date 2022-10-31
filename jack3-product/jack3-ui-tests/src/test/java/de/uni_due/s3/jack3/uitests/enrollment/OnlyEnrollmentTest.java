package de.uni_due.s3.jack3.uitests.enrollment;

import static de.uni_due.s3.jack3.uitests.utils.Find.collectElementIDs;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.waitUntilPageHasLoaded;
import static de.uni_due.s3.jack3.uitests.utils.Student.openCourseOffer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.graphene.Graphene;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Driver;

/**
 * Tests the enrollment features without a course.
 * 
 * @author lukas.glaser
 */
class OnlyEnrollmentTest extends AbstractSeleniumTest {

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private EnrollmentService enrollmentService;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();

		final User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);
		userBusiness.createUser("student", "secret", "student@foobar.com", false, false);

		final PresentationFolder root = folderBusiness.getPresentationRoot();
		final PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote", root);
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());

		final CourseOffer offer = courseBusiness.createCourseOffer("Kursangebot", null, offerFolder, lecturer);
		offer.setExplicitEnrollment(true);
		offer.setCanBeVisible(true);
		courseBusiness.updateCourseOffer(offer);
	}

	@Test
	@Order(1)
	void assertNotRegistered1() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("student").orElseThrow(AssertionError::new);

		assertFalse(enrollmentBusiness.getEnrollment(student, courseOffer).isPresent());
	}

	@Test
	@Order(2)
	@RunAsClient
	void enterMainMenu() {
		login("student", "secret");
		openCourseOffer(0, 0);

		// This is a another way to assert the invisibility of elements. The test runs faster than with assertNotVisible
		Set<String> ids = collectElementIDs(find(By.id("page-content")));

		// Only the enrollment panel should be shown because user is not enrolled
		assertTrue(ids.contains("headerForm"), "Header was not shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentPanel"), "Enrollment panel was not shown");
		assertFalse(ids.contains("submissionForm:submissionPanel"), "Submission panel was shown");
		assertFalse(ids.contains("reviewForm:reviewTable"), "Review table was shown");

		// Only the not interacted info box should be shown
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");

		assertTrue(ids.contains("enrollmentForm:enrollAction"), "Enroll button was not shown");
	}

	@Test
	@Order(3)
	void assertNotRegistered2() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("student").orElseThrow(AssertionError::new);

		assertFalse(enrollmentBusiness.getEnrollment(student, courseOffer).isPresent());
	}

	@Test
	@Order(4)
	@RunAsClient
	void enrollUser() {
		assumeLogin();

		// Test if enrollment info is shown when the user enrolls himself
		Graphene.guardAjax(find(By.id("enrollmentForm:enrollAction"))).click();

		// This is a another way to assert the invisibility of elements. The test runs faster than with assertNotVisible
		Set<String> ids = collectElementIDs(find(By.id("page-content")));

		// Only the enrollment panel should be shown because the course offer has no course
		assertTrue(ids.contains("headerForm"), "Header was not shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentPanel"), "Enrollment panel was not shown");
		assertFalse(ids.contains("submissionForm:submissionPanel"), "Submission panel was shown");
		assertFalse(ids.contains("reviewForm:reviewTable"), "Review table was shown");

		// Only the enrollment info box should be shown
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");

		assertTrue(ids.contains("enrollmentForm:disenrollAction"), "Disenroll button was not shown");
	}

	@Test
	@Order(5)
	void assertEnrolled() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("student").orElseThrow(AssertionError::new);

		assertTrue(enrollmentBusiness.isEnrolled(student, courseOffer));
	}

	@Test
	@Order(6)
	@RunAsClient
	void disenrollUser() {
		assumeLogin();

		// Test if disenrollment info is shown when the user disenrolls himself
		Graphene.guardAjax(find(By.id("enrollmentForm:disenrollAction"))).click();

		// This is a another way to assert the invisibility of elements. The test runs faster than with assertNotVisible
		Set<String> ids = collectElementIDs(find(By.id("page-content")));

		// Only the enrollment panel should be shown because the course offer has no course
		assertTrue(ids.contains("headerForm"), "Header was not shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentPanel"), "Enrollment panel was not shown");
		assertFalse(ids.contains("submissionForm:submissionPanel"), "Submission panel was shown");
		assertFalse(ids.contains("reviewForm:reviewTable"), "Review table was shown");

		// Only the enrollment info box should be shown
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was not shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");

		assertTrue(ids.contains("enrollmentForm:enrollAction"), "Enroll button was not shown");
	}

	@Test
	@Order(7)
	void assertDisenrolled() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("student").orElseThrow(AssertionError::new);

		assertTrue(enrollmentBusiness.getEnrollment(student, courseOffer).isPresent());
		assertTrue(enrollmentBusiness.isDisenrolled(student, courseOffer));
	}

	@Test
	@Order(8)
	void limitMaxParticipants() throws Exception { // NOSONAR

		// First we delete all enrollments to "start fresh"
		CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		for (Enrollment enrollment : enrollmentService.getEnrollments(courseOffer)) {
			enrollmentService.deleteEnrollment(enrollment);
		}

		courseOffer.setMaxAllowedParticipants(1);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
		enrollmentBusiness.enrollUser(userBusiness.getUserByName("student").orElseThrow(AssertionError::new),
				courseOffer);

		// Create a new student for enrollment check
		userBusiness.createUser("newstudent", "secret", "newstudent@foobar.com", false, false);
	}

	@Test
	@Order(9)
	@RunAsClient
	void enrollmentFail() {
		login("newstudent", "secret");
		openCourseOffer(0, 0);

		// No actions available
		Set<String> ids = collectElementIDs(find(By.id("page-content")));
		assertFalse(ids.contains("enrollmentForm:globalPasswortInput"), "Password input was shown");
		assertFalse(ids.contains("enrollmentForm:enrollAction"), "Enroll button was shown");
		assertFalse(ids.contains("enrollmentForm:joinWaitingListAction"), "Waiting list button was shown");
		assertFalse(ids.contains("disenrollAction"), "Disenroll button was shown");

		// Not interacted info should be shown
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");
	}

	@Test
	@Order(10)
	void enableWaitingList() throws Exception { // NOSONAR
		CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		courseOffer.setEnableWaitingList(true);
		courseOffer = courseBusiness.updateCourseOffer(courseOffer);
	}

	@Test
	@Order(11)
	@RunAsClient
	void joinWaitingList() {
		assumeLogin();

		// Reload the current page
		Driver.get().navigate().refresh();
		waitUntilPageHasLoaded();
		Set<String> ids = collectElementIDs(find(By.id("page-content")));

		// Not interacted info should be shown
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");

		// Waiting list button should be shown
		assertFalse(ids.contains("enrollmentForm:globalPasswortInput"), "Password input was shown");
		assertFalse(ids.contains("enrollmentForm:enrollAction"), "Enroll button was shown");
		assertTrue(ids.contains("enrollmentForm:joinWaitingListAction"), "Waiting list button was not shown");
		assertFalse(ids.contains("enrollmentForm:disenrollAction"), "Disenroll button was shown");

		// User joins the waiting list
		Graphene.guardAjax(find(By.id("enrollmentForm:joinWaitingListAction"))).click();
		ids = collectElementIDs(find(By.id("page-content")));

		// Now the student should be on waiting list and the waiting list info should be shown
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was not shown");

		// Disenroll button should be shown
		assertFalse(ids.contains("enrollmentForm:globalPasswortInput"), "Password input was shown");
		assertFalse(ids.contains("enrollmentForm:enrollAction"), "Enroll button was shown");
		assertFalse(ids.contains("enrollmentForm:joinWaitingListAction"), "Waiting list button was shown");
		assertTrue(ids.contains("enrollmentForm:disenrollAction"), "Disenroll button was not shown");
	}

	@Test
	@Order(12)
	void assertOnWaitingList() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("newstudent").orElseThrow(AssertionError::new);

		assertTrue(enrollmentBusiness.getEnrollment(student, courseOffer).isPresent());
		assertTrue(enrollmentBusiness.isOnWaitingList(student, courseOffer));
	}

	@Test
	@Order(13)
	@RunAsClient
	void disenrollFromWaitingList() {
		Graphene.guardAjax(find(By.id("enrollmentForm:disenrollAction"))).click();
		Set<String> ids = collectElementIDs(find(By.id("page-content")));

		// Now the student should be disenrolled again
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarNotInteracted"), "Not interacted info was shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarEnrolled"), "Enrollment info was shown");
		assertTrue(ids.contains("enrollmentForm:enrollmentInfoBarDisenrolled"), "Disenrollment info was not shown");
		assertFalse(ids.contains("enrollmentForm:enrollmentInfoBarWaitingList"), "Waiting list info was shown");

		// Waiting list button should be shown again because the course is still full and the student is not enrolled
		assertFalse(ids.contains("enrollmentForm:globalPasswortInput"), "Password input was shown");
		assertFalse(ids.contains("enrollmentForm:enrollAction"), "Enroll button was shown");
		assertTrue(ids.contains("enrollmentForm:joinWaitingListAction"), "Waiting list button was not shown");
		assertFalse(ids.contains("enrollmentForm:disenrollAction"), "Disenroll button was shown");
	}

	@Test
	@Order(14)
	void assertDisenrolledAfterLeavingWaitingList() {
		final CourseOffer courseOffer = courseBusiness.getAllCourseOffers().get(0);
		final User student = userBusiness.getUserByName("newstudent").orElseThrow(AssertionError::new);

		assertTrue(enrollmentBusiness.getEnrollment(student, courseOffer).isPresent());
		assertTrue(enrollmentBusiness.isDisenrolled(student, courseOffer));
	}

}
