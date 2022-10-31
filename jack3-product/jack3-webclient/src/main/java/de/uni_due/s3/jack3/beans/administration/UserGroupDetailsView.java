package de.uni_due.s3.jack3.beans.administration;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.TransferEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.DualListModel;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.UserRightsDialogView;
import de.uni_due.s3.jack3.beans.data.UserRightsData;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.comparators.UserRightsTreeOrder;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

@ViewScoped
@Named
public class UserGroupDetailsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -9106229043171440107L;
	private static final Comparator<TreeNode> TREE_NODE_COMPARATOR = new UserRightsTreeOrder();

	private long userGroupId;
	private UserGroup userGroup;
	private DualListModel<User> memberUsersModel;
	private DualListModel<UserGroup> memberGroupsModel;
	private TreeNode contentTree;
	private TreeNode presentationTree;
	private String userGroupName;
	private String userGroupDescription;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private UserRightsDialogView userRightsDialog;

	public void loadView() throws IOException {
		final var foundUserGroup = userBusiness.getUserGroupWithLazyDataById(userGroupId);
		if (foundUserGroup.isEmpty()) {
			sendErrorResponse(404, getLocalizedMessage("tenantadmin.userGroupDetails.userGroupNotFound"));
			return;
		}
		userGroup = foundUserGroup.get();

		final List<User> allUsers = userBusiness.getAllUsersWithEditRights();
		final List<User> memberUsers = new ArrayList<>(userGroup.getMemberUsers());
		allUsers.removeAll(memberUsers);
		Collections.sort(memberUsers);
		memberUsersModel = new DualListModel<>(allUsers, memberUsers);

		final List<UserGroup> allGroups = userBusiness.getAllUserGroups();
		final List<UserGroup> memberGroups = new ArrayList<>(userGroup.getMemberGroups());
		allGroups.remove(userGroup);
		allGroups.removeAll(memberGroups);
		// Remove all user groups containing the selected user group because adding these user groups would cause a circle
		final List<UserGroup> parentGroups = new ArrayList<>(allGroups.size());
		for (final UserGroup other : allGroups) {
			if (other.containsGroupAsAnyMember(userGroup)) {
				parentGroups.add(other);
			}
		}
		allGroups.removeAll(parentGroups);
		Collections.sort(memberGroups);

		memberGroupsModel = new DualListModel<>(allGroups, memberGroups);

		createSelectedUserGroupTrees();
		userGroupName = userGroup.getName();
		userGroupDescription = userGroup.getDescription();
	}

	public void openUserRightsDialog(Folder folder) {
		if (folder == null) {
			return;
		}
		userRightsDialog.loadDialogAsAdmin(folder, null, userGroup);
	}

	public void deleteUserGroup() throws IOException {
		userBusiness.deleteUserGroup(userGroup);
		redirect(viewId.getTenantUserManagement());
	}

	public void saveGeneralInformation() {
		final String msgTarget = "generalInformation";
		if (userGroupName == null || userGroupName.isBlank()) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUserGroup.nameEmpty");
			return;
		}
		final var userGroupWithSameName = userBusiness.getUserGroup(userGroupName.strip());
		if (userGroupWithSameName.isPresent() && !userGroupWithSameName.get().equals(userGroup)) {
			addFacesMessage(msgTarget, FacesMessage.SEVERITY_ERROR, null, "tenantadmin.createUserGroup.nameNotUnique");
			return;
		}
		userBusiness.updateUserGroupInformation(userGroup, userGroupName, userGroupDescription);
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
	}

	public void onMemberUsersTransfer(TransferEvent event) {
		for (final Object o : event.getItems()) {
			final User touchedUser = (User) o;
			userBusiness.switchMembershipOfUser(userGroup, touchedUser);
		}
		Collections.sort(memberUsersModel.getSource());
		Collections.sort(memberUsersModel.getTarget());
	}

	public void onMemberGroupsTransfer(TransferEvent event) {
		for (final Object o : event.getItems()) {
			final UserGroup touchedGroup = (UserGroup) o;
			if (touchedGroup.equals(userGroup)) {
				continue;
			}
			userBusiness.switchMembershipOfUserGroup(userGroup, touchedGroup);
		}
		Collections.sort(memberGroupsModel.getSource());
		Collections.sort(memberGroupsModel.getTarget());
	}

	public void updateUserRights(UserRightsData userRightsData) {
		folderBusiness.updateFolderRightsForUserGroup(userRightsData.getFolder(), userRightsData.getUserGroup(),
				userRightsData.getRights());
		refreshRightsOnUserGroupTree(contentTree);
		refreshRightsOnUserGroupTree(presentationTree);
	}

	private void refreshRightsOnUserGroupTree(TreeNode node) {
		final Object data = node.getData();
		if (data instanceof UserRightsData) {
			final UserRightsData urd = (UserRightsData) data;
			final Folder folder = folderBusiness.getFolderWithManagingRights(urd.getFolder()).orElseThrow();
			urd.updateData(folder.getManagingUserGroups().get(urd.getUserGroup()),
					folder.getInheritedManagingUserGroups().get(urd.getUserGroup()));
		}
		for (final TreeNode child : node.getChildren()) {
			refreshRightsOnUserGroupTree(child);
		}
	}

	// -----------------------------------
	// ---------- Tree handling ----------
	// -----------------------------------

	private void createSelectedUserGroupTrees() {
		// Lookup all content folders for this user group
		final var contentFolders = folderBusiness.getAllContentFoldersForUserGroup(userGroup);
		final var contentRoot = folderBusiness.getContentRoot();
		Map<Folder, TreeNode> folderMap = new HashMap<>();
		TreeNode root = new DefaultTreeNode();

		// Create the content tree
		processContentFolders(contentFolders, contentRoot, folderMap, root);
		sortTreeNodes(folderMap, root);
		contentTree = root;

		// Lookup all presentation folders for this user group
		final var presentationFolders = folderBusiness.getAllPresentationFoldersForUserGroup(userGroup);
		final var presentationRoot = folderBusiness.getPresentationRoot();
		folderMap = new HashMap<>();
		root = new DefaultTreeNode();

		// Create the presentation tree
		processPresentationFolders(folderMap, root, presentationFolders, presentationRoot);
		sortTreeNodes(folderMap, root);
		presentationTree = root;
	}

	private void sortTreeNodes(Map<Folder, TreeNode> folderMap, TreeNode root) {
		for (final TreeNode node : folderMap.values()) {
			node.getChildren().sort(TREE_NODE_COMPARATOR);
		}

		root.getChildren().sort(TREE_NODE_COMPARATOR);
	}

	private void processContentFolders(final List<ContentFolder> folders,
			final ContentFolder contentRoot, Map<Folder, TreeNode> folderMap, TreeNode root) {
		for (Folder folder : folders) {
			if (!folderMap.containsKey(folder)) {
				UserRightsData data = new UserRightsData(userGroup, folder, null,
						folder.getManagingUserGroups().get(userGroup),
						folder.getInheritedManagingUserGroups().get(userGroup), false);

				TreeNode node = new DefaultTreeNode(data);
				folderMap.put(folder, node);

				iterateParentContentFolders(contentRoot, folderMap, root, folder, data, node);
			}
		}
	}

	private void iterateParentContentFolders(final ContentFolder contentRoot,
			Map<Folder, TreeNode> folderMap, TreeNode root, Folder folder, UserRightsData data, TreeNode node) {
		while (folder.getParentFolder() != null) {
			if (folderMap.containsKey(folder.getParentFolder())) {
				final TreeNode parentNode = folderMap.get(folder.getParentFolder());
				addChildNodeToParentNode(parentNode, node);
				return;
			} else if (folder.getParentFolder().equals(contentRoot)) {
				addChildNodeToParentNode(root, node);
				final Optional<User> owner = userBusiness.getUserOwningThisFolder(folder);
				if (owner.isPresent()) {
					data.setFolderAlias(owner.get().getLoginName());
				} else {
					getLogger().info("No owner for folder " + folder);
				}
				return;
			} else {
				final Folder parentFolder = folderBusiness.getFolderWithManagingRights(folder.getParentFolder())
						.orElseThrow(AssertionError::new);

				final UserRightsData parentData = new UserRightsData(userGroup, parentFolder, null,
						parentFolder.getManagingUserGroups().get(userGroup),
						parentFolder.getInheritedManagingUserGroups().get(userGroup), false);

				node = addNodeToNewParent(folderMap, node, parentFolder, parentData);
				folder = parentFolder;
				data = parentData;
			}
		}
	}

	private void processPresentationFolders(Map<Folder, TreeNode> folderMap, TreeNode root,
			final List<PresentationFolder> presentationFolders, final PresentationFolder presentationRoot) {
		for (Folder folder : presentationFolders) {
			if (!folderMap.containsKey(folder)) {
				UserRightsData data = new UserRightsData(userGroup, folder, null,
						folder.getManagingUserGroups().get(userGroup),
						folder.getInheritedManagingUserGroups().get(userGroup), false);

				TreeNode node = new DefaultTreeNode(data);
				folderMap.put(folder, node);

				while (folder.getParentFolder() != null) {
					if (folderMap.containsKey(folder.getParentFolder())) {
						final TreeNode parentNode = folderMap.get(folder.getParentFolder());
						addChildNodeToParentNode(parentNode, node);
						break;
					} else if (folder.getParentFolder().equals(presentationRoot)) {
						addChildNodeToParentNode(root, node);
						break;
					} else {
						final Folder parentFolder = folderBusiness.getFolderWithManagingRights(folder.getParentFolder())
								.orElseThrow(AssertionError::new);

						final UserRightsData parentData = new UserRightsData(userGroup, parentFolder, null,
								parentFolder.getManagingUserGroups().get(userGroup),
								parentFolder.getInheritedManagingUserGroups().get(userGroup), false);

						node = addNodeToNewParent(folderMap, node, parentFolder, parentData);
						folder = parentFolder;
					}
				}
			}
		}
	}

	private TreeNode addNodeToNewParent(Map<Folder, TreeNode> folderMap, TreeNode node, final Folder parentFolder,
			final UserRightsData parentData) {
		final TreeNode newParent = new DefaultTreeNode(parentData);
		folderMap.put(parentFolder, newParent);
		addChildNodeToParentNode(newParent, node);

		return newParent;
	}

	private void addChildNodeToParentNode(TreeNode parent, TreeNode child) {
		parent.getChildren().add(child);
		child.setParent(parent);
	}

	// -----------------------------------------
	// ---------- Getters and Setters ----------
	// -----------------------------------------

	public long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public String getUserGroupNameFromObject() {
		return userGroup.getName();
	}

	public DualListModel<User> getMemberUsersModel() {
		return memberUsersModel;
	}

	public void setMemberUsersModel(DualListModel<User> memberUsersModel) {
		this.memberUsersModel = memberUsersModel;
	}

	public DualListModel<UserGroup> getMemberGroupsModel() {
		return memberGroupsModel;
	}

	public void setMemberGroupsModel(DualListModel<UserGroup> memberGroupsModel) {
		this.memberGroupsModel = memberGroupsModel;
	}

	public TreeNode getContentTree() {
		return contentTree;
	}

	public TreeNode getPresentationTree() {
		return presentationTree;
	}

	public String getUserGroupName() {
		return userGroupName;
	}

	public void setUserGroupName(String userGroupName) {
		this.userGroupName = userGroupName;
	}

	public String getUserGroupDescription() {
		return userGroupDescription;
	}

	public void setUserGroupDescription(String userGroupDescription) {
		this.userGroupDescription = userGroupDescription;
	}

}
