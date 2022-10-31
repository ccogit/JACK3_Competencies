package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Click.rightClick;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Misc.waitUntilPageHasLoaded;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage.Permission;

public class MyWorkspacePage {

	private static final String CONTENT_TREE_ID = "treeForm:contentTree:";

	private static final String SHOW_INTERNAL_DESCRIPTIONS_ID = "treeForm:descriptionSwitch";
	private static final String SHOW_TAGS_ID = "treeForm:tagSwitch";
	private static final String SEARCH_INPUT_ID = "treeForm:searchContentTree";

	private static final String SHARED_CONTENT_ID = CONTENT_TREE_ID + "1:sharedFolderName";
	private static final String PERSONAL_FOLDER_ID = CONTENT_TREE_ID + "0:personalFolderName";
	
	private static final String DESCRIPTION_SWITCH = "treeForm:descriptionSwitch";
	private static final String TAG_SWITCH = "treeForm:tagSwitch";
	
	//ContextMenu options for folders:
	private static final String CREATE_NEW_FOLDER_ID = "treeForm:contextMenuContentTreePersonalFolderCreateFolder";
	private static final String CREATE_NEW_COURSE_ID = "treeForm:contextMenuContentTreePersonalFolderCreateCourse";
	private static final String CREATE_NEW_EXERCISE_ID = "treeForm:contextMenuContentTreePersonalFolderCreateExercise";
	private static final String OPEN_EDIT_RIGHTS_MENU_ID = "treeForm:contextMenuContentTreePersonalFolderEditRights";
	private static final String IMPORT_EXERCISE_ID = "treeForm:contextMenuContentTreePersonalFolderImport";
	private static final String IMPORT_JACK2_EXERCISE_ID = "treeForm:contextMenuContentTreePersonalFolderJack2ExerciseImport";
	private static final String RENAME_FOLDER_ID = "treeForm:contextMenuContentTreeEmptyFolderRenameFolder";
	private static final String DELETE_FOLDER_ID = "treeForm:contextMenuContentTreeEmptyFolderDeleteFolder";

	private static final String NEW_FOLDER_NAME_ID = ":newFolderName";
	private static final String RENAME_FOLDER = "treeForm:contextMenuContentTreeFolderRenameFolder";
	private static final String RENAME_EMPTY_FOLDER = "treeForm:contextMenuContentTreeEmptyFolderRenameFolder";
	private static final String RENAME_NOCHANGE_FOLDER = "contextMenuContentTreeNoChangeFolderRenameFolder";
	private static final String RENAME_FOLDER_TEXT = "renameFolderDialogForm:folderRenameText";
	private static final String RENAME_FOLDER_OK = "renameFolderDialogForm:renameFolderDialogOk";

	//ContextMenu options for Courses:
	private static final String DUPLICATE_COURSE_ID = "treeForm:contextMenuContentTreeCourseDuplicate";
	private static final String DELETE_COURSE_ID = "treeForm:contextMenutContentTreeCourseDeleteCourse";

	private static final String DUPLICATE_COURSE_DIALOG_INPUT = "duplicateCourseForm:duplicateCourseDialogInput";
	private static final String DUPLICATE_COURSE_OK = "duplicateCourseForm:duplicateCourseDialogOk";

	private static final String NEW_COURSE_NAME_ID = ":newCourseName";

	//ContextMenu options for exercises:
	private static final String DELETE_EXERCISE_ID = "treeForm:contextMenutContentTreeCourseDeleteExercise";
	private static final String DUPLICATE_EXERCISE_ID = "treeForm:contextMenutContentTreeCourseCopyExercise";
	private static final String EXPORT_EXERCISE_ID = "treeForm:contextMenuContentTreeExportExercise";

	private static final String NEW_EXERCISE_NAME_ID = ":newExerciseName";
	private static final String DUPLICATE_EXERCISE_DIALOG_ID = "duplicateExerciseForm:duplicateExerciseDialog";

	public static void navigateToPage() {
		navigate(JackUrl.MY_WORKSPACE);
	}

	private static WebElement getElement(String name) {
		// Search through xPath the WebElement which contains the folderName
		try {
			waitClickable(getByForElement(name));
			return Driver.get().findElement(getByForElement(name));
		} catch (TimeoutException e) {
			throw new TimeoutException("Element could not be found! Maybe the parent Folder was not expanded", e);
		}
	}

	private static List<WebElement> getElements(String name) {
		// Search through xPath the WebElement which contains the folderName
		waitClickable(By.xpath("//*[text()[contains(., '" + name + "')]]"));
		return Driver.get().findElements(By.xpath("//*[text()[contains(., '" + name + "')]]"));
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
			throw new Error("The WebElement '" + element + "' could not be found");
		}
	}

	public static WebElement getPersonalFolder() {
		waitClickable(By.id(PERSONAL_FOLDER_ID));
		return find(PERSONAL_FOLDER_ID);
	}

	/**
	 * find a single Exercise using his name
	 *
	 * @param name
	 *            the name of the Exercise
	 * @return returns the Exercise with the given name
	 */
	public static WebElement getExercise(String name) {
		return getElement(name);
	}

	/**
	 * Use this Method if there are multiple exercises with the same name
	 *
	 * @param name
	 *            the name of the Exercises
	 * @return returns a list of all exercises with this name
	 */
	public static List<WebElement> getExercises(String name) {
		return getElements(name);
	}

	public static void openExercise(WebElement exercise) {
		waitClickable(exercise);
		exercise.click();
		//Wait until the page is loaded
		ExerciseEditPage.getTitle();
		waitUntilActionBarIsNotActive();
	}

	/**
	 * find a single Course using his name
	 *
	 * @param name
	 *            the name of the Course
	 * @return returns the Course with the given name
	 */
	public static WebElement getCourse(String name) {
		return getElement(name);
	}

	public static void openCourse(WebElement course) {
		waitClickable(course);
		course.click();
		CourseEditPage.getTitle();
		waitUntilPageHasLoaded();
	}

	/**
	 * Use this Method if there are multiple Courses with the same name
	 *
	 * @param name
	 *            the name of the Courses
	 * @return returns a list of all Courses with this name
	 */
	public static List<WebElement> getCourses(String name) {
		return getElements(name);
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
	 * Use this Method if there are multiple Folders with the same name
	 *
	 * @param name
	 *            the name of the Folders
	 * @return returns a list of all Folders with this name
	 */
	public static List<WebElement> getFolders(String name) {
		return getElements(name);
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
		waitUntilActionBarIsNotActive();
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

	public static void checkThatFolderHasNoContextMenu(WebElement folder) {
		waitClickable(folder);
		rightClick(folder);

		String folderId = folder.getAttribute("id");

		// We have different id's depending on if the folder is the Personal Folder,
		// the folder has childElements and the folder has no child Elements.
		// Because of this we have to check which case we have
		if (folderId.equals(PERSONAL_FOLDER_ID)) {
			//folder is the PersonalFolder
			waitNotClickable(By.id(CREATE_NEW_FOLDER_ID));
		} else if (folderId.contains("empty")) {
			//The folder is empty
			waitNotClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "Empty")));
		} else if (folderId.toLowerCase().contains("nochange")) {
			//The Folder i a noChange Folder
			waitNotClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "NoChange")));
		} else {
			//The folder has child Elements
			waitNotClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "")));
		}
	}

	public static void checkThatFolderHasNoContextMenu(String folderName) {
		checkThatFolderHasNoContextMenu(getFolder(folderName));
	}

	public static void createFolder(WebElement parentFolder, String newFolderName) {
		waitClickable(parentFolder);

		String levelOfElement = getLevelOfElement(parentFolder);
		String parentFolderId = parentFolder.getAttribute("id");
		rightClick(parentFolder);
		// We have different id's depending on if the parentFolder is the Personal Folder,
		// the parentFolder has childElements and the parentFolder has no child Elements.
		// Because of this we have to check which case we have
		if (parentFolderId.equals(PERSONAL_FOLDER_ID)) {
			//Parentfolder is the PersonalFolder
			waitClickable(By.id(CREATE_NEW_FOLDER_ID));
			click(By.id(CREATE_NEW_FOLDER_ID));
		} else if (parentFolderId.contains("empty")) {
			//The Parentfolder is empty
			waitClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "Empty")));
			click(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "Empty")));
		} else if (parentFolderId.toLowerCase().contains("nochange")) {
			//The Parentfolder is a noChange Folder
			waitClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "NoChange")));
			click(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "NoChange")));
		} else {
			//The Parentfolder has child Elements
			waitClickable(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "")));
			click(By.id(CREATE_NEW_FOLDER_ID.replace("Personal", "")));
		}

		waitClickable(By.id(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_FOLDER_NAME_ID));
		find(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_FOLDER_NAME_ID).sendKeys(newFolderName + Keys.ENTER);

		//Lets the Program wait until the jack successful proceeds the drag and drop changes
		getElement(newFolderName);
	}

	public static void renameFolder(WebElement folder, String newName) {
		waitClickable(folder);
		String folderId = folder.getAttribute("id");
		rightClick(folder);
		// We have different id's depending on if the folder has childElements or the folder has no child Elements.
		// Because of this we have to check which case we have

		if (folderId.contains("empty")) {
			//The Parentfolder is empty
			waitClickable(By.id(RENAME_EMPTY_FOLDER));
			click(find(RENAME_EMPTY_FOLDER));
			find(RENAME_FOLDER_TEXT).sendKeys(newName);

			//Confirm the new Name
			find(RENAME_FOLDER_OK).click();
		} else if (folderId.toLowerCase().contains("nochange")) {
			//It is a noChange Folder
			waitClickable(By.id(RENAME_NOCHANGE_FOLDER));
			click(find(RENAME_NOCHANGE_FOLDER));
			find(RENAME_FOLDER_TEXT).sendKeys(newName);

			//Confirm the new Name
			find(RENAME_FOLDER_OK).click();
		} else {
			//The Parentfolder has child Elements
			waitClickable(By.id(RENAME_FOLDER));
			click(find(RENAME_FOLDER));
			find(RENAME_FOLDER_TEXT).sendKeys(newName);

			//Confirm the new Name
			find(RENAME_FOLDER_OK).click();
		}
	}

	public static void removeFolder(WebElement folder) {
		waitVisible(folder);
		waitClickable(folder);
		rightClick(folder);
		// We have different id's depending on if the folder has childElements or the folder has no child Elements.
		// Because of this we have to check which case we have

		if (folder.getAttribute("id").contains("empty")) {
			//The Parentfolder is empty
			waitClickable(By.id(DELETE_FOLDER_ID));
			click(find(DELETE_FOLDER_ID));
		} else {
			//The Parentfolder has child Elements
			waitClickable(By.id(DELETE_FOLDER_ID.replace("empty", "")));
			click(find(DELETE_FOLDER_ID.replace("empty", "")));
		}
	}

	public static void createExercise(WebElement parentFolder, String exerciseName) {
		waitClickable(parentFolder);

		String levelOfElement = getLevelOfElement(parentFolder);
		String parentId = parentFolder.getAttribute("id");

		rightClick(parentFolder);
		if (parentId.equals(PERSONAL_FOLDER_ID)) {
			waitClickable(By.id(CREATE_NEW_EXERCISE_ID));
			click(By.id(CREATE_NEW_EXERCISE_ID));
		} else if (parentId.contains("empty")) {
			waitClickable(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "Empty")));
			click(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "Empty")));
		} else if (parentId.toLowerCase().contains("nochange")) {
			waitClickable(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "NoChange")));
			click(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "NoChange")));
		} else {
			waitClickable(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "")));
			click(By.id(CREATE_NEW_EXERCISE_ID.replace("Personal", "")));
		}

		waitClickable(By.id(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_EXERCISE_NAME_ID));
		find(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_EXERCISE_NAME_ID).sendKeys(exerciseName + Keys.ENTER);
	}

	public static void duplicateExercise(WebElement exercise, String nameForTheDuplicate) {
		waitClickable(exercise);
		rightClick(exercise);
		waitClickable(By.id(DUPLICATE_EXERCISE_ID));
		click(find(DUPLICATE_EXERCISE_ID));
		waitClickable(By.id(DUPLICATE_EXERCISE_DIALOG_ID));
		find(DUPLICATE_EXERCISE_DIALOG_ID + "Input").sendKeys(nameForTheDuplicate);
		click(find(DUPLICATE_EXERCISE_DIALOG_ID + "Ok"));
	}

	public static void duplicateCourse(WebElement course, String nameForTheDuplicate, String nameOfTargetFolder) {
		waitClickable(course);
		rightClick(course);
		waitClickable(By.id(DUPLICATE_COURSE_ID));
		click(find(DUPLICATE_COURSE_ID));
		waitClickable(By.id(DUPLICATE_COURSE_DIALOG_INPUT));
		find(DUPLICATE_COURSE_DIALOG_INPUT).sendKeys(nameForTheDuplicate);
		click(find(DUPLICATE_COURSE_OK));
		waitUntilActionBarIsNotActive();
	}

	public static void createCourse(WebElement parentFolder, String courseName) {
		waitClickable(parentFolder);

		String levelOfElement = getLevelOfElement(parentFolder);
		String parentId = parentFolder.getAttribute("id");

		rightClick(parentFolder);
		if (parentId.equals(PERSONAL_FOLDER_ID)) {
			waitClickable(By.id(CREATE_NEW_COURSE_ID));
			click(By.id(CREATE_NEW_COURSE_ID));
		} else if (parentId.contains("empty")) {
			waitClickable(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "Empty")));
			click(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "Empty")));
		} else if (parentId.toLowerCase().contains("nochange")) {
			waitClickable(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "NoChange")));
			click(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "NoChange")));
		} else {
			waitClickable(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "")));
			click(By.id(CREATE_NEW_COURSE_ID.replace("Personal", "")));
		}

		waitClickable(By.id(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_COURSE_NAME_ID));
		find(CONTENT_TREE_ID + levelOfElement + "_0" + NEW_COURSE_NAME_ID).sendKeys(courseName + Keys.ENTER);
	}

	public static void moveElement(WebElement elementToBeMoved, WebElement target) {
		waitUntilActionBarIsNotActive();
		waitClickable(elementToBeMoved);
		waitClickable(target);

		// Dragged and dropped.
		new Actions(Driver.get()).dragAndDrop(elementToBeMoved, target).build().perform();
		waitUntilActionBarIsNotActive();
	}
	
	public static By getByForElement(String name) {
		return By.xpath("//*[contains(text(), '" + name + "')]");
	}

	public static void moveElementWithChangingOwner(WebElement elementToBeMoved, WebElement target,
			boolean justMoveACopy) {
		moveElement(elementToBeMoved, target);

		String id = elementToBeMoved.getAttribute("id");
		if (id.contains("Course")) {
			//We moved a Course
			if (justMoveACopy) {
				waitClickable(By.id("moveCourseForm:buttonDialogMoveCourseDuplicate"));
				click(find("moveCourseForm:buttonDialogMoveCourseDuplicate"));
			} else {
				waitClickable(By.id("moveCourseForm:buttonDialogMoveCourse"));
				click(find("moveCourseForm:buttonDialogMoveCourse"));
			}

		} else if (id.contains("Exercise")) {
			//We moved an Exercise
			if (justMoveACopy) {
				waitClickable(By.id("moveExerciseForm:buttonDialogMoveExerciseDuplicate"));
				click(find("moveExerciseForm:buttonDialogMoveExerciseDuplicate"));
			} else {
				waitClickable(By.id("moveExerciseForm:buttonDialogMoveExercise"));
				click(find("moveExerciseForm:buttonDialogMoveExercise"));
			}
		} else {
			//We moved a Folder
			if (justMoveACopy) {
				// see JACK/jack3-core#561
				throw new UnsupportedOperationException("It is not possible yet to duplicate folders.");
			} else {
				waitClickable(By.id("moveFolderForm:buttonDialogMoveFolder"));
				click(find("moveFolderForm:buttonDialogMoveFolder"));
			}
		}
		
		waitUntilActionBarIsNotActive();
	}

	public static UserRightsTablePage openUserRightsForFolder(WebElement folder) {
		waitClickable(folder);

		rightClick(folder);
		String folderId = folder.getAttribute("id");
		// We have different id's depending on if the folder is the Personal Folder,
		// the folder has childElements and the folder has no child Elements.
		// Because of this we have to check which case we have

		if (folderId.equals(PERSONAL_FOLDER_ID)) {
			//folder is the PersonalFolder
			waitClickable(By.id(OPEN_EDIT_RIGHTS_MENU_ID));
			click(By.id(OPEN_EDIT_RIGHTS_MENU_ID));
		} else if (folderId.contains("empty")) {
			//The folder is empty
			waitClickable(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "Empty")));
			click(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "Empty")));
		} else if (folderId.toLowerCase().contains("nochange")) {
			//It is a noChange Folder
			waitClickable(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "NoChange")));
			click(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "NoChange")));
		} else {
			//The Folder has child Elements
			waitClickable(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "")));
			click(By.id(OPEN_EDIT_RIGHTS_MENU_ID.replace("Personal", "")));
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
	
	public static void displayTags() {
		waitClickable(By.id(TAG_SWITCH));
		find(TAG_SWITCH).click();
	}
	
	public static void displayInternalDescriptions() {
		waitClickable(By.id(DESCRIPTION_SWITCH));
		find(DESCRIPTION_SWITCH).click();
	}

}