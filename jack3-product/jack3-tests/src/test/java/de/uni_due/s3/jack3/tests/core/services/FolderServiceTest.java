package de.uni_due.s3.jack3.tests.core.services;

import static de.uni_due.s3.jack3.tests.utils.Assert.assertEqualsEntityListUnordered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.ejb.EJBException;
import javax.persistence.NonUniqueResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class FolderServiceTest extends AbstractBasicTest {

	private User owner = TestDataFactory.getUser("FolderOwner");

	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();
		persistUser();
		// We explicitly don't call "getAdmin()" because we don't want to have the automatic personal folder.
		baseService.persist(owner);
	}

	/**
	 * Test empty database
	 */
	@Test
	void getEmptyFolderList() {
		folderService.deleteFolder(folderService.getContentRoot());
		folderService.deleteFolder(folderService.getPresentationRoot());

		// database is empty, there are no folders at all
		assertTrue(folderService.getAllContentFoldersForUser(owner).isEmpty());
		assertTrue(folderService.getAllContentFoldersForUser(user).isEmpty());
		assertTrue(folderService.getAllPresentationFolders().isEmpty());

		assertNull(user.getPersonalFolder());
		assertNull(owner.getPersonalFolder());

		assertEquals(0, folderService.getNoOfContentFolders());
		assertEquals(0, folderService.getNoOfPresentationFolder());
	}

	/**
	 * Create duplicate folder
	 */
	@Test
	void createDuplicateFolders() {

		folderService.persistFolder(
				TestDataFactory.getContentFolder("First Content Folder", null));
		folderService.persistFolder(
				TestDataFactory.getContentFolder("First Content Folder", null));
		// There should be 3 content folders, with the original content root
		assertEquals(Long.valueOf(3), (Long) (hqlService.getSingleResult("select count(cf) from ContentFolder cf").get(0)));
	}

	/**
	 * Get all content folders for user
	 */
	@Test
	void getAllContentFoldersForUser() {

		ContentFolder folder1 = TestDataFactory.getContentFolder("Content Folder 1", null, owner);
		ContentFolder folder2 = TestDataFactory.getContentFolder("Content Folder 2", null, owner);
		folderService.persistFolder(folder1);
		folderService.persistFolder(folder2);

		// get all content folders should find folder1 and folder2
		assertEqualsEntityListUnordered(Arrays.asList(folder1, folder2), folderService.getAllContentFoldersForUser(owner));
	}

	/**
	 * Get all content folders for user group
	 */
	@Test
	void getAllContentFoldersForGroup() {

		UserGroup group = TestDataFactory.getUserGroup("User Group");
		userGroupService.persistUserGroup(group);

		ContentFolder folder1 = TestDataFactory.getContentFolder("Content Folder 1", null);
		folder1.addUserGroupRight(group, AccessRight.getFull());
		folderService.persistFolder(folder1);

		ContentFolder folder2 = TestDataFactory.getContentFolder("Content Folder 2", null);
		folder2.addUserGroupRight(group, AccessRight.getFull());
		folderService.persistFolder(folder2);

		// get all content folders should find folder1 and folder2
		assertEquals(Arrays.asList(folder1, folder2), folderService.getAllContentFoldersForUserGroup(group));
	}

	/**
	 * Get all presentation folders
	 */
	@Test
	void getAllPresentationFolders() {
		PresentationFolder root = folderService.getPresentationFolderWithLazyData(folderService.getPresentationRoot());
		PresentationFolder folder1 = TestDataFactory.getPresentationFolder("Presentation Folder 1", null);
		PresentationFolder folder2 = TestDataFactory.getPresentationFolder("Presentation Folder 2", null);
		root.addChildFolder(folder1);
		root.addChildFolder(folder2);
		folderService.persistFolder(folder1);
		folderService.persistFolder(folder2);

		root = folderService.mergePresentationFolder(root);

		// get all presentation folders should find folder1 and folder2 and the presentation root Folder.
		folderService.getAllPresentationFolders();
		assertEquals(Arrays.asList(folder1, folder2, root),
				folderService.getAllPresentationFolders());
	}

	/**
	 * Get all content folders for user
	 */
	@Test
	void getAllPresentationFoldersForUser() {

		PresentationFolder folder1 = TestDataFactory.getPresentationFolder("Presentation Folder 1", null);
		folder1.addUserRight(owner, AccessRight.getFull());
		folderService.persistFolder(folder1);

		PresentationFolder folder2 = TestDataFactory.getPresentationFolder("Presentation Folder 2", null);
		folder2.addUserRight(owner, AccessRight.getFull());
		folderService.persistFolder(folder2);

		// get all content folders should find folder1 and folder2
		assertEquals(Arrays.asList(folder1, folder2), folderService.getAllPresentationFoldersForUser(owner));
	}

	/**
	 * Get all content folders for user group
	 */
	@Test
	void getAllPresentationFoldersForGroup() {

		UserGroup group = TestDataFactory.getUserGroup("User Group");
		userGroupService.persistUserGroup(group);

		PresentationFolder folder1 = TestDataFactory.getPresentationFolder("Presentation Folder 1", null);
		folder1.addUserGroupRight(group, AccessRight.getFull());
		folderService.persistFolder(folder1);

		PresentationFolder folder2 = TestDataFactory.getPresentationFolder("Presentation Folder 2", null);
		folder2.addUserGroupRight(group, AccessRight.getFull());
		folderService.persistFolder(folder2);

		// get all content folders should find folder1 and folder2
		assertEquals(Arrays.asList(folder1, folder2),
				folderService.getAllPresentationFoldersForUserGroup(group));
	}

	/**
	 * Get content root correctly
	 */
	@Test
	void getCorrectContentRoot() {
		// removing the contentRoot created by the firstTimeSetup
		folderService.deleteFolder(folderService.getContentRoot());

		ContentFolder folder = TestDataFactory.getContentFolder("Content Root", null);
		folderService.persistFolder(folder);
		assertEquals(folder, folderService.getContentRoot());
	}

	/**
	 * Get no content root -> AssertionError
	 */
	@Test
	void getNoContentRoot() {
		folderService.deleteFolder(folderService.getContentRoot());

		EJBException thrown = assertThrows(EJBException.class, () -> {
			folderService.getContentRoot();
		});
		assertTrue(thrown.getCause() instanceof AssertionError);
	}

	/**
	 * Get multiple content roots -> NonUniqueResultException
	 */
	@Test
	void getMultipleContentRoots() {

		folderService.persistFolder(
				TestDataFactory.getContentFolder("Content Root 1", null));
		folderService.persistFolder(
				TestDataFactory.getContentFolder("Content Root 2", null));

		EJBException thrown = assertThrows(EJBException.class, () -> {
			folderService.getContentRoot();
		});
		assertTrue(thrown.getCause() instanceof NonUniqueResultException);
	}

	/**
	 * Get presentation root correctly
	 */
	@Test
	void getCorrectPresentationRoot() {
		// removing the presentationFolder created by the firstTimeSetup
		folderService.deleteFolder(folderService.getPresentationRoot());

		PresentationFolder folder = TestDataFactory.getPresentationFolder("Presentation Root", null);
		folderService.persistFolder(folder);
		assertEquals(folder, folderService.getPresentationRoot());
	}

	/**
	 * Get no presentation root -> AssertionError
	 */
	@Test
	void getNoPresentationRoot() throws Throwable {
		folderService.deleteFolder(folderService.getPresentationRoot());
		EJBException thrown = assertThrows(EJBException.class, () -> {
			folderService.getPresentationRoot();
		});
		assertTrue(thrown.getCause() instanceof AssertionError);
	}

	/**
	 * Get multiple presentation roots -> AssertionError
	 */
	@Test
	void getMultiplePresentationRoots() {

		folderService
				.persistFolder(TestDataFactory.getPresentationFolder("Presentation Root 1", null));
		folderService
				.persistFolder(TestDataFactory.getPresentationFolder("Presentation Root 2", null));

		EJBException thrown = assertThrows(EJBException.class, () -> {
			folderService.getPresentationRoot();
		});
		assertTrue(thrown.getCause() instanceof NonUniqueResultException);
	}

	/**
	 * Get folder with managing rights
	 */
	@Test
	void getFolderWithManagingRights() {

		UserGroup group = TestDataFactory.getUserGroup("User Group");
		userGroupService.persistUserGroup(group);

		PresentationFolder folder = TestDataFactory.getPresentationFolder("Presentation Folder", null);
		folder.addUserGroupRight(group, AccessRight.getFull());
		folder.addUserRight(owner, AccessRight.getFull());
		folderService.persistFolder(folder);

		PresentationFolder getFolderWithRightsFromDB = folderService.getFolderWithManagingRights(folder,
				PresentationFolder.class)
																	.orElseThrow(AssertionError::new);
		assertEquals(folder, getFolderWithRightsFromDB);
		assertTrue(getFolderWithRightsFromDB.getManagingUserGroups().containsKey(group));
		assertTrue(getFolderWithRightsFromDB.getManagingUsers().containsKey(owner));
	}

	/**
	 * Get content folder by ID
	 */
	@Test
	void getContentFolderById() {

		ContentFolder folder = TestDataFactory.getContentFolder("Content Folder", null);
		folderService.persistFolder(folder);

		assertEquals(folder, folderService.getContentFolderById(folder.getId())
													.orElseThrow(AssertionError::new));
	}

	/**
	 * Get presentation folder by ID
	 */
	@Test
	void getPresentationFolderById() {

		PresentationFolder folder = TestDataFactory.getPresentationFolder("Presentation Folder", null);
		folderService.persistFolder(folder);

		assertEquals(folder, folderService.getPresentationFolderById(folder.getId())
													.orElseThrow(AssertionError::new));
	}

	/**
	 * Get number of content folders
	 */
	@Test
	void countContentFolders() {

		for (int i = 0; i < 3; i++) {
			folderService.persistFolder(
					TestDataFactory.getContentFolder("Content Folder", null));
		}
		// the 3 added Folders and the root Folder, so 4 Folders should be counted
		assertEquals(4, folderService.getNoOfContentFolders());
	}

	/**
	 * Get number of presentation folders
	 */
	@Test
	void countPresentationFolders() {

		for (int i = 0; i < 3; i++) {
			folderService
					.persistFolder(TestDataFactory.getPresentationFolder("Content Folder", null));
		}

		// the 3 added Folders and the root Folder, so 4 Folders should be counted
		assertEquals(4, folderService.getNoOfPresentationFolder());
	}

	/**
	 * Get content folder with lazy data
	 */
	@Test
	void getContentFolderWithLazyData() {

		ContentFolder folder = TestDataFactory.getContentFolder("Content Folder", null);
		folderService.persistFolder(folder);

		ContentFolder folderWithLazyData = folderService.getContentFolderWithLazyData(folder);
		assertEquals(folder, folderWithLazyData);

		assertTrue(folderWithLazyData.getChildrenFolder().isEmpty());
		assertTrue(folderWithLazyData.getChildrenCourses().isEmpty());
		assertTrue(folderWithLazyData.getChildrenExercises().isEmpty());
		assertTrue(folderWithLazyData.getManagingUsers().isEmpty());
		assertTrue(folderWithLazyData.getManagingUserGroups().isEmpty());
		assertTrue(folderWithLazyData.getInheritedManagingUsers().isEmpty());
		assertTrue(folderWithLazyData.getInheritedManagingUserGroups().isEmpty());
	}

	/**
	 * Get presentation folder with lazy data
	 */
	@Test
	void getPresentationFolderWithLazyData() {

		PresentationFolder folder = TestDataFactory.getPresentationFolder("Presentation Folder", null);
		folderService.persistFolder(folder);

		PresentationFolder folderWithLazyData = folderService.getPresentationFolderWithLazyData(folder);
		assertEquals(folder, folderWithLazyData);

		assertTrue(folderWithLazyData.getChildrenFolder().isEmpty());
		// assertTrue(folderWithLazyData.get.getChildrenCourseOffers().isEmpty());
		assertTrue(folderWithLazyData.getManagingUsers().isEmpty());
		assertTrue(folderWithLazyData.getManagingUserGroups().isEmpty());
		assertTrue(folderWithLazyData.getInheritedManagingUsers().isEmpty());
		assertTrue(folderWithLazyData.getInheritedManagingUserGroups().isEmpty());
	}

}
