package de.uni_due.s3.jack3.tests.integration;

import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

/**
 * Test class for inherited and direct User(Group) rights.
 */
class FolderRightsTest extends AbstractBasicTest {

	private User admin;
	private User lecturer;
	private User student;

	private UserGroup adminGroup;
	private UserGroup editorGroup;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	/**
	 * Persist users, groups and folders
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		admin = getAdmin("admin");
		lecturer = getLecturer("lecturer");
		student = getStudent("student");

		adminGroup = getUserGroup("Admin Group", admin);
		editorGroup = getUserGroup("Editors", adminGroup);

		persistFolder();
		folder = folderService	.getFolderWithManagingRights(folder, ContentFolder.class)
								.orElseThrow(AssertionError::new);
	}

	/**
	 * Get an empty map of managing users
	 */
	@Test
	void getEmptyManagingUsers() {
		assertTrue(folder.getManagingUsers().isEmpty());
	}

	/**
	 * Get an empty map of managing user groups
	 */
	@Test
	void getEmptyManagingGroups() {
		assertTrue(folder.getManagingUserGroups().isEmpty());
	}

	/**
	 * Get an empty map of all managing users
	 */
	@Test
	void getEmptyAllManagingUsers() {
		assertTrue(authorizationBusiness.getAllManagingUsers(folder).isEmpty());
	}

	/**
	 * Get an empty map of all managing user groups
	 */
	@Test
	void getEmptyAllManagingGroups() {
		assertTrue(authorizationBusiness.getAllManagingUserGroups(folder).isEmpty());
	}

	/**
	 * Get an empty map of all inherited managing users
	 */
	@Test
	void getEmptyInheritedManagingUsers() {
		assertTrue(folder.getInheritedManagingUsers().isEmpty());
	}

	/**
	 * Get an empty map of all inherited managing user groups
	 */
	@Test
	void getEmptyInheritedManagingGroups() {
		assertTrue(folder.getInheritedManagingUserGroups().isEmpty());
	}

	/**
	 * Get empty complete managing users
	 */
	@Test
	void getEmptyCompleteManagingUsers() {
		assertTrue(authorizationBusiness.getCompleteManagingUsersMap(false, folder).isEmpty());
	}

	/**
	 * Add user rights
	 */
	@Test
	void addUserRights() {

		HashMap<User, AccessRight> rights = new HashMap<>();
		rights.put(admin, AccessRight.getFull());
		rights.put(lecturer, AccessRight.getFromFlags(READ, WRITE));
		rights.put(student, AccessRight.getFromFlags(READ));

		rights.forEach((user, right) -> folder.addUserRight(user, right));

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getManagingUsers());
		assertEquals(rights, authorizationBusiness.getAllManagingUsers(folder));
	}

	/**
	 * Add inherited user rights
	 */
	@Test
	void addInheritedUserRights() {

		HashMap<User, AccessRight> rights = new HashMap<>();
		rights.put(admin, AccessRight.getFull());
		rights.put(lecturer, AccessRight.getFromFlags(READ, WRITE));
		rights.put(student, AccessRight.getFromFlags(READ));

		rights.forEach((user, right) -> folder.addInheritedUserRight(user, right));

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getInheritedManagingUsers());
		assertEquals(rights, authorizationBusiness.getAllManagingUsers(folder));
	}

	/**
	 * Add inherited user right map
	 */
	@Test
	void addInheritedUserRightMap() {

		HashMap<User, AccessRight> rights = new HashMap<>();
		rights.put(admin, AccessRight.getFull());
		rights.put(lecturer, AccessRight.getFromFlags(READ, WRITE));
		rights.put(student, AccessRight.getFromFlags(READ));

		folder.addInheritedUserRights(rights);

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getInheritedManagingUsers());
		assertEquals(rights, authorizationBusiness.getAllManagingUsers(folder));
	}

	/**
	 * Add user group rights
	 */
	@Test
	void addGroupRights() {

		HashMap<UserGroup, AccessRight> rights = new HashMap<>();
		rights.put(adminGroup, AccessRight.getFull());
		rights.put(editorGroup, AccessRight.getFromFlags(READ, WRITE));

		rights.forEach((group, right) -> folder.addUserGroupRight(group, right));

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getManagingUserGroups());
		assertEquals(rights, authorizationBusiness.getAllManagingUserGroups(folder));
	}

	/**
	 * Add inherited user group rights
	 */
	@Test
	void addInheritedGroupRights() {

		HashMap<UserGroup, AccessRight> rights = new HashMap<>();
		rights.put(adminGroup, AccessRight.getFull());
		rights.put(editorGroup, AccessRight.getFromFlags(READ, WRITE));

		rights.forEach((group, right) -> folder.addInheritedUserGroupRight(group, right));

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getInheritedManagingUserGroups());
		assertEquals(rights, authorizationBusiness.getAllManagingUserGroups(folder));
	}

	/**
	 * Add inherited user group right map
	 */
	@Test
	void addInheritedGroupRightMap() {

		HashMap<UserGroup, AccessRight> rights = new HashMap<>();
		rights.put(adminGroup, AccessRight.getFull());
		rights.put(editorGroup, AccessRight.getFromFlags(READ, WRITE));

		folder.addInheritedUserGroupRights(rights);

		folder = folderService.mergeContentFolder(folder);

		assertEquals(rights, folder.getInheritedManagingUserGroups());
		assertEquals(rights, authorizationBusiness.getAllManagingUserGroups(folder));
	}

	/**
	 * Get all managing users
	 */
	@Test
	@Disabled("folder.getCompleteManagingUsersMap does not work, see #135")
	void getCompleteManagingUsersMap() {

		folder.addUserGroupRight(editorGroup, AccessRight.getFromFlags(READ, WRITE));
		folder.addUserRight(student, AccessRight.getFromFlags(READ));
		folder = folderService.mergeContentFolder(folder);

		HashMap<User, AccessRight> expectedMap = new HashMap<>();
		expectedMap.put(admin, AccessRight.getFromFlags(READ, WRITE));
		expectedMap.put(lecturer, AccessRight.getFromFlags(READ, WRITE));
		expectedMap.put(student, AccessRight.getFromFlags(READ));

		assertEquals(expectedMap, authorizationBusiness.getCompleteManagingUsersMap(false, folder));
	}

	/**
	 * Delete all inherited rights
	 */
	@Test
	void deleteInheritedRights() {

		folder.addInheritedUserRight(lecturer, AccessRight.getFromFlags(READ));
		folder.addInheritedUserGroupRight(editorGroup, AccessRight.getFromFlags(READ, WRITE));
		folder = folderService.mergeContentFolder(folder);

		// collections with rights should not be empty
		assertFalse(folder.getInheritedManagingUsers().isEmpty());
		assertFalse(folder.getInheritedManagingUserGroups().isEmpty());

		// remove all rights, collections should be empty
		folder.deleteAllInheritedRights();
		folder = folderService.mergeContentFolder(folder);

		assertTrue(folder.getInheritedManagingUsers().isEmpty());
		assertTrue(folder.getInheritedManagingUserGroups().isEmpty());
	}

	/**
	 * Delete all user rights
	 */
	@Test
	void deleteUserRights() {

		folder.addUserRight(admin, AccessRight.getFull());
		folder = folderService.mergeContentFolder(folder);

		// user rights should contain admin
		assertTrue(folder.getManagingUsers().containsKey(admin));

		// remove right, collection should be empty
		folder.deleteAllUserRights(admin);
		folder = folderService.mergeContentFolder(folder);
		assertTrue(folder.getInheritedManagingUsers().isEmpty());
	}

}
