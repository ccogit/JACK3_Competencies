package de.uni_due.s3.jack3.tests.integration;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Additional tests for folder rights management of UserBusiness
 * 
 * @author lukas.glaser
 *
 */
class UserBusinessFolderRightsTest extends AbstractBusinessTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	User user;
	User owner;

	@BeforeEach
	void setupUsers() {
		user = getLecturer("user");
		owner = getLecturer("owner");
	}

	/**
	 * Tests getting a maximum right for a user and folder.
	 * 
	 * Test 1: User has all rights on her/his personal folder and sub folders
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight1() throws ActionNotAllowedException {
		Folder folder = user.getPersonalFolder();
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));

		Folder subFolder = folderBusiness.createContentFolder(user, "Sub Folder", folder);
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, subFolder));
	}

	/**
	 * Test 2: User has no rights on a personal folder of another user
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight2() throws ActionNotAllowedException {
		Folder folder = owner.getPersonalFolder();
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));

		Folder subFolder = folderBusiness.createContentFolder(owner, "Sub Folder", folder);
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, subFolder));
	}

	/**
	 * Test 3: User has no rights on root folder
	 */
	@Test
	void getMaximumRight3() {
		Folder folder = folderBusiness.getContentRoot();
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

	/**
	 * Test 4: User has specific rights on a folder created by an other user, but no rights on the other's personal
	 * folder
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight4() throws ActionNotAllowedException {
		Folder personalFolder = owner.getPersonalFolder();
		Folder folder = folderBusiness.createContentFolder(owner, "Folder", personalFolder);

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, personalFolder));

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFromFlags(READ, EXTENDED_READ));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, personalFolder));

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, personalFolder));

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, personalFolder));
	}

	/**
	 * Test 5: User has specific rights on a parent folder
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight5() throws ActionNotAllowedException {
		Folder personalFolder = owner.getPersonalFolder();
		Folder parentFolder = folderBusiness.createContentFolder(owner, "Folder", personalFolder);
		Folder folder = folderBusiness.createContentFolder(owner, "Folder", parentFolder);

		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, parentFolder));
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUser(parentFolder, user, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, parentFolder));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUser(parentFolder, user, AccessRight.getFromFlags(READ, EXTENDED_READ));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ),
				authorizationBusiness.getMaximumRightForUser(user, parentFolder));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUser(parentFolder, user, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, parentFolder));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUser(parentFolder, user, AccessRight.getFull());
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, parentFolder));
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

	/**
	 * Test 6: User has specific rights on a parent folder and specific rights on the folder
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight6() throws ActionNotAllowedException {
		Folder personalFolder = owner.getPersonalFolder();
		// Owner's personal folder --> parent folder --> sub folder
		Folder parentFolder = folderBusiness.createContentFolder(owner, "Parent Folder", personalFolder);
		Folder subFolder = folderBusiness.createContentFolder(owner, "Sub Folder", personalFolder);

		// Add READ to parent folder and READ to sub folder: Folder has READ rights on sub folder
		folderBusiness.updateFolderRightsForUser(parentFolder, user, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUser(subFolder, user, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, subFolder));

		// Add READWRITE to sub folder: Folder has READWRITE rights on sub folder
		folderBusiness.updateFolderRightsForUser(subFolder, user, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, subFolder));
	}

	/**
	 * Test 7: User belongs to a group that has no rights on a folder
	 */
	@Test
	void getMaximumRight7() {
		UserGroup group = getUserGroup("User Group");
		userBusiness.addUserToUserGroup(user, group);
		Folder personalFolder = owner.getPersonalFolder();

		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, personalFolder));
	}

	/**
	 * Test 8: User belongs to a group that has direct rights on a folder
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight8() throws ActionNotAllowedException {
		UserGroup group = getUserGroup("User Group");
		userBusiness.addUserToUserGroup(user, group);
		Folder folder = folderBusiness.createContentFolder(owner, "Content Folder", owner.getPersonalFolder());

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ, EXTENDED_READ));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, folder));

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFull());
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

	/**
	 * Test 9: User belongs to a group that has inherited rights from a parent folder
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight9() throws ActionNotAllowedException {
		UserGroup group = getUserGroup("User Group");
		userBusiness.addUserToUserGroup(user, group);
		Folder parentFolder = folderBusiness.createContentFolder(owner, "Parent Folder", owner.getPersonalFolder());
		Folder subFolder = folderBusiness.createContentFolder(owner, "Sub Folder", parentFolder);

		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, subFolder));

		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group,
				AccessRight.getFromFlags(READ, EXTENDED_READ));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ),
				authorizationBusiness.getMaximumRightForUser(user, subFolder));

		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, subFolder));

		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFull());
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, subFolder));
	}

	/**
	 * Test 10: User belongs to a group that has inherited rights from a parent folder and direct rights
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight10() throws ActionNotAllowedException {
		UserGroup group = getUserGroup("User Group");
		userBusiness.addUserToUserGroup(user, group);
		Folder parentFolder = folderBusiness.createContentFolder(owner, "Parent Folder", owner.getPersonalFolder());
		Folder subFolder = folderBusiness.createContentFolder(owner, "Sub Folder", parentFolder);

		// Direct rights: READ | Inherited rights: READ | Target result: READ
		folderBusiness.updateFolderRightsForUserGroup(subFolder, group, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ), authorizationBusiness.getMaximumRightForUser(user, subFolder));

		// Direct rights: EXTENDED_READ | Inherited rights: READ | Target result: EXTENDED_READ
		folderBusiness.updateFolderRightsForUserGroup(subFolder, group, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group,
				AccessRight.getFromFlags(READ, EXTENDED_READ));
		assertEquals(AccessRight.getFromFlags(READ, EXTENDED_READ),
				authorizationBusiness.getMaximumRightForUser(user, subFolder));

		// Direct rights: READWRITE | Inherited rights: READ | Target result: READWRITE
		folderBusiness.updateFolderRightsForUserGroup(subFolder, group, AccessRight.getFromFlags(READ, WRITE));
		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, subFolder));

		// Direct rights: READWRITE | Inherited rights: EXTENDED_READ
		// Target result: ALL (combination of READWRITE and EXTENDED_READ)
		folderBusiness.updateFolderRightsForUserGroup(subFolder, group, AccessRight.getFromFlags(READ, EXTENDED_READ));
		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(EXTENDED_READ, WRITE),
				authorizationBusiness.getMaximumRightForUser(user, subFolder));

		// Direct rights: ALL | Inherited rights: READ | Target result: ALL
		folderBusiness.updateFolderRightsForUserGroup(subFolder, group, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(parentFolder, group, AccessRight.getFromFlags(READ));
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, subFolder));
	}

	/**
	 * Test 11: User belongs to a group that has rights on a folder and the user has also direct rights on it
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getMaximumRight11() throws ActionNotAllowedException {
		UserGroup group = getUserGroup("User Group");
		// Insert user to user group
		userBusiness.addUserToUserGroup(user, group);
		Folder folder = folderBusiness.createContentFolder(owner, "Folder", owner.getPersonalFolder());

		// User right: READ | Group right: READWRITE | Target result: READWRITE
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFromFlags(READ));
		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFromFlags(READ, WRITE), authorizationBusiness.getMaximumRightForUser(user, folder));

		// User right: ALL | Group right: READWRITE | Target result: ALL
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ, WRITE));
		assertEquals(AccessRight.getFull(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

	@Test
	void getMaximumRightFail1() {
		assertThrows(IllegalArgumentException.class, () -> {
			authorizationBusiness.getMaximumRightForUser(user, (Folder) null);
		});
	}

	@Test
	void getMaximumRightFail2() {
		final var folder = user.getPersonalFolder();
		assertThrows(IllegalArgumentException.class, () -> {
			authorizationBusiness.getMaximumRightForUser(null, folder);
		});
	}

	@Test
	void getMaximumRightFail3() {
		assertThrows(IllegalArgumentException.class, () -> authorizationBusiness.getMaximumRightForUser((User) null, (Folder) null));
	}

	/**
	 * Tests removing existing rights from a user
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void removeUserRights() throws ActionNotAllowedException {
		Folder folder = folderBusiness.createContentFolder(owner, "Folder", owner.getPersonalFolder());

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFromFlags(READ));

		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getNone());
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

	/**
	 * Tests removing existing rights from a user group
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void removeUserGroupRights() throws ActionNotAllowedException {
		UserGroup group = getUserGroup("User Group");
		userBusiness.addUserToUserGroup(user, group);
		Folder folder = folderBusiness.createContentFolder(owner, "Folder", owner.getPersonalFolder());

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getFromFlags(READ));

		folderBusiness.updateFolderRightsForUserGroup(folder, group, AccessRight.getNone());
		assertEquals(AccessRight.getNone(), authorizationBusiness.getMaximumRightForUser(user, folder));
	}

}
