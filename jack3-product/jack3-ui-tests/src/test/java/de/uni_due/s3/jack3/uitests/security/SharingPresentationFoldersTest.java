package de.uni_due.s3.jack3.uitests.security;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.MANAGE;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage.Permission;

/**
 * This tests rights for Presentation Folders based on {@link SharingContentFoldersTest}.
 */
class SharingPresentationFoldersTest extends AbstractSeleniumTest {

	// Folder names
	// NOTE: Because we lookup folders via xpath and 'contains(text()...' (see AvailableCoursesPage#getElement(String)),
	// each folder name MUST NOT contain the name of an other folder. We bypass this problem by adding a unique ID to
	// each folder name.
	private static final String ROOT_FOLDER = "Root 0";
	private static final String MY_FOLDER = "myFolder 1";
	private static final String READ_ONLY_FOLDER = "Read only 2";
	private static final String EXTENDED_READ_FOLDER = "Extended read 3";
	private static final String EDIT_FOLDER = "Edit 4";
	private static final String EDIT_SUBFOLDER = "Edit (subfolder) 5";
	private static final String EDIT_FOLDER_COPY = "Edit (copy) 6";
	private static final String EXTENDED_EDIT_FOLDER = "Extended read and edit 7";
	private static final String MANAGE_FOLDER = "Manage 8";
	private static final String MANAGE_SUBFOLDER = "Manage sub 9";

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderbusiness;

	@Inject
	private CourseBusiness courseBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer1 = userBusiness.createUser("lecturer1", "secret", "lecturer@foobar.com", false, true);
		userBusiness.createUser("lecturer2", "secret", "lecturer@foobar.com", false, true);

		try {
			PresentationFolder root = folderbusiness.createPresentationFolder(ROOT_FOLDER,
					folderbusiness.getPresentationRoot());
			folderbusiness.updateFolderRightsForUser(root, lecturer1, AccessRight.getFull());

			folderbusiness.createPresentationFolder(MY_FOLDER, root);
			folderbusiness.createPresentationFolder(READ_ONLY_FOLDER, root);
			folderbusiness.createPresentationFolder(EXTENDED_READ_FOLDER, root);
			folderbusiness.createPresentationFolder(EDIT_FOLDER, root);
			folderbusiness.createPresentationFolder(EDIT_FOLDER_COPY, root);
			folderbusiness.createPresentationFolder(EXTENDED_EDIT_FOLDER, root);
			folderbusiness.createPresentationFolder(MANAGE_FOLDER, root);
		} catch (Exception e) {
			fail("It was not possible to setup the test folders.", e);
		}
	}

	@Test
	@Order(1)
	@RunAsClient
	void shareFolders() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		login("lecturer1", "secret");
		AvailableCoursesPage.navigateToPage();
		AvailableCoursesPage.expandFolder(ROOT_FOLDER);

		// Set the same rights as in SharingContentFoldersTest
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", EDIT_FOLDER, Permission.write);
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", EDIT_FOLDER_COPY, Permission.write);
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", EXTENDED_EDIT_FOLDER, Permission.write,
				Permission.extendedRead);
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", EXTENDED_READ_FOLDER, Permission.read,
				Permission.extendedRead);
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", READ_ONLY_FOLDER, Permission.read);
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", MANAGE_FOLDER, Permission.manage);

		// Test that you can reset Rights
		var userRightsTable = AvailableCoursesPage.openUserRightsForFolder(MY_FOLDER);
		userRightsTable.changeRights("lecturer2", Permission.read);
		userRightsTable.resetChanges();
		userRightsTable.saveChanges();

		logout();
	}

	@Test
	@Order(2)
	void verifySharedFolders() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<PresentationFolder> sharedFolders = folderbusiness.getAllPresentationFoldersForUser(lecturer2);

		// lecturer1 should have all 8 folders
		assertEquals(8, folderbusiness.getAllPresentationFoldersForUser(lecturer1).size());
		// lecturer1 should have 7 folders (ROOT and myFolder were not shared)
		assertEquals(6, sharedFolders.size());

		//Check that lecturer 2 has the correct Rights on the SharedFolders 
		for (PresentationFolder folder : sharedFolders) {
			switch (folder.getName()) {
			case READ_ONLY_FOLDER:
				assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			case EXTENDED_READ_FOLDER:
				assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ),
						authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			case EDIT_FOLDER:
				assertEquals(AccessRight.getFromFlags(READ, WRITE),
						authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			case EDIT_FOLDER_COPY:
				assertEquals(AccessRight.getFromFlags(READ, WRITE),
						authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			case EXTENDED_EDIT_FOLDER:
				assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ, WRITE),
						authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			case MANAGE_FOLDER:
				assertEquals(AccessRight.getFromFlags(MANAGE),
						authorizationBusiness.getMaximumRightForUser(lecturer2, folder));
				break;
			default:
				fail(folder.getName() + " should not be in the shared folder list for lecturer2");
			}
		}
	}

	@Test
	@Order(3)
	@RunAsClient
	void createCourseOffers() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		login("lecturer2", "secret");
		AvailableCoursesPage.navigateToPage();

		AvailableCoursesPage.expandFolder(ROOT_FOLDER);

		// Try to create course offers where the user doesn't have Edit rights
		// Checking is performed in the next test case method
		AvailableCoursesPage.checkElementHasNoContextMenu(AvailableCoursesPage.getFolder(ROOT_FOLDER));
		AvailableCoursesPage.checkElementHasNoContextMenu(AvailableCoursesPage.getFolder(READ_ONLY_FOLDER));
		AvailableCoursesPage.checkElementHasNoContextMenu(AvailableCoursesPage.getFolder(EXTENDED_READ_FOLDER));

		// Create course offers in the folders where the user has Edit rights
		AvailableCoursesPage.createCourseOffer(EDIT_FOLDER, "CO1");
		AvailableCoursesPage.createCourseOffer(EDIT_FOLDER, "CO2");
		AvailableCoursesPage.createCourseOffer(EXTENDED_EDIT_FOLDER, "CO3");
		AvailableCoursesPage.createCourseOffer(MANAGE_FOLDER, "CO4");
		AvailableCoursesPage.createCourseOffer(MANAGE_FOLDER, "CO5");
	}

	@Test
	@Order(4)
	void verifyCreatedCourseOffers() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();
		List<CourseOffer> allCourseOffers = courseBusiness.getAllCourseOffers();
		// There should be 5 CourseOffers
		assertEquals(5, allCourseOffers.size());
		// Both User should have access to the CourseOffers

		// Lecturer2 should be in managingUsers because Lecturer1 give him access directly for the folder (with
		// the CourseOffer).
		assertTrue(folderbusiness.getPresentationFolderWithLazyData(allCourseOffers.get(0).getFolder())
				.getManagingUsers().containsKey(lecturer2));
		assertTrue(folderbusiness.getPresentationFolderWithLazyData(allCourseOffers.get(1).getFolder())
				.getManagingUsers().containsKey(lecturer2));

		// Lecturer1 should be instead in inheritedManagingUsers because he has access to the root folder and the child
		// Folders inherited the AccessRights
		assertTrue(folderbusiness.getPresentationFolderWithLazyData(allCourseOffers.get(0).getFolder())
				.getInheritedManagingUsers().containsKey(lecturer1));
		assertTrue(folderbusiness.getPresentationFolderWithLazyData(allCourseOffers.get(1).getFolder())
				.getInheritedManagingUsers().containsKey(lecturer1));

		for (CourseOffer courseOffer : allCourseOffers) {
			switch (courseOffer.getName()) {
			case "CO1":
			case "CO2":
				assertEquals(EDIT_FOLDER, courseOffer.getFolder().getName());
				break;
			case "CO3":
				assertEquals(EXTENDED_EDIT_FOLDER, courseOffer.getFolder().getName());
				break;
			case "CO4":
			case "CO5":
				assertEquals(MANAGE_FOLDER, courseOffer.getFolder().getName());
				break;
			default:
				fail("Unexpected course offer name: " + courseOffer.getName());
			}
		}
	}

	@Test
	@Order(5)
	@RunAsClient
	void moveCourseOffers() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		// lecturer2 is still logged in
		assumeLogin();

		// Do some drag and Drop actions which are not allowed
		// Unfortunately we have to reload the page after every drag and drop event.
		// Otherwise the Webdriver has problems to wait until the event is fully handled.
		// We could get Exceptions like RectUndefinedException or StaleElementReference

		// Note: The page reload is in the "moveElement" method

		// Try to move course offers into a Folder with no Edit rights
		AvailableCoursesPage.moveElement(AvailableCoursesPage.getCourseOffer("CO1"),
				AvailableCoursesPage.getFolder(READ_ONLY_FOLDER));

		AvailableCoursesPage.expandFolders(EXTENDED_EDIT_FOLDER);
		AvailableCoursesPage.moveElement(AvailableCoursesPage.getCourseOffer("CO3"),
				AvailableCoursesPage.getFolder(EXTENDED_READ_FOLDER));

		// Try to move a course offer into a Folder with Edit rights where the action is rejected because the rights would change
		AvailableCoursesPage.expandFolders(EDIT_FOLDER);
		AvailableCoursesPage.moveElement(AvailableCoursesPage.getCourseOffer("CO1"),
				AvailableCoursesPage.getFolder(EXTENDED_EDIT_FOLDER));

		// Move a course offer into a Folder with the same rights --> Action should work!
		AvailableCoursesPage.moveElement(AvailableCoursesPage.getCourseOffer("CO2"),
				AvailableCoursesPage.getFolder(EDIT_FOLDER_COPY));

		// Move a course offer with right-changes where the user has Manage rights
		// Should work, but a warning is displayed to decide if the element is moved or just copied
		AvailableCoursesPage.expandFolders(MANAGE_FOLDER);
		AvailableCoursesPage.moveElementWithChangingOwner(AvailableCoursesPage.getCourseOffer("CO4"),
				AvailableCoursesPage.getFolder(EDIT_FOLDER_COPY), false);

		// Move a copy(!) of the Course Offer (similar to the previous case)
		AvailableCoursesPage.moveElementWithChangingOwner(AvailableCoursesPage.getCourseOffer("CO5"),
				AvailableCoursesPage.getFolder(EDIT_FOLDER_COPY), true);
	}

	@Test
	@Order(6)
	void verifyMovedCourseOffers() {

		var courseOffers = courseBusiness.getAllCourseOffers();
		assertEquals(6, courseOffers.size());

		// We use a trick to detect the copied course offer:
		// Select "CO5" with the higher ID and rename it temporarily(!)
		courseOffers.stream()
				.filter(co -> co.getName().equals("CO5"))
				.sorted(Comparator.comparing(CourseOffer::getId).reversed())
				.findFirst()
				.orElseThrow()
				.setName("CO5-copy");

		// Verify the correct folders
		for (final var courseOffer : courseOffers) {
			switch (courseOffer.getName()) {
			case "CO1":
				assertEquals(EDIT_FOLDER, courseOffer.getFolder().getName());
				break;
			case "CO3":
				assertEquals(EXTENDED_EDIT_FOLDER, courseOffer.getFolder().getName());
				break;
			case "CO5":
				assertEquals(MANAGE_FOLDER, courseOffer.getFolder().getName());
				break;
			case "CO2":
			case "CO4":
			case "CO5-copy": // see above
				assertEquals(EDIT_FOLDER_COPY, courseOffer.getFolder().getName());
				break;
			default:
				fail("Unexpected course offer name: " + courseOffer.getName());
			}
		}
	}

	@Test
	@Order(7)
	@RunAsClient
	void moveFolders() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		// lecturer2 is still logged in
		assumeLogin();

		// Try to move a Folder with Edit Rights into a Folder with no Edit rights.
		// and try to move a Folder without Edit Rights into a Folder with Edit Rights.
		// Both shouldn't work due to the lack of rights
		AvailableCoursesPage.moveElement(MyWorkspacePage.getFolder(EDIT_FOLDER),
				AvailableCoursesPage.getFolder(READ_ONLY_FOLDER));
		AvailableCoursesPage.moveElement(MyWorkspacePage.getFolder(READ_ONLY_FOLDER),
				AvailableCoursesPage.getFolder(EDIT_FOLDER));

		// We create two sub folders to test moving operations
		// Testing moving operations is not possible with the existing folders because lecturer2 has no rights on the
		// root folder

		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder(EDIT_FOLDER), EDIT_SUBFOLDER);
		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder(MANAGE_FOLDER), MANAGE_SUBFOLDER);

		// Try move action that should be rejected because of changing rights
		AvailableCoursesPage.moveElement(MyWorkspacePage.getFolder(EDIT_SUBFOLDER),
				AvailableCoursesPage.getFolder(MANAGE_FOLDER));

		// Move a Folder into another Folder where both folders have equal rights AND the user has Edit rights
		// --> This should work
		AvailableCoursesPage.expandFolders(EDIT_FOLDER);
		AvailableCoursesPage.moveElement(MyWorkspacePage.getFolder(EDIT_SUBFOLDER),
				AvailableCoursesPage.getFolder(EDIT_FOLDER_COPY));

		// Move a Folder into another Folder with changing rights and Manage right
		// --> A warning is shown first, after confirmation the action should work
		AvailableCoursesPage.expandFolders(MANAGE_FOLDER);
		AvailableCoursesPage.moveElementWithChangingOwner(MyWorkspacePage.getFolder(MANAGE_SUBFOLDER),
				AvailableCoursesPage.getFolder(EDIT_FOLDER), false);

	}

	@Test
	@Order(8)
	void verifyMovedFolders() {
		// Verify the correct folders
		final var root = folderbusiness.getPresentationRoot().getName();
		for (final var folder : folderbusiness.getAllPresentationFolders()) {
			if (folder.isRoot())
				continue; // not a subject of this test case

			switch (folder.getName()) {
			case ROOT_FOLDER:
				assertEquals(root, folder.getParentFolder().getName());
				break;
			// Not changed
			case MY_FOLDER:
			case READ_ONLY_FOLDER:
			case EXTENDED_READ_FOLDER:
			case EDIT_FOLDER:
			case EDIT_FOLDER_COPY:
			case EXTENDED_EDIT_FOLDER:
			case MANAGE_FOLDER:
				assertEquals(ROOT_FOLDER, folder.getParentFolder().getName());
				break;
			// Moved
			case EDIT_SUBFOLDER:
				assertEquals(EDIT_FOLDER_COPY, folder.getParentFolder().getName());
				break;
			// Moved with confirmation
			case MANAGE_SUBFOLDER:
				assertEquals(EDIT_FOLDER, folder.getParentFolder().getName());
				break;
			default:
				fail("Unexpected folder name: " + folder.getName());
			}
		}
	}

	@Test
	@Order(9)
	@RunAsClient
	void removeSharedRights() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		login("lecturer1", "secret");
		AvailableCoursesPage.navigateToPage();

		AvailableCoursesPage.expandFolder(ROOT_FOLDER);

		// Remove rights for Lecturer2 for a folder
		AvailableCoursesPage.changeRightsOfLecturer("lecturer2", EDIT_FOLDER, Permission.write, Permission.read);

		logout();
	}

	@Test
	@Order(10)
	void verifyChangedRights() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<PresentationFolder> allFolders = folderbusiness.getAllPresentationFoldersForUser(lecturer1);
		List<PresentationFolder> sharedFolders = folderbusiness.getAllPresentationFoldersForUser(lecturer2);

		assertEquals(10, allFolders.size());
		assertEquals(6, sharedFolders.size());

		PresentationFolder folder = allFolders
				.stream()
				.filter(f -> f.getName().equals(EDIT_FOLDER))
				.findAny()
				.orElseThrow();

		// Lecturer2 doesn't have AccessRights anymore
		folder = folderbusiness.getPresentationFolderWithLazyData(folder);
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(lecturer2, folder));

		// And also on the children Folder 
		folder = folderbusiness.getPresentationFolderWithLazyData(folder.getChildrenFolder().iterator().next());
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(lecturer2, folder));

	}

}
