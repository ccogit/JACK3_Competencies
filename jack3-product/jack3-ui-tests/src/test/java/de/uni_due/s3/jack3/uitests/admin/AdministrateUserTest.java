package de.uni_due.s3.jack3.uitests.admin;


import static de.uni_due.s3.jack3.uitests.utils.Misc.assumeLogin;
import static de.uni_due.s3.jack3.uitests.utils.Misc.login;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.uitests.utils.AbstractSeleniumTest;
import de.uni_due.s3.jack3.uitests.utils.pages.AdministrationUserPage;

class AdministrateUserTest extends AbstractSeleniumTest {

	@Inject
	private UserBusiness userBusiness;

	@Override
	protected void initializeTest() {
		super.initializeTest();
		userBusiness.createUser("admin", "secret", "admin@foobar.com", true, true);
	}

	/**
	 * Create 3 more users: Admin2, Lecturer, Student
	 */
	@Test
	@Order(1)
	@RunAsClient
	void createUsers() { // NOSONAR no assertions here
		login("admin", "secret");
		AdministrationUserPage.navigateToPage();
		
		//create another admin
		AdministrationUserPage.createNewUser("Admin2", "admin2@uni.de", true, true);
		
		//create lecturer
		AdministrationUserPage.createNewUser("Lecturer", "lecturer@uni.de", true, false);

		//create student
		AdministrationUserPage.createNewUser("Student", "student@uni.de", false, false);
	}
	
	@Test
	@Order(2)
	void verifyCreatedUser() {
		verifyUserExists("admin2", "admin2@uni.de", true, true);
		verifyUserExists("lecturer", "lecturer@uni.de", true, false);
		verifyUserExists("student", "student@uni.de", false, false);
	}
	
	@Test
	@Order(3)
	@RunAsClient
	void modifyCreatedUsers() { // NOSONAR no assertions here
		assumeLogin();
		
		//remove all rights from admin
		AdministrationUserPage.editUser("admin2", "admin2@uni.com", false, false);
			
		//give lecturer admin right but remove edit right
		AdministrationUserPage.editUser("lecturer", "lecturer@uni.com", false, true);
		
		//give all rights to student
		AdministrationUserPage.editUser("student", "student@uni.com", true, true);
	}
	
	@Test
	@Order(4)
	void verifyEditedUser() {
		verifyUserExists("admin2", "admin2@uni.com", false, false);
		verifyUserExists("lecturer", "lecturer@uni.com", false, true);
		verifyUserExists("student", "student@uni.com", true, true);
	}
	
	@Test
	@Order(5)
	@RunAsClient
	void createUserGroups() { // NOSONAR no assertions here
		assumeLogin();
		AdministrationUserPage.createNewUserGroup("myUserGroup", "this is my user group");
		AdministrationUserPage.createNewUserGroup("other group", "bla bla");
		
		AdministrationUserPage.editUserGroup("other group", "other group", "");
		AdministrationUserPage.addUserToUserGroup("myUserGroup", "student");
		
		AdministrationUserPage.addUserToUserGroup("other group", "admin");
		AdministrationUserPage.addUserToUserGroup("other group", "student");
	}
	
	@Test
	@Order(6)
	void verifyUserGroups() {		
		verifyUserGroup("myUserGroup", "this is my user group", 1, new HashSet<>(Arrays.asList(userBusiness.getUserByName("student").get())));
		verifyUserGroup("other group", "", 2, new HashSet<>(Arrays.asList(userBusiness.getUserByName("student").get(),userBusiness.getUserByName("admin").get())));
	}
	
	
	@Test
	@Order(7)
	@RunAsClient
	void removeUserAndUserGroup() { // NOSONAR no assertions here
		assumeLogin();
		
		AdministrationUserPage.removeUser("lecturer");
		AdministrationUserPage.removeUserGroup("myUserGroup");
	}
	
	@Test
	@Order(8)
	void verifyRemovedUserAndUserGroup() {
		Optional<User> lecturer = userBusiness.getUserByName("lecturer");
		assertTrue(lecturer.isEmpty(), "admin2 was found in the DB although he should be deleted");
		
		Optional<UserGroup> userGroup = userBusiness.getUserGroup("myUserGroup");
		assertTrue(userGroup.isEmpty(), "UserGroup 'myUserGroup' was  found in the DB although it should had been deleted.");
	}

	private void verifyUserExists(String userName, String email, boolean shouldBeEditor, boolean shouldBeAdmin) {
		Optional<User> found = userBusiness.getUserByName(userName);
		assertTrue(found.isPresent(), "User '" + userName + "' was not found in database.");
		final User user = found.get();
		assertEquals(user.getEmail(), email);
		assertEquals(user.isHasEditRights(), shouldBeEditor, userName + "should be editor: " + shouldBeEditor);
		assertEquals(user.isHasAdminRights(),shouldBeAdmin, userName + "should be admin: " + shouldBeAdmin);
	}
	
	private void verifyUserGroup(String userGroupName, String shouldBeDescription, int shouldBeNumberOfMembers, Set<User> shouldBeMembers) {
		Optional<UserGroup> found = userBusiness.getUserGroup(userGroupName);
		assertTrue(found.isPresent(), "UserGroup '" + userGroupName + "' was not found in database.");
		final UserGroup userGroup = userBusiness.getUserGroupWithLazyData(found.get());
		
		assertEquals(shouldBeDescription, userGroup.getDescription(), userGroupName + "should have the following description: " + shouldBeDescription + ". But has: " + userGroup.getDescription());
		assertEquals(shouldBeNumberOfMembers, userGroup.getNumberOfMemberUsers(), userGroupName + "should have the following number of members: " + shouldBeNumberOfMembers + ". But has: " + userGroup.getNumberOfMemberUsers());
		assertEquals(shouldBeMembers, userGroup.getMemberUsers(), userGroupName + "doesn't have the expected members");
	}

}
