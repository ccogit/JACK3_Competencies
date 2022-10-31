package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotVisible;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.uitests.utils.Click;
import de.uni_due.s3.jack3.uitests.utils.Find;
import de.uni_due.s3.jack3.uitests.utils.JackUrl;
import de.uni_due.s3.jack3.uitests.utils.Misc;
import de.uni_due.s3.jack3.uitests.utils.Time;


public class AdministrationUserPage {
	
		
	private static final String CREATE_USER_BUTTON = "userManagement:cbCreateUser";
	private static final String NEW_USERNAME_INPUT = "createUserForm:newName";
	private static final String NEW_EMAIL_INPUT = "createUserForm:newEmail";
	private static final String NEW_ADMINRIGHTS = "createUserForm:newHasAdminRights";
	private static final String NEW_LECTURERRIGHTS = "createUserForm:newHasEditRights";
	private static final String NEW_USER_CREATE = "createUserForm:cbCreateNewUserDialog";
	
	private static final String CREATE_USERGROUP_BUTTON = "userManagement:cbCreateUserGroup";
	private static final String NEW_USERGROUPNAME_INPUT = "createUserGroupForm:newGroupName";
	private static final String NEW_USERGROUP_DESCRIPTION = "createUserGroupForm:newGroupDescription";
	private static final String NEW_USERGROUP_CREATE = "createUserGroupForm:cbCreateNewUserGroup";
	
	private static final String USER_TABLE = "userManagement:dtUserTable_data";
	
	
	private static final String USER_DETAILS_EMAIL = "generalInformation:userEmail";
	private static final String USER_DETAILS_ADMINRIGHTS = "generalInformation:userHasAdminRights";
	private static final String USER_DETAILS_LECTURERRIGHTS = "generalInformation:userHasEditRights";
	private static final String DELETE_USER = "toolbar:deleteUser";
	private static final String DELETE_USER_YES = "deleteUserForm:cbUserDialogDeletionYes";
	private static final String SAVE_USER_DETAILS = "generalInformation:saveInformation";
	private static final String CLOSE_USER_DETAILS = "toolbar:backToOverview";
	
	private static final String USERGROUP_TABLE = "userManagement:dtUserGroup_data";
	
	private static final String USERGROUP_DETAILS_NAME = "generalInformation:userGroupName";
	private static final String USERGROUP_DETAILS_DESCRIPTION = "generalInformation:userGroupDescription";
	private static final String USERGROUP_DETAIALS_MEMBER_TOGGLER = "memberInformation:userGroupMemberUsers_toggler";
	private static final String USERGROUP_DETAILS_MEMBERPICKLIST = "memberInformation:memberPickList";
	private static final String DELETE_USERGROUP = "toolbar:deleteUserGroup";
	private static final String DELETE_USERGROUP_YES = "globalConfirmForm:confirmOk";
	private static final String SAVE_USERGROUP_DETAILS = "generalInformation:saveInformation";
	private static final String CLOSE_USERGROUP_DETAILS = "toolbar:backToOverview";
	
	private static final String USERGROUP_MEMBERGROUPS = "memberInformation:userGroupMemberGroups";

	public static void navigateToPage() {
		navigate(JackUrl.USER_MANAGEMENT);
		waitClickable(By.id(CREATE_USER_BUTTON));
	}
	
	public static void createNewUser(String userName, String email, boolean lecturerRights, boolean adminRights) {
		waitClickable(By.id(CREATE_USER_BUTTON));
		find(CREATE_USER_BUTTON).click();
		waitClickable(By.id(NEW_USERNAME_INPUT));
		
		find(NEW_USERNAME_INPUT).sendKeys(userName);
		find(NEW_EMAIL_INPUT).sendKeys(email);
		
		if(adminRights) {
			find(NEW_ADMINRIGHTS).click();
		}
		
		if(lecturerRights) {
			find(NEW_LECTURERRIGHTS).click();
		}
		
		find(NEW_USER_CREATE).click();
		waitNotVisible(By.id(NEW_USER_CREATE));
	}
	
	public static void createNewUserGroup(String name, String description) {
		waitClickable(By.id(CREATE_USERGROUP_BUTTON));
		find(CREATE_USERGROUP_BUTTON).click();
		waitClickable(By.id(NEW_USERGROUPNAME_INPUT));
		find(NEW_USERGROUPNAME_INPUT).sendKeys(name);
		find(NEW_USERGROUP_DESCRIPTION).sendKeys(description);
		find(NEW_USERGROUP_CREATE).click();
		
		waitNotVisible(By.id(NEW_USERGROUP_CREATE));
	}
	
	public static void editUser(String userName, String newEmail, boolean lecturerRights, boolean adminRights){
		waitClickable(By.id(USER_TABLE));
		findChildElementByTagAndText(find(USER_TABLE), "td", userName).click();
		
		waitClickable(By.id(SAVE_USER_DETAILS));
		find(USER_DETAILS_EMAIL).clear();
		find(USER_DETAILS_EMAIL).sendKeys(newEmail);
		
		if(find(USER_DETAILS_ADMINRIGHTS+"_input").isSelected() != adminRights){
			find(USER_DETAILS_ADMINRIGHTS).click();
		}
		
		if(find(USER_DETAILS_LECTURERRIGHTS+"_input").isSelected() != lecturerRights){
			find(USER_DETAILS_LECTURERRIGHTS).click();
		}
		
		find(SAVE_USER_DETAILS).click();
		find(CLOSE_USER_DETAILS).click();
		waitClickable(By.id(USER_TABLE));
	}
	
	public static void removeUser(String userName) {
		waitClickable(By.id(USER_TABLE));
		findChildElementByTagAndText(find(USER_TABLE), "td", userName).click();
		
		waitClickable(By.id(DELETE_USER));
		find(DELETE_USER).click();
		waitClickable(By.id(DELETE_USER_YES));
		find(DELETE_USER_YES).click();		
		
		navigateToPage();
	}
	
	public static void addUserToUserGroup(String userGroupName, String userName) {
		openUserGroupDetails(userGroupName);
		waitClickable(By.id(USERGROUP_DETAIALS_MEMBER_TOGGLER));
		
		//check if we have to expand the members category
		try {
			if(!find(USERGROUP_DETAILS_MEMBERPICKLIST).isDisplayed()){
				find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
				waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
			}
		}catch(Exception e) {
			find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
			waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
		}
		
		//select user
		findChildElementByTagAndText(find(USERGROUP_DETAILS_MEMBERPICKLIST), "li", userName).click();
		
		//add user
		findChildElementByTagAndText(find(USERGROUP_DETAILS_MEMBERPICKLIST), "button", "Add").click();
		
		closeAndSaveUserGroupDetails();
	}
	
	//this method will most likely not work, because selenium doesn't wait long enough between adding  user to the usergroup.
	//use @addUserToUserGroup(String, String) instead and call it multiple times.
	@Deprecated()
	public static void addUsersToUserGroup(String userGroupName, String...userNames) {
		openUserGroupDetails(userGroupName);
		waitClickable(By.id(USERGROUP_DETAIALS_MEMBER_TOGGLER));
		
		//check if we have to expand the members category
		try {
			if(!find(USERGROUP_DETAILS_MEMBERPICKLIST).isDisplayed()){
				find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
				waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
			}
		}catch(Exception e) {
			find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
			waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
		}
		
		Misc.scrollElementIntoView(find(USERGROUP_MEMBERGROUPS));
		
		for(String userName: userNames) {
			WebElement unserNameElement = findChildElementByTagAndText(find(USERGROUP_DETAILS_MEMBERPICKLIST), "li", userName);
			waitClickable(unserNameElement);
			Click.doubleClick(unserNameElement);
			Time.waitUntilActionBarIsNotActive();
		}

		closeAndSaveUserGroupDetails();
	}
	
	public static void removeUserFromUserGroup(String userGroupName, String userName) {
		openUserGroupDetails(userGroupName);
		waitClickable(By.id(USERGROUP_DETAIALS_MEMBER_TOGGLER));
		
		//check if we have to expand the members category
		try {
			if(!find(USERGROUP_DETAILS_MEMBERPICKLIST).isDisplayed()){
				find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
				waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
			}
		}catch(Exception e) {
			find(USERGROUP_DETAIALS_MEMBER_TOGGLER).click();
			waitClickable(By.id(USERGROUP_DETAILS_MEMBERPICKLIST));
		}
		
		findChildElementByTagAndText(find(USERGROUP_DETAILS_MEMBERPICKLIST), "li", userName).click();
		
		findChildElementByTagAndText(find(USERGROUP_DETAILS_MEMBERPICKLIST), "button", "Remove").click();
		
		closeAndSaveUserGroupDetails();
	}
	
	public static void editUserGroup(String oldUserGroupName, String newUserGroupName, String newDescription) {
		openUserGroupDetails(oldUserGroupName);
		find(USERGROUP_DETAILS_NAME).clear();
		find(USERGROUP_DETAILS_NAME).sendKeys(newUserGroupName);
		find(USERGROUP_DETAILS_DESCRIPTION).clear();
		find(USERGROUP_DETAILS_DESCRIPTION).sendKeys(newDescription);
		
		closeAndSaveUserGroupDetails();
	}
	
	public static void removeUserGroup(String userGroupName) {
		openUserGroupDetails(userGroupName);
		find(DELETE_USERGROUP).click();
		waitClickable(By.id(DELETE_USERGROUP_YES));
		find(DELETE_USERGROUP_YES).click();
		
		waitClickable(By.id(USERGROUP_TABLE));
	}
	
	private static void openUserGroupDetails(String userGroupName) {		
		waitClickable(By.id(USERGROUP_TABLE));
		Find.findChildElementByTagAndText(find(USERGROUP_TABLE), "td", userGroupName).click();
		waitClickable(By.id(USERGROUP_DETAILS_NAME));
	}
	
	private static void closeAndSaveUserGroupDetails() {
		find(SAVE_USERGROUP_DETAILS).click();
		find(CLOSE_USERGROUP_DETAILS).click();
		waitClickable(By.id(USERGROUP_TABLE));
	}
	
}
