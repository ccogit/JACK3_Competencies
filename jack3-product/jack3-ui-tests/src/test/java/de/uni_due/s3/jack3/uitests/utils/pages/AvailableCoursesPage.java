package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithAjax;
import static de.uni_due.s3.jack3.uitests.utils.Click.rightClick;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.uitests.utils.Misc;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage.Permission;
public class AvailableCoursesPage {

	private static final String CONTENT_TREE_ID = "treeForm:courseOfferTree:";

	//folder types:
	private static final String EMPTY_FOLDER_NAME = ":emptyFolderName";
	private static final String FOLDER_NAME = ":folderName";

	private static final String MENU_BUTTON_PRESENTATION_VIEW = "treeForm:menubuttonPresentationView2_button";
	private static final String SHOW_ALL_COURSEOFFERS = "treeForm:showAllCourseOffersViewActive";
	private static final String SHOW_ONLY_MY_COURSEOFFERS = "treeForm:showMyCourseOffersView";

	private static final String SEARCH_COURSEOFFERS = "treeForm:searchCourseOffers";

	private static final String CREATE_FOLDER_IN_EMPTY_FOLDER = "treeForm:contextCreateFolder";
	private static final String CREATE_FOLDER_IN_NO_DELETE_FOLDER = "treeForm:onlyAddFolderCreateFolder";
	private static final String CREATE_FOLDER_IN_STANDARD_FOLDER = "treeForm:contextEmptyFolderCreate";

	private static final String DELETE_STANDARD_FOLDER = "treeForm:contextEmptyFolderDeleteFolder";
	private static final String DELETE_EMPTY_FOLDER = "treeForm:contextCourseOfferTreeDeleteFolder";

	private static final String RENAME_EMPTY_FOLDER = "treeForm:contextRenameEmptyFolder";
	private static final String RENAME_STANDARD_FOLDER = "treeForm:contextFolderRename";

	private static final String RENAME_FOLDER_INPUT = "renameFolderDialogForm:folderRenameText";
	private static final String RENAME_FOLDER_INPUT_OK = "renameFolderDialogForm:renameFolderDialogOk";


	private static final String OPEN_EDIT_RIGHTS_EMPTY_FOLDER = "treeForm:contextEditRights";
	private static final String OPEN_EDIT_RIGHTS_STANDARD_FOLDER = "treeForm:contextEmptyFolderEditRights";

	private static final String NEW_FOLDER_NAME_ID = ":newFolderName";

	private static final String CREATE_COURSEOFFER_IN_NO_DELETE_FOLDER = "treeForm:onlyAddFolderNewCourseOffer";
	private static final String CREATE_COURSEOFFER_IN_EMPTY_FOLDER = "treeForm:contextNewCourseOffer";
	private static final String CREATE_COURSEOFFER_IN_STANDARD_FOLDER = "treeForm:contextEmptyFolderNewCourseOffer";
	private static final String NEW_COURSEOFFER_NAME_ID = ":newCourseOfferName";

	public static void navigateToPage() {
		navigate(JackUrl.AVAILABLE_COURSES);
	}

	public static void showOnlyMyCourseOffers() {
		waitClickable(By.id(MENU_BUTTON_PRESENTATION_VIEW));
		click(find(MENU_BUTTON_PRESENTATION_VIEW));
		waitClickable(By.id(SHOW_ONLY_MY_COURSEOFFERS));
		click(find(SHOW_ONLY_MY_COURSEOFFERS));
	}

	public static void showAllCourseOffers() {
		waitClickable(By.id(MENU_BUTTON_PRESENTATION_VIEW));
		click(find(MENU_BUTTON_PRESENTATION_VIEW));
		waitClickable(By.id(SHOW_ALL_COURSEOFFERS));
		click(find(SHOW_ALL_COURSEOFFERS));
	}

	public static void searchForCourseOffers(String searchString) {
		waitClickable(By.id(SEARCH_COURSEOFFERS));
		find(SEARCH_COURSEOFFERS).sendKeys(searchString);
	}

	private static WebElement getElement(String name) {
		waitUntilActionBarIsNotActive();
		final String contentTreeWithoutSuffix = CONTENT_TREE_ID.substring(0, CONTENT_TREE_ID.length() - 1);
		waitClickable(By.id(contentTreeWithoutSuffix));
		// Search through xPath the WebElement which contains the folderName
		// Note that we have to search in the tree, because common search strings like 'CourseOffer' are also part of
		// some JavaScripts, which would be found instead of the tree element without the first line.
		final WebElement result = find(By.id(contentTreeWithoutSuffix))
				.findElement(By.xpath(".//*[contains(text(), '" + name + "')]"));
		try {
			waitClickable(result);
			return result;
		} catch (TimeoutException e) {
			throw new TimeoutException(
					"Element '" + name + "' could not be found! Maybe the parent Folder was not expanded", e);
		}
	}

	private static String getLevelOfElement(WebElement element) {
		String id = element.getAttribute("id");
		Pattern MY_PATTERN = Pattern.compile(":\\d(.*\\d)?:");
		Matcher m = MY_PATTERN.matcher(id);

		if (m.find()) {
			String result = m.group();
			result = result.replace(":", "");
			return result;
		} else {
			throw new NotFoundException("The WebElement '" + element + "' could not be found");
		}
	}

	private static String getLevelOfNoDeleteFolder(WebElement element) {
		WebElement tree = find(CONTENT_TREE_ID.substring(0, CONTENT_TREE_ID.length() - 1));
		WebElement liElement = tree.findElements(By.tagName("li")).stream()
				.filter(ele -> "onlyAddFolder".equals(ele.getAttribute("data-nodetype")))
				.filter(ele -> findChildren(ele).contains(element)).findFirst().orElseThrow(() -> new NotFoundException(
						"The 'li' Element which contains " + element + " could not be found"));

		return liElement.getAttribute("data-rowkey");
	}
	/**
	 * find a single CourseOffer using his name
	 * 
	 * @param name
	 *            the name of the CourseOffer
	 * @return returns the CourseOffer with the given name
	 */
	public static WebElement getCourseOffer(String name) {
		return getElement(name);
	}

	public static void openCourseOffer(String courseOfferName) {
		openCourseOffer(getCourseOffer(courseOfferName));
	}

	public static void openCourseOffer(WebElement courseOffer) {
		waitClickable(courseOffer);
		click(courseOffer);
	}

	/**
	 * find a single Folder using his name
	 * 
	 * @param name
	 *            the name of the Folder
	 * @return returns the Folder with the given name
	 */
	public static WebElement getFolder(String name) {
		return getElement(name);
	}

	/**
	 * Expands a single Folder by its name
	 * 
	 * @param folderName
	 *            the Name of the Folder
	 */
	public static void expandFolder(String folderName) {
		expandFolder(getElement(folderName));
	}

	/**
	 * Expands a single Folder
	 * 
	 * @param folder
	 *            the Folder which gets expanded
	 */
	public static void expandFolder(WebElement folder) {
		waitClickable(folder);
		click(folder);
	}

	/**
	 * Expands multiple Folders by their names
	 * 
	 * @param folderNames
	 *            the names of the folders
	 */
	public static void expandFolders(String... folderNames) {
		for (String folderName : folderNames) {
			expandFolder(folderName);
		}
	}

	/**
	 * Expands multiple folders
	 * 
	 * @param folders
	 *            the folders which will be expanded
	 */
	public static void expandFolders(WebElement... folders) {
		for (WebElement folder : folders) {
			expandFolder(folder);
		}
	}

	public static void createFolder(WebElement parentFolder, String newFolderName) {
		waitClickable(parentFolder);
		String levelOfElement;
		String parentFolderId = parentFolder.getAttribute("id");

		rightClick(parentFolder);

		if (parentFolderId.isBlank()) {
			throw new IllegalArgumentException("Passed 'parentFolder' parameter does not have an ID.");
		} else if (parentFolderId.toLowerCase().contains("empty")) {
			waitClickable(By.id(CREATE_FOLDER_IN_EMPTY_FOLDER));
			click(find(CREATE_FOLDER_IN_EMPTY_FOLDER));
			levelOfElement = getLevelOfElement(parentFolder);
		} else if (parentFolderId.contains("onlyAddFolder")) {
			waitClickable(By.id(CREATE_FOLDER_IN_NO_DELETE_FOLDER));
			click(find(CREATE_FOLDER_IN_NO_DELETE_FOLDER));
			levelOfElement = getLevelOfElement(parentFolder);
		} else {
			waitClickable(By.id(CREATE_FOLDER_IN_STANDARD_FOLDER));
			click(find(CREATE_FOLDER_IN_STANDARD_FOLDER));
			levelOfElement = getLevelOfElement(parentFolder);
		}

		waitClickable(By.id(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_FOLDER_NAME_ID));
		find(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_FOLDER_NAME_ID).sendKeys(newFolderName + Keys.ENTER);

		//Lets the Program wait until jack successful proceeds the drag and drop changes
		getElement(newFolderName);
	}

	public static WebElement createTopLevelFolder(String newFolderName) {

		waitVisible(By.id("rootFolderCreationForm:createNewRootForAdministrators"),
				"The panel for creating top-level folders is not visible. Maybe the user doesn't have Administrator rights.");

		find("rootFolderCreationForm:createNewRootName").sendKeys(newFolderName);
		clickWithAjax(By.id("rootFolderCreationForm:createNewRootButton"));

		return getFolder(newFolderName);
	}

	public static void renameFolder(WebElement folder, String newName) {

		waitClickable(folder);
		String levelOfElement;
		String folderId = folder.getAttribute("id");

		rightClick(folder);

		if (folderId.toLowerCase().contains("empty")) {
			waitClickable(By.id(RENAME_EMPTY_FOLDER));
			click(find(RENAME_EMPTY_FOLDER));
			levelOfElement = getLevelOfElement(folder);

			find(RENAME_FOLDER_INPUT).sendKeys(newName);

			//Confirm the new Name
			waitClickable(By.id(RENAME_FOLDER_INPUT_OK));
			click(find(RENAME_FOLDER_INPUT_OK));

		} else {
			waitClickable(By.id(RENAME_STANDARD_FOLDER));
			click(find(RENAME_STANDARD_FOLDER));
			levelOfElement = getLevelOfElement(folder);

			find(RENAME_FOLDER_INPUT).sendKeys(newName);

			//Confirm the new Name
			waitClickable(By.id(RENAME_FOLDER_INPUT_OK));
			click(find(RENAME_FOLDER_INPUT_OK));
		}


	}

	public static void removeFolder(WebElement folder) {
		waitClickable(folder);
		String folderId = folder.getAttribute("id");
		rightClick(folder);

		// We have different id's depending on if the folder has childElements or the folder has no child Elements.
		// Because of this we have to check which case we have
		if (folderId.toLowerCase().contains("empty")) {
			waitClickable(By.id(DELETE_EMPTY_FOLDER));
			click(find(DELETE_EMPTY_FOLDER));
		} else {
			waitClickable(By.id(DELETE_STANDARD_FOLDER));
			click(find(DELETE_STANDARD_FOLDER));
		}
		
		Misc.waitUntilPageHasLoaded();
		Misc.reloadPage();

	}

	public static void moveElement(WebElement elementToBeMoved, WebElement target) {
		waitClickable(elementToBeMoved);
		waitClickable(target);

		// Dragged and dropped.
		new Actions(Driver.get()).dragAndDrop(elementToBeMoved, target).build().perform();
		Misc.waitUntilPageHasLoaded();
		Misc.reloadPage();
	}

	public static void moveElementWithoutReload(WebElement elementToBeMoved, WebElement target) {
		waitClickable(elementToBeMoved);
		waitClickable(target);

		// Dragged and dropped.
		new Actions(Driver.get()).dragAndDrop(elementToBeMoved, target).build().perform();
	}

	public static void moveElementWithChangingOwner(WebElement elementToBeMoved, WebElement target,
			boolean justMoveACopy) {
		moveElementWithoutReload(elementToBeMoved, target);

		String id = elementToBeMoved.getAttribute("id");
		if (id.substring(id.lastIndexOf(':')).contains("Offer")) { // ID starts with "treeForm:courseOfferTree"
			//We moved a Course Offer
			if (justMoveACopy) {
				waitClickable(By.id("moveCourseOfferForm:buttonDialogMoveCourseOfferDuplicate"));
				click(find("moveCourseOfferForm:buttonDialogMoveCourseOfferDuplicate"));
			} else {
				waitClickable(By.id("moveCourseOfferForm:buttonDialogMoveCourseOffer"));
				click(find("moveCourseOfferForm:buttonDialogMoveCourseOffer"));
			}

		} else {
			//We moved a Folder
			if (justMoveACopy) {
				// see JACK/jack3-core#561
				throw new UnsupportedOperationException("It is not possible yet to duplicate folders.");
			} else {
				waitClickable(By.id("movePresentationFolderForm:buttonDialogMovePresentationFolder"));
				click(find("movePresentationFolderForm:buttonDialogMovePresentationFolder"));
			}
		}
	}

	public static UserRightsTablePage openUserRightsForFolder(WebElement folder) {
		waitClickable(folder);
		rightClick(folder);
		String folderId = folder.getAttribute("id");


		// We have different id's depending on if the folder has childElements or the folder has no child Elements.
		// Because of this we have to check which case we have
		if (folderId.toLowerCase().contains("empty")) {
			waitClickable(By.id(OPEN_EDIT_RIGHTS_EMPTY_FOLDER));
			click(find(OPEN_EDIT_RIGHTS_EMPTY_FOLDER));
		} else {
			waitClickable(By.id(OPEN_EDIT_RIGHTS_STANDARD_FOLDER));
			click(find(OPEN_EDIT_RIGHTS_STANDARD_FOLDER));
		}
		return new UserRightsTablePage();
	}

	public static UserRightsTablePage openUserRightsForFolder(String folderName) {
		return openUserRightsForFolder(getFolder(folderName));
	}

	public static void changeRightsOfLecturer(String lecturerName, String folderName, Permission... permissions) {
		changeRightsOfLecturer(lecturerName, getFolder(folderName), permissions);
	}

	public static void changeRightsOfLecturer(String lecturerName, WebElement folder, Permission... permissions) {
		UserRightsTablePage userRightsTablePage = openUserRightsForFolder(folder);
		userRightsTablePage.changeRights(lecturerName, permissions);

		userRightsTablePage.saveChanges();
	}

	public static void createCourseOffer(String parentFolderName, String courseOfferName) {
		createCourseOffer(getFolder(parentFolderName), courseOfferName);
	}



	public static void createCourseOffer(WebElement parentFolder, String courseOfferName) {
		waitClickable(parentFolder);
		String levelOfElement;
		String parentFolderId = parentFolder.getAttribute("id");

		rightClick(parentFolder);
		if (parentFolderId.toLowerCase().contains("empty")) {
			waitClickable(By.id(CREATE_COURSEOFFER_IN_EMPTY_FOLDER));
			click(find(CREATE_COURSEOFFER_IN_EMPTY_FOLDER));
			levelOfElement = getLevelOfElement(parentFolder);
		} else if (parentFolderId.contains("onlyAddFolder") || parentFolderId.isEmpty()) {
			waitClickable(By.id(CREATE_COURSEOFFER_IN_NO_DELETE_FOLDER));
			click(find(CREATE_COURSEOFFER_IN_NO_DELETE_FOLDER));
			levelOfElement = getLevelOfNoDeleteFolder(parentFolder);
		} else {
			waitClickable(By.id(CREATE_COURSEOFFER_IN_STANDARD_FOLDER));
			click(find(CREATE_COURSEOFFER_IN_STANDARD_FOLDER));
			levelOfElement = getLevelOfElement(parentFolder);
		}
		waitClickable(By.id(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_COURSEOFFER_NAME_ID));
		find(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_COURSEOFFER_NAME_ID).sendKeys(courseOfferName + Keys.ENTER);

		//Lets the Program wait until jack successful proceeds the drag and drop changes
		getElement(courseOfferName);
	}

	public static void checkElementHasNoContextMenu(WebElement element) {
		waitClickable(element);

		rightClick(element);
		String elementId = element.getAttribute("id");
		
		if (!elementId.isEmpty()) {
			throw new AssertionFailedError("The Element '" + element + "' has an context menu!");
		}
		
		try {
			waitNotClickable(By.id(CREATE_COURSEOFFER_IN_NO_DELETE_FOLDER));
		} catch (TimeoutException e) {
			throw new AssertionFailedError("The Element '" + element + "' has an context menu!");
		}
	}
}
