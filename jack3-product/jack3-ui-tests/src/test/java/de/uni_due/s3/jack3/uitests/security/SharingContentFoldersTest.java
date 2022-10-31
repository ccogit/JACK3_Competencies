package de.uni_due.s3.jack3.uitests.security;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.MANAGE;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertUnorderedListEquals;
import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static de.uni_due.s3.jack3.uitests.utils.Misc.scrollElementIntoView;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage.Permission;

class SharingContentFoldersTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderbusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	//Folder names
	// NOTE: Because we lookup folders via xpath and 'contains(text()...' (see MyWorkspacePage#getElement(String)),
	// each folder name MUST NOT contain the name of another folder. We bypass this problem by adding a unique ID to
	// each folder name.
	private static final String MY_FOLDER = "myFolder 1";
	private static final String READ_ONLY_FOLDER = "Read only 2";
	private static final String EXTENDED_READ_FOLDER = "Extended read 3";
	private static final String EDIT_FOLDER = "Edit 4";
	private static final String EDIT_SUBFOLDER = "Edit (subfolder) 5";
	private static final String EDIT_FOLDER_COPY = "Edit (copy) 6";
	private static final String EXTENDED_EDIT_FOLDER = "Extended read and edit 7";
	private static final String MANAGE_FOLDER = "Manage 8";
	private static final String MANAGE_SUBFOLDER = "Manage sub 9";

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer1 = userBusiness.createUser("lecturer1", "secret", "lecturer@foobar.com", false, true);
		userBusiness.createUser("lecturer2", "secret", "lecturer@foobar.com", false, true);

		try {
			folderbusiness.createContentFolder(lecturer1, MY_FOLDER, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, READ_ONLY_FOLDER, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, EXTENDED_READ_FOLDER, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, EDIT_FOLDER, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, EDIT_FOLDER_COPY, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, EXTENDED_EDIT_FOLDER, lecturer1.getPersonalFolder());
			folderbusiness.createContentFolder(lecturer1, MANAGE_FOLDER, lecturer1.getPersonalFolder());
		} catch (Exception e) {
			fail("It was not possible to setup the test folders.", e);
		}
	}

	@Test
	@Order(1)
	@RunAsClient
	void shareFolders() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer1", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();

		// Expand PersonalFolder
		MyWorkspacePage.expandFolder(MyWorkspacePage.getPersonalFolder());

		UserRightsTablePage userRightsTable;
		// Set the UserRights for the "Edit" Folder. Lecurer2 will get the edit right.
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", EDIT_FOLDER, Permission.write);
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", EDIT_FOLDER_COPY, Permission.write);

		// Set the UserRights for the "Extended Read and Edit" Folder. Lecurer2 will get the edit and extended Read right.
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", EXTENDED_EDIT_FOLDER, Permission.write,
				Permission.extendedRead);

		// Set the UserRights for the "Extended Read" Folder. Lecurer2 will get the read and extended read right.
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", EXTENDED_READ_FOLDER, Permission.read,
				Permission.extendedRead);

		// Set the UserRights for the "Read only" Folder. Lecurer2 will get the read right.
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", READ_ONLY_FOLDER, Permission.read);

		// Test that you can reset Rights
		userRightsTable = MyWorkspacePage.openUserRightsForFolder(MY_FOLDER);
		userRightsTable.changeRights("lecturer2", Permission.read);
		userRightsTable.resetChanges();
		userRightsTable.saveChanges();

		// Set the UserRights for the "Manage" folder
		MyWorkspacePage.changeRightsOfLecturer("lecturer2", MANAGE_FOLDER, Permission.manage);

		logout();
	}

	@Test
	@Order(2)
	void verifySharedFolders() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<ContentFolder> sharedFoldersAndPersonalFolder = folderbusiness.getAllContentFoldersForUser(lecturer2);

		//lecturer 1 should have 8 folders
		assertEquals(8, folderbusiness.getAllContentFoldersForUser(lecturer1).size());
		//lecturer 2 should have 7 folders (the folder myFolder wasn't shared)
		assertEquals(7, sharedFoldersAndPersonalFolder.size());

		//Check that lecturer 2 has the correct Rights on the SharedFolders 
		for (ContentFolder folder : sharedFoldersAndPersonalFolder) {
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
				if (!folder.getName().equals("personalFolder")) {
					fail(folder.getName() + " should not be in the shared folder list for lecturer2");
				}
			}
		}
	}

	@Test
	@Order(3)
	@RunAsClient
	void createExercises() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer2", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();
		//expand lecturer1 Folder
		MyWorkspacePage.expandFolder("lecturer1");

		// Check that the user can't crease a Exercise in the Folders where the user doesn't have Edit rights
		MyWorkspacePage.checkThatFolderHasNoContextMenu(READ_ONLY_FOLDER);
		MyWorkspacePage.checkThatFolderHasNoContextMenu(EXTENDED_READ_FOLDER);

		// Create a Exercise in the Folders where the user has Edit rights
		MyWorkspacePage.createExercise(MyWorkspacePage.getFolder(EDIT_FOLDER), "A1");
		MyWorkspacePage.createExercise(MyWorkspacePage.getFolder(EDIT_FOLDER), "A2");
		MyWorkspacePage.createExercise(MyWorkspacePage.getFolder(EXTENDED_EDIT_FOLDER), "A3");
		MyWorkspacePage.createExercise(MyWorkspacePage.getFolder(MANAGE_FOLDER), "A4");
		MyWorkspacePage.createExercise(MyWorkspacePage.getFolder(MANAGE_FOLDER), "A5");

		logout();
	}

	@Test
	@Order(4)
	void verifyCreatedExercises() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<Exercise> exercisesFromLecturer1 = exerciseBusiness.getAllExercisesForUser(lecturer1);
		List<Exercise> exercisesFromLecturer2 = exerciseBusiness.getAllExercisesForUser(lecturer2);

		//Lecturer 1 should have all 5 Exercises
		assertEquals(5, exercisesFromLecturer1.size());
		// Because the Exercises are created in a sharedFolder both user should have access to the Exercises
		assertEquals(exercisesFromLecturer1, exercisesFromLecturer2);
		// Not all Exercises are in the same folder
		assertFalse(exercisesFromLecturer1.get(0).getFolder().equals(exercisesFromLecturer1.get(1).getFolder())
				&& exercisesFromLecturer1.get(0).getFolder().equals(exercisesFromLecturer1.get(2).getFolder()));
	}

	@Test
	@Order(5)
	@RunAsClient
	void moveExercises() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer2", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();

		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", EDIT_FOLDER, EDIT_FOLDER_COPY, EXTENDED_EDIT_FOLDER);

		// try to move the Exercises into a Folder with no Edit rights.
		// This shouldn't work because the target folders are read-only
		MyWorkspacePage.moveElement(MyWorkspacePage.getExercise("A1"), MyWorkspacePage.getFolder(READ_ONLY_FOLDER));
		MyWorkspacePage.moveElement(MyWorkspacePage.getExercise("A3"),
				MyWorkspacePage.getFolder(EXTENDED_READ_FOLDER));

		// Move an Exercise into a Folder with Edit rights
		// Shouldn't work because the rights are not equal
		MyWorkspacePage.moveElement(MyWorkspacePage.getExercise("A1"),
				MyWorkspacePage.getFolder(EXTENDED_EDIT_FOLDER));

		// Move an Exercise into a Folder with Edit rights
		// Should work because rights are NOT changed
		MyWorkspacePage.moveElement(MyWorkspacePage.getExercise("A2"),
				MyWorkspacePage.getFolder(EDIT_FOLDER_COPY));

		//Expand Folders again
		MyWorkspacePage.expandFolders("lecturer1", EDIT_FOLDER, EDIT_FOLDER_COPY, EXTENDED_EDIT_FOLDER);

		//Move exercise 'A1' into your own personalFolder
		// Shouldn't work because the rights are not equal
		MyWorkspacePage.moveElement(MyWorkspacePage.getExercise("A1"),
				MyWorkspacePage.getPersonalFolder());

		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", MANAGE_FOLDER);

		// Move a duplicate(!) of the Exercise into your own personalFolder
		// Should work because lecturer2 has MANAGE rights on the folder of A4
		MyWorkspacePage.moveElementWithChangingOwner(MyWorkspacePage.getExercise("A4"),
				MyWorkspacePage.getPersonalFolder(), true);

		// Move an Exercise into your own personalFolder
		// Should work because lecturer2 has MANAGE rights on the folder of A5
		MyWorkspacePage.moveElementWithChangingOwner(MyWorkspacePage.getExercise("A5"),
				MyWorkspacePage.getPersonalFolder(), false);
		
		logout();
	}

	@Test
	@Order(6)
	void verifyMovedExercises() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<Exercise> exercisesFromLecturer1 = exerciseBusiness.getAllExercisesForUser(lecturer1);
		List<Exercise> exercisesFromLecturer2 = exerciseBusiness.getAllExercisesForUser(lecturer2);

		//After moving the exercises lecturer 2 should have 6 exercises (one exercise was duplicated)
		assertEquals(6, exercisesFromLecturer2.size());
		//Lecturer 1 should only have 4 exercises left (one was stolen)
		assertEquals(4, exercisesFromLecturer1.size());

		ContentFolder personalFolderFromLecturer2 = lecturer2.getPersonalFolder();
		personalFolderFromLecturer2 = folderbusiness.getContentFolderWithLazyData(personalFolderFromLecturer2);

		//The Personal Folder from Lecturer 2 should have 2 exercises
		assertEquals(2, personalFolderFromLecturer2.getChildrenExercises().size());

		//In the PersonalFolder should be the exercises A4 and A5
		String exerciseNames = "";
		for (AbstractExercise exercise : personalFolderFromLecturer2.getChildrenExercises()) {
			exerciseNames += exercise.getName();
		}
		assertTrue(exerciseNames.contains("A4"));
		assertTrue(exerciseNames.contains("A5"));

		//Lecturer 1 should have access to the exercises A1-A4
		exerciseNames = "";
		for (AbstractExercise exercise : exercisesFromLecturer1) {
			exerciseNames += exercise.getName();
		}
		assertTrue(exerciseNames.contains("A1"));
		assertTrue(exerciseNames.contains("A2"));
		assertTrue(exerciseNames.contains("A3"));
		assertTrue(exerciseNames.contains("A4"));
	}

	@Test
	@Order(7)
	@RunAsClient
	void createCourses() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer2", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();
		//Expand lecturer1 folder
		MyWorkspacePage.getFolder("lecturer1").click();

		// Check that the user can't crease a Course in the Folders where the user doesn't have Edit rights
		MyWorkspacePage.checkThatFolderHasNoContextMenu(READ_ONLY_FOLDER);
		MyWorkspacePage.checkThatFolderHasNoContextMenu(EXTENDED_READ_FOLDER);

		// Create a Course in the Folders where the user has Edit rights
		MyWorkspacePage.createCourse(MyWorkspacePage.getFolder(EDIT_FOLDER), "C1");
		MyWorkspacePage.createCourse(MyWorkspacePage.getFolder(EDIT_FOLDER), "C2");
		MyWorkspacePage.createCourse(MyWorkspacePage.getFolder(EXTENDED_EDIT_FOLDER), "C3");
		MyWorkspacePage.createCourse(MyWorkspacePage.getFolder(MANAGE_FOLDER), "C4");
		MyWorkspacePage.createCourse(MyWorkspacePage.getFolder(MANAGE_FOLDER), "C5");

		logout();
	}

	@Test
	@Order(8)
	void verifyCreatedCourses() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<Course> coursesFromLecturer1 = courseBusiness.getAllCoursesForUser(lecturer1);
		List<Course> coursesFromLecturer2 = courseBusiness.getAllCoursesForUser(lecturer2);

		//lecturer 1 should have 5 Courses
		assertEquals(5, coursesFromLecturer1.size());
		// Because the Courses are created in a sharedFolder both user should have access to the Courses
		assertEquals(coursesFromLecturer1, coursesFromLecturer2);
		// Not all Courses are in the same Folder
		assertFalse(coursesFromLecturer1.get(0).getFolder().equals(coursesFromLecturer1.get(1).getFolder())
				&& coursesFromLecturer1.get(0).getFolder().equals(coursesFromLecturer1.get(2).getFolder()));
	}

	@Test
	@Order(9)
	@RunAsClient
	void moveCourses() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer2", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();

		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", EDIT_FOLDER, EDIT_FOLDER_COPY, EXTENDED_EDIT_FOLDER);

		// try to move the Course into a Folder with no Edit rights.
		// This shouldn't work because the target folders are read-only
		
		scrollElementIntoView(MyWorkspacePage.getFolder(READ_ONLY_FOLDER));
				
		MyWorkspacePage.moveElement(MyWorkspacePage.getCourse("C1"),
				MyWorkspacePage.getFolder(READ_ONLY_FOLDER));
		MyWorkspacePage.moveElement(MyWorkspacePage.getCourse("C3"),
				MyWorkspacePage.getFolder(EXTENDED_READ_FOLDER));

		// Move a Course into a Folder with Edit rights
		MyWorkspacePage.moveElement(MyWorkspacePage.getCourse("C1"),
				MyWorkspacePage.getFolder(EXTENDED_EDIT_FOLDER));

		// Move a Course into a Folder with Edit rights
		// Shouldn't work because the rights are not equal
		MyWorkspacePage.moveElement(MyWorkspacePage.getCourse("C2"),
				MyWorkspacePage.getFolder(EDIT_FOLDER_COPY));

		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", EDIT_FOLDER, EDIT_FOLDER_COPY, EXTENDED_EDIT_FOLDER);

		// Move a course into your own personalFolder
		// Shouldn't work because the rights are not equal
		MyWorkspacePage.moveElement(MyWorkspacePage.getCourse("C1"),
				MyWorkspacePage.getPersonalFolder());

		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", MANAGE_FOLDER);

		// Move a duplicate(!) of the Course into your own personalFolder
		// Should work because lecturer2 has MANAGE rights on the folder of C4
		MyWorkspacePage.moveElementWithChangingOwner(MyWorkspacePage.getCourse("C4"),
				MyWorkspacePage.getPersonalFolder(), true);

		// Move a Course into your own personalFolder
		// Should work because lecturer2 has MANAGE rights on the folder of C5
		
		scrollElementIntoView(MyWorkspacePage.getExercise("C5"));
		MyWorkspacePage.moveElementWithChangingOwner(MyWorkspacePage.getExercise("C5"),
				MyWorkspacePage.getPersonalFolder(), false);
		
		logout();
	}

	@Test
	@Order(10)
	void verifyMovedCourses() {
		User lecturer1 = userBusiness.getUserByName("lecturer1").get();
		User lecturer2 = userBusiness.getUserByName("lecturer2").get();

		List<Course> CoursesFromLecturer1 = courseBusiness.getAllCoursesForUser(lecturer1);
		List<Course> CoursesFromLecturer2 = courseBusiness.getAllCoursesForUser(lecturer2);

		//After moving the courses lecturer 2 should have 6 courses (one course was duplicated)
		assertEquals(6, CoursesFromLecturer2.size());
		//lecturer 1 should only have 4 courses left (one was stolen)
		assertEquals(4, CoursesFromLecturer1.size());

		ContentFolder personalFolderFromLecturer2 = folderbusiness
				.getContentFolderWithLazyData(lecturer2.getPersonalFolder());

		//In the personalFolder from lecturer 2 should be 2 courses
		assertEquals(2, personalFolderFromLecturer2.getChildrenCourses().size());

		List<String> folderNames = CoursesFromLecturer2.stream().map(c -> c.getFolder().getName())
				.collect(Collectors.toList());

		//The courses from lecturer2 should have the following parentFolders:
		assertTrue(folderNames.contains(EDIT_FOLDER));
		assertTrue(folderNames.contains(EXTENDED_EDIT_FOLDER));
		assertTrue(folderNames.contains("personalFolder"));
	}

	@Test
	@Order(11)
	@RunAsClient
	void moveFolders() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer2", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();
		//Expand Folders
		MyWorkspacePage.expandFolders("lecturer1", EDIT_FOLDER, MANAGE_FOLDER);

		// try to move a Folder with Edit Rights into a Folder with no Edit rights.
		// and try to move a Folder without Edit Rights into a Folder with Edit Rights.
		// Both shouldn't work!

		MyWorkspacePage.moveElement(MyWorkspacePage.getFolder(EDIT_FOLDER),
				MyWorkspacePage.getFolder(READ_ONLY_FOLDER));
		MyWorkspacePage.moveElement(MyWorkspacePage.getFolder(READ_ONLY_FOLDER),
				MyWorkspacePage.getFolder(EDIT_FOLDER));

		// We create two sub folders to test moving operations
		// Testing moving operations is not possible with the existing folders because lecturer2 has no rights on the
		// personal folder of lecturer1

		MyWorkspacePage.createFolder(MyWorkspacePage.getFolder(EDIT_FOLDER), EDIT_SUBFOLDER);
		MyWorkspacePage.createFolder(MyWorkspacePage.getFolder(MANAGE_FOLDER), MANAGE_SUBFOLDER);

		// Move a Folder into another Folder where both Folders have equal rights and the user has EDIT rights
		MyWorkspacePage.moveElement(MyWorkspacePage.getFolder(EDIT_SUBFOLDER),
				MyWorkspacePage.getFolder(EDIT_FOLDER));

		// Move a Folder into your own Personalfolder
		MyWorkspacePage.moveElementWithChangingOwner(MyWorkspacePage.getFolder(MANAGE_SUBFOLDER),
				MyWorkspacePage.getPersonalFolder(), false);

		logout();
	}

	@Test
	@Order(12)
	void verifyMovedFolders() {
		// ---------- Lecturer 1 ----------
		var lect = userBusiness.getUserByName("lecturer1").get();

		// After lecturer2 moved one folder into his personalFolder both User should have 9 Folders
		var folders = folderbusiness.getAllContentFoldersForUser(lect);
		assertEquals(9, folders.size());

		// The "Manage sub" folder was stolen by lecturer2
		var actualFolderNames = folders.stream().map(Folder::getName).collect(Collectors.toList());
		var expectedFolderNames = Arrays.asList(
				EDIT_FOLDER, EDIT_FOLDER_COPY, EDIT_SUBFOLDER, EXTENDED_EDIT_FOLDER, EXTENDED_READ_FOLDER,
				MANAGE_FOLDER, READ_ONLY_FOLDER, MY_FOLDER, ContentFolder.PERSONAL_FOLDER_NAME);
		assertUnorderedListEquals(expectedFolderNames, actualFolderNames);

		// ---------- Lecturer 2 ----------
		lect = userBusiness.getUserByName("lecturer2").get();

		folders = folderbusiness.getAllContentFoldersForUser(lect);
		assertEquals(9, folders.size());

		// The "Manage sub" folder was stolen by lecturer2
		// lecturer2 does not have "MY_FOLDER" which is not available for him
		actualFolderNames = folders.stream().map(Folder::getName).collect(Collectors.toList());
		expectedFolderNames = Arrays.asList(
				EDIT_FOLDER, EDIT_FOLDER_COPY, EDIT_SUBFOLDER, EXTENDED_EDIT_FOLDER, EXTENDED_READ_FOLDER,
				MANAGE_FOLDER, MANAGE_SUBFOLDER, READ_ONLY_FOLDER, ContentFolder.PERSONAL_FOLDER_NAME);
		assertUnorderedListEquals(expectedFolderNames, actualFolderNames);
	}

}
