package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.data.UserRightsData;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;

/**
 * Dialog for editing user rights on a folder
 */
@ViewScoped
@Named
public class UserRightsDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 3635942135953153236L;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authBusiness;

	private Folder folder;
	private boolean isInAdminView;
	private boolean includeCurrentUser;
	private User additionalExcludedUser;
	private UserGroup additionalExcludedGroup;

	private List<User> usersWithEditRights;
	private List<UserRightsData> userRightsData;
	private List<UserRightsData> userGroupRightsData;

	private String breadcrumb;

	public Folder getFolder() {
		return folder;
	}

	public List<User> getUsersWithEditRights() {
		return usersWithEditRights;
	}

	public List<UserRightsData> getUserRightsData() {
		return userRightsData;
	}

	public List<UserRightsData> getUserGroupRightsData() {
		return userGroupRightsData;
	}

	public String getBreadcrumb() {
		return breadcrumb;
	}

	/**
	 * Loads userdata for the current folder. The current user will be excluded.
	 * 
	 * @param folderToLoad
	 */
	public void loadDialog(Folder folderToLoad) {
		isInAdminView = false;
		loadDialog(folderToLoad, false, null, null);
	}

	/**
	 * Load userdata for the current folder and an Administrator. The current user will be included in the view, but if
	 * a specific User or a UserGroup is selected, this group will be excluded from the dialog.
	 * 
	 * @param folderToLoad
	 * @param additionalExcludedUser
	 *            Additional excluded user
	 * @param additionalExcludedGroup
	 *            Additional excluded user group
	 */
	public void loadDialogAsAdmin(Folder folderToLoad, User additionalExcludedUser, UserGroup additionalExcludedGroup) {
		if (!authBusiness.hasAdminRights(getCurrentUser())) {
			throw new JackSecurityException("This function is only allowed for Administrators!");
		}

		isInAdminView = true;
		loadDialog(folderToLoad, true, additionalExcludedUser, additionalExcludedGroup);
	}

	/**
	 * (Re)load userdata for the current folder
	 * 
	 * @param folderToLoad
	 * @param includeCurrentUser
	 *            If the current user is included
	 * @param additionalExcludedUser
	 *            Additional excluded user
	 * @param additionalExcludedGroup
	 *            Additional excluded user group
	 */
	private void loadDialog(Folder folderToLoad, boolean includeCurrentUser, User additionalExcludedUser,
			UserGroup additionalExcludedGroup) {

		// Save parameters
		this.includeCurrentUser = includeCurrentUser;
		this.additionalExcludedUser = additionalExcludedUser;
		this.additionalExcludedGroup = additionalExcludedGroup;

		// Refresh list with editors
		usersWithEditRights = userBusiness.getAllUsersWithEditRights();

		// Load folder with breadcrumb
		folder = folderBusiness.getFolderWithManagingRights(folderToLoad).orElseThrow(AssertionError::new);
		loadFolderBreadcrumb();

		// Load user rights
		userRightsData = new ArrayList<>();
		userGroupRightsData = new ArrayList<>();

		for (final User user : usersWithEditRights) {

			// Check if user is explicitly excluded
			final boolean userIsExcluded = additionalExcludedUser != null &&
					additionalExcludedUser.getId() == user.getId();
			if (userIsExcluded) {
				continue;
			}

			// Only include current user if specified
			final boolean userMatchesCurrentUser = user.getId() == getCurrentUser().getId();
			if (userMatchesCurrentUser && !includeCurrentUser) {
				continue;
			}

			// Exclude the folder's owner
			final boolean userOwnsFolder = folderBusiness.isOwnedBy(folder, user);
			if (userOwnsFolder) {
				continue;
			}

			final AccessRight ownRight = folder.getManagingUsers().get(user);
			final AccessRight inheritedRight = folder.getInheritedManagingUsers().get(user);
			userRightsData.add(new UserRightsData(user, folder, null, ownRight, inheritedRight, false));
		}

		// Load user group rights
		for (final UserGroup group : userBusiness.getAllUserGroups()) {
			// Check if user group is explicitly excluded
			final boolean groupIsExcluded = additionalExcludedGroup != null
					&& additionalExcludedGroup.getId() == group.getId();
			if (groupIsExcluded) {
				continue;
			}

			AccessRight ownRight = folder.getManagingUserGroups().get(group);
			AccessRight inheritedRight = folder.getInheritedManagingUserGroups().get(group);
			userGroupRightsData.add(new UserRightsData(group, folder, null, ownRight, inheritedRight, false));
		}
	}

	private void loadFolderBreadcrumb() {

		final List<Folder> breadcrumbFolders = folder.getBreadcrumb();
		breadcrumbFolders.add(folder);
		final StringBuilder breadcrumbBuilder = new StringBuilder();
		final ContentFolder contentRoot = folderBusiness.getContentRoot();
		final PresentationFolder presentationRoot = folderBusiness.getPresentationRoot();

		for (Folder breadcrumbFolder : breadcrumbFolders) {

			if (breadcrumbFolder.getParentFolder() != null) {
				if (breadcrumbFolder instanceof ContentFolder
						&& breadcrumbFolder.getParentFolder().equals(contentRoot)) {
					// If the folder is directly one level below content root, the folder is a personal folder, so we have
					// to lookup the owner of this folder to replace the folder's name
					final Optional<User> owner = userBusiness.getUserOwningThisFolder(breadcrumbFolder);
					if (owner.isPresent()) {
						breadcrumbBuilder.append(owner.get().getLoginName());
					} else {
						// All content folders on the level below the root folder must have an owner!
						getLogger().fatalf("No owner for folder %s!", breadcrumbFolder);
					}
				} else if (breadcrumbFolder instanceof PresentationFolder
						&& breadcrumbFolder.getParentFolder().equals(presentationRoot)) {
					breadcrumbBuilder.append(breadcrumbFolder.getName());
				} else {
					breadcrumbBuilder.append(" > ").append(breadcrumbFolder.getName());
				}
			}

		}

		breadcrumb = breadcrumbBuilder.toString();
	}

	/**
	 * Unload the dialog and unset all variables
	 */
	public void unloadDialog() {
		folder = null;
		additionalExcludedUser = null;
		usersWithEditRights = null;
		userRightsData = null;
		userGroupRightsData = null;
	}

	/**
	 * Reload all data and set them to the current database state
	 */
	public void resetDialog() {
		loadDialog(folder, includeCurrentUser, additionalExcludedUser, additionalExcludedGroup);
	}

	/**
	 * Save all data to the database
	 */
	public void saveAndClose() {

		if (isInAdminView && !authBusiness.hasAdminRights(getCurrentUser())) {
			throw new JackSecurityException("Used UserRightsDialog in Administrator mode without admin rights!");
		} else if (!isInAdminView && !authBusiness.canManage(getCurrentUser(), folder)) {
			unloadDialog();
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			return;
		}

		// Update user rights
		for (UserRightsData data : userRightsData) {
			folderBusiness.updateFolderRightsForUser(data.getFolder(), data.getUser(), data.getRights());
		}

		// Update user group rights
		for (UserRightsData data : userGroupRightsData) {
			folderBusiness.updateFolderRightsForUserGroup(data.getFolder(), data.getUserGroup(), data.getRights());
		}

		unloadDialog();
	}

	/**
	 * Set missing rights
	 * 
	 * @param rights
	 */
	public void updateUserRights(UserRightsData rights) {
		if (rights.isWriteRights() || rights.isExtendedReadRights() || rights.isGradeRights()) {
			rights.setReadRights(true);
		}
		if (rights.isManageRights()) {
			rights.setReadRights(true);
			rights.setExtendedReadRights(true);
			rights.setWriteRights(true);
			rights.setGradeRights(true);
		}
	}

}
