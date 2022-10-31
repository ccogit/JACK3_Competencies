package de.uni_due.s3.jack3.beans.administration;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.primefaces.PrimeFaces;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

@ViewScoped
@Named
public class UserManagementView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1251408690245492099L;

	private List<User> userList;
	private List<User> filteredUserList;
	private String newUserLoginName;
	private String newUserEmail;
	private boolean newUserAdminRights;
	private boolean newUserEditRights;

	private List<UserGroup> userGroupList;
	private String newUserGroupName;
	private String newUserGroupDescription;

	@Inject
	private UserBusiness userBusiness;

	public void createUser() {
		final String msgTarget = "createUserDialog";
		// We show an error message if the user's loginname is empty.
		if (newUserLoginName == null || newUserLoginName.isBlank()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUser.nameEmpty");
			return;
		}
		// We show an error message if there already is a user with the given name.
		if (userBusiness.getUserByName(newUserLoginName.strip()).isPresent()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUser.nameNotUnique");
			return;
		}
		// We show an error message if there already is a user with the given email.
		if (userBusiness.getUserByEmail(newUserEmail.strip()).isPresent()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUser.emailNotUnique");
			return;
		}

		try {
			userBusiness.createUser(getCurrentUser(), newUserLoginName.strip(), newUserEmail.strip(),
					newUserAdminRights, newUserEditRights, getResourceBundle(), getTenantUrl());
		} catch (final MessagingException e) {
			// A MessagingException signals failure to send the credentials. The user was created anyways.
			getLogger().warnf("Failed to send credentials to user %s.", newUserLoginName, e);
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN,
					"tenantadmin.credentialsMail.failedSummary", "tenantadmin.credentialsMail.failedDetail");
		}
		closeUserCreationDialog(true);
	}

	public void closeUserCreationDialog(boolean update) {
		newUserLoginName = null;
		newUserEmail = null;
		newUserAdminRights = false;
		newUserEditRights = false;
		PrimeFaces.current().executeScript("PF('createUserDialog').hide()");
		PrimeFaces.current().ajax().update(":createUserForm:createUserDialog");
		if (update) {
			refreshUserList();
			PrimeFaces.current().ajax().update(":userManagement:dtUserTable");
		}
	}

	public void createUserGroup() {
		final String msgTarget = "createUserGroupDialog";
		// The same conditions as for users (see above) apply, except the unique email
		if (newUserGroupName == null || newUserGroupName.isBlank()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUserGroup.nameEmpty");
			return;
		}
		if (userBusiness.getUserGroup(newUserGroupName.strip()).isPresent()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUserGroup.nameNotUnique");
			return;
		}

		userBusiness.createUserGroup(newUserGroupName.strip(), newUserGroupDescription);
		closeUserGroupCreationDialog(true);
	}

	public void closeUserGroupCreationDialog(boolean update) {
		newUserGroupName = null;
		newUserGroupDescription = null;
		PrimeFaces.current().executeScript("PF('createUserGroupDialog').hide()");
		PrimeFaces.current().ajax().update(":createUserGroupForm:createUserGroupDialog");
		if (update) {
			refreshUserGroupList();
			PrimeFaces.current().ajax().update(":userManagement:dtUserGroup");
		}
	}

	public List<User> getFilteredUserList() {
		return filteredUserList;
	}

	public String getNewUserEmail() {
		return newUserEmail;
	}

	public String getNewUserGroupDescription() {
		return newUserGroupDescription;
	}

	public String getNewUserGroupName() {
		return newUserGroupName;
	}

	public String getNewUserLoginName() {
		return newUserLoginName;
	}

	public String getLoginNameRegex() {
		return User.LOGIN_NAME_REGEX;
	}

	public List<UserGroup> getUserGroupList() {
		if (userGroupList == null) {
			refreshUserGroupList();
		}
		return userGroupList;
	}

	public List<User> getUserList() {
		if (userList == null) {
			refreshUserList();
		}
		return userList;
	}

	public boolean isNewUserAdminRights() {
		return newUserAdminRights;
	}

	public boolean isNewUserEditRights() {
		return newUserEditRights;
	}

	private void refreshUserGroupList() {
		userGroupList = userBusiness.getAllUserGroups();
	}

	private void refreshUserList() {
		userList = userBusiness.getAllUsers();
		filteredUserList = null;
	}

	public void setFilteredUserList(List<User> filteredUserList) {
		this.filteredUserList = filteredUserList;
	}

	public void setNewUserAdminRights(boolean newUserAdminRights) {
		this.newUserAdminRights = newUserAdminRights;
	}

	public void setNewUserEditRights(boolean newUserEditRights) {
		this.newUserEditRights = newUserEditRights;
	}

	public void setNewUserEmail(String newUserEmail) {
		this.newUserEmail = newUserEmail;
	}

	public void setNewUserGroupDescription(String newUserGroupDescription) {
		this.newUserGroupDescription = newUserGroupDescription;
	}

	public void setNewUserGroupName(String newUserGroupName) {
		this.newUserGroupName = newUserGroupName;
	}

	public void setNewUserLoginName(String newUserLoginName) {
		this.newUserLoginName = newUserLoginName;
	}

	public User getSelectedUser() {
		// not important because after selecting a user, we redirect (see below)
		return null;
	}

	public void setSelectedUser(User selectedUser) throws IOException {
		if (selectedUser != null) {
			redirect(viewId.getUserDetails().withParam(selectedUser));
		}
	}

	public UserGroup getSelectedUserGroup() {
		// not important because after selecting a user group, we redirect (see below)
		return null;
	}

	public void setSelectedUserGroup(UserGroup selectedUserGroup) throws IOException {
		if (selectedUserGroup != null) {
			redirect(viewId.getUserGroupDetails().withParam(selectedUserGroup));
		}
	}

	public void setUserGroupList(List<UserGroup> userGroupList) {
		this.userGroupList = userGroupList;
	}

	public void setUserList(List<User> userList) {
		this.userList = userList;
	}

}
