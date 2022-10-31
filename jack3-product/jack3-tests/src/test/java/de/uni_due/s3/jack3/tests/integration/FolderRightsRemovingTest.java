package de.uni_due.s3.jack3.tests.integration;

import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * This test class tests removing / getting only specific user rights for a folder. See JACK/jack3-core#467.
 */
class FolderRightsRemovingTest extends AbstractBusinessTest {

	@Inject
	private FolderBusiness folderBusiness;

	@Test
	void getContentFoldersForUser() throws ActionNotAllowedException {
		// Create two users with lecturer rights
		User owner = getLecturer("owner");
		User lecturer1 = getLecturer("lecturer1");
		User lecturer2 = getLecturer("lecturer2");

		ContentFolder root = owner.getPersonalFolder();
		ContentFolder subdirectory = folderBusiness.createContentFolder(owner, "Subdirectory", root);

		/*-
		 * root
		 * - owner's personal folder
		 *   - Subdirectory
		 */

		// Give both lecturers access to subdirectory
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer2, AccessRight.getFromFlags(READ, WRITE));

		// "getFolderWithManagingRights" includes both rights:
		Folder result1 = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertEquals(AccessRight.getFromFlags(READ), result1.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result1.getManagingUsers().get(lecturer2));

		// Check that "getAllContentFoldersForUser(lecturer1)" includes rights for lecturer2
		Folder result2 = folderBusiness.getAllContentFoldersForUser(lecturer1).get(0);
		assertEquals(AccessRight.getFromFlags(READ), result2.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result2.getManagingUsers().get(lecturer2));
	}

	@Test
	void getContentFoldersForUserGroup() throws ActionNotAllowedException {
		// Create two users with lecturer rights
		User owner = getLecturer("owner");
		UserGroup lecturers1 = getUserGroup("lecturers1");
		UserGroup lecturers2 = getUserGroup("lecturers2");

		ContentFolder root = owner.getPersonalFolder();
		ContentFolder subdirectory = folderBusiness.createContentFolder(owner, "Subdirectory", root);

		/*-
		 * root
		 * - owner's personal folder
		 *   - Subdirectory
		 */

		// Give both lecturer groups access to subdirectory
		folderBusiness.updateFolderRightsForUserGroup(subdirectory, lecturers1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUserGroup(subdirectory, lecturers2, AccessRight.getFromFlags(READ, WRITE));

		// "getFolderWithManagingRights" includes both rights:
		Folder result1 = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertEquals(AccessRight.getFromFlags(READ), result1.getManagingUserGroups().get(lecturers1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result1.getManagingUserGroups().get(lecturers2));

		// Check that "getAllContentFoldersForUserGroup(lecturers1)" includes rights for lecturers2
		Folder result2 = folderBusiness.getAllContentFoldersForUserGroup(lecturers1).get(0);
		assertEquals(AccessRight.getFromFlags(READ), result2.getManagingUserGroups().get(lecturers1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result2.getManagingUserGroups().get(lecturers2));
	}

	@Test
	void getPresentationFoldersForUser() {
		// Create two users with lecturer rights
		User lecturer1 = getLecturer("lecturer1");
		User lecturer2 = getLecturer("lecturer2");

		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder subdirectory = folderBusiness.createPresentationFolder("Subdirectory", root);

		/*-
		 * root
		 * - Subdirectory
		 */

		// Give both lecturers access to subdirectory
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer2, AccessRight.getFromFlags(READ, WRITE));

		// "getFolderWithManagingRights" includes both rights:
		Folder result1 = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertEquals(AccessRight.getFromFlags(READ), result1.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result1.getManagingUsers().get(lecturer2));

		// Check that "getAllPresentationFoldersForUser(lecturer1)" includes rights for lecturer2
		Folder result2 = folderBusiness.getAllPresentationFoldersForUser(lecturer1).get(0);
		assertEquals(AccessRight.getFromFlags(READ), result2.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result2.getManagingUsers().get(lecturer2));
	}

	@Test
	void getPresentationFoldersForUserGroup() {
		// Create two users with lecturer rights
		UserGroup lecturers1 = getUserGroup("lecturers1");
		UserGroup lecturers2 = getUserGroup("lecturers2");

		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder subdirectory = folderBusiness.createPresentationFolder("Subdirectory", root);

		/*-
		 * root
		 * - Subdirectory
		 */

		// Give both lecturer groups access to subdirectory
		folderBusiness.updateFolderRightsForUserGroup(subdirectory, lecturers1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUserGroup(subdirectory, lecturers2, AccessRight.getFromFlags(READ, WRITE));

		// "getFolderWithManagingRights" includes both rights:
		Folder result1 = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertEquals(AccessRight.getFromFlags(READ), result1.getManagingUserGroups().get(lecturers1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result1.getManagingUserGroups().get(lecturers2));

		// Check that "getAllPresentationFoldersForUserGroup(lecturers1)" includes rights for lecturers2
		Folder result2 = folderBusiness.getAllPresentationFoldersForUserGroup(lecturers1).get(0);
		assertEquals(AccessRight.getFromFlags(READ), result2.getManagingUserGroups().get(lecturers1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result2.getManagingUserGroups().get(lecturers2));
	}

	@Test
	void revokeUserRightsOnContentFolder() throws ActionNotAllowedException {
		// Create two users with lecturer rights
		User owner = getLecturer("owner");
		User lecturer1 = getLecturer("lecturer1");
		User lecturer2 = getLecturer("lecturer2");

		ContentFolder root = owner.getPersonalFolder();
		ContentFolder subdirectory = folderBusiness.createContentFolder(owner, "Subdirectory", root);

		/*-
		 * root
		 * - owner's personal folder
		 *   - Subdirectory
		 */

		// Give both lecturers access to subdirectory
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer2, AccessRight.getFromFlags(READ, WRITE));

		// Remove folder rights for lecturer1
		folderBusiness.removeUserRightsOnNonPersonalContentFolders(lecturer1);

		// Check that "removeUserRightsOnNonPersonalContentFolders(lecturer1)" removes only the rights for lecturer1
		Folder result = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertNull(result.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result.getManagingUsers().get(lecturer2));
	}

	@Test
	void revokeUserRightsOnPresentationFolder() {
		// Create two users with lecturer rights
		User lecturer1 = getLecturer("lecturer1");
		User lecturer2 = getLecturer("lecturer2");

		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder subdirectory = folderBusiness.createPresentationFolder("Subdirectory", root);

		/*-
		 * root
		 * - Subdirectory
		 */

		// Give both lecturers access to subdirectory
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer1, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(subdirectory, lecturer2, AccessRight.getFromFlags(READ, WRITE));

		// Remove folder rights for lecturer1
		folderBusiness.removeUserRightsOnPresentationFolders(lecturer1);

		// Check that "removeUserRightsOnPresentationFolders(lecturer1)" removes only the rights for lecturer1
		Folder result = folderBusiness.getFolderWithManagingRights(subdirectory).orElseThrow(AssertionError::new);
		assertNull(result.getManagingUsers().get(lecturer1));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), result.getManagingUsers().get(lecturer2));
	}

}
