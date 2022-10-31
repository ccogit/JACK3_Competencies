package de.uni_due.s3.jack3.uitests.lecturer;

import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;

class PresentationFolderTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		User lecturer1 = userBusiness.createUser("lecturer", "secret", "lecturer@foobar.com", false, true);

		PresentationFolder root = folderBusiness.createPresentationFolder("Root", folderBusiness.getPresentationRoot());
		folderBusiness.updateFolderRightsForUser(root, lecturer1, AccessRight.getFull());
	}

	@Test
	@Order(1)
	@RunAsClient
	void createFolders() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		login("lecturer", "secret");
		assumeLogin();
		AvailableCoursesPage.navigateToPage();

		// Create 4 Folders
		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder("Root"), "Meine Kursangebote");
		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder("Root"), "Meine anderen Kursangebote");
		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder("Root"), "Ordner zum löschen");
		AvailableCoursesPage.createFolder(AvailableCoursesPage.getFolder("Root"), "Ordner zum verschieben");

		// Delete the Folder "Ordner zum löschen"
		AvailableCoursesPage.removeFolder(AvailableCoursesPage.getFolder("Ordner zum löschen"));
		
		AvailableCoursesPage.expandFolder("Root");

		// Move Folder "Ordner zum verschieben" in to the Folder "Meine Kursangebote"
		AvailableCoursesPage.moveElement(AvailableCoursesPage.getFolder("Ordner zum verschieben"),
				AvailableCoursesPage.getFolder("Meine Kursangebote"));

		// Rename the folder "Ordner zum verschieben" into "Verschobener Ordner"
		AvailableCoursesPage.expandFolder("Meine Kursangebote");
		AvailableCoursesPage.renameFolder(AvailableCoursesPage.getFolder("Ordner zum verschieben"),
				"Verschobener Ordner");

		logout();
	}

	@Test
	@Order(2)
	void verifyFolders() {
		User lecturer = userBusiness.getUserByName("lecturer").orElseThrow(AssertionFailedError::new);
		// User should have 4 Folders (the root and 3 created Folders).
		List<PresentationFolder> presentationFolders = folderBusiness.getAllPresentationFoldersForUser(lecturer);
		assertEquals(4, presentationFolders.size());

		for (PresentationFolder folder : presentationFolders) {
			folder = folderBusiness.getPresentationFolderWithLazyData(folder);
			switch (folder.getName()) {
			case "Meine Kursangebote":
				assertEquals(1, folder.getChildrenFolder().size());
				break;
			case "Meine anderen Kursangebote":
				assertEquals(0, folder.getChildrenFolder().size());
				break;
			case "Verschobener Ordner":
				assertEquals(0, folder.getChildrenFolder().size());
				break;
			case "Root":
				assertEquals(2, folder.getChildrenFolder().size());
				break;
			case "Ordner zum verschieben":
				throw new AssertionFailedError(
						"Der Ordner 'Ordner zum verschieben' wurde nicht in 'Verschobener Ordner' umbenant.");
			default:
				throw new AssertionFailedError(
						"The Folder with the name \"" + folder.getName() + "\" konnte nicht gefunden werden");
			}
		}

	}

}
