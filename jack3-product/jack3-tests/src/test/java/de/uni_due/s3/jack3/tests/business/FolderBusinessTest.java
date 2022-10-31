package de.uni_due.s3.jack3.tests.business;

import static de.uni_due.s3.jack3.tests.utils.Assert.assertEqualsEntityListUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;
import de.uni_due.s3.jack3.tests.utils.Node;

/**
 * Tests for FolderBusiness.
 *
 * @author lukas.glaser
 *
 */
class FolderBusinessTest extends AbstractBusinessTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	private User admin;

	private final AccessRight READ = AccessRight.getFromFlags(AccessRight.READ);
	private final AccessRight READWRITE = AccessRight.getFromFlags(AccessRight.WRITE);
	private final AccessRight EXTENDED_READ = AccessRight.getFromFlags(AccessRight.EXTENDED_READ);
	private final AccessRight ALL = AccessRight.getFull();

	/**
	 * Prepare tests: Delete entities of previous tests and create root folders
	 */
	@BeforeEach
	void prepareTest() {
		admin = userBusiness.createUser("Admin", "secret", "foo@bar.com", true, true);
	}

	private ContentFolder getFolderLazyData(ContentFolder folder) {
		return folderBusiness.getContentFolderWithLazyData(folder);
	}

	private PresentationFolder getFolderLazyData(PresentationFolder folder) {
		return folderBusiness.getPresentationFolderWithLazyData(folder);
	}

	@Test
	void createPresentationFolder() {
		PresentationFolder folder = folderBusiness.createPresentationFolder("Presentation Folder",
				folderBusiness.getPresentationRoot());
		assertTrue(getFolderLazyData(folderBusiness.getPresentationRoot()).getChildrenFolder().contains(folder));
	}

	@Test
	void resetFolderRights() throws ActionNotAllowedException {
		User user = userBusiness.createUser("User", "secret", "foo@bar.com", true, true);
		ContentFolder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());

		// Add a sample inherited READ-right for user
		folder.addInheritedUserRight(user, READ);
		folderBusiness.updateFolder(folder);
		assertEquals(READ, getFolderLazyData(folder).getInheritedManagingUsers().get(user));

		// Resetting rights should clear inherited rights map
		folderBusiness.resetFolderRights(folder);
		assertFalse(getFolderLazyData(folder).getInheritedManagingUsers().containsKey(user));
	}

	@Test
	void updateFolderRightsForUsers() throws ActionNotAllowedException {
		User user = userBusiness.createUser("User", "secret", "foo@bar.com", true, true);
		ContentFolder contentFolder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());

		folderBusiness.updateFolderRightsForUser(contentFolder, user, READ);
		assertEquals(READ, getFolderLazyData(contentFolder).getManagingUsers().get(user));

		PresentationFolder presentationFolder = folderBusiness.createPresentationFolder("Presentation Folder",
				folderBusiness.getPresentationRoot());
		folderBusiness.updateFolderRightsForUser(presentationFolder, user, READ);
		assertEquals(READ, getFolderLazyData(presentationFolder).getManagingUsers().get(user));
	}

	@Test
	void updateFolderRightsForGroups() throws ActionNotAllowedException {
		UserGroup group = userBusiness.createUserGroup("Group", "Description");
		ContentFolder contentFolder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());

		folderBusiness.updateFolderRightsForUserGroup(contentFolder, group, READ);
		assertEquals(READ, getFolderLazyData(contentFolder).getManagingUserGroups().get(group));

		PresentationFolder presentationFolder = folderBusiness.createPresentationFolder("Presentation Folder",
				folderBusiness.getPresentationRoot());
		folderBusiness.updateFolderRightsForUserGroup(presentationFolder, group, READ);
		assertEquals(READ, getFolderLazyData(presentationFolder).getManagingUserGroups().get(group));
	}

	@Test
	void removeUserRights1() throws ActionNotAllowedException {
		User user = userBusiness.createUser("User", "secret", "foo@bar.com", true, true);
		ContentFolder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());
		folderBusiness.updateFolderRightsForUser(folder, user, AccessRight.getFull());
		assertEquals(AccessRight.getFull(), getFolderLazyData(folder).getManagingUsers().get(user));

		folderBusiness.removeUserRightsOnNonPersonalContentFolders(user);
		assertFalse(getFolderLazyData(folder).getManagingUsers().containsKey(user));
	}

	@Test
	void createContentFolder() throws ActionNotAllowedException {
		ContentFolder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());
		assertEquals(AccessRight.getFull(), getFolderLazyData(folder).getInheritedManagingUsers().get(admin));
		assertTrue(getFolderLazyData(admin.getPersonalFolder()).getChildrenFolder().contains(folder));
	}

	@Test
	void moveContentFolder() throws ActionNotAllowedException {
		ContentFolder folder1 = folderBusiness.createContentFolder(admin, "Content Folder 1", admin.getPersonalFolder());
		ContentFolder folder2 = folderBusiness.createContentFolder(admin, "Content Folder 2", admin.getPersonalFolder());

		// Move folder2 into folder1
		folderBusiness.moveContentFolder(admin, folder2, folder1);
		ContentFolder folder1Updated = folderBusiness.getContentFolderById(folder1.getId()).get();
		ContentFolder folder2Updated = folderBusiness.getContentFolderById(folder2.getId()).get();
		assertTrue(getFolderLazyData(folder1Updated).getChildrenFolder().contains(folder2Updated));
		assertEquals(folder1Updated,folder2Updated.getParentFolder());
	}

	// ************************************
	// Tests for delegates on FolderService
	// ************************************

	@Test
	void deleteFolder() throws ActionNotAllowedException {
		ContentFolder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());

		assertTrue(getFolderLazyData(admin.getPersonalFolder()).getChildrenFolder().contains(folder));
		folderBusiness.deleteFolder(admin, folder);
		assertFalse(getFolderLazyData(admin.getPersonalFolder()).getChildrenFolder().contains(folder));
	}

	@Test
	void getFolderWithManagingRights1() throws ActionNotAllowedException {
		ContentFolder contentFolder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());
		Folder folder = folderBusiness	.getFolderWithManagingRights(contentFolder)
										.orElseThrow(AssertionError::new);
		// Check if we can access the collections
		assertEquals(0, folder.getManagingUsers().size());
		assertEquals(0, folder.getManagingUserGroups().size());
		assertEquals(1, folder.getInheritedManagingUsers().size());
		assertEquals(0, folder.getInheritedManagingUserGroups().size());
	}

	@Test
	void updateFolder() throws ActionNotAllowedException {
		Folder folder = folderBusiness.createContentFolder(admin, "Content Folder", admin.getPersonalFolder());
		folder.setName("Content Folder 2");
		folder = folderBusiness.updateFolder(folder);

		assertEquals("Content Folder 2", folder.getName());
	}

	@Test
	void getAllPresentationFoldersForUsers() {
		User user1 = userBusiness.createUser("User1", "secret", "foo@bar.com", true, true);
		User user2 = userBusiness.createUser("User2", "secret", "foo@bar.com", true, true);
		PresentationFolder root = folderBusiness.getPresentationRoot();

		PresentationFolder folder1 = folderBusiness.createPresentationFolder("Folder1", root);
		PresentationFolder folder2 = folderBusiness.createPresentationFolder("Folder2", root);
		PresentationFolder folder3 = folderBusiness.createPresentationFolder("Folder3", root);
		PresentationFolder folder4 = folderBusiness.createPresentationFolder("Folder4", root);
		PresentationFolder folder5 = folderBusiness.createPresentationFolder("Folder4", folder4);

		folderBusiness.updateFolderRightsForUser(folder1, user1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUser(folder2, user1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUser(folder3, user2, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUser(folder4, user2, AccessRight.getFull());

		List<PresentationFolder> foldersForUser1 = folderBusiness.getAllPresentationFoldersForUser(user1);
		List<PresentationFolder> foldersForUser2 = folderBusiness.getAllPresentationFoldersForUser(user2);
		List<PresentationFolder> expectedFoldersForUser1 = Arrays.asList(folder1, folder2);
		List<PresentationFolder> expectedFoldersForUser2 = Arrays.asList(folder3, folder4, folder5);

		// Assert that these lists contain the same elements
		assertTrue(foldersForUser1.containsAll(expectedFoldersForUser1));
		assertTrue(expectedFoldersForUser1.containsAll(foldersForUser1));
		assertTrue(foldersForUser2.containsAll(expectedFoldersForUser2));
		assertTrue(expectedFoldersForUser2.containsAll(foldersForUser2));
	}

	@Test
	void getAllPresentationFoldersForGroups() {
		UserGroup group1 = userBusiness.createUserGroup("Group11", "Description");
		UserGroup group2 = userBusiness.createUserGroup("Group2", "Description");
		PresentationFolder root = folderBusiness.getPresentationRoot();

		PresentationFolder folder1 = folderBusiness.createPresentationFolder("Folder1", root);
		PresentationFolder folder2 = folderBusiness.createPresentationFolder("Folder2", root);
		PresentationFolder folder3 = folderBusiness.createPresentationFolder("Folder3", root);
		PresentationFolder folder4 = folderBusiness.createPresentationFolder("Folder4", root);
		PresentationFolder folder5 = folderBusiness.createPresentationFolder("Folder4", folder4);

		folderBusiness.updateFolderRightsForUserGroup(folder1, group1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder2, group1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder3, group2, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder4, group2, AccessRight.getFull());

		List<PresentationFolder> foldersForUser1 = folderBusiness.getAllPresentationFoldersForUserGroup(group1);
		List<PresentationFolder> foldersForUser2 = folderBusiness.getAllPresentationFoldersForUserGroup(group2);
		List<PresentationFolder> expectedFoldersForUser1 = Arrays.asList(folder1, folder2);
		List<PresentationFolder> expectedFoldersForUser2 = Arrays.asList(folder3, folder4, folder5);

		// Assert that these lists contain the same elements
		assertTrue(foldersForUser1.containsAll(expectedFoldersForUser1));
		assertTrue(expectedFoldersForUser1.containsAll(foldersForUser1));
		assertTrue(foldersForUser2.containsAll(expectedFoldersForUser2));
		assertTrue(expectedFoldersForUser2.containsAll(foldersForUser2));
	}

	@Test
	void getPresentationRoot() {
		// Note: Returning type of "get*Root" is NOT an Optional. In case of an empty result, an Exception is thrown.
		assertNotNull(folderBusiness.getPresentationRoot());
	}

	@Test
	void getAllContentFoldersForUser1() throws ActionNotAllowedException {
		User user1 = userBusiness.createUser("User1", "secret", "foo@bar.com", true, true);

		ContentFolder folder1 = folderBusiness.createContentFolder(user1, "Folder1", user1.getPersonalFolder());
		ContentFolder folder2 = folderBusiness.createContentFolder(user1, "Folder2", user1.getPersonalFolder());

		List<ContentFolder> foldersForUser1 = folderBusiness.getAllContentFoldersForUser(user1);
		List<ContentFolder> expectedFoldersForUser1 = Arrays.asList(folder1, folder2, user1.getPersonalFolder());

		// Assert that these lists contain the same elements
		assertEqualsEntityListUnordered(expectedFoldersForUser1, foldersForUser1);
	}

	/**
	 * JACK/jack3-core#562
	 * @throws ActionNotAllowedException 
	 */
	@Test
	void getAllContentFoldersForUser2() throws ActionNotAllowedException {
		User user1 = userBusiness.createUser("user1", "secret", "foo1@bar.com", false, true);
		User user2 = userBusiness.createUser("user2", "secret", "foo2@bar.com", false, true);

		ContentFolder folder1 = folderBusiness.createContentFolder(user1, "Folder1", user1.getPersonalFolder());
		ContentFolder folder2 = folderBusiness.createContentFolder(user1, "Folder2", user1.getPersonalFolder());

		// 1. user1 shares his personal folder
		folderBusiness.updateFolderRightsForUser(user1.getPersonalFolder(), user2, READWRITE);

		// 2. user2 moves folder1 in folder2
		folderBusiness.moveContentFolder(user2, folder1, folder2);
		folderBusiness.resetFolderRights(folder1);

		// 3. Call getAllContentFoldersForUser(user1)
		List<ContentFolder> foldersForUser1 = folderBusiness.getAllContentFoldersForUser(user1);
		List<ContentFolder> expectedFoldersForUser1 = Arrays.asList(folder1, folder2, user1.getPersonalFolder());

		assertEqualsEntityListUnordered(expectedFoldersForUser1, foldersForUser1);

		// 4. Call getAllContentFoldersForUser(user2)
		List<ContentFolder> foldersForUser2 = folderBusiness.getAllContentFoldersForUser(user2);
		List<ContentFolder> expectedFoldersForUser2 = Arrays.asList(folder1, folder2, user2.getPersonalFolder(),
				user1.getPersonalFolder());

		// Assert that these lists contain the same elements
		assertEqualsEntityListUnordered(expectedFoldersForUser2, foldersForUser2);
	}

	@Test
	void getAllContentFoldersForGroups() throws ActionNotAllowedException {
		UserGroup group1 = userBusiness.createUserGroup("Group11", "Description");
		UserGroup group2 = userBusiness.createUserGroup("Group2", "Description");
		ContentFolder root = admin.getPersonalFolder();
				
		ContentFolder folder1 = folderBusiness.createContentFolder(admin, "Folder1", root);
		ContentFolder folder2 = folderBusiness.createContentFolder(admin, "Folder2", root);
		ContentFolder folder3 = folderBusiness.createContentFolder(admin, "Folder3", root);
		ContentFolder folder4 = folderBusiness.createContentFolder(admin, "Folder4", root);
		ContentFolder folder5 = folderBusiness.createContentFolder(admin, "Folder4", folder4);

		folderBusiness.updateFolderRightsForUserGroup(folder1, group1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder2, group1, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder3, group2, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(folder4, group2, AccessRight.getFull());

		List<ContentFolder> foldersForUser1 = folderBusiness.getAllContentFoldersForUserGroup(group1);
		List<ContentFolder> foldersForUser2 = folderBusiness.getAllContentFoldersForUserGroup(group2);
		List<ContentFolder> expectedFoldersForUser1 = Arrays.asList(folder1, folder2);
		List<ContentFolder> expectedFoldersForUser2 = Arrays.asList(folder3, folder4, folder5);

		// Assert that these lists contain the same elements
		assertTrue(foldersForUser1.containsAll(expectedFoldersForUser1));
		assertTrue(expectedFoldersForUser1.containsAll(foldersForUser1));
		assertTrue(foldersForUser2.containsAll(expectedFoldersForUser2));
		assertTrue(expectedFoldersForUser2.containsAll(foldersForUser2));
	}

	@Test
	void getContentRoot() {
		// Note: Returning type of "get*Root" is NOT an Optional. In case of an empty result, an Exception is thrown.
		assertNotNull(folderBusiness.getContentRoot());
	}

	@Test
	void getAllPresentationFolders() {
		PresentationFolder root = folderBusiness.getPresentationRoot();
		PresentationFolder folder1 = folderBusiness.createPresentationFolder("Folder1", root);
		PresentationFolder folder2 = folderBusiness.createPresentationFolder("Folder2", root);
		PresentationFolder folder3 = folderBusiness.createPresentationFolder("Folder3", root);
		PresentationFolder folder4 = folderBusiness.createPresentationFolder("Folder4", root);
		PresentationFolder folder5 = folderBusiness.createPresentationFolder("Folder4", folder4);

		List<PresentationFolder> expectedFolders = Arrays.asList(root, folder1, folder2, folder3, folder4, folder5);
		assertTrue(folderBusiness.getAllPresentationFolders().containsAll(expectedFolders));
	}

	@Test
	void getFolderById() {
		long id = folderBusiness.getContentRoot().getId();
		assertEquals(folderBusiness.getContentRoot(), folderBusiness.getContentFolderById(id)
																			.orElseThrow(AssertionError::new));

		id = folderBusiness.getPresentationRoot().getId();
		assertEquals(folderBusiness.getPresentationRoot(),
				folderBusiness.getPresentationFolderById(id)
																				.orElseThrow(AssertionError::new));
	}

	/**
	 * Personal folder should grant full rights to the owner
	 */
	@Test
	void personalFolderRights() {
		final User user = userBusiness.createUser("lecturer", "secret", "lec@turer.com", false, true);
		final ContentFolder personalFolder = folderBusiness.getContentFolderWithLazyData(user.getPersonalFolder());
		assertTrue(personalFolder.getManagingUsers().containsKey(user), "Managing users not contain the owner.");
		assertEquals(AccessRight.getFull(), personalFolder.getManagingUsers().get(user));
		assertEquals(AccessRight.getFull(), authorizationBusiness.getAllManagingUsers(personalFolder).get(user));
	}

	@Test
	void ownerForContentFolder() throws ActionNotAllowedException {
		final User user = userBusiness.createUser("lecturer", "secret", "lec@turer.com", false, true);
		final User user2 = userBusiness.createUser("lecturer2", "secret", "lec2@turer.com", false, true);
		final ContentFolder root = folderBusiness.getContentRoot();
		final ContentFolder personalFolder = folderBusiness.getContentFolderWithLazyData(user.getPersonalFolder());
		final ContentFolder sub1 = folderBusiness.createContentFolder(user, "Subfolder1", personalFolder);
		final ContentFolder sub2 = folderBusiness.createContentFolder(user, "Subfolder2", sub1);

		assertThrows(NullPointerException.class, () -> {
			folderBusiness.getOwnerOfContentFolder(null);
		});
		assertThrows(NullPointerException.class, () -> {
			folderBusiness.getOwnerOfContentFolder(root);
		});
		assertEquals(user, folderBusiness.getOwnerOfContentFolder(personalFolder));
		assertEquals(user, folderBusiness.getOwnerOfContentFolder(sub1));
		assertEquals(user, folderBusiness.getOwnerOfContentFolder(sub2));

		assertFalse(folderBusiness.isOwnedBy(root, user));
		assertTrue(folderBusiness.isOwnedBy(personalFolder, user));
		assertTrue(folderBusiness.isOwnedBy(sub1, user));
		assertTrue(folderBusiness.isOwnedBy(sub2, user));

		assertFalse(folderBusiness.isOwnedBy(root, user2));
		assertFalse(folderBusiness.isOwnedBy(personalFolder, user2));
		assertFalse(folderBusiness.isOwnedBy(sub1, user2));
		assertFalse(folderBusiness.isOwnedBy(sub2, user2));
	}

	//TODO Discuss alternatives for this test
	/**
	 * Disabled because test can't create a ContentFolder in ContentRoot because no User has the right for that
	 * @throws ActionNotAllowedException
	 */
	@Disabled("Test in this form not executable. See Code for Documentation")
	@Test
	void getOwnerForContentFolderIllegalState() throws ActionNotAllowedException {
		final ContentFolder root = folderBusiness.getContentRoot();
		// This is no personal folder because no user owns the folder.
		// -> So "getOwnerOfContentFolder" should not be able to find the owner.
		final ContentFolder noPersonalFolder = folderBusiness.createContentFolder(admin, "noPersonalFolder", root);
		final ContentFolder sub1 = folderBusiness.createContentFolder(admin, "Subfolder1", noPersonalFolder);
		final ContentFolder sub2 = folderBusiness.createContentFolder(admin, "Subfolder2", sub1);

		assertThrows(IllegalStateException.class, () -> {
			folderBusiness.getOwnerOfContentFolder(noPersonalFolder);
		});
		assertThrows(IllegalStateException.class, () -> {
			folderBusiness.getOwnerOfContentFolder(sub1);
		});
		assertThrows(IllegalStateException.class, () -> {
			folderBusiness.getOwnerOfContentFolder(sub2);
		});
	}

	@Test
	void isPersonalFolder() throws ActionNotAllowedException {
		final User user = userBusiness.createUser("lecturer", "secret", "lec@turer.com", false, true);
		final ContentFolder root = folderBusiness.getContentRoot();
		final ContentFolder personalFolder = folderBusiness.getContentFolderWithLazyData(user.getPersonalFolder());
		final ContentFolder f1 = folderBusiness.createContentFolder(user, ContentFolder.PERSONAL_FOLDER_NAME, personalFolder);
		final ContentFolder f2 = folderBusiness.createContentFolder(user, "Subfolder", f1);

		assertFalse(folderBusiness.isPersonalFolder(root));
		assertTrue(folderBusiness.isPersonalFolder(personalFolder));
		assertFalse(folderBusiness.isPersonalFolder(f1));
		assertFalse(folderBusiness.isPersonalFolder(f2));

		final PresentationFolder presRoot = folderBusiness.getPresentationRoot();
		final PresentationFolder pres1 = folderBusiness.createPresentationFolder("personalFolder", presRoot);
		final PresentationFolder pres2 = folderBusiness.createPresentationFolder("personalFolder", pres1);
		final PresentationFolder pres3 = folderBusiness.createPresentationFolder("folder", pres2);

		assertFalse(folderBusiness.isPersonalFolder(presRoot));
		assertFalse(folderBusiness.isPersonalFolder(pres1));
		assertFalse(folderBusiness.isPersonalFolder(pres2));
		assertFalse(folderBusiness.isPersonalFolder(pres3));
	}

	@Test
	void testManagingMaps1() {
		// We use the root folder as a "dummy" for this test
		ContentFolder folder = getFolderLazyData(folderBusiness.getContentRoot());

		final User[] users = new User[4];
		for (int i = 0; i < 4; i++) {
			users[i] = userBusiness.createUser("user" + i, "secret", null, false, true);
		}
		final UserGroup[] groups = new UserGroup[3];
		for (int i = 0; i < 3; i++) {
			groups[i] = userBusiness.createUserGroup("group" + i, "");
		}

		userBusiness.addUserToUserGroup(users[0], groups[0]);
		userBusiness.addUserGroupToUserGroup(groups[1], groups[0]);
		userBusiness.addUserToUserGroup(users[1], groups[1]);
		userBusiness.addUserGroupToUserGroup(groups[2], groups[1]);
		userBusiness.addUserToUserGroup(users[2], groups[2]);

		folderBusiness.updateFolderRightsForUser(folder, users[3], READWRITE);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[0], READ);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[1], EXTENDED_READ);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[2], ALL);

		folder = getFolderLazyData(folderBusiness.getContentRoot());

		/*-
		 * Scenario: 4 users and 3 user groups  | right         | resulting right 
		 * - user0                              |               | READ
		 * - user1                              |               | EXTENDED_READ
		 * - user2                              |               | ALL
		 * - user3                              | READWRITE     | READWRITE
		 * - group0 contains user0 and group1   | READ          | READ
		 * - group1 contains user1 and group2   | EXTENDED_READ | EXTENDED_READ
		 * - group2 contains user2              | ALL           | ALL
		 */

		final Map<User, AccessRight> userMap = authorizationBusiness.getAllManagingUsers(folder);
		assertNull(userMap.get(users[0]));
		assertNull(userMap.get(users[1]));
		assertNull(userMap.get(users[2]));
		assertEquals(READWRITE, userMap.get(users[3]));

		final Map<UserGroup, AccessRight> userGroupMap = authorizationBusiness.getAllManagingUserGroups(folder);
		assertEquals(READ, userGroupMap.get(groups[0]));
		assertEquals(EXTENDED_READ, userGroupMap.get(groups[1]));
		assertEquals(ALL, userGroupMap.get(groups[2]));

		final Map<User, AccessRight> completeUserMap = authorizationBusiness.getCompleteManagingUsersMap(true, folder);
		assertEquals(READ, completeUserMap.get(users[0]));
		assertEquals(EXTENDED_READ, completeUserMap.get(users[1]));
		assertEquals(ALL, completeUserMap.get(users[2]));
		assertEquals(READWRITE, completeUserMap.get(users[3]));
	}

	@Test
	void testManagingMaps2() {
		// We use the root folder as a "dummy" for this test
		ContentFolder folder = getFolderLazyData(folderBusiness.getContentRoot());

		final User[] users = new User[4];
		for (int i = 0; i < 4; i++) {
			users[i] = userBusiness.createUser("user" + i, "secret", null, false, true);
		}
		final UserGroup[] groups = new UserGroup[3];
		for (int i = 0; i < 3; i++) {
			groups[i] = userBusiness.createUserGroup("group" + i, "");
		}

		userBusiness.addUserToUserGroup(users[0], groups[0]);
		userBusiness.addUserGroupToUserGroup(groups[1], groups[0]);
		userBusiness.addUserToUserGroup(users[1], groups[1]);
		userBusiness.addUserGroupToUserGroup(groups[2], groups[1]);
		userBusiness.addUserToUserGroup(users[2], groups[2]);

		folderBusiness.updateFolderRightsForUser(folder, users[3], READWRITE);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[0], ALL);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[1], READ);
		folderBusiness.updateFolderRightsForUserGroup(folder, groups[2], EXTENDED_READ);

		folder = getFolderLazyData(folderBusiness.getContentRoot());

		/*-
		 * Scenario: 4 users and 3 user groups  | right         | resulting right 
		 * - user0                              |               | ALL
		 * - user1                              |               | ALL
		 * - user2                              |               | ALL
		 * - user3                              | READWRITE     | ALL
		 * - group0 contains user0 and group1   | ALL           | ALL
		 * - group1 contains user1 and group2   | READ          | ALL
		 * - group2 contains user2              | EXTENDED_READ | ALL
		 */

		final Map<User, AccessRight> userMap = authorizationBusiness.getAllManagingUsers(folder);
		assertNull(userMap.get(users[0]));
		assertNull(userMap.get(users[1]));
		assertNull(userMap.get(users[2]));
		assertEquals(READWRITE, userMap.get(users[3]));

		final Map<UserGroup, AccessRight> userGroupMap = authorizationBusiness.getAllManagingUserGroups(folder);
		assertEquals(ALL, userGroupMap.get(groups[0]));
		assertEquals(READ, userGroupMap.get(groups[1]));
		assertEquals(EXTENDED_READ, userGroupMap.get(groups[2]));

		final Map<User, AccessRight> completeUserMap = authorizationBusiness.getCompleteManagingUsersMap(true, folder);
		assertEquals(ALL, completeUserMap.get(users[0]));
		assertEquals(ALL, completeUserMap.get(users[1]));
		assertEquals(ALL, completeUserMap.get(users[2]));
		assertEquals(READWRITE, completeUserMap.get(users[3]));
	}

	@Test
	void testManagingMaps3() {
		// We use the root folder as a "dummy" for this test
		ContentFolder folder = getFolderLazyData(folderBusiness.getContentRoot());

		final User dummyUser = userBusiness.createUser("dummyUser", "secret", null, false, true);
		final UserGroup dummyGroup0 = userBusiness.createUserGroup("dummyGroup1", "");
		final UserGroup dummyGroup1 = userBusiness.createUserGroup("dummyGroup2", "");
		final UserGroup dummyGroup2 = userBusiness.createUserGroup("dummyGroup3", "");

		userBusiness.addUserToUserGroup(dummyUser, dummyGroup1);
		userBusiness.addUserToUserGroup(dummyUser, dummyGroup2);
		userBusiness.addUserGroupToUserGroup(dummyGroup1, dummyGroup0);
		userBusiness.addUserGroupToUserGroup(dummyGroup2, dummyGroup0);

		folderBusiness.updateFolderRightsForUserGroup(folder, dummyGroup0, ALL);

		folder = getFolderLazyData(folderBusiness.getContentRoot());

		/*-
		 * Scenario:                            | right         | resulting right 
		 * - dummyUser                          |               | ALL
		 * - group0 contains group1 and group2  | ALL           | ALL
		 * - group1 contains dummyUser          |               | ALL
		 * - group2 contains dummyUser          |               | ALL
		 */

		final Map<User, AccessRight> userMap = authorizationBusiness.getAllManagingUsers(folder);
		assertNull(userMap.get(dummyUser));

		final Map<UserGroup, AccessRight> userGroupMap = authorizationBusiness.getAllManagingUserGroups(folder);
		assertEquals(ALL, userGroupMap.get(dummyGroup0));
		assertNull(userGroupMap.get(dummyGroup1));
		assertNull(userGroupMap.get(dummyGroup2));

		final Map<User, AccessRight> completeUserMap = authorizationBusiness.getCompleteManagingUsersMap(true, folder);
		assertEquals(ALL, completeUserMap.get(dummyUser));
	}
	
	/*
	 *      root
	 *     /     \
	 *    A       B
	 *   / \     / \
	 *  C   D   E   F
	 */
	/**
	 * Procudes a sample tree.
	 */
	private Node<PresentationFolder> createHierarchyForLinkedCourseTest() {
		final var root = new Node<PresentationFolder>(folderBusiness.getPresentationRoot());
		final var a = root.addChild(folderBusiness.createPresentationFolder("A", root.getData())).getData();
		final var b = root.addChild(folderBusiness.createPresentationFolder("B", root.getData())).getData();
		root.getChildren().get(0).addChild(folderBusiness.createPresentationFolder("C", a));
		root.getChildren().get(0).addChild(folderBusiness.createPresentationFolder("D", a));
		root.getChildren().get(1).addChild(folderBusiness.createPresentationFolder("E", b));
		root.getChildren().get(1).addChild(folderBusiness.createPresentationFolder("F", b));
		return root;
	}
	
	/**
	 * Maps the first character of each folder to the corresponding folder.
	 */
	private Map<Character, PresentationFolder> mapNameToFolder(Node<PresentationFolder> root) {
		final Map<Character, PresentationFolder> result = new HashMap<>();
		result.put(null, root.getData()); // Special case: "null" for the root
		for (PresentationFolder folder : root.getAllChildrenData()) {
			result.put(folder.getName().charAt(0), folder);
		}
		return result;
	}
	
	private void reloadMap(Map<Character, PresentationFolder> map) {
		map.replaceAll((c, f) -> baseService.findById(PresentationFolder.class, f.getId(), false).orElseThrow());
	}

	@Test
	void switchLinkedCourse() throws Exception {
		final var root = createHierarchyForLinkedCourseTest();
		final var folders = mapNameToFolder(root);

		// Activate linked courses for folder A
		assertFalse(folders.get('A').isContainsLinkedCourses());
		boolean newLinkedValueForA = folderBusiness.switchLinkedCourses(folders.get('A'));
		reloadMap(folders);
		
		assertTrue(newLinkedValueForA);
		assertFalse(folders.get(null).isContainsLinkedCourses());
		assertTrue(folders.get('A').isContainsLinkedCourses());
		assertFalse(folders.get('B').isContainsLinkedCourses());
		assertFalse(folders.get('C').isContainsLinkedCourses());
		assertFalse(folders.get('D').isContainsLinkedCourses());
		assertFalse(folders.get('E').isContainsLinkedCourses());
		assertFalse(folders.get('F').isContainsLinkedCourses());
	}
	
	@Test
	void switchLinkedCourseFailed() throws Exception {
		final var root = createHierarchyForLinkedCourseTest();
		final var folders = mapNameToFolder(root);

		// Activate linked courses for folder A
		assertFalse(folders.get('A').isContainsLinkedCourses());
		folderBusiness.switchLinkedCourses(folders.get('A'));
		reloadMap(folders);

		// This should fail: If A has linked courses, B cannot unlinked
		assertThrows(ActionNotAllowedException.class, () -> folderBusiness.switchLinkedCourses(folders.get('C')));
	}

	@Test
	void getHighestLinkedCourseFolder() throws Exception {
		final var root = createHierarchyForLinkedCourseTest();
		final var folders = mapNameToFolder(root);

		// Activate linked courses for folder A
		assertFalse(folders.get('A').isContainsLinkedCourses());
		folderBusiness.switchLinkedCourses(folders.get('A'));
		reloadMap(folders);

		assertFalse(folderBusiness.getHighestLinkedCourseFolder(folders.get(null)).isPresent());
		assertTrue(folderBusiness.getHighestLinkedCourseFolder(folders.get('A')).isPresent());
		assertTrue(folderBusiness.getHighestLinkedCourseFolder(folders.get('C')).isPresent());
		assertTrue(folderBusiness.getHighestLinkedCourseFolder(folders.get('D')).isPresent());
		assertFalse(folderBusiness.getHighestLinkedCourseFolder(folders.get('B')).isPresent());
		assertFalse(folderBusiness.getHighestLinkedCourseFolder(folders.get('E')).isPresent());
		assertFalse(folderBusiness.getHighestLinkedCourseFolder(folders.get('F')).isPresent());

		assertEquals(folders.get('A'), folderBusiness.getHighestLinkedCourseFolder(folders.get('A')).get());
		assertEquals(folders.get('A'), folderBusiness.getHighestLinkedCourseFolder(folders.get('C')).get());
		assertEquals(folders.get('A'), folderBusiness.getHighestLinkedCourseFolder(folders.get('D')).get());
	}

	@Test
	void hasLinkedCourses() throws Exception {
		final var root = createHierarchyForLinkedCourseTest();
		final var folders = mapNameToFolder(root);

		// Activate linked courses for folder A
		assertFalse(folders.get('A').isContainsLinkedCourses());
		folderBusiness.switchLinkedCourses(folders.get('A'));
		reloadMap(folders);

		assertFalse(folderBusiness.hasLinkedCourses(folders.get(null)));
		assertTrue(folderBusiness.hasLinkedCourses(folders.get('A')));
		assertTrue(folderBusiness.hasLinkedCourses(folders.get('C')));
		assertTrue(folderBusiness.hasLinkedCourses(folders.get('D')));
		assertFalse(folderBusiness.hasLinkedCourses(folders.get('B')));
		assertFalse(folderBusiness.hasLinkedCourses(folders.get('E')));
		assertFalse(folderBusiness.hasLinkedCourses(folders.get('F')));
	}

	@Test
	void hasInheritedLinkedCourses() throws Exception {
		final var root = createHierarchyForLinkedCourseTest();
		final var folders = mapNameToFolder(root);

		// Activate linked courses for folder A
		assertFalse(folders.get('A').isContainsLinkedCourses());
		folderBusiness.switchLinkedCourses(folders.get('A'));
		reloadMap(folders);

		assertFalse(folderBusiness.hasInheritedLinkedCourses(folders.get(null)));
		assertFalse(folderBusiness.hasInheritedLinkedCourses(folders.get('A')));
		assertTrue(folderBusiness.hasInheritedLinkedCourses(folders.get('C')));
		assertTrue(folderBusiness.hasInheritedLinkedCourses(folders.get('D')));
		assertFalse(folderBusiness.hasInheritedLinkedCourses(folders.get('B')));
		assertFalse(folderBusiness.hasInheritedLinkedCourses(folders.get('E')));
		assertFalse(folderBusiness.hasInheritedLinkedCourses(folders.get('F')));
	}

}
