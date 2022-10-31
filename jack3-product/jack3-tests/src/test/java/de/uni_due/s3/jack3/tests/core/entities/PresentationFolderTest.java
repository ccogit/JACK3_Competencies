package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class PresentationFolderTest extends AbstractBasicTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUser();
		persistPresentationFolder();
		presentationFolder = folderService.getPresentationFolderWithLazyData(presentationFolder);
	}

	/**
	 * Add a course offer to a presentation folder and return the merged folder
	 */
	private PresentationFolder addOfferToFolder(PresentationFolder root, CourseOffer offer) {
		root.addChildCourseOffer(offer);
		baseService.persist(offer);
		return folderService.mergePresentationFolder(root);
	}

	/**
	 * Add an other presentation folder and return the merged folder
	 */
	private PresentationFolder addFolderToFolder(PresentationFolder root, PresentationFolder folder) {
		root.addChildFolder(folder);
		folderService.persistFolder(folder);
		return folderService.mergePresentationFolder(root);
	}

	@Test
	void insertCourseOffer() {
		CourseOffer offer = new CourseOffer("Course Offer", null);
		presentationFolder = addOfferToFolder(presentationFolder, offer);

		assertTrue(presentationFolder.getChildrenCourseOffer().contains(offer));
	}

	@Test
	void removeCourseOffer() {
		CourseOffer offer = new CourseOffer("Course Offer", null);
		presentationFolder = addOfferToFolder(presentationFolder, offer);

		presentationFolder.removeCourseOffer(offer);
		presentationFolder = folderService.mergePresentationFolder(presentationFolder);
		baseService.deleteEntity(offer);

		presentationFolder = folderService.getPresentationFolderWithLazyData(presentationFolder);
		assertFalse(presentationFolder.getChildrenCourseOffer().contains(offer));
	}

	@Test
	void insertPresentationFolder() {
		PresentationFolder subFolder = TestDataFactory.getPresentationFolder("Sub Folder", null);
		presentationFolder = addFolderToFolder(presentationFolder, subFolder);

		assertTrue(presentationFolder.getChildrenFolder().contains(subFolder));
	}

	@Test
	void removePresentationFolder() {
		PresentationFolder subFolder = TestDataFactory.getPresentationFolder("Sub Folder", null);
		presentationFolder = addFolderToFolder(presentationFolder, subFolder);

		presentationFolder.removeChildFolder(subFolder);
		presentationFolder = folderService.mergePresentationFolder(presentationFolder);
		folderService.deleteFolder(subFolder);

		presentationFolder = folderService.getPresentationFolderWithLazyData(presentationFolder);
		assertFalse(presentationFolder.getChildrenFolder().contains(subFolder));
	}

}
