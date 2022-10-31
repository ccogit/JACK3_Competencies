package de.uni_due.s3.jack3.tests.utils;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import de.uni_due.s3.jack3.business.FirstTimeSetupBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.LiveHqlService;
import de.uni_due.s3.jack3.services.UserGroupService;
import de.uni_due.s3.jack3.services.UserService;

/**
 * Basic test class, provides most important services (Development, LiveHql, User, UserGroup, Folder) with methods to
 * persist basic entities. By default it cleares the database after each test.
 *
 * @author lukas.glaser
 */
public abstract class AbstractBasicTest extends AbstractTest {

	// default entities
	protected User user = TestDataFactory.getUser("User");
	protected UserGroup userGroup = new UserGroup("User Group", "Description");
	protected ContentFolder folder = new ContentFolder("Content Folder");
	protected PresentationFolder presentationFolder = new PresentationFolder("Presentation Folder");

	/**
	 * Injected {@linkplain UserService}, persists and deletes users
	 */
	@Inject
	protected UserService userService;

	/**
	 * Injected {@linkplain UserGroupService}, persists and deletes user groups
	 */
	@Inject
	protected UserGroupService userGroupService;

	/**
	 * Injected {@linkplain FolderService}, provides methods for content folders and presentation folders
	 */
	@Inject
	protected FolderService folderService;

	/**
	 * Injected {@linkplain LiveHqlService} for direct HQL queries
	 */
	@Inject
	protected LiveHqlService hqlService;

	/**
	 * Injected {@linkplain DevelopmentService} for clearing the database after testing
	 */
	@Inject
	protected DevelopmentService devService;

	/**
	 * Injected {@linkplain FirstTimeSetupBusiness} for first time setup after clearing database
	 */
	@Inject
	protected FirstTimeSetupBusiness firstTimeSetupBusiness;

	/**
	 * Inject {@linkplain FolderBusiness} to manage user rights on folder
	 */
	@Inject
	protected FolderBusiness folderBusiness;

	/**
	 * Before-method, overrided by test classes
	 */
	@BeforeEach
	protected void doBeforeTest() {
		firstTimeSetupBusiness.doFirstTimeSetup();
	}

	/**
	 * After-method, overrided by test classes. It calls the developmentService's clear-method.
	 */
	@AfterEach
	final void doAfterTest() {
		devService.deleteTenantDatabase(EDatabaseType.H2);
	}

	/**
	 * Persist default user
	 */
	protected final void persistUser() {
		userService.persistUser(user);
	}

	/**
	 * Persist default user group
	 */
	protected final void persistUserGroup() {
		userGroupService.persistUserGroup(userGroup);
	}

	/**
	 * Persist default content folder
	 */
	protected final void persistFolder() {
		folderService.persistFolder(folder);
	}

	/**
	 * Persist default presentation folder
	 */
	protected final void persistPresentationFolder() {
		folderService.persistFolder(presentationFolder);
	}
}
