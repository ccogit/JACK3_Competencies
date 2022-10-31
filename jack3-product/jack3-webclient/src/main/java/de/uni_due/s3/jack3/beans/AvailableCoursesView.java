package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.data.PendingDragDropAction;
import de.uni_due.s3.jack3.beans.dialogs.DeletionDialogView;
import de.uni_due.s3.jack3.beans.dialogs.FolderRenameDialogView;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.DragDropException;
import de.uni_due.s3.jack3.business.exceptions.FolderException;
import de.uni_due.s3.jack3.business.helpers.ECourseOfferAccess;
import de.uni_due.s3.jack3.business.helpers.EFolderChildType;
import de.uni_due.s3.jack3.comparators.PresentationTreeOrder;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.enums.ETreeNodeType;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@ViewScoped
@Named
public class AvailableCoursesView extends AbstractContentTreeView<PresentationFolder> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String searchCourseOffers = "";
	private String newRootFolderName = null;
	private TreeNode selectedPresentationNode;
	private TreeNode rootPresentationNode;

	/** Corresponds to a folder ID. May be null and is loaded from UserSession in this case. */
	private Long locationId;
	/** The folder which belongs to the ID above, if present. */
	private PresentationFolder locationFolder;

	private HashMap<Folder, TreeNode> presentationFolderMap;
	private HashMap<CourseOffer, TreeNode> courseMap;
	private Map<PresentationFolder, AccessRight> presentationFolderRightsMap;
	private Map<PresentationFolder, String> parallelHintMap = new HashMap<>();

	// Saves course offers with their free places. If there are no free places, "0" is saved,
	// In case of an unlimited number, an empty optional is saved.
	private Map<CourseOffer, Optional<Long>> freePlacesCache = new HashMap<>();

	private TreeNode inputNode;

	private String newPresentationFolderName;
	private String newCourseOfferName;

	private String duplicateCourseOfferName;
	private String duplicateCourseOfferWarning;

	private boolean filterMyCourseOffers = false;
	
	private PendingDragDropAction<PresentationFolder, CourseOffer> moveCourseOfferAction;
	private PendingDragDropAction<PresentationFolder, PresentationFolder> moveFolderAction;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private UserSession userSession;
	
	@Inject
	private DeletionDialogView deletionDialogView;

	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private FolderRenameDialogView folderRenameDialogView;

	public void clearInputTreeNode() {
		if (inputNode != null) {
			inputNode.getParent().getChildren().remove(inputNode);
			inputNode = null;
		}
		newPresentationFolderName = "";
		newCourseOfferName = "";
	}

	@Override
	protected Optional<PresentationFolder> getSelectedFolder() {
		if (selectedPresentationNode != null && selectedPresentationNode.getData() instanceof PresentationFolder) {
			return Optional.of((PresentationFolder) selectedPresentationNode.getData());
		}
		return Optional.empty();
	}

	/**
	 * Listener for context menu. Displays the input text field for a new
	 * content folder.
	 */
	public void createNewEmptyPresentationFolder() {
		inputNode = new DefaultTreeNode("");
		inputNode.setType("newFolder");

		inputNode.setParent(selectedPresentationNode);
		selectedPresentationNode.setExpanded(true);
		selectedPresentationNode.getChildren().add(0, inputNode);
	}

	/**
	 * Listener for context menu. Displays the input text field for a new
	 * content folder.
	 */
	public void createNewEmptyCourseOffer() {
		inputNode = new DefaultTreeNode("");
		inputNode.setType("newCourseOffer");

		inputNode.setParent(selectedPresentationNode);
		selectedPresentationNode.setExpanded(true);
		selectedPresentationNode.getChildren().add(0, inputNode);
	}

	/**
	 * Listener for remote command. Creates a new content folder or renames an
	 * existing one based on the text field input.
	 */
	public void createNewPresentationFolder() {
		selectedPresentationNode = inputNode.getParent();
		selectedPresentationNode.setSelected(true);

		final PresentationFolder parent = (PresentationFolder) (inputNode.getParent().getData());

		Optional<String> failureMessage = validateName(parent, newPresentationFolderName,
				EFolderChildType.PRESENTATION_FOLDER);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			clearInputTreeNode();
			return;
		}

		final PresentationFolder newFolder = folderBusiness.createPresentationFolder(newPresentationFolderName,
				parent);
		final TreeNode newFolderNode = new DefaultTreeNode(newFolder);
		newFolderNode.setType(ETreeNodeType.EMPTY_FOLDER_TYPE.getName());

		presentationFolderMap.get(parent).getChildren().add(newFolderNode);
		if (presentationFolderMap.get(parent).getType().equals(ETreeNodeType.EMPTY_FOLDER_TYPE.getName())) {
			presentationFolderMap.get(parent).setType(ETreeNodeType.PLAIN_FOLDER_TYPE.getName());
		}
		presentationFolderMap.put(newFolder, newFolderNode);
		insertTreeNode(presentationFolderMap.get(parent).getChildren(), newFolderNode);

		presentationFolderRightsMap.put(newFolder, authorizationBusiness.getMaximumRightForUser(getCurrentUser(), newFolder));
		refreshShownAccessRights();

		clearInputTreeNode();
		updateSingleSearchString(newFolder);
	}

	/**
	 * Listener for remote command that adds a new root folder (only available for Administrators)
	 */
	public void createNewRootPresentationFolder() {

		if (newRootFolderName == null || newRootFolderName.isBlank()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", "global.invalidName.empty");
			return;
		}

		PresentationFolder newFolder;
		try {
			newFolder = folderBusiness.createTopLevelPresentationFolder(getCurrentUser(), newRootFolderName);
		} catch (ActionNotAllowedException e) {
			throw new JackSecurityException("createNewRootPresentationFolder was called without admin rights.", e);
		}

		final TreeNode newFolderNode = new DefaultTreeNode(newFolder);
		newFolderNode.setType(ETreeNodeType.EMPTY_FOLDER_TYPE.getName());
		newFolderNode.setParent(rootPresentationNode);

		rootPresentationNode.getChildren().add(newFolderNode); // Add as a child of the root node
		presentationFolderMap.put(newFolder, newFolderNode); // Add in the caching folder-node map
		insertTreeNode(rootPresentationNode.getChildren(), newFolderNode); // Insert node in our tree model
		presentationFolderRightsMap.put(newFolder, AccessRight.getFull()); // Add in the caching folder-rights map

		refreshShownAccessRights();
		newRootFolderName = null;
		updateSingleSearchString(newFolder);
	}

	public boolean isUserIsAdmin() {
		return getCurrentUser().isHasAdminRights();
	}
	
	public boolean isUserAllowedToManageFolder() {
		if(selectedPresentationNode == null) return false;
		
		Object data = selectedPresentationNode.getData();
		return data instanceof PresentationFolder && presentationFolderRightsMap.get(data).isManage();
	}

	/**
	 * Listener for remote command. Creates a new course offer or renames an
	 * existing one based on the text field input.
	 */
	public void createNewCourseOffer() {
		selectedPresentationNode = inputNode.getParent();
		selectedPresentationNode.setSelected(true);

		final PresentationFolder parent = (PresentationFolder) (inputNode.getParent().getData());

		Optional<String> failureMessage = validateName(parent, newCourseOfferName, EFolderChildType.COURSEOFFER);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			clearInputTreeNode();
			return;
		}

		final CourseOffer courseOffer = courseBusiness.createCourseOffer(newCourseOfferName, null, parent,
				getCurrentUser());
		final TreeNode newCourseOfferNode = new DefaultTreeNode(courseOffer);
		newCourseOfferNode.setType(ETreeNodeType.EDIT_RIGHTS_OFFER_TYPE.getName());

		presentationFolderMap.get(parent).getChildren().add(newCourseOfferNode);
		if (presentationFolderMap.get(parent).getType().equals(ETreeNodeType.EMPTY_FOLDER_TYPE.getName())) {
			presentationFolderMap.get(parent).setType(ETreeNodeType.PLAIN_FOLDER_TYPE.getName());
		}
		courseMap.put(courseOffer, newCourseOfferNode);
		insertTreeNode(presentationFolderMap.get(parent).getChildren(), newCourseOfferNode);

		clearInputTreeNode();
		updateSingleSearchString(courseOffer);
	}

	/**
	 * Duplicates a course offer.
	 */
	public void duplicateCourseoffer() {
		CourseOffer courseOffer = (CourseOffer) selectedPresentationNode.getData();
		final PresentationFolder parent = courseOffer.getFolder();

		Optional<String> failureMessage = validateName(parent, duplicateCourseOfferName, EFolderChildType.COURSEOFFER);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			return;
		}

		try {
			final CourseOffer newCourseOffer = courseBusiness.duplicateCourseOffer(courseOffer,
					duplicateCourseOfferName, getCurrentUser(), courseOffer.getFolder());
			final TreeNode newCourseOfferNode = new DefaultTreeNode(newCourseOffer);
			newCourseOfferNode.setType(ETreeNodeType.EDIT_RIGHTS_OFFER_TYPE.getName());

			presentationFolderMap.get(parent).getChildren().add(newCourseOfferNode);
			courseMap.put(newCourseOffer, newCourseOfferNode);
			insertTreeNode(presentationFolderMap.get(parent).getChildren(), newCourseOfferNode);
			updateSingleSearchString(newCourseOffer);
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
	}

	public void prepareDuplicateCourseOffer() {
		duplicateCourseOfferName = "";
		duplicateCourseOfferWarning = null;

		if (selectedPresentationNode != null) {

			final CourseOffer offer = (CourseOffer) selectedPresentationNode.getData();

			// Show a warning if the user has no rights on the offer's course
			if (offer.getCourse() != null) {
				AbstractCourse course = offer.getCourse();
				if (course.isFrozen()) {
					long id = course.getRealCourseId();
					course = courseBusiness.getCourseByCourseID(id);
				}
				ContentFolder folder = folderBusiness.getFolderForAbstractCourse(course);
				if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), folder)) {
					duplicateCourseOfferWarning = formatLocalizedMessage(
							"start.presentation.duplicateCourseOffer.noRightsForCourse",
							new Object[] { course.getName() });
				}
			}
		}
	}

	public void deletePresentationFolder() {
		if (selectedPresentationNode == null) {
			return;
		}

		final PresentationFolder folder = (PresentationFolder) selectedPresentationNode.getData();
		final boolean isTopLevelFolder = folder.getParentFolder().isRoot();

		if (isTopLevelFolder) {
			// Try to delete a top-level folder, only for Administrators
			try {
				folderBusiness.deleteTopLevelPresentationFolder(getCurrentUser(), folder);
			} catch (AuthorizationException e) {
				// No WRITE rights
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
				return;
			} catch (ActionNotAllowedException e) {
				// User is not an Admin
				throw new JackSecurityException(
						"deletePresentationFolder was called with a top-level folder, but without admin rights.", e);
			}
		} else {
			// Try to delete a "normal" folder
			try {
				folderBusiness.deleteFolder(getCurrentUser(), folder);
			} catch (ActionNotAllowedException e) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
				return;
			}
		}

		// When we are here, deleting was successful. Now we need to remove the folder from the view and cache.
		selectedPresentationNode.getParent().getChildren().remove(selectedPresentationNode);
		if (!isTopLevelFolder) {
			// The parent folder may be empty now
			selectedPresentationNode = presentationFolderMap.get(folder.getParentFolder());
			selectedPresentationNode.setSelected(true);

			// Here, the new type of the parent node must be computed dynamically,
			// because the user may don't have rights e.g. to delete or rename the parent element.
			updateType(selectedPresentationNode);
		}
		presentationFolderRightsMap.remove(folder);
		shownAccessRights.remove(folder);
		presentationFolderMap.remove(folder);
	}

	public void deleteCourseOffer() {

		if (selectedPresentationNode == null) {
			throw new IllegalStateException("No Node selected.");
		}
		final CourseOffer courseOffer = (CourseOffer) selectedPresentationNode.getData();
		if (!courseBusiness.isStringEqualsCourseOfferName(deletionDialogView.getInputTextForDeletion(), courseOffer)) {
			throw new IllegalStateException("Written Name for deletion does not match the Name of the CourseOffer.");
		}
		String tmpCourseOfferName = "";
		tmpCourseOfferName = courseOffer.toString();

		try {
			courseBusiness.deleteCourseOffer(getCurrentUser(), courseOffer);
			removeCourseOfferNodeFromTree(courseOffer);
			getLogger()
					.info(getCurrentUser().getLoginName() + " successfully deleted CourseOffer " + tmpCourseOfferName + ".");
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
			getLogger().warn(
					getCurrentUser().getLoginName() + " failed to delete CourseOffer" + tmpCourseOfferName + ": Insufficient rights.");
		} finally {
			deletionDialogView.closeDeletionDialog();
			PrimeFaces.current().ajax().update(":treeForm:courseOfferTree");
		}
	}

	private void removeCourseOfferNodeFromTree(final CourseOffer courseOffer) {
		TreeNode presentationNode = courseMap.get(courseOffer);
		presentationNode.getParent().getChildren().remove(presentationNode);
		presentationNode = presentationFolderMap.get(courseOffer.getFolder());
		presentationNode.setSelected(true);

		// Here, the new type of the parent node must be computed dynamically,
		// because the user may don't have rights e.g. to delete or rename the parent element.
		updateType(selectedPresentationNode);
	}
	
	public boolean isAllowedToDeleteCourseOffer() {
		return authorizationBusiness.isAllowedToDeleteCourseOffer(getCurrentUser(), (CourseOffer) selectedPresentationNode.getData());
	}
	
	public void prepareCourseOfferDeletion(ActionEvent ignored) {
		if (selectedPresentationNode != null) {
			final CourseOffer courseOffer = (CourseOffer) selectedPresentationNode.getData();
			deletionDialogView.prepareDeletionDialog(courseOffer.getName());;
		}
	}

	private void expandTree(TreeNode treeNode) {
		for (; treeNode != null; treeNode = treeNode.getParent()) {
			treeNode.setExpanded(true);
			treeNode.setSelected(true);
		}
	}

	private void filterPresentationTree() {
		shrinkTree(rootPresentationNode);
		if (searchCourseOffers != null && !searchCourseOffers.isEmpty()) {
			filterTree(rootPresentationNode);
		} else {
			refreshPresentationTree(false);
		}
	}

	private void filterTree(final TreeNode treeNode) {
		boolean match;
		for (final TreeNode tn : treeNode.getChildren()) {
			if (NumberUtils.isParsable(searchCourseOffers)) {
				match = getIdForTreeNode(tn) == Long.parseLong(searchCourseOffers);
			} else {
				match = getNameForTreeNode(tn).toLowerCase().contains(searchCourseOffers.toLowerCase());
			}
			if (match) {
				expandTree(tn);
			}
			tn.setSelected(match);
			filterTree(tn);
		}
	}

	/**
	 * Copy in {@link de.uni_due.s3.jack3.beans.data.ContentTree}
	 * 
	 * @param node
	 * @return
	 */
	protected String getNameForTreeNode(TreeNode node) {
		final Object treeData = node.getData();
		if (treeData instanceof Namable) {
			return ((Namable) treeData).getName();
		}
		return "";
	}

	/**
	 * Copy in {@link de.uni_due.s3.jack3.beans.data.ContentTree}
	 * 
	 * @param node
	 * @return
	 */
	protected long getIdForTreeNode(TreeNode node) {
		final Object treeData = node.getData();
		if (treeData instanceof AbstractEntity) {
			return ((AbstractEntity) treeData).getId();
		}
		return -1;
	}
	
	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public void loadLocation() {
		if (locationId == null) {
			locationId = userSession.getLatestPresentationLocationId();
		}
		if (locationId == null) {
			// Neither in the parameter nor in the session is a last location stored
			return;
		}
		var foundLocation = folderBusiness.getPresentationFolderById(locationId);
		if (foundLocation.isPresent()) {
			// Location ID points to a valid Presentation Folder
			userSession.setLatestPresentationLocationId(locationId);
			locationFolder = foundLocation.get();
		}
	}

	public String getNewPresentationFolderName() {
		return newPresentationFolderName;
	}

	public TreeNode getRootPresentationNode() {
		if (rootPresentationNode == null) {
			refreshPresentationTree(false);
			updateAllSearchStrings();
		}
		return rootPresentationNode;
	}

	public String getSearchCourseOffers() {
		return searchCourseOffers;
	}

	public long getSelectedNodeId() {
		if (selectedPresentationNode != null) {
			if (selectedPresentationNode.getData() instanceof Folder) {
				return ((Folder) selectedPresentationNode.getData()).getId();
			} else if (selectedPresentationNode.getData() instanceof CourseOffer) {
				return ((CourseOffer) selectedPresentationNode.getData()).getId();
			}
		}
		return 0;
	}

	public TreeNode getSelectedPresentationNode() {
		return selectedPresentationNode;
	}

	public String getSelectedNodeName() {
		if (selectedPresentationNode == null) {
			return null;
		}
		var data = selectedPresentationNode.getData();
		if (data instanceof Namable) {
			return ((Namable) data).getName();
		} else {
			return null;
		}
	}

	private void insertTreeNode(List<TreeNode> children, TreeNode newChild) {
		final PresentationTreeOrder order = new PresentationTreeOrder();
		for (int i = 0; i < children.size(); i++) {
			if (order.compare(children.get(i), newChild) > 0) {
				children.add(i, newChild);
				return;
			}
		}

		children.add(newChild);
	}

	/**
	 * Event handler for ajax event from the UI in case of drag and drop of elements
	 * in the folder tree.
	 *
	 * @param event
	 *            The {@link TreeDragDropEvent} issued by the UI.
	 */
	public void onDragDrop(TreeDragDropEvent event) {
		// Get dragged element and drop target from the event.
		final TreeNode dragNode = event.getDragNode();
		final TreeNode dropNode = event.getDropNode();

		try {
			// The action is redirected to the handle... methods
			if (dragNode.getData() instanceof CourseOffer) {
				handleDragDropOnCourseOffer(dragNode, dropNode);
			} else if (dragNode.getData() instanceof PresentationFolder) {
				handleDragDropOnPresentationFolder(dragNode, dropNode);
			}
		} catch (ActionNotAllowedException e) {
			// If action is not allowed, we first reset the UI.
			// This is equivalent to "cancelPendingMoveOperation()" but without the pendingDragDropAction object.
			if (dragNode.getData() instanceof PresentationFolder) {
				final PresentationFolder dragFolder = (PresentationFolder) dragNode.getData();
				dropNode.getChildren().remove(dragNode);
				insertTreeNode(presentationFolderMap.get(dragFolder.getParentFolder()).getChildren(), dragNode);
			} else if (dragNode.getData() instanceof CourseOffer) {
				final CourseOffer dragCourse = (CourseOffer) dragNode.getData();
				presentationFolderMap.get(dragCourse.getFolder()).getChildren().add(dragNode);
				dropNode.getChildren().remove(dragNode);
				insertTreeNode(presentationFolderMap.get(dragCourse.getFolder()).getChildren(), dragNode);
			}
			// Then we issue an error message to the user.
			var msgDetail = getMovementErrorMessageText(e);
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.dragNotAllowed", msgDetail);
		}
	}

	private void handleDragDropOnPresentationFolder(final TreeNode dragNode, final TreeNode dropNode)
			throws ActionNotAllowedException {
		final PresentationFolder draggedObject = (PresentationFolder) dragNode.getData();
		final PresentationFolder dragFolder = (PresentationFolder) draggedObject.getParentFolder();
		final PresentationFolder dropFolder = (PresentationFolder) dropNode.getData();

		// NOTE: This implements a fail-fast strategy. If the move operation is not allowed, this method throws an
		//       Eexception, which is then handled immediately by the caller. The warning message will be skipped.
		//       Without this line, the warning would be displayed even if the operation is not allowed.
		authorizationBusiness.ensureIsAllowedToMoveFolder(getCurrentUser(), draggedObject, dropFolder);

		var warning = getMoveWarning(dragFolder, dropFolder);
		if (warning.isEmpty()) {
			// The Presentation Folder can be moved immediately
			moveFolderAction = new PendingDragDropAction<>(null, dropFolder, draggedObject);
			executePendingMoveOperation();
		} else {
			// Show the warning to the user and ask for confirmation
			moveFolderAction = new PendingDragDropAction<>(warning.get(), dropFolder, draggedObject);
			PrimeFaces.current().executeScript("PF('movePresentationFolder').show()");
		}
	}

	private void handleDragDropOnCourseOffer(final TreeNode dragNode, final TreeNode dropNode)
			throws ActionNotAllowedException {
		final CourseOffer draggedObject = (CourseOffer) dragNode.getData();
		final PresentationFolder dragFolder = draggedObject.getFolder();
		final PresentationFolder dropFolder = (PresentationFolder) dropNode.getData();

		// NOTE: This implements a fail-fast strategy. If the move operation is not allowed, this method throws an
		//       Eexception, which is then handled immediately by the caller. The warning message will be skipped.
		//       Without this line, the warning would be displayed even if the operation is not allowed.
		authorizationBusiness.ensureIsAllowedToMoveElement(getCurrentUser(), draggedObject.getFolder(), dropFolder);

		var warning = getMoveWarning(dragFolder, dropFolder);
		if (warning.isEmpty()) {
			// The Course Offer can be moved immediately
			moveCourseOfferAction = new PendingDragDropAction<>(null, dropFolder, draggedObject);
			executePendingMoveOperation();
		} else {
			// Show the warning to the user and ask for confirmation
			moveCourseOfferAction = new PendingDragDropAction<>(warning.get(), dropFolder, draggedObject);
			PrimeFaces.current().executeScript("PF('moveCourseOffer').show()");
		}
	}

	/**
	 * Handler for the action that will be executed when the user cancels a move action with "No, don't move element" or
	 * when an Exception was thrown during the move action.
	 */
	public void cancelPendingMoveOperation() {
		if (moveCourseOfferAction != null) {
			final CourseOffer moved = moveCourseOfferAction.getObjectToMove();
			final PresentationFolder source = moved.getFolder();
			final PresentationFolder target = moveCourseOfferAction.getTargetFolder();

			// Undo the move operation: Move the course offer back from target to source
			presentationFolderMap.get(target).getChildren().remove(courseMap.get(moved));
			insertTreeNode(presentationFolderMap.get(source).getChildren(), courseMap.get(moved));
			moveCourseOfferAction = null;

		} else if (moveFolderAction != null) {
			final PresentationFolder moved = moveFolderAction.getObjectToMove();
			final PresentationFolder source = (PresentationFolder) moved.getParentFolder();
			final PresentationFolder target = moveFolderAction.getTargetFolder();

			// Undo the move operation: Move the presentation folder back from target to source
			presentationFolderMap.get(target).getChildren().remove(presentationFolderMap.get(moved));
			insertTreeNode(presentationFolderMap.get(source).getChildren(), presentationFolderMap.get(moved));
			moveFolderAction = null;
		}
	}

	/**
	 * Handler for the action that will be executed when the user confirms a move action with "Yes, move element".
	 */
	public void onPendingMoveOperationConfirm() {
		try {
			executePendingMoveOperation();
		} catch (ActionNotAllowedException e) {
			cancelPendingMoveOperation();
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.dragNotAllowed", getMovementErrorMessageText(e));
		}
	}

	/**
	 * Handler for the action that will be executed when the user confirms a move action with "Yes, but make a copy".
	 * This could be the case if the user has no MANAGE rights on the target folder.
	 */
	public void duplicateCourseOfferAfterDragAndDrop() {
		// First we have to undo the action because the original course offer should be left in its current place.
		// A temporary action is saved because "cancelPendingMoveOperation()" clears the move action
		final var action = moveCourseOfferAction;
		cancelPendingMoveOperation();

		try {
			final CourseOffer toDrag = action.getObjectToMove();
			final PresentationFolder target = action.getTargetFolder();

			final CourseOffer newCourseOffer = courseBusiness.duplicateCourseOffer(toDrag, toDrag.getName(),
					getCurrentUser(), target);

			// Then we create a new duplicated course offer and place it in the target folder
			final TreeNode newCourseOfferNode = new DefaultTreeNode(newCourseOffer);
			newCourseOfferNode.setType(ETreeNodeType.EDIT_RIGHTS_OFFER_TYPE.getName());
			presentationFolderMap.get(target).getChildren().add(newCourseOfferNode);
			courseMap.put(newCourseOffer, newCourseOfferNode);
			insertTreeNode(presentationFolderMap.get(target).getChildren(), newCourseOfferNode);
			processRightsOfPresentationTree();
			updateSingleSearchString(newCourseOffer);
		} catch (final ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed", null);
		}
	}

	/**
	 * Execution of the actual move operation.
	 */
	private void executePendingMoveOperation() throws ActionNotAllowedException {
		if (moveCourseOfferAction != null) {
			final CourseOffer toMove = moveCourseOfferAction.getObjectToMove();
			final PresentationFolder source = toMove.getFolder();
			final PresentationFolder target = moveCourseOfferAction.getTargetFolder();
			
			// First we update the actual business data ...
			// The caller above will handle thrown Exceptions
			courseBusiness.moveCourseOffer(toMove, target, getCurrentUser());
			
			// ... then we update the UI data
			presentationFolderMap.get(source).getChildren().remove(courseMap.get(toMove));
			insertTreeNode(presentationFolderMap.get(target).getChildren(), courseMap.get(toMove));
			processRightsOfPresentationTree();
			moveCourseOfferAction = null;

		} else if (moveFolderAction != null) {
			final PresentationFolder toMove = moveFolderAction.getObjectToMove();
			final PresentationFolder source = (PresentationFolder) toMove.getParentFolder();
			final PresentationFolder target = moveFolderAction.getTargetFolder();

			// First we update the actual business data ...
			// The caller above will handle thrown Exceptions
			folderBusiness.movePresentationFolder(getCurrentUser(), toMove, target);
			folderBusiness.resetFolderRights(toMove);

			// ... then we update the UI data
			presentationFolderMap.get(source).getChildren().remove(presentationFolderMap.get(toMove));
			insertTreeNode(presentationFolderMap.get(target).getChildren(), presentationFolderMap.get(toMove));
			processRightsOfPresentationTree();

			// We have to update the folder rights of all subfolders because folder hierarchy has changed
			presentationFolderRightsMap.replaceAll((folder, right) -> {
				if (folder.isChildOf(toMove)) {
					return authorizationBusiness.getMaximumRightForUser(getCurrentUser(), folder);
				}
				return right;
			});
			refreshShownAccessRights();
			// Previously, at this point the full tree was rebuilt. The statements above only update a part of the tree.
			// If this way leads to errors, we can go back to the complete update.
//			refreshPresentationTree(filterMyCourseOffers);
			moveFolderAction = null;
		}
	}

	private Optional<String> getMoveWarning(final PresentationFolder source, final PresentationFolder target) {
		// NOTE: We don't use "authorizationBusiness.canManage(source)" because "source" is already the parent folder!
		boolean wouldLoseManageRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), source)
				.isManage() && !authorizationBusiness.getMaximumRightForUser(getCurrentUser(), target).isManage();
		if (wouldLoseManageRights) {
			return Optional.of(getLocalizedMessage("start.confirmMoveOperation.loseManageRight"));
		}
		return Optional.empty();
	}

	private String getMovementErrorMessageText(final ActionNotAllowedException e) {
		// TODO Wenn wir auf Java 17 umgestiegen sind, sollte dieser Teil mit Pattern Matching für switch und instanceof umgeschrieben werden.
		if ((e instanceof FolderException)) {
			final FolderException fExc = FolderException.class.cast(e);
			switch (fExc.getType()) {
			case RECURSION:
				return "start.dragNotAllowed.recursion";
			case PERSONAL_FOLDER:
				return "start.dragNotAllowed.personalFolder";
			case ROOT:
				return "start.dragNotAllowed.virtualDropNode";
			default:
				getLogger().error("Unhandled FolderException", fExc);
				return null;
			}
		} else if ((e instanceof AuthorizationException)) {
			final AuthorizationException aExc = AuthorizationException.class.cast(e);
			switch (aExc.getType()) {
			case RIGHTS_WILL_CHANGE:
				return "start.dragNotAllowed.rightsWillChange";
			case DRAG_TARGET_RIGHT_IS_NOT_WRITE:
				return "start.dragNotAllowed.noSourceRights";
			case DROP_TARGET_RIGHT_IS_NOT_WRITE:
				return "start.dragNotAllowed.noTargetRights";
			default:
				getLogger().error("Unhandled AuthorizationException", aExc);
				return null;
			}
		} else if ((e instanceof DragDropException)) {
			final DragDropException dExc = DragDropException.class.cast(e);
			switch (dExc.getType()) {
			case TARGET_IS_NOT_FOLDER:
				return "start.dragNotAllowed.noFolder";
			default:
				getLogger().error("Unhandled DragDropException", dExc);
				return null;
			}
		} else {
			getLogger().error("Unhandled ActionNotAllowedException.", e);
			return null;
		}
	}

	/**
	 * @return Failure message for validation of a new / renamed object
	 */
	private Optional<String> validateName(PresentationFolder parent, String name, EFolderChildType childType) {
		if (JackStringUtils.isBlank(name)) {
			return Optional.of("global.invalidName.empty");
		}

		return Optional.empty();
	}

	public void validateCourseOfferRename(FacesContext context, UIComponent component, Object value)
			throws ValidatorException {

		final String newName = (String) value;
		final String oldName = (String) component.getAttributes().get("oldValue");
		if (oldName.equals(newName)) {
			return;
		}

		CourseOffer courseOffer = (CourseOffer) component.getAttributes().get("courseOffer");
		PresentationFolder parent = courseOffer.getFolder();
		Optional<String> validationMessage = validateName(parent, newName, EFolderChildType.COURSEOFFER);
		if (validationMessage.isPresent()) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidName"),
							getLocalizedMessage(validationMessage.get())));
		}
	}

	public void onNodeCollapse(NodeCollapseEvent event) {
		TreeNode currentNode = event.getTreeNode();
		currentNode.setExpanded(false);
		userSession.removeExpandedFolder(currentNode);
	}

	public void onNodeExpand(NodeExpandEvent event) {
		TreeNode currentNode = event.getTreeNode();
		currentNode.setExpanded(true);
		userSession.addExpandedFolder(currentNode);
	}

	private void showExpandedState() {
		List<Folder> expandedFolder = userSession.getExpandedFolders();
		for (Folder folder : expandedFolder) {
			if (presentationFolderMap.containsKey(folder)) {
				TreeNode treeNode = presentationFolderMap.get(folder);
				treeNode.setExpanded(true);
				while (treeNode.getParent() != null) {
					treeNode = treeNode.getParent();
					treeNode.setExpanded(true);
				}
			}
		}
	}

	public void onNodeSelect(NodeSelectEvent event) {
		selectedPresentationNode = event.getTreeNode();
		selectedPresentationNode.setExpanded(!selectedPresentationNode.isExpanded());
		updateLocationId();
	}

	public void onTreeContextMenu(NodeSelectEvent event) {
		if (selectedPresentationNode != null) {
			selectedPresentationNode.setSelected(false);
		}
		selectedPresentationNode = event.getTreeNode();
		selectedPresentationNode.setSelected(true);
	}

	public void updateLocationId() {
		if (selectedPresentationNode.getData() instanceof PresentationFolder) {
			final PresentationFolder folder = (PresentationFolder) selectedPresentationNode.getData();
			userSession.setLatestPresentationLocationId(folder.getId());
		} else if (selectedPresentationNode.getData() instanceof CourseOffer) {
			final CourseOffer offer = (CourseOffer) selectedPresentationNode.getData();
			userSession.setLatestPresentationLocationId(offer.getFolder().getId());
		}
	}

	/**
	 * <p>
	 * Generates the tree for availableCourses.xhtml. The idea is basically
	 * to create the tree by starting from its leaves (the course offers) and to
	 * recursively walk up to the root node from there. The tree's root node is
	 * the unique folder that has no parent folder which is being created during
	 * the FirstTimeSetup. This is why this particular folder will represent the
	 * root node of the tree, which is returned by the method
	 * getRootPresentationNode.
	 * </p>
	 *
	 * <p>
	 * Tree nodes always get a certain type depending on the context menus that
	 * shall be rendered in the view.
	 * </p>
	 *
	 * <ul>
	 * <li><b>folder</b> is for folders that the user has rights on and that
	 * contains children so he can't delete it (menu item is disabled)</li>
	 * <li><b>emptyFolder</b> is for folders that the user has rights on but do
	 * not contain children</li>
	 * <li><b>noActionFolder</b> is for folders that the user is not allowed to
	 * make any actions on</li>
	 * <li><b>noDeleteFolder</b> is for folders that the user is not allowed to
	 * delete but that he perform any other actions on</li>
	 * <li><b>offer</b> is for course offers the user is allowed to edit</li>
	 * <li><b>noRightsOffer</b> is for course offers the user is not allowed to
	 * edit (but allowed to participate in them instead)</li>
	 * </ul>
	 * 
	 * @param onlyMyOffers
	 *            If set to true, nodes of type <b>noRightsOffer</b> are no
	 *            included in the tree
	 */
	private void refreshPresentationTree(boolean onlyMyOffers) {
		presentationFolderMap = new HashMap<>();
		courseMap = new HashMap<>();

		final List<CourseOffer> courseOffers = courseBusiness.getAllCourseOffers();
		// The list of presentation folders, the user has rights on
		List<PresentationFolder> folderList = folderBusiness.getAllPresentationFoldersForUser(getCurrentUser());

		// We add the folders where the user is in a user group that grants him
		// rights on the folder
		final List<UserGroup> groups = userBusiness.getUserGroupsForUser(getCurrentUser());
		for (final UserGroup userGroup : groups) {
			folderList.addAll(folderBusiness.getAllPresentationFoldersForUserGroup(userGroup));
		}

		// Generate folder rights map for all folders
		presentationFolderRightsMap = new HashMap<>();
		for (PresentationFolder folder : folderList) {
			final AccessRight right = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), folder);
			if (!right.isNone()) {
				presentationFolderRightsMap.put(folder, right);
			}
		}
		refreshShownAccessRights();

		// We iterate over the course offers first and create a tree structure
		// by walking through the tree from the leaves to the root node
		processCourseOffersForPresentationTree(onlyMyOffers, courseOffers);

		// Next we iterate over all the folders that the user has rights on
		processPresentationFoldersForPresentationTree(folderList);

		// Now we can set the correct rights for each element in the tree
		processRightsOfPresentationTree();

		// Sort the children of all nodes
		for (final TreeNode node : presentationFolderMap.values()) {
			node.getChildren().sort(new PresentationTreeOrder());
		}

		// If we know in which folder the user was at his/her latest interaction, we expand the tree up to this point
		if (locationFolder != null && presentationFolderMap.get(locationFolder) != null) {
			// Expand the node
			TreeNode tempNode = presentationFolderMap.get(locationFolder);
			tempNode.setExpanded(true);
			// Expand the parents
			while (tempNode.getParent() != null) {
				tempNode = tempNode.getParent();
				tempNode.setExpanded(true);
			}
		}

		// Finally, we set the pointer to the root node
		if (presentationFolderMap.isEmpty()) {
			// Special case: No folders yet!
			final PresentationFolder rootFolder = folderBusiness.getPresentationRoot();
			// We create one
			rootPresentationNode = new DefaultTreeNode(rootFolder);
			rootPresentationNode.setType(ETreeNodeType.PLAIN_FOLDER_TYPE.getName());
			presentationFolderMap.put(rootFolder, rootPresentationNode);
		} else {
			// Get the node for the presentation root folder
			rootPresentationNode = presentationFolderMap.get(folderBusiness.getPresentationRoot());
		}

		showExpandedState();
	}





	private void processRightsOfPresentationTree() {
		for (Entry<Folder, TreeNode> treeElement : presentationFolderMap.entrySet()) {
			Folder folder = treeElement.getKey();
			TreeNode treeNode = treeElement.getValue();

			treeNode.setType(computeFolderType((PresentationFolder) folder, treeNode).getName());

			for (TreeNode child : treeNode.getChildren()) {
				if (child.getData() instanceof CourseOffer) {
					child.setType(computeCourseOfferType((CourseOffer) child.getData()).getName());
				}
			}
		}
	}

	private void updateType(final TreeNode node) {
		if (node.getData() instanceof PresentationFolder) {
			final PresentationFolder presentationFolder = (PresentationFolder) node.getData();
			node.setType(computeFolderType(presentationFolder, node).getName());
		} else if (node.getData() instanceof CourseOffer) {
			final CourseOffer offer = (CourseOffer) node.getData();
			node.setType(computeCourseOfferType(offer).getName());
		}
	}

	private ETreeNodeType computeFolderType(final PresentationFolder folder, final TreeNode node) {
		if (folder.isRoot()) {
			// Noone has rights on the root
			return ETreeNodeType.NO_ACTION_FOLDER_TYPE;
		}

		boolean normalEditRights = !folder.getParentFolder().isRoot()
				&& authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder.getParentFolder());
		boolean adminEditRights = folder.getParentFolder().isRoot()
				&& authorizationBusiness.hasAdminRights(getCurrentUser())
				&& authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder);
		if (normalEditRights || adminEditRights) {
			// User is admin and the folder is a top-level folder OR ...
			// user has edit rights on the parent folder, so he gets a folder with full rights ...
			if (node.getChildCount() > 0) {
				// ... which is not empty (and thus cannot be deleted)
				return ETreeNodeType.PLAIN_FOLDER_TYPE;
			} else {
				// ... or empty (and thus may also be deleted)
				return ETreeNodeType.EMPTY_FOLDER_TYPE;
			}
		}

		if (authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder)) {
			// User has edit rights on this folder, so he may add elements
			return ETreeNodeType.ONLY_ADD_FOLDER_TYPE;
		}

		if (authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), folder)) {
			// User has read rights on this folder, so he can see the participants view
			return ETreeNodeType.READ_RIGHTS_FOLDER;
		}

		// User has no relevant edit rights, hence he also has no rights on this folder
		return ETreeNodeType.NO_ACTION_FOLDER_TYPE;
	}

	private ETreeNodeType computeCourseOfferType(final CourseOffer offer) {
		// This cannot be computed from the cached rights, because it must also be checked whether the user is allowed to see the Course Offer at all
		final var visibility = authorizationBusiness.getCourseOfferVisibilityForUser(getCurrentUser(), offer);
		switch (visibility) {
		case EDIT:
			return ETreeNodeType.EDIT_RIGHTS_OFFER_TYPE;
		case READ:
			return ETreeNodeType.READ_RIGHTS_OFFER_TYPE;
		case SEE_AS_STUDENT:
			return ETreeNodeType.STUDENT_OFFER_TYPE;
		default:
			return ETreeNodeType.NO_RIGHTS_OFFER_TYPE;
		}
	}

	private void processPresentationFoldersForPresentationTree(final List<PresentationFolder> folderList) {
		for (final PresentationFolder folder : folderList) {
			// We create a new node for the tree, only when it does not exist yet, which means that it is not contained
			// in the map
			if (!presentationFolderMap.containsKey(folder)) {
				TreeNode tempFolderNode = new DefaultTreeNode(folder);
				Folder parentFolder = folder.getParentFolder();
				presentationFolderMap.put(folder, tempFolderNode);

				// Next, we iterate over the parent folders again, until we reach the root node
				iterateParentFoldersForPresentationTree(tempFolderNode, parentFolder);
			}
		}
	}

	private void iterateParentFoldersForPresentationTree(TreeNode tempFolderNode, Folder parentFolder) {
		while (parentFolder != null) {
			if (presentationFolderMap.containsKey(parentFolder)) {
				// If we have found the parent folder, we insert the child
				presentationFolderMap.get(parentFolder).getChildren().add(0, tempFolderNode);
				return;
			}

			// No node exists for the parent, so we need to create it
			final TreeNode tempFolderNode2 = new DefaultTreeNode(parentFolder);
			tempFolderNode2.getChildren().add(0, tempFolderNode);
			presentationFolderMap.put(parentFolder, tempFolderNode2);
			tempFolderNode = tempFolderNode2;

			// Continue searching parents
			parentFolder = parentFolder.getParentFolder();
		}
	}

	private void processCourseOffersForPresentationTree(boolean onlyMyOffers, final List<CourseOffer> courseOffers) {
		for (final CourseOffer courseOffer : courseOffers) {

			final ECourseOfferAccess access = authorizationBusiness.getCourseOfferVisibilityForUser(getCurrentUser(),
					courseOffer);

			// We only add the course offer to the tree if the user has edit rights or the course offer may be shown to
			// the user and the user has not chosen to filter only his editable course offers.
			if (access == ECourseOfferAccess.EDIT || (access != ECourseOfferAccess.NONE && !onlyMyOffers)) {
				final TreeNode tempOfferNode = new DefaultTreeNode(courseOffer);
				courseMap.put(courseOffer, tempOfferNode);
				insertCourseOfferIntoTree(courseOffer.getFolder(), tempOfferNode);
			}
		}
	}

	/**
	 * Adds a course offer node to the folder tree. Creates nodes for parent folders if necessary.
	 */
	private void insertCourseOfferIntoTree(Folder tempFolder, final TreeNode tempOfferNode) {
		if (presentationFolderMap.containsKey(tempFolder)) {
			// When the map contains the course offer's parent, we just add the courseOffer to its children.
			presentationFolderMap.get(tempFolder).getChildren().add(tempOfferNode);
		} else {
			// In any other case, we create a new folder and add it to the map
			TreeNode tempFolderNode = new DefaultTreeNode(tempFolder);
			tempFolderNode.getChildren().add(tempOfferNode);
			presentationFolderMap.put(tempFolder, tempFolderNode);
			// We iterate over the parents until they are null, which is when we have reached the root node.
			// Again we follow the same routine as described above, checking whether we have already added the
			// folder to the map.
			while (tempFolder.getParentFolder() != null) {
				tempFolder = tempFolder.getParentFolder();
				if (presentationFolderMap.containsKey(tempFolder)) {
					presentationFolderMap.get(tempFolder).getChildren().add(0, tempFolderNode);
					break;
				} else {
					final TreeNode tempFolderNode2 = new DefaultTreeNode(tempFolder);
					tempFolderNode2.getChildren().add(0, tempFolderNode);
					presentationFolderMap.put(tempFolder, tempFolderNode2);
					tempFolderNode = tempFolderNode2;
				}
			}
		}
	}

	public void setNewPresentationFolderName(String newPresentationFolderName) {
		this.newPresentationFolderName = newPresentationFolderName;
	}

	public void setSearchCourseOffers(String searchCourseOffers) {
		if (!searchCourseOffers.equals(this.searchCourseOffers)) {
			this.searchCourseOffers = searchCourseOffers;
			filterPresentationTree();
		}
	}

	public void setSelectedPresentationNode(TreeNode selectedPresentationNode) {
		this.selectedPresentationNode = selectedPresentationNode;
	}

	private void shrinkTree(TreeNode node) {
		if (node == null) {
			return;
		}

		node.setExpanded(false);
		for (final TreeNode tn : node.getChildren()) {
			shrinkTree(tn);
		}
	}

	public void updateFolder(Folder folder) {
		folderBusiness.updateFolder(folder);
	}

	public void updateCourseOffer(CourseOffer courseOffer) {
		courseBusiness.updateCourseOffer(courseOffer);
	}

	public String getNewCourseOfferName() {
		return newCourseOfferName;
	}

	public void setNewCourseOfferName(String newCourseOfferName) {
		this.newCourseOfferName = newCourseOfferName;
	}

	public String getDuplicateCourseOfferName() {
		return duplicateCourseOfferName;
	}

	public void setDuplicateCourseOfferName(String duplicateCourseOfferName) {
		this.duplicateCourseOfferName = duplicateCourseOfferName;
	}

	public String getDuplicateCourseOfferWarning() {
		return duplicateCourseOfferWarning;
	}

	public boolean isFilterMyCourseOffers() {
		return filterMyCourseOffers;
	}

	public void setFilterMyCourseOffers(boolean filterMyCourseOffers) {
		this.filterMyCourseOffers = filterMyCourseOffers;
	}

	public String getNewRootFolderName() {
		return newRootFolderName;
	}

	public void setNewRootFolderName(String newRootFolderName) {
		this.newRootFolderName = newRootFolderName;
	}

	public String getMoveOperationConfirmText() {
		if (moveCourseOfferAction != null) {
			return moveCourseOfferAction.getConfirmText();
		} else if (moveFolderAction != null) {
			return moveFolderAction.getConfirmText();
		}
		return null;
	}

	public void showMyCourseOffers() {
		filterMyCourseOffers = true;
		refreshPresentationTree(true);
	}

	public void showAllCourseOffers() {
		filterMyCourseOffers = false;
		refreshPresentationTree(false);
	}

	public String getFreePlacesMessageForCourseOffer(CourseOffer courseOffer) {
		// Lazy-loading
		freePlacesCache.putIfAbsent(courseOffer, enrollmentBusiness.getFreePlaces(courseOffer));
		Optional<Long> freePlaces = freePlacesCache.get(courseOffer);
		if (freePlaces.isPresent()) {
			return formatLocalizedMessage("startPresentationTabView.freePlaces", new Object[] { freePlaces.get() });
		} else {
			// No limit
			return "";
		}
	}

	public void linkCourses() {
		final PresentationFolder selectedFolder = (PresentationFolder) selectedPresentationNode.getData();
		try {
			boolean linked = folderBusiness.switchLinkedCourses(selectedFolder);
			if (linked) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, null, "startPresentationTabView.infoCoursesLinked");
			} else {
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, null, "startPresentationTabView.infoCoursesUnlinked");
			}
			// The folder itself and all sub folders must be reloaded
			parallelHintMap.remove(selectedFolder);
			parallelHintMap.keySet().removeIf(f -> f.isChildOf(selectedFolder));

		} catch (ActionNotAllowedException e) {
			PresentationFolder folder = folderBusiness
					.getPresentationFolderById(selectedFolder.getId())
					.orElseThrow(NoSuchJackEntityException::new);
			PresentationFolder highestLinkedCourse = folderBusiness
					.getHighestLinkedCourseFolder(folder)
					.orElseThrow(() -> new IllegalStateException("Folder " + folder
							+ " inherited linkedCourse property, but there was no folder with linked courses."));
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"start.linkFolderForbiddenParent", highestLinkedCourse.getName());
		}
	}

	public String getParallelHint(PresentationFolder folder) {
		return parallelHintMap.computeIfAbsent(folder, this::computeParallelHint);
	}

	private String computeParallelHint(PresentationFolder folder) {
		folder = folderBusiness.getPresentationFolderById(folder.getId()).orElseThrow(NoSuchJackEntityException::new);

		// If user is a student, don't distinguish between inherited and direct linking
		if (presentationFolderRightsMap.getOrDefault(folder, AccessRight.getNone()).isNone()) {
			if (folderBusiness.hasLinkedCourses(folder)) {
				return '(' + getLocalizedMessage("start.linkedFolderHint") + ')';
			} else {
				return null;
			}
		}

		if (folderBusiness.hasInheritedLinkedCourses(folder)) {
			return '(' + getLocalizedMessage("start.linkedFolderHint.inherited") + ')';
		} else if (folder.isContainsLinkedCourses()) {
			return '(' + getLocalizedMessage("start.linkedFolderHint") + ')';
		}
		return null;
	}

	public boolean canUserEditCourseOffer(CourseOffer currentCourseOffer) {
		return authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), currentCourseOffer.getFolder());
	}

	@Override
	protected Map<PresentationFolder, AccessRight> computeFolderRightsMap() {
		return presentationFolderRightsMap;
	}

	/**
	 * @return the deletionDialogView
	 */
	public DeletionDialogView getDeletionDialogView() {
		return deletionDialogView;
	}

	/**
	 * @param deletionDialogView the deletionDialogView to set
	 */
	public void setDeletionDialogView(DeletionDialogView deletionDialogView) {
		this.deletionDialogView = deletionDialogView;
	}

	/**
	 * Used to bind values to columns of the primefaces datatable.
	 * Example: value="#{enrollment.name}" ={@literal >} createValueExpression("#{enrollment.name}", String.class)
	 * 
	 * @param valueExpression
	 * @param valueType
	 * @return ValueExpression
	 */
	// REVIEW: unused
	private static ValueExpression createValueExpression(String valueExpression, Class<?> valueType) {
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().getExpressionFactory().createValueExpression(context.getELContext(),
				valueExpression, valueType);
	}

	public void redirectToParticipantsOverviewForFolder() throws IOException {
		PresentationFolder folder = (PresentationFolder) selectedPresentationNode.getData();
		redirect(viewId.getCourseOfferParticipants().withParam(folder));
	}

	public void redirectToParticipantsOverviewForCourseOffer() throws IOException {
		CourseOffer offer = (CourseOffer) selectedPresentationNode.getData();
		redirect(viewId.getCourseOfferParticipants().withParam(offer));
	}

	/**
	 * Gets the current name of the folder and open the rename-Dialog.
	 * 
	 */
	public void openRenameFolderDialog() {
		Folder selectedFolder = (Folder) this.selectedPresentationNode.getData();

		this.folderRenameDialogView.openRenameFolderDialog(selectedFolder);
	}

	@Override
	protected List<AbstractEntity> computeEntitiesInTree() {
		final var result = new ArrayList<AbstractEntity>(courseMap.size() + presentationFolderMap.size());
		result.addAll(courseMap.keySet());
		result.addAll(presentationFolderMap.keySet());
		return result;
	}

	/**
	 * Computes a search string:
	 * <ul>
	 * <li>Course Offer: name (inherited), ID</li>
	 * <li>Presentation Folder: name (inherited)</li>
	 * </ul>
	 */
	@Override
	protected void generateSearchString(AbstractEntity entity, StringJoiner joiner) {
		super.generateSearchString(entity, joiner);
		if (entity instanceof CourseOffer) {
			joiner.add(Long.toString(entity.getId()));
		}
	}
}