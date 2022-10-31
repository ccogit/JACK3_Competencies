package de.uni_due.s3.jack3.uitests.admin;

import static de.uni_due.s3.jack3.entities.AccessRight.EXTENDED_READ;
import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static de.uni_due.s3.jack3.entities.AccessRight.WRITE;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static de.uni_due.s3.jack3.uitests.utils.Misc.logout;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.AvailableCoursesPage;
import de.uni_due.s3.jack3.uitests.utils.pages.UserRightsTablePage;

/**
 * Tests creating and deleting first-level Presentation Folders as an Administrator
 */
class CourseManagementTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	// Names of the Users
	private final String matheLehrer123 = "MatheLehrer123";
	private final String matheSHK = "MatheSHK";
	private final String chemieProfi = "ChemieProfi";
	private final String chemieRockt = "ChemieRockt";
	private final String infoProf = "InformatikProf";
	private final String infoSHK = "InformatikSHK";
	private final String infoSystems = "InformationSystemDude";

	// Names of the folders
	private final String statistikFolder = "Statistik Kurse";
	private final String mathematikFolder = "Mathematik Kurse";
	private final String informatikFolder = "Informatik Kurse";
	private final String testateFolder = "Testate";
	private final String folderToBeDeleated = "Ordner zum Löschen";
	private final String chemieFolder = "Chemie Kurse";

	// The different types of permissions
	final UserRightsTablePage.Permission write = UserRightsTablePage.Permission.write;
	final UserRightsTablePage.Permission extendedRead = UserRightsTablePage.Permission.extendedRead;
	final UserRightsTablePage.Permission read = UserRightsTablePage.Permission.read;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		userBusiness.createUser("admin", "secret", "superuser@foobar.com", true, true);

		userBusiness.createUser(matheLehrer123, "mathe", "mathe@foobar.com", false, true);
		userBusiness.createUser(matheSHK, "matheshk", "mathe-shk@foobar.com", false, true);
		userBusiness.createUser(chemieProfi, "H20", "chemie@foobar.com", false, true);
		userBusiness.createUser(chemieRockt, "123", "rocker@foobar.com", false, true);
		userBusiness.createUser(infoProf, "prof", "Info@foobar.com", false, true);
		userBusiness.createUser(infoSHK, "shk", "shk@foobar.com", false, true);
		userBusiness.createUser(infoSystems, "Dude", "dude@foobar.com", false, true);
	}

	@RunAsClient
	@Test
	@Order(1)
	void changesInCourseManagements() { // NOSONAR This test ensures that the actions are possible, for assertions see the next test case
		login("admin", "secret");

		// Create PresentationFolders
		var statistik = AvailableCoursesPage.createTopLevelFolder(statistikFolder);
		var mathematik = AvailableCoursesPage.createTopLevelFolder(mathematikFolder);
		var informatik = AvailableCoursesPage.createTopLevelFolder(informatikFolder);
		AvailableCoursesPage.createFolder(informatik, testateFolder);
		var toDelete = AvailableCoursesPage.createTopLevelFolder(folderToBeDeleated);

		// delete a PresentationFolder
		AvailableCoursesPage.removeFolder(toDelete);

		// rename the Folder 'Statistik Aufgaben' to 'Chemie Kurse'
		AvailableCoursesPage.renameFolder(statistik, chemieFolder);

		// give the Lecturers rights to their folders
		// Informatik
		UserRightsTablePage editPermissionPage = AvailableCoursesPage.openUserRightsForFolder(informatikFolder);
		editPermissionPage.changeRights(infoProf, write, extendedRead);
		editPermissionPage.changeRights(infoSHK, write);
		editPermissionPage.changeRights(infoSystems, read);
		editPermissionPage.saveChanges();

		// Mathematik
		editPermissionPage = AvailableCoursesPage.openUserRightsForFolder(mathematikFolder);
		editPermissionPage.changeRights(matheLehrer123, write, extendedRead);
		editPermissionPage.changeRights(matheSHK, write);
		editPermissionPage.saveChanges();

		// Chemie
		editPermissionPage = AvailableCoursesPage.openUserRightsForFolder(chemieFolder);
		editPermissionPage.changeRights(chemieProfi, write, extendedRead);
		editPermissionPage.changeRights(chemieRockt, write, extendedRead);
		editPermissionPage.saveChanges();

		logout();
	}

	@Test
	@Order(2)
	void verifyChanges() {
		List<PresentationFolder> allPresentationFolders = folderBusiness.getAllPresentationFolders();
		PresentationFolder root = folderBusiness
				.getPresentationFolderWithLazyData(folderBusiness.getPresentationRoot());
		// there should be the Root Folder and 4 SubFolder
		assertEquals(5, allPresentationFolders.size());
		assertEquals(3, root.getChildrenFolder().size());

		for (Folder folder : root.getChildrenFolder()) {
			folder = folderBusiness.getPresentationFolderWithLazyData(folder);
			Map<User, AccessRight> accesRights = authorizationBusiness.getAllManagingUsers(folder);
			// Note that "admin" has created the folders and therefore she also has rights

			switch (folder.getName()) {
			case informatikFolder:
				assertEquals(1, folder.getChildrenFolder().size());
				assertEquals(4, accesRights.size());
				assertEquals(AccessRight.getFromFlags(READ, WRITE, EXTENDED_READ),
						accesRights.get(userBusiness.getUserByName(infoProf).get()));
				assertEquals(AccessRight.getFromFlags(READ, WRITE),
						accesRights.get(userBusiness.getUserByName(infoSHK).get()));
				assertEquals(AccessRight.getFromFlags(READ),
						accesRights.get(userBusiness.getUserByName(infoSystems).get()));
				assertEquals(AccessRight.getFull(),
						accesRights.get(userBusiness.getUserByName("admin").get()));
				break;
			case mathematikFolder:
				assertEquals(0, folder.getChildrenFolder().size());
				assertEquals(3, accesRights.size());
				assertEquals(AccessRight.getFromFlags(READ, WRITE, EXTENDED_READ),
						accesRights.get(userBusiness.getUserByName(matheLehrer123).get()));
				assertEquals(AccessRight.getFromFlags(READ, WRITE),
						accesRights.get(userBusiness.getUserByName(matheSHK).get()));
				assertEquals(AccessRight.getFull(),
						accesRights.get(userBusiness.getUserByName("admin").get()));
				break;
			case chemieFolder:
				assertEquals(0, folder.getChildrenFolder().size());
				assertEquals(3, accesRights.size());
				assertEquals(AccessRight.getFromFlags(READ, WRITE, EXTENDED_READ),
						accesRights.get(userBusiness.getUserByName(chemieRockt).get()));
				assertEquals(AccessRight.getFromFlags(READ, WRITE, EXTENDED_READ),
						accesRights.get(userBusiness.getUserByName(chemieProfi).get()));
				assertEquals(AccessRight.getFull(),
						accesRights.get(userBusiness.getUserByName("admin").get()));
				break;
			default:
				throw new AssertionFailedError("The Folder with the Name '" + folder.getName() + "' was not expected.");
			}
		}

	}

}
