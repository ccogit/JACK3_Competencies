package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.reloadPage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.Find;
import de.uni_due.s3.jack3.uitests.utils.pages.MyWorkspacePage;

/**
 * Tests creating folders, exercises and courses
 * 
 * @author kilian.kraus
 *
 */
class ContentFolderTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;
	
	@Inject
	private ExerciseBusiness exerciseBusiness;
	
	@Inject
	private CourseBusiness courseBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);
		
		try {
			ContentFolder folder = folderBusiness.createContentFolder(lecturer, "sample folder", lecturer.getPersonalFolder());
			Exercise exericse = exerciseBusiness.createExercise("example Exercise", lecturer, folder , "DE");
			exericse.setInternalNotes("my internal notes");
			exerciseBusiness.addTagToExercise(exericse, "STUFF");
			exerciseBusiness.addTagToExercise(exericse, "TAG");
			exerciseBusiness.updateExercise(exericse);
		}catch (ActionNotAllowedException e) {
			throw new AssertionFailedError("Could not create exercise or folder, because of missing rights.", e);
		}

	}

	@Test
	@Order(1)
	@RunAsClient
	void createFolders() { // NOSONAR Only runs ins the UI, no assertions
		login("lecturer", "secret");
		assumeLogin();

		MyWorkspacePage.navigateToPage();

		//Create Folders
		MyWorkspacePage.createFolder(MyWorkspacePage.getPersonalFolder(), "Meine erstellten Aufgaben");
		MyWorkspacePage.createFolder(MyWorkspacePage.getPersonalFolder(), "Meine erstellten Kurse");
		MyWorkspacePage.createFolder(MyWorkspacePage.getFolder("Meine erstellten Aufgaben"), "Informatik Aufgaben");
		MyWorkspacePage.createFolder(MyWorkspacePage.getFolder("Meine erstellten Aufgaben"), "Mathematik Aufgaben");
		MyWorkspacePage.createFolder(MyWorkspacePage.getPersonalFolder(), "Statistik Aufgaben");

		// move Folder "Statistik Aufgaben" in to the Folder "Mathematik Aufgaben"
		MyWorkspacePage.moveElement(MyWorkspacePage.getFolder("Statistik Aufgaben"),
				MyWorkspacePage.getFolder("Mathematik Aufgaben"));

		//after the drag and drop event the page would reload anyways.
		//But it is hard to tell how long the test has to wait until the page gets reloaded. 
		//Because of this we reload the page manually.
		reloadPage();

		//Remove the Folder "Meine Kurse"
		MyWorkspacePage.removeFolder(MyWorkspacePage.getFolder("Meine erstellten Kurse"));

		//expand "Meine Aufgaben"
		MyWorkspacePage.getFolder("Meine erstellten Aufgaben").click();

		//rename the Folder "Informatik Aufgaben" to "neuer Name"
		MyWorkspacePage.renameFolder(MyWorkspacePage.getFolder("Informatik Aufgaben"), "Neuer Name");
	}

	@Test
	@Order(2)
	void verifyFolders() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionFailedError::new);
		List<ContentFolder> contentFolders = folderBusiness.getAllContentFoldersForUser(lecturer);
		// User should have 6 Folders. The Personal Folder, 1 automatically created folder and 4 manually created Folders (one Folder was deleted).
		assertEquals(6, contentFolders.size());

		for (ContentFolder folder : contentFolders) {
			folder = folderBusiness.getContentFolderWithLazyData(folder);
			switch (folder.getName()) {
			case "Meine erstellten Aufgaben":
				assertEquals(2, folder.getChildrenFolder().size());
				break;
			case "Neuer Name":
				assertEquals(0, folder.getChildrenFolder().size());
				break;
			case "Mathematik Aufgaben":
				assertEquals(1, folder.getChildrenFolder().size());
				break;
			case "Statistik Aufgaben":
				assertEquals(0, folder.getChildrenFolder().size());
				break;
			case "personalFolder":
				assertEquals(2, folder.getChildrenFolder().size());
				break;
			case "sample folder":
				//this is the automatically created folder
				break;
			default:
				throw new AssertionFailedError(
						"Der ContentFolder mit dem Namen: '" + folder.getName() + "' konnte nicht gefunden werden.");
			}
		}
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void displayTagsAndInternalNotes() { // NOSONAR Only runs ins the UI, no assertions
		assumeLogin();
		MyWorkspacePage.expandFolder("sample folder");
		
		MyWorkspacePage.displayTags();
		//check that the tags are display under the exercise
		assertTrue(Find.getParent(MyWorkspacePage.getExercise("example Exercise")).getText().contains("STUFF"));
		assertTrue(Find.getParent(MyWorkspacePage.getExercise("example Exercise")).getText().contains("TAG"));
		
		MyWorkspacePage.displayInternalDescriptions();
		//check that the internal description is displayed under the exercise
		assertTrue(Find.getParent(MyWorkspacePage.getExercise("example Exercise")).getText().contains("my internal notes"));
	}
}
