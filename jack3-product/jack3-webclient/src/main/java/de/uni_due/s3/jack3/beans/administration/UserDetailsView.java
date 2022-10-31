package de.uni_due.s3.jack3.beans.administration;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
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

import org.primefaces.PrimeFaces;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.DualListModel;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.UserRightsDialogView;
import de.uni_due.s3.jack3.beans.data.UserRightsData;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.comparators.UserRightsTreeOrder;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.enums.ERightsToRemove;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.services.UserService;

@ViewScoped
@Named
public class UserDetailsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 911855678772173859L;
	private static final Comparator<TreeNode> TREE_NODE_COMPARATOR = new UserRightsTreeOrder();

	private long userId;
	private User user;
	private DualListModel<UserGroup> assignedUserGroupsModel;
	private TreeNode contentTree;
	private TreeNode presentationTree;
	private List<CourseRecord> openCourseRecords;

	private String userEmail;
	private boolean hasUserAdminRights;
	private boolean hasUserEditRights;
	private boolean originalEditRights;
	private ERightsToRemove rightsToRemove;

	private boolean isUserDeletable;
	private boolean isPersonalFolderOfUserDeletable;
	private List<Submission> submissionsForDeletion;
	private List<Enrollment> enrollmentsForDeletion;
	private List<CourseRecord> emptyCourseRecordsForDeletion;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private UserService userService;

	@Inject
	private UserRightsDialogView userRightsDialog;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private EnrollmentService enrollmentService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private CourseRecordService courseRecordService;

	public void loadView() throws IOException {
		final var foundUser = userBusiness.getUserById(userId);
		if (foundUser.isEmpty()) {
			sendErrorResponse(404, getLocalizedMessage("tenantadmin.userDetails.userNotFound"));
			return;
		}
		user = foundUser.get();

		openCourseRecords = enrollmentBusiness.getOpenCourseRecords(user);
		createSelectedUserTrees();
		updateSelectedUserGroups();

		userEmail = user.getEmail();
		hasUserAdminRights = user.isHasAdminRights();
		hasUserEditRights = user.isHasEditRights();
		originalEditRights = user.isHasEditRights();
		rightsToRemove = null;
	}

	public void openUserRightsDialog(Folder folder) {
		if (folder == null) {
			return;
		}
		userRightsDialog.loadDialogAsAdmin(folder, user, null);
	}

	public void saveGeneralInformation() {
		final var userWithSameEmail = userBusiness.getUserByEmail(userEmail.strip());
		if (userWithSameEmail.isPresent() && !userWithSameEmail.get().equals(user)) {
			addFacesMessage("generalInformation", FacesMessage.SEVERITY_ERROR, null,
					"tenantadmin.createUser.emailNotUnique");
			return;
		}
		userBusiness.updateUserInformation(user, userEmail, hasUserAdminRights, hasUserEditRights);
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
		originalEditRights = hasUserEditRights;
	}

	public void onAssignedGroupsTransfer(TransferEvent event) {
		for (final Object o : event.getItems()) {
			final UserGroup touchedUserGroup = (UserGroup) o;
			userBusiness.switchMembershipOfUser(touchedUserGroup, user);
		}
		Collections.sort(assignedUserGroupsModel.getSource());
		Collections.sort(assignedUserGroupsModel.getTarget());
	}

	public void updateUserRights(UserRightsData userRightsData) {
		folderBusiness.updateFolderRightsForUser(userRightsData.getFolder(), userRightsData.getUser(),
				userRightsData.getRights());

		refreshRightsOnUserTree(contentTree);
		refreshRightsOnUserTree(presentationTree);
	}

	private void refreshRightsOnUserTree(TreeNode node) {
		final Object data = node.getData();
		if (data instanceof UserRightsData) {
			final UserRightsData urd = (UserRightsData) data;
			final Folder folder = folderBusiness.getFolderWithManagingRights(urd.getFolder())
					.orElseThrow(AssertionError::new);
			urd.updateData(folder.getManagingUsers().get(urd.getUser()),
					folder.getInheritedManagingUsers().get(urd.getUser()));
		}
		for (final TreeNode child : node.getChildren()) {
			refreshRightsOnUserTree(child);
		}
	}

	private void updateSelectedUserGroups() {
		final List<UserGroup> userGroups = userBusiness.getUserGroupsForUser(user);
		final List<UserGroup> availableGroups = userBusiness.getAllUserGroups();
		availableGroups.removeAll(userGroups);
		assignedUserGroupsModel = new DualListModel<>(availableGroups, userGroups);
	}

	public void removeUserRights() {
		if (rightsToRemove == null) {
			// No rights to remove
			return;
		}

		switch (rightsToRemove) {
		case ALL:
			folderBusiness.removeUserRightsOnPresentationFolders(user);
			folderBusiness.removeUserRightsOnNonPersonalContentFolders(user);
			break;
		case CONTENT_FOLDERS:
			folderBusiness.removeUserRightsOnNonPersonalContentFolders(user);
			break;
		case PRESENTATION_FOLDERS:
			folderBusiness.removeUserRightsOnPresentationFolders(user);
			break;
		}

		createSelectedUserTrees();
		updateSelectedUserGroups();

		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, null, "tenantadmin.removeRights.done");
	}

	// ---------------------------------------
	// ---------- Deletion handling ----------
	// ---------------------------------------

	/**
	 * All Items for the deletionDialog are gathered:
	 * <ul>
	 * <li> is personal Folder, if one exists, empty or all contained data deletable</li>
	 * <li> exist submissions from the user</li>
	 * <li> exist empty courseRecords from the user</li>
	 * <li> exist enrollments from the user</li>
	 * </ul>
	 * If there are no items which are connected to the user, user deletion is allowed.
	 *
	 */
	public void prepareDeletionDialog() {
		// Personal Folder
		if (user.getPersonalFolder() != null) {
			isPersonalFolderOfUserDeletable = folderBusiness.isContentFolderDeletable(user.getPersonalFolder(), user);
		} else {
			isPersonalFolderOfUserDeletable = true;
		}
		// Submissions
		submissionsForDeletion = submissionService.getAllSubmissionsForUser(user);
		// Empty Course Records
		emptyCourseRecordsForDeletion = courseRecordService.getAllEmptyCourseRecordsForUser(user);
		// Enrollments
		enrollmentsForDeletion = enrollmentService.getEnrollments(user, EEnrollmentStatus.ENROLLED);

		isUserDeletable = isPersonalFolderOfUserDeletable && submissionsForDeletion.isEmpty()
				&& emptyCourseRecordsForDeletion.isEmpty() && enrollmentsForDeletion.isEmpty();
	}

	public void closeUserDeletionDialog() {
		isUserDeletable = false;
		PrimeFaces.current().executeScript("PF('deleteUserDialog').hide()");
	}

	/**
	 * Deletion is only allowed, if none or only empty data is connected to the user.
	 * 
	 * If deletion is allowed:
	 * <ul>
	 * <li>Removes all rights from the user</li>
	 * <li>Deletes the personal folder, if one exists</li>
	 * <li>Removes the user from all userGroups</li>
	 * <li>Deletes the user</li>
	 * <li>Closes the Deletion-Dialog and updates the UI</li>
	 * </ul>
	 * 
	 * If an error occurs, a error message is shown to the user.
	 *
	 */
	public void deleteUser() {
		//is there data which is connected to the user?
		if (isUserDeletable) {
			try {
				//remove rights
				folderBusiness.removeUserRightsOnPresentationFolders(user);
				folderBusiness.removeUserRightsOnNonPersonalContentFolders(user);
				//delete personal Folder
				if (user.getPersonalFolder() != null) {
					folderBusiness.deletePersonalFolder(user);
				}
				//remove user from usergroups
				userBusiness.removeUserFromAllUserGroups(user);
				//delete user
				userService.removeUser(user);
				//update UI
				closeUserDeletionDialog();
				redirect(viewId.getTenantUserManagement());
			} catch (Exception e) {
				e.printStackTrace();
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "tenantadmin.deleteUser.deletionFailed",
						"tenantadmin.deleteUser.error");
			}
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "tenantadmin.deleteUser.deletionFailed",
					"tenantadmin.deleteUser.notAllowed");
		}
	}

	/**
	 * Creates the YourareHereModel for the courseOffer/course/exercise
	 * and redirects to the CourseRecordSubmissions(CourseRecords) or SubmissionsDetails(Exercise Submissions) page.
	 *
	 * @param submission
	 */
	public void redirectToSubmission(Submission submission) {
		Optional<CourseOffer> maybeCourseOffer = submission.getCourseOffer();

		AbstractExercise exercise = submission.getExercise();

		try {
			if (maybeCourseOffer.isPresent()) {
				CourseOffer courseoffer = maybeCourseOffer.get();
				//courseOffer
				createYouAreHereModelForCourseOffer(courseoffer);

				redirect(viewId.getCourseRecordSubmissions()
						.withParam("courseRecord", submission.getCourseRecord().getId())
						.withParam("courseOffer", courseoffer.getId()));

			} else if (submission.getCourseRecord() != null) {
				//course
				AbstractCourse course = submission.getCourseRecord().getCourse();
				createYouAreHereModelForCourse(course, false);
				redirect(viewId.getCourseRecordSubmissions()
						.withParam("courseRecord", submission.getCourseRecord().getId())
						.withParam("course", course.getId()));

			} else {
				//exercise
				createYouAreHereModelForExercise(exercise);
				redirect(viewId.getSubmissionDetails().withParam("submission", submission.getId()));
			}
		} catch (IOException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "tenantadmin.deleteUser.redirectionError", null);
		}
	}

	/**
	 * Creates YouarehereModel for the courseOffer and redirects to the CourseOfferStatistics
	 *
	 * @param enrollment
	 */
	public void redirectToEnrollment(Enrollment enrollment) {
		try {
			CourseOffer courseOffer = enrollment.getCourseOffer();
			redirect(viewId.getCourseOfferParticipants().withParam(courseOffer));
		} catch (IOException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "tenantadmin.deleteUser.redirectionError", null);
		}
	}

	/**
	 * Creates the YourareHereModel for the courseOffer/ or course
	 * and redirects to the CourseRecordSubmissions(CourseRecords) page.
	 *
	 * @param courseRecord
	 */
	public void redirectToEmptyCourseRecord(CourseRecord courseRecord) {
		Optional<CourseOffer> maybeCourseOffer = courseRecord.getCourseOffer();

		try {
			if (maybeCourseOffer.isPresent()) {
				CourseOffer courseoffer = maybeCourseOffer.get();
				//courseOffer
				createYouAreHereModelForCourseOffer(courseoffer);

				redirect(viewId.getCourseRecordSubmissions().withParam("courseRecord", courseRecord.getId())
						.withParam("courseOffer", courseoffer.getId()));

			} else {
				//course
				AbstractCourse course = courseRecord.getCourse();
				createYouAreHereModelForCourse(course, false);
				redirect(viewId.getCourseRecordSubmissions().withParam("courseRecord", courseRecord.getId())
						.withParam("course", course.getId()));
			}
		} catch (IOException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "tenantadmin.deleteUser.redirectionError", null);
		}

	}

	// -----------------------------------
	// ---------- Tree handling ----------
	// -----------------------------------

	private void createSelectedUserTrees() {
		// Lookup all content folders for this user
		final var contentFolders = folderBusiness.getAllContentFoldersForUser(user);
		final var contentRoot = folderBusiness.getContentRoot();
		Map<Folder, TreeNode> folderMap = new HashMap<>();
		TreeNode root = new DefaultTreeNode();

		// Create the content tree
		processContentFolders(contentFolders, contentRoot, folderMap, root);
		sortTreeNodes(folderMap, root);
		contentTree = root;

		// Lookup all presentation folders for this user
		final var presentationFolders = folderBusiness.getAllPresentationFoldersForUser(user);
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

	private void processContentFolders(final List<ContentFolder> contentFolders,
			final ContentFolder contentRoot, Map<Folder, TreeNode> folderMap, TreeNode root) {
		for (Folder folder : contentFolders) {
			if (!folderMap.containsKey(folder)) {
				UserRightsData data = new UserRightsData(user, folder, null,
						folder.getManagingUsers().get(user),
						folder.getInheritedManagingUsers().get(user),
						folder.isChildOf(user.getPersonalFolder()));

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

				final UserRightsData parentData = new UserRightsData(user, parentFolder, null,
						parentFolder.getManagingUsers().get(user),
						parentFolder.getInheritedManagingUsers().get(user),
						parentFolder.isChildOf(user.getPersonalFolder()));

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
				UserRightsData data = new UserRightsData(user, folder, null,
						folder.getManagingUsers().get(user),
						folder.getInheritedManagingUsers().get(user), false);

				TreeNode node = new DefaultTreeNode(data);
				folderMap.put(folder, node);

				iterateParentPresentationFolders(folderMap, root, presentationRoot, folder, node);
			}
		}
	}

	private void iterateParentPresentationFolders(Map<Folder, TreeNode> folderMap, TreeNode root,
			final PresentationFolder presentationRoot, Folder folder, TreeNode node) {
		while (folder.getParentFolder() != null) {
			if (folderMap.containsKey(folder.getParentFolder())) {
				final TreeNode parentNode = folderMap.get(folder.getParentFolder());
				addChildNodeToParentNode(parentNode, node);
				return;
			} else if (folder.getParentFolder().equals(presentationRoot)) {
				addChildNodeToParentNode(root, node);
				return;
			} else {
				final Folder parentFolder = folderBusiness.getFolderWithManagingRights(folder.getParentFolder())
						.orElseThrow(AssertionError::new);

				final UserRightsData parentData = new UserRightsData(user, parentFolder, null,
						parentFolder.getManagingUsers().get(user),
						parentFolder.getInheritedManagingUsers().get(user), false);

				node = addNodeToNewParent(folderMap, node, parentFolder, parentData);
				folder = parentFolder;
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

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public DualListModel<UserGroup> getAssignedUserGroupsModel() {
		return assignedUserGroupsModel;
	}

	public void setAssignedUserGroupsModel(DualListModel<UserGroup> assignedUserGroupsModel) {
		this.assignedUserGroupsModel = assignedUserGroupsModel;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserName() {
		return user.getLoginName();
	}

	public LocalDateTime getUserLastLogin() {
		return user.getLastLogin();
	}

	public boolean isHasUserAdminRights() {
		return hasUserAdminRights;
	}

	public void setHasUserAdminRights(boolean hasUserAdminRights) {
		this.hasUserAdminRights = hasUserAdminRights;
	}

	public boolean isHasUserEditRights() {
		return hasUserEditRights;
	}

	public void setHasUserEditRights(boolean hasUserEditRights) {
		this.hasUserEditRights = hasUserEditRights;
	}

	public ERightsToRemove getRightsToRemove() {
		return rightsToRemove;
	}

	public ERightsToRemove[] getAllRightsToRemove() {
		return ERightsToRemove.values();
	}

	public void setRightsToRemove(ERightsToRemove rightsToRemove) {
		this.rightsToRemove = rightsToRemove;
	}

	public TreeNode getContentTree() {
		return contentTree;
	}

	public TreeNode getPresentationTree() {
		return presentationTree;
	}

	public List<CourseRecord> getOpenCourseRecords() {
		return openCourseRecords;
	}

	public boolean isOriginalEditRights() {
		return originalEditRights;
	}

	public boolean isUserDeletable() {
		return isUserDeletable;
	}

	public boolean isPersonalFolderOfUserDeletable() {
		return isPersonalFolderOfUserDeletable;
	}

	public List<Submission> getSubmissionsForDeletion() {
		return submissionsForDeletion;
	}

	public List<Enrollment> getEnrollmentsForDeletion() {
		return enrollmentsForDeletion;
	}

	public List<CourseRecord> getEmptyCourseRecordsForDeletion() {
		return emptyCourseRecordsForDeletion;
	}


}
