package de.uni_due.s3.jack3.tests.business.enrollment;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Abstract class providing utilities for tests of the course registration feature.
 * 
 * @author lukas.glaser
 */
public abstract class AbstractEnrollmentBusinessTest extends AbstractBusinessTest {

	protected User lecturer;
	protected CourseOffer offer;
	protected PresentationFolder folder;
	private ContentFolder contentFolder;

	@Inject
	protected EnrollmentBusiness enrollmentBusiness;

	@Inject
	protected FolderBusiness folderBusiness;

	@Override
	@BeforeEach
	protected void beforeTest() {
		super.beforeTest();
		lecturer = getLecturer("lecturer");

		folder = TestDataFactory.getPresentationFolder("Folder", null);
		baseService.persist(folder);
		offer = getCourseOffer("Course Offer");
		// By default nothing is explicit (only in the tests!)
		offer.setExplicitEnrollment(false);
		offer.setCanBeVisible(true);
		offer = baseService.merge(offer);

		folderBusiness.updateFolderRightsForUser(folder, lecturer, AccessRight.getFull());
	}

	protected CourseOffer getCourseOffer(String name) {
		final CourseOffer courseOffer = new CourseOffer(name, null);
		courseOffer.setCanBeVisible(true);
		folder.addChildCourseOffer(courseOffer);
		folder = baseService.merge(folder);
		baseService.persist(courseOffer);
		return courseOffer;
	}

	protected CourseOffer getCourseOffer(String name, PresentationFolder folder) {
		final CourseOffer courseOffer = new CourseOffer(name, null);
		courseOffer.setCanBeVisible(true);
		folder.addChildCourseOffer(courseOffer);
		folder = baseService.merge(folder);
		baseService.persist(courseOffer);
		return courseOffer;
	}

	protected PresentationFolder getSubfolder(String name) {
		folder = folderBusiness.getPresentationFolderWithLazyData(folder);
		final PresentationFolder subfolder = new PresentationFolder(name);
		folder.addChildFolder(subfolder);
		folder = baseService.merge(folder);
		baseService.persist(subfolder);
		return subfolder;
	}

	protected Course getCourse(String name) {
		final Course course = new Course(name);
		if (contentFolder == null) {
			contentFolder = TestDataFactory.getContentFolder("Folder", null);
			baseService.persist(contentFolder);
		}
		contentFolder.addChildCourse(course);
		baseService.merge(contentFolder);
		baseService.persist(course);
		return course;
	}

}
