package de.uni_due.s3.jack3.tests.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import javax.inject.Inject;
import javax.security.auth.login.CredentialException;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.BcryptBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

class UserBusinessTest extends AbstractBusinessTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private BcryptBusiness bcryptBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	/**
	 * Creates and persists a user
	 */
	private User createUser(String name, boolean hasAdminRights, boolean hasEditRights) {
		return userBusiness.createUser(name, "secret", name.toLowerCase() + "@foobar.com", hasAdminRights,
				hasEditRights);
	}

	/**
	 * Creates and persists a user group
	 */
	private UserGroup createUserGroup(String name) {
		return userBusiness.createUserGroup(name, "Description of " + name);
	}

	/**
	 * Tests creating a user with admin rights and checks if the user has a personal folder
	 */
	@Test
	void createAdminUser() {
		User admin = createUser("Admin", true, true);
		User userFromDB = userBusiness	.getUserByName("Admin")
										.orElseThrow(AssertionError::new);

		assertEquals(admin, userFromDB);
		assertEquals("admin", userFromDB.getLoginName());
		assertEquals("admin@foobar.com", userFromDB.getEmail());
		assertTrue(userFromDB.isHasAdminRights());
		assertTrue(userFromDB.isHasEditRights());

		assertTrue(userBusiness.getAllUsers().contains(userFromDB));

		// Check if a personal folder was created and the user has all rights over it
		assertNotNull(admin.getPersonalFolder());
		ContentFolder personalFolder = folderBusiness.getContentFolderWithLazyData(userFromDB.getPersonalFolder());
		assertEquals(AccessRight.getFull(), personalFolder.getManagingUsers().get(userFromDB));
		assertEquals(admin, userBusiness.getUserOwningThisFolder(personalFolder)
												.orElseThrow(AssertionError::new));
	}

	/**
	 * Tests creating a user without admin rights and checks if the user has no personal folder
	 */
	@Test
	void createNormalUser() {
		User user = createUser("User", false, false);
		User userFromDB = userBusiness	.getUserByName("User")
										.orElseThrow(AssertionError::new);

		assertEquals(user, userFromDB);
		assertEquals("user", userFromDB.getLoginName());
		assertEquals("user@foobar.com", userFromDB.getEmail());
		assertFalse(userFromDB.isHasAdminRights());
		assertFalse(userFromDB.isHasEditRights());

		assertTrue(userBusiness.getAllUsers().contains(userFromDB));

		// Check if a personal folder was created and the user has all rights over it
		ContentFolder personalFolder = userFromDB.getPersonalFolder();
		assertNull(personalFolder);
	}

	/**
	 * Tests creating a normal user first and then add admin rights.
	 */
	@Test
	void addUserRights() {
		User user = createUser("User", false, false);
		assertNull(user.getPersonalFolder());

		// A personal folder should be created
		user.setHasEditRights(true);
		user = userBusiness.updateUser(user);
		assertNotNull(user.getPersonalFolder());
	}

	/**
	 * Tests creating an admin user and then remove admin rights.
	 * @throws ActionNotAllowedException
	 */
	@Test
	void revokeUserAdminRights() throws ActionNotAllowedException {
		User admin = createUser("Admin", true, true);
		User user = createUser("User", false, true);
		assertNotNull(user.getPersonalFolder());
		assertNotNull(admin.getPersonalFolder());

		// admin creates a new folder and add rights for user
		Folder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());

		assertEquals(AccessRight.getFull(),
				folderBusiness	.getFolderWithManagingRights(folder)
								.orElseThrow(AssertionError::new)
								.getManagingUsers()
								.get(user));
		user.setHasEditRights(false);
		user = userBusiness.updateUser(user);
		assertFalse(folderBusiness.getFolderWithManagingRights(folder)
											.orElseThrow(AssertionError::new)
											.getManagingUsers()
											.containsKey(user));
	}

	/**
	 * Tests creating an empty user group
	 */
	@Test
	void createEmptyUserGroup() {
		UserGroup group = createUserGroup("Group");
		UserGroup groupFromDB = userBusiness.getUserGroup("Group")
											.orElseThrow(AssertionError::new);

		assertEquals(group, groupFromDB);
		assertEquals("Group", groupFromDB.getName());
		assertEquals("Description of Group", groupFromDB.getDescription());

		assertTrue(userBusiness.getAllUserGroups().contains(groupFromDB));
	}

	/**
	 * Tests inserting a user into a user group
	 */
	@Test
	void addUserToUserGroup() {
		User user = createUser("User", true, true);
		UserGroup group = createUserGroup("Group");

		userBusiness.addUserToUserGroup(user, group);

		assertTrue(userBusiness.getUserGroupsForUser(user).contains(group));
		assertTrue(userBusiness.getAllUsersForUserGroup(group).contains(user));
	}

	/**
	 * Tests deleting a user group
	 */
	@Test
	void deleteUserGroup() {
		UserGroup toBeDeleted = createUserGroup("Group");
		assertNotNull(userBusiness.getUserGroup("Group"));

		userBusiness.deleteUserGroup(toBeDeleted);
		assertFalse(userBusiness.getUserGroup("Group").isPresent());
	}

	/**
	 * Tests deleting a user group with user
	 */
	@Test
	void deleteUserGroupWithUser() {
		User user = createUser("User", true, true);
		UserGroup toBeDeleted = createUserGroup("Group");
		assertNotNull(userBusiness.getUserGroup("Group"));
		userBusiness.addUserToUserGroup(user, toBeDeleted);

		userBusiness.deleteUserGroup(toBeDeleted);
		assertFalse(userBusiness.getUserGroup("Group").isPresent());
	}

	@Test
	void createUserWithEmptyPasswordFailsTest() {
		try {
			userBusiness.createUser("User", "", "user@foobar.com", true, true);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(userBusiness.getUserByName("User").isPresent());
		}
	}

	/**
	 * Tests creating a user with an empty name
	 */
	@Test
	void createUserWithEmptyName() {
		try {
			userBusiness.createUser("", "secret", "user@foobar.com", true, true);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(userBusiness.getUserByName("").isPresent());
		}
	}

	/**
	 * Tests if the user password could be changed
	 */
	@Test
	void changeUserPassword() throws Exception {
		User user = createUser("User", false, false);

		Password oldPassword = user.getPassword();
		user = userBusiness.changeUserPassword(user, "secret", "newpassword");

		assertTrue(bcryptBusiness.matches("newpassword", user.getPassword()));
		assertNotEquals(oldPassword, user.getPassword());

	}

	/**
	 * Tests if the user password could not be changed by giving a wrong old password
	 */
	@Test
	void changeUserPasswordFail() {
		User user = createUser("User", false, false);

		// Expect a CredentialException, fail if no Exception occured and check if the password was not changed
		try {
			user = userBusiness.changeUserPassword(user, "wrongPassword", "newpassword");
			fail();
		} catch (CredentialException e) {
			assertTrue(bcryptBusiness.matches("secret", user.getPassword()));
		}
	}

	/**
	 * Tests if the user password could not be changed by giving an empty new password
	 */
	@Test
	void changeUserPasswordToEmptyFail() throws Exception {

		User user = createUser("User", false, false);
		// Expect an IllegalArgumentException, fail if no Exception occurred and check if the password was not changed
		try {
			user = userBusiness.changeUserPassword(user, "secret", "");
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(bcryptBusiness.matches("secret", user.getPassword()));
		}
	}

	/**
	 * Tests adding a user into a user group
	 */
	@Test
	void addUsersToUserGroup() {
		User user = createUser("User", true, true);
		UserGroup group = createUserGroup("Group");

		userBusiness.addUserToUserGroup(user, group);
		assertTrue(userBusiness.getAllUsersForUserGroup(group).contains(user));
		assertTrue(userBusiness.getUserGroupsForUser(user).contains(group));
	}

	/**
	 * Tests getting all users with edit rights
	 */
	@Test
	void getAllUsersWithEditRights() {
		User admin1 = createUser("Admin1", true, true);
		User admin2 = createUser("Admin2", true, true);
		User user1 = createUser("User1", false, false);
		User user2 = createUser("User2", false, false);

		List<User> usersWithEditRights = userBusiness.getAllUsersWithEditRights();
		assertTrue(usersWithEditRights.contains(admin1));
		assertTrue(usersWithEditRights.contains(admin2));
		assertFalse(usersWithEditRights.contains(user1));
		assertFalse(usersWithEditRights.contains(user2));
	}

	/**
	 * Tests inserting a user group into a user group
	 */
	@Test
	void addUserGroupToUserGroup() {
		// Create the following hierarchy: Parent Group --> Member Group --> User
		UserGroup parent = createUserGroup("Parent");
		UserGroup member = createUserGroup("Member");

		User user = createUser("User", true, true);
		userBusiness.addUserToUserGroup(user, member);
		userBusiness.addUserGroupToUserGroup(member, parent);

		List<UserGroup> groupsForUser = userBusiness.getUserGroupsForUser(user);
		assertTrue(groupsForUser.contains(member));
		assertTrue(groupsForUser.contains(parent));
	}

	/**
	 * Tests removing a user from a user group
	 */
	@Test
	void removeUserFromUserGroup() {
		User user = createUser("User", true, true);
		UserGroup group = createUserGroup("Group");

		userBusiness.addUserToUserGroup(user, group);
		userBusiness.removeUserFromUserGroup(user, group);

		assertFalse(userBusiness.getUserGroupsForUser(user).contains(group));
		assertFalse(userBusiness.getAllUsersForUserGroup(group).contains(user));
	}

	/**
	 * Tests removing a user from all groups
	 */
	@Test
	void removeUserFromAllUserGroups() {
		User user = createUser("User", true, true);
		UserGroup group1 = createUserGroup("Group 1");
		UserGroup group2 = createUserGroup("Group 2");
		UserGroup group3 = createUserGroup("Group 3");

		userBusiness.addUserToUserGroup(user, group1);
		userBusiness.addUserToUserGroup(user, group2);
		userBusiness.addUserToUserGroup(user, group3);
		assertFalse(userBusiness.getUserGroupsForUser(user).isEmpty());
		assertFalse(userBusiness.getAllUsersForUserGroup(group1).isEmpty());
		assertFalse(userBusiness.getAllUsersForUserGroup(group2).isEmpty());
		assertFalse(userBusiness.getAllUsersForUserGroup(group3).isEmpty());

		userBusiness.removeUserFromAllUserGroups(user);

		assertTrue(userBusiness.getUserGroupsForUser(user).isEmpty());
		assertTrue(userBusiness.getAllUsersForUserGroup(group1).isEmpty());
		assertTrue(userBusiness.getAllUsersForUserGroup(group2).isEmpty());
		assertTrue(userBusiness.getAllUsersForUserGroup(group3).isEmpty());
	}

	/**
	 * Tests removing a user group from a user group
	 */
	@Test
	void removeUserGroupFromUserGroup() {
		// Create the following hierarchy: Parent Group --> Member Group --> User
		UserGroup parent = createUserGroup("Parent");
		UserGroup member = createUserGroup("Member");

		User user = createUser("User", true, true);
		userBusiness.addUserToUserGroup(user, member);
		userBusiness.addUserGroupToUserGroup(member, parent);

		// user belongs to both groups
		assertTrue(userBusiness.getUserGroupsForUser(user).contains(member));
		assertTrue(userBusiness.getUserGroupsForUser(user).contains(parent));

		userBusiness.removeUserGroupFromUserGroup(member, parent);
		// memberGroup was removed from parentGroup; user only belongs to memberGroup
		assertTrue(userBusiness.getUserGroupsForUser(user).contains(member));
		assertFalse(userBusiness.getUserGroupsForUser(user).contains(parent));
	}

	/**
	 * Tests removing a user from all user groups with group hierarchy
	 */
	@Test
	void removeUserFromAllGroupsWithHierarchy() {
		// Create the following hierarchy: Parent Group --> Member Group --> User
		UserGroup parent = createUserGroup("Parent");
		UserGroup member = createUserGroup("Member");

		User user = createUser("User", true, true);
		userBusiness.addUserToUserGroup(user, member);
		userBusiness.addUserGroupToUserGroup(member, parent);
		userBusiness.removeUserFromAllUserGroups(user);

		assertFalse(userBusiness.getUserGroupsForUser(user).contains(member));
		assertFalse(userBusiness.getUserGroupsForUser(user).contains(parent));
	}

	/**
	 * Tests removing a user from a user group that is no member
	 */
	@Test
	void removeUserFromUserGroupFail() {
		User user = createUser("User", true, true);
		UserGroup group = createUserGroup("Group");

		userBusiness.removeUserFromUserGroup(user, group);

		assertFalse(userBusiness.getUserGroupsForUser(user).contains(group));
		assertFalse(userBusiness.getAllUsersForUserGroup(group).contains(user));
	}
}
