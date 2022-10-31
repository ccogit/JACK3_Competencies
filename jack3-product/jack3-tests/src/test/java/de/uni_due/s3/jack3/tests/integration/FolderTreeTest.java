package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

/**
 * Extra class for testing a folder hierarchy.
 */
class FolderTreeTest extends AbstractBasicTest {

	@Inject
	private FolderService folderService;

	private ContentFolder root;
	private ContentFolder[] subFolders = new ContentFolder[6];

	/*-
	 * The following hierarchy is used:
	 *
	 *      root
	 *     /    \
	 *    0      1
	 *   / \    / \
	 *  2   3  4   5
	 */

	/**
	 * Create folder hierarchy
	 */
	@Override
	@BeforeEach
	protected void doBeforeTest() {
		super.doBeforeTest();

		// persist all folders
		root = folderService.getContentFolderWithLazyData(folderService.getContentRoot());
		subFolders[0] = createContentFolder("Folder 0");
		subFolders[1] = createContentFolder("Folder 1");
		subFolders[2] = createContentFolder("Folder 2", 0);
		subFolders[3] = createContentFolder("Folder 3", 0);
		subFolders[4] = createContentFolder("Folder 4", 1);
		subFolders[5] = createContentFolder("Folder 5", 1);

		// get folders with lazy data
		for (int i = 0; i < subFolders.length; i++) {
			subFolders[i] = folderService.getContentFolderWithLazyData(subFolders[i]);
		}
	}

	/**
	 * Create contentFolder as sub folder of 'root'
	 */
	private ContentFolder createContentFolder(String folderName) {
		ContentFolder newFolder = new ContentFolder(folderName);
		root.addChildFolder(newFolder);

		// merge root
		folderService.persistFolder(newFolder);
		root = folderService.mergeContentFolder(root);
		return newFolder;
	}

	/**
	 * Create contentFolder as sub folder located in parentFolder
	 */
	private ContentFolder createContentFolder(String folderName, int parentIndex) {

		ContentFolder newFolder = new ContentFolder(folderName);

		subFolders[parentIndex].addChildFolder(newFolder);

		// merge folder in the array
		folderService.persistFolder(newFolder);
		subFolders[parentIndex] = folderService.mergeContentFolder(subFolders[parentIndex]);
		return newFolder;
	}

	/**
	 * Check if two collections contain the same elements, order not matters
	 */
	private void assertCollectionEquality(Collection<?> expected, Collection<?> actual) {
		assertEquals(expected.size(), actual.size());
		assertTrue(expected.containsAll(actual));
		assertTrue(actual.containsAll(expected));
	}

	/**
	 * Check if all folders are persisted
	 */
	@Test
	void getNumberOfFolders() {

		assertEquals(7, folderService.getNoOfContentFolders());
		List<ContentFolder> allFolders = new ArrayList<>(7);
		allFolders.addAll(Arrays.asList(subFolders));
		allFolders.add(root);
		assertCollectionEquality(allFolders, baseService.findAll(ContentFolder.class));
	}

	/**
	 * Get correct root
	 */
	@Test
	void getRoot() {
		assertEquals(root, folderService.getContentRoot());
	}

	/**
	 * Get childrens of the folders from FolderService
	 */
	@Test
	void getChildrenFolderFromService() {
		// root, folder 0 and folder 1 should have childrens
		assertCollectionEquality(Arrays.asList(subFolders[0], subFolders[1]), folderService.getChildrenFolder(root));
		assertCollectionEquality(Arrays.asList(subFolders[2], subFolders[3]),
				folderService.getChildrenFolder(subFolders[0]));
		assertCollectionEquality(Arrays.asList(subFolders[4], subFolders[5]),
				folderService.getChildrenFolder(subFolders[1]));

		// folder 2-5 should not have any children
		assertTrue(folderService.getChildrenFolder(subFolders[2]).isEmpty());
		assertTrue(folderService.getChildrenFolder(subFolders[3]).isEmpty());
		assertTrue(folderService.getChildrenFolder(subFolders[4]).isEmpty());
		assertTrue(folderService.getChildrenFolder(subFolders[5]).isEmpty());
	}

	/**
	 * Get Breadcrumb as folder list of folders
	 */
	@Test
	void getBreadcrumb() {
		// only breadcrums of folder 2-5
		assertTrue(root.getBreadcrumb().isEmpty());
		
		assertTrue(subFolders[0].getBreadcrumb().size() == 1);
		assertTrue(subFolders[1].getBreadcrumb().size() == 1);

		assertEquals(Arrays.asList(root, subFolders[0]), subFolders[2].getBreadcrumb());
		assertEquals(Arrays.asList(root, subFolders[0]), subFolders[3].getBreadcrumb());
		assertEquals(Arrays.asList(root, subFolders[1]), subFolders[4].getBreadcrumb());
		assertEquals(Arrays.asList(root, subFolders[1]), subFolders[5].getBreadcrumb());
	}

	/**
	 * Get parent folder
	 */
	@Test
	void getParentFolder() {
		assertNull(root.getParentFolder());
		assertEquals(root, subFolders[0].getParentFolder());
		assertEquals(root, subFolders[1].getParentFolder());
		assertEquals(subFolders[0], subFolders[2].getParentFolder());
		assertEquals(subFolders[0], subFolders[3].getParentFolder());
		assertEquals(subFolders[1], subFolders[4].getParentFolder());
		assertEquals(subFolders[1], subFolders[5].getParentFolder());
	}

	/**
	 * Check if Folder.isChildOf works correctly
	 */
	@Test
	void checkIsChildOf() {
		assertTrue(subFolders[2].isChildOf(subFolders[0]));
		assertFalse(subFolders[0].isChildOf(subFolders[2]));

		assertTrue(subFolders[2].isChildOf(root));
		assertFalse(root.isChildOf(subFolders[2]));

		assertTrue(subFolders[0].isChildOf(root));
		assertFalse(root.isChildOf(subFolders[0]));

		assertFalse(subFolders[2].isChildOf(subFolders[5]));
		assertFalse(subFolders[5].isChildOf(subFolders[2]));

		assertFalse(subFolders[4].isChildOf(subFolders[0]));
		assertFalse(subFolders[0].isChildOf(subFolders[4]));

		assertFalse(root.isChildOf(root.getParentFolder()));
	}

	/**
	 * Get childrens of the folders from Folder
	 */
	@Test
	void getChildrenFolderFromFolder() {
		// root, folder 0 and folder 1 should have childrens
		assertCollectionEquality(Arrays.asList(subFolders[0], subFolders[1]), root.getChildrenFolder());
		assertCollectionEquality(Arrays.asList(subFolders[2], subFolders[3]), subFolders[0].getChildrenFolder());
		assertCollectionEquality(Arrays.asList(subFolders[4], subFolders[5]), subFolders[1].getChildrenFolder());

		// folder 2-5 should not have any children
		assertTrue(subFolders[2].getChildrenFolder().isEmpty());
		assertTrue(subFolders[3].getChildrenFolder().isEmpty());
		assertTrue(subFolders[4].getChildrenFolder().isEmpty());
		assertTrue(subFolders[5].getChildrenFolder().isEmpty());
	}

	/**
	 * Remove children
	 */
	@Test
	void removeChildFolder() {

		root.removeChildFolder(subFolders[1]);

		// first merge the sub folder (otherwise it is not removed from root)
		subFolders[1] = folderService.mergeContentFolder(subFolders[1]);
		subFolders[1] = folderService.getContentFolderWithLazyData(subFolders[1]);

		// then merge root folder
		root = folderService.mergeContentFolder(root);
		root = folderService.getContentFolderWithLazyData(root);

		/*-
		 * Folder 1 is removed:
		 *
		 *      root
		 *     /    \
		 *    0     (1)
		 *   / \    / \
		 *  2   3 (4) (5)
		 */

		assertNull(subFolders[1].getParentFolder());
		assertFalse(root.getChildrenFolder().contains(subFolders[1]));
		assertFalse(subFolders[1].isChildOf(root));
	}
}
