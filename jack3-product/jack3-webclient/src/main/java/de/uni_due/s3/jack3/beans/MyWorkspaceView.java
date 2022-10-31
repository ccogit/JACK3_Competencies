package de.uni_due.s3.jack3.beans;

import static de.uni_due.s3.jack3.utils.JackFileUtils.filterNonAlphNumChars;
import static de.uni_due.s3.jack3.utils.JackFileUtils.urlEncode;
import static de.uni_due.s3.jack3.utils.JackFileUtils.zipFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.TreeDragDropEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.data.ContentTree;
import de.uni_due.s3.jack3.beans.dialogs.FolderRenameDialogView;
import de.uni_due.s3.jack3.beans.dialogs.TreeContentDeletionDialogView;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.SerDeBusiness;
import de.uni_due.s3.jack3.business.StatisticsBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.DragDropException;
import de.uni_due.s3.jack3.business.exceptions.DragDropException.EType;
import de.uni_due.s3.jack3.business.exceptions.FolderException;
import de.uni_due.s3.jack3.business.helpers.EFolderChildType;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.maintenance.TempDir;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.util.TreeNodeUtils;
import de.uni_due.s3.jack3.utils.JackStringUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

@ViewScoped
@Named
public class MyWorkspaceView extends AbstractContentTreeView<ContentFolder> {

	/**
	 * TOC:
	 * * NEW
	 * ** GENERAL
	 * ** DIALOG
	 * *** DIALOG:HELPER
	 * ** EVENT
	 * *** EVENT:HELPER
	 * *** EVENT:EXECUTE_CHANGE
	 * * SEARCH
	 * * INDEV
	 */

	private static final long serialVersionUID = 1L;

	private TreeNode inputNode;

	private String newExerciseName;
	private String newCourseName;
	private String newContentFolderName;

	private String duplicateExerciseName;
	private String courseDuplicateName;
	private String courseDuplicateWarning;

	// for Drag and Drop events
	// TODO Can be replaced with instances of "PendingDragDropAction" in the future.
	private ContentFolder folderToDrag;
	private Exercise exerciseToDrag;
	private Course courseToDrag;
	private ContentFolder folderToDrop;

	/** Corresponds to a folder ID. May be null and is loaded from UserSession in this case. */
	private Long locationId;

	private String moveOperationConfirmText;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ImportExerciseDialogView importExerciseDialog;

	@Inject
	private UserSession userSession;

	@Inject
	private SerDeBusiness serDeBusiness;

	@Inject
	private BaseService baseService;
	
	@Inject
	private StatisticsBusiness statisticsBusiness;

	@Inject
	private TreeContentDeletionDialogView treeContentDeletionDialogView;

	@Inject
	private UserBusiness userBusiness;

	private ContentTree contentTree;

	@Inject
	private FolderRenameDialogView folderRenameDialogView;

	////////////////////////////////	NEW		////////////////////////////////

	///////////////////////////////		GENERAL	////////////////////////////////

	/**
	 * Gets the ContentTree and, if necessary, initialize it prior.
	 *
	 * @return the contentTree
	 */
	public ContentTree getContentTree() {
		if (contentTree == null) {
			contentTree = new ContentTree(userSession, folderBusiness);
		}
		if (!contentTree.isBuilded()) {
			if (!contentTree.isDataSetForBuild()) {
				setVariablesOfContentTree();
			}
			final var watch = new StopWatch().start();
			contentTree.buildTree();
			getLogger().debugf("Building the content tree took %s", watch.stop().getElapsedMilliseconds());

			updateAllSearchStrings();
		}
		return contentTree;
	}

	/**
	 * Sets the for the Building of the ContentTree necessary Variables
	 */
	private void setVariablesOfContentTree() {
		final Map<ContentFolder, AccessRight> contentFolderRightMap = folderBusiness
				.getContentFoldersWithAtLeastReadRightForUser(getCurrentUser());
		contentTree.setContentFolderRightsMap(contentFolderRightMap);
		contentTree.setCurrentUser(getCurrentUser());
		contentTree.setCourseList(
				courseBusiness.getAllCoursesForContentFolderList(new ArrayList<ContentFolder>(contentFolderRightMap.keySet())));
		contentTree.setExerciseList(
				exerciseBusiness.getAllExercisesForContentFolderList(new ArrayList<ContentFolder>(contentFolderRightMap.keySet())));
		contentTree.setContentRoot(folderBusiness.getContentRoot());
		contentTree.setExpandedFolderList(userSession.getExpandedFolders());
		contentTree.setStoreExpandedNode(userSession::addExpandedFolder);
		contentTree.setRemoveExpandedNode(userSession::removeExpandedFolder);
		refreshShownAccessRights();
	}

	/**
	 * Invokes an Update from the Tree.
	 */
	private void updateTree() {
		PrimeFaces.current().ajax().update(":treeForm:contentTree");
	}
	
	/**
	 * Voids the ContenTree and reloads the current Site. To avoid unnecesary load, {@link #updateTree()} should be prefered if possible.
	 */
	public void reloadSite() {
		contentTree = null; //forget the old Tree
		PrimeFaces.current().executeScript("window.location.reload(true)");
	}

	public String getNewContentFolderName() {
		return newContentFolderName;
	}

	public String getNewCourseName() {
		return newCourseName;
	}

	public String getNewExerciseName() {
		return newExerciseName;
	}

	public long getSelectedFolderId() {
		if ((contentTree.getSelectedTreeNode() != null)
				&& (contentTree.getSelectedTreeNode().getData() instanceof Folder)) {
			return ((Folder) contentTree.getSelectedTreeNode().getData()).getId();
		} else {
			return 0;
		}
	}

	public TreeNode getTreeSelectedNode() {
		return contentTree.getSelectedTreeNode();
	}
	
	public boolean isUserAllowedToManageFolder() {
		if(contentTree.getSelectedTreeNode() == null) return false;
		
		Object data = contentTree.getSelectedTreeNode().getData();
		return data instanceof ContentFolder && contentTree.getContentFolderRightsMap().get(data).isManage();
	}

	public String getCourseDuplicateName() {
		return courseDuplicateName;
	}

	public void setCourseDuplicateName(final String courseDuplicateName) {
		this.courseDuplicateName = courseDuplicateName;
	}
	
	public String getCourseDuplicateWarning() {
		return courseDuplicateWarning;
	}

	public String getMoveOperationConfirmText() {
		return moveOperationConfirmText;
	}

	public String getSelectedNodeName() {
		if (getTreeSelectedNode() == null) {
			return null;
		}
		var data = getTreeSelectedNode().getData();
		if (data instanceof Namable) {
			return ((Namable) data).getName();
		} else {
			return null;
		}
	}

	public void setDuplicateExerciseName(final String duplicateExerciseName) {
		this.duplicateExerciseName = duplicateExerciseName;
	}

	public String getDuplicateExerciseName() {
		return duplicateExerciseName;
	}

	public TreeNode getContentTreeRoot() {
		return getContentTree().getTreeRootNode();
	}

	public void setNewContentFolderName(final String newFolderName) {
		newContentFolderName = newFolderName;
	}

	public void setNewCourseName(final String newCourseName) {
		this.newCourseName = newCourseName;
	}

	public void setNewExerciseName(final String newExerciseName) {
		this.newExerciseName = newExerciseName;
	}

	public void updateFolder(final Folder folder) {
		folderBusiness.updateFolder(folder);
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public void loadLocation() {
		if (locationId == null) {
			locationId = userSession.getLatestContentLocationId();
		}
		if (locationId == null) {
			// Neither in the parameter nor in the session is a last location stored
			return;
		}
		var foundLocation = folderBusiness.getContentFolderById(locationId);
		if (foundLocation.isPresent()) {
			// Location ID points to a valid Content Folder
			userSession.setLatestContentLocationId(locationId);
		}
	}

	public String getPersonalFolderName(final ContentFolder personalFolder) {
		return folderBusiness.getOwnerOfContentFolder(personalFolder).getLoginName();
	}

	@Override
	protected Map<ContentFolder, AccessRight> computeFolderRightsMap() {
		return contentTree.getContentFolderRightsMap();
	}

	///////////////////////////////		DIALOG	////////////////////////////////

	/**
	 * Checks if the selected course and all related frozen courses can be deleted (only test submissions, not
	 * referenced by courseoffer)
	 * and displays the confirmation dialog or a message.
	 */
	public void confirmCourseDeletion() {
		final TreeNode selectedNode = contentTree.getSelectedTreeNode();
		if (selectedNode != null && selectedNode.getData() instanceof Course) {
			final Course course = (Course) selectedNode.getData();
			boolean canbeDeleted = true;

			//check frozen courses
			final List<FrozenCourse> frozencourses = courseBusiness.getFrozenRevisionsForCourse(course);
			for (final FrozenCourse frozencourse : frozencourses) {
				final String courseOfferReferencingCourse = courseBusiness
						.getCourseOffersReferencingCourseAsString(frozencourse);
				if (!courseOfferReferencingCourse.isEmpty()) {
					addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnCourseNotDeleted",
							"startContentTabView.warnCourseNotDeletedMsg", courseOfferReferencingCourse);
					canbeDeleted = false;
				}
			}

			//check course
			final String courseOfferReferencingCourse = courseBusiness.getCourseOffersReferencingCourseAsString(course);
			if (courseBusiness.courseOrFrozenCourseHasNormalSubmissions(course)) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnCourseNotRemoved",
						"startContentTabView.warnCourseNormalSubmissions");
			} else if (!courseOfferReferencingCourse.isEmpty()) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnCourseNotDeleted",
						"startContentTabView.warnCourseNotDeletedMsg", courseOfferReferencingCourse);
			} else if (canbeDeleted) {
				PrimeFaces.current().executeScript("PF('deleteCourse').show();");
			}
		}
	}

	/**
	 * Checks if a selected exercise can be deleted and displays the confirmation dialog or a message.
	 */
	public void confirmExerciseDeletion() {
		final TreeNode selectedNode = contentTree.getSelectedTreeNode();
		if ((selectedNode != null) && (selectedNode.getData() instanceof Exercise)) {
			final Exercise exercise = (Exercise) selectedNode.getData();
			final boolean hasNormalSubmissions = exerciseBusiness.hasExerciseNormalSubmissions(exercise);
			final boolean hasCourseTestSubmissions = exerciseBusiness.hasExerciseCourseTestSubmissions(exercise);
			
			
			if (hasNormalSubmissions) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnExerciseNotRemoved",
						"startContentTabView.warnExerciseNormalSubmissions");
			} else if (hasCourseTestSubmissions) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnExerciseNotRemoved",
						"startContentTabView.warnExerciseCourseTestSubmission");

			} else if (!isExcerciseReferencedFromCourseByFixedListProvider(exercise)) {
				PrimeFaces.current().executeScript("PF('deleteExercise').show();");
			}
			
		}
	}
	
	/**
	 * Opens the renameDialog for the currently selected Folder.
	 * 
	 */
	public void openRenameFolderDialog() {
		Folder selectedFolder = (Folder) this.getTreeSelectedNode().getData();

		this.folderRenameDialogView.openRenameFolderDialog(selectedFolder);
	}


	///////////////////////////////		DIALOG:HELPER	////////////////////////

	/**
	 * Creates the message for deleting an exercise
	 */
	public String getConfirmExerciseDeletionMessage() {
		final TreeNode selectedNode = contentTree.getSelectedTreeNode();
		if ((selectedNode != null) && (selectedNode.getData() instanceof Exercise)) {
			final Exercise exercise = (Exercise) selectedNode.getData();

			final StringBuilder messageToUser = new StringBuilder();

			long countTestSubmissions = statisticsBusiness.countAllSubmissions(exercise)
					- statisticsBusiness.countSubmissions(exercise);
			if (countTestSubmissions > 0) {
				messageToUser.append(formatLocalizedMessage("start.deleteExercise.questionWithSubmissions",
						new Object[] { countTestSubmissions }));
			} else {
				messageToUser.append(getLocalizedMessage("start.deleteExercise.question"));
			}
 
			final List<AbstractCourse> coursesReferencingExcercisePerFolderProvider = courseBusiness
					.getCoursesContainingContentFolderByFolderProvider(exercise.getFolder());
			if (!coursesReferencingExcercisePerFolderProvider.isEmpty()) {
				final String courseNamesReferenced = coursesReferencingExcercisePerFolderProvider.stream()
						.map(AbstractCourse::getName).collect(Collectors.joining(","));
				messageToUser.append(" ");
				messageToUser
						.append(formatLocalizedMessage("start.deleteExercise.questionWithWithCourseFolderReferenced",
								new Object[] { courseNamesReferenced }));
			}
			return messageToUser.toString();
		} else {
			// Default message if the exercise could not be getted
			return formatLocalizedMessage("start.deleteExercise.questionWithSubmissions", new Object[] { "" });
		}
	}

	private boolean isExcerciseReferencedFromCourseByFixedListProvider(final AbstractExercise abstractExercise) {
		final List<AbstractCourse> coursesReferencingExcercise = courseBusiness.getAbstractCoursesWithExerciseProviderContainingExercise(abstractExercise);
		if (!coursesReferencingExcercise.isEmpty()) {
			final String courseNamesReferenced = coursesReferencingExcercise.stream().map(this::mapCourseToName)
					.collect(Collectors.joining(","));
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "startContentTabView.warnExerciseNotRemoved",
					"startContentTabView.warnExerciseNotRemovedMsg", courseNamesReferenced);
			return true;
		}
		return false;
	}

	private String mapCourseToName(final AbstractCourse abstractCourse) {
		if (abstractCourse.isFrozen()) {
			return abstractCourse.getName() + " (" + getLocalizedMessage("exerciseEdit.frozenVersion") + ")";
		}
		return abstractCourse.getName();
	}

	///////////////////////////////		EVENT	////////////////////////////////	

	/**
	 * Event handler for ajax event from the UI in case of drag and drop of elements
	 * in the folder tree.
	 *
	 * @param event
	 *            The {@link TreeDragDropEvent} issued by the UI.
	 */
	public void onDragDrop(final TreeDragDropEvent event) {
		// Get dragged element and drop target from the event.
		final TreeNode dragNode = event.getDragNode();
		final TreeNode dropNode = event.getDropNode();

		try {
			if (dragNode.getData() instanceof Exercise) {
				handleDragDropOnExercise(dragNode, dropNode);
			} else if (dragNode.getData() instanceof Course) {
				handleDragDropOnCourse(dragNode, dropNode);
			} else if (dragNode.getData() instanceof ContentFolder) {
				handleDragDropOnContentFolder(dragNode, dropNode);
			} else {
				throw new UnsupportedOperationException("Unkown Entity draged");
			}
		} catch (final ActionNotAllowedException e) {
			handleDragDropException(e, dropNode, dragNode);
		}

		updateTree();

	}

	public void moveExercise() {
		if (folderToDrop == null || exerciseToDrag == null) {
			throw new IllegalStateException("Requierd Variables are not set.");
		}
		try {
			moveExercise(folderToDrop, exerciseToDrag);
		} catch (final ActionNotAllowedException e) {
			handleDragDropException(e);
		}
		updateTree();
	}

	public void moveCourse() {
		if (folderToDrop == null || courseToDrag == null) {
			throw new IllegalStateException("Requierd Variables are not set.");
		}
		try {
			moveCourse(folderToDrop, courseToDrag);
		} catch (final ActionNotAllowedException e) {
			handleDragDropException(e);
		}
		updateTree();
	}

	public void moveFolder() {
		if (folderToDrop == null || folderToDrag == null) {
			throw new IllegalStateException("Requierd Variables are not set.");
		}
		try {
			moveFolder(folderToDrop, folderToDrag);
		} catch (final ActionNotAllowedException e) {
			handleDragDropException(e);
		}
		
		//In this case a reload is recommended to avoid data inconsistencies. Otherwise use {@link #updateTree()}
		reloadSite();
	}

	public StreamedContent getExerciseExportXml(final Exercise exercise) {
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), exercise.getFolder())) {
			throw new JackSecurityException(getCurrentUser() + " is not allowed to read from " + exercise.getFolder());
		}

		final String xml = serDeBusiness.exerciseToXml(exercise);
		return DefaultStreamedContent.builder() //
				.contentType("text/xml") //
				.name(urlEncode(exercise.getName()) + ".xml") //
				.stream(() -> new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) //
				.build();
	}

	public void clearInputTreeNode() {
		contentTree.removeTemporaryNodeForNewObject(inputNode);
		newCourseName = "";
		newExerciseName = "";
		newContentFolderName = "";
	}

	@Override
	protected Optional<ContentFolder> getSelectedFolder() {
		if (contentTree.getSelectedTreeNode() != null
				&& contentTree.getSelectedTreeNode().getData() instanceof ContentFolder) {
			return Optional.of((ContentFolder) contentTree.getSelectedTreeNode().getData());
		}
		return Optional.empty();
	}

	/**
	 * Listener for context menu. Displays the input text field for a new
	 * content folder.
	 */
	public void createNewEmptyContentFolder() {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(),
				(Folder) contentTree.getSelectedTreeNode().getData())) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			return;
		}
		inputNode = contentTree
				.addTemporaryNodeForNewFolder((ContentFolder) contentTree.getSelectedTreeNode().getData());
	}

	/**
	 * Listener for remote command. Creates a new content folder based on the
	 * text field input.
	 */
	public void createNewContentFolderFromTree() {

		contentTree.selectTreeNode(inputNode.getParent());

		final ContentFolder parent = (ContentFolder) (inputNode.getParent().getData());

		final Optional<String> failureMessage = validateName(parent, newContentFolderName,
				EFolderChildType.CONTENT_FOLDER);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			clearInputTreeNode();
			return;
		}
		try {
			final ContentFolder newFolder = folderBusiness.createContentFolder(getCurrentUser(), newContentFolderName,
					parent);
			contentTree.addFolderToTree(newFolder,
					authorizationBusiness.getMaximumRightForUser(getCurrentUser(), newFolder));
			refreshShownAccessRights();
			updateSingleSearchString(newFolder);
		} catch (final ActionNotAllowedException e) {
			handleExceptionMessageForCreateNewFolder(e);
		}
		clearInputTreeNode();
		updateTree();

	}

	/**
	 * Listener for context menu. Displays the input text field for a new
	 * course.
	 */
	public void createNewEmptyCourse() {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(),
				(Folder) contentTree.getSelectedTreeNode().getData())) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			return;
		}
		inputNode = contentTree
				.addTemporaryNodeForNewCourse((ContentFolder) contentTree.getSelectedTreeNode().getData());
	}

	/**
	 * Listener for remote command. Creates a new course based on the text field
	 * input.
	 */
	public void createNewCourseFromTree() {
		contentTree.selectTreeNode(inputNode.getParent());

		final ContentFolder parent = (ContentFolder) inputNode.getParent().getData();

		final Optional<String> failureMessage = validateName(parent, newCourseName, EFolderChildType.COURSE);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			clearInputTreeNode();
			return;
		}

		final User lecturer = getCurrentUser();

		Course tempCourse;
		try {
			tempCourse = courseBusiness.createCourse(newCourseName, lecturer, parent);
			contentTree.addCourseNodeToTree(parent, tempCourse);
			updateSingleSearchString(tempCourse);
		} catch (final ActionNotAllowedException e) {
			handleExceptionMessageForCreateNewCourse(e);
		}
		clearInputTreeNode();
		updateTree();
	}

	/**
	 * Listener for context menu. Displays the input text field for a new
	 * exercise.
	 */
	public void createNewEmptyExercise() {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(),
				(Folder) contentTree.getSelectedTreeNode().getData())) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			return;
		}
		inputNode = contentTree
				.addTemporaryNodeForNewExercise((ContentFolder) contentTree.getSelectedTreeNode().getData());
	}

	/**
	 * Listener for remote command. Creates a new exercise based on the text
	 * field input.
	 */
	public void createNewExerciseFromTree() {
		contentTree.selectTreeNode(inputNode.getParent());

		final ContentFolder parent = (ContentFolder) inputNode.getParent().getData();

		final Optional<String> failureMessage = validateName(parent, newExerciseName, EFolderChildType.EXERCISE);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			clearInputTreeNode();
			return;
		}

		final User lecturer = getCurrentUser();
		final String language = getUserLanguage().toLanguageTag();

		try {
			final Exercise tempExercise = exerciseBusiness.createExercise(newExerciseName, lecturer, parent, language);
			contentTree.addExerciseNodeToTree(parent, tempExercise);
			updateSingleSearchString(tempExercise);
			
		} catch (final ActionNotAllowedException e) {
			handleExceptionMessageForCreateNewExercise(e);
		}
		clearInputTreeNode();
		updateTree();
	}

	public void deleteContentFolder() {
		if (contentTree.getSelectedTreeNode() != null) {
			final ContentFolder folderToDelete = (ContentFolder) contentTree.getSelectedTreeNode().getData();
			final String folderName = folderToDelete.getName();
			try {
				folderBusiness.deleteFolder(getCurrentUser(), folderToDelete);
				contentTree.removeFolderFromTree(folderToDelete);
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "startContentTabView.folderRemoved",
						"startContentTabView.folderRemovedMsg", folderName);
				searchStrings.remove(folderToDelete.getId());
			} catch (final ActionNotAllowedException e) {
				handleExceptionMessageForDeleteContentFolder(e);
			}
		}
		updateTree();
	}

	public void deleteCourse() {
		if (contentTree.getSelectedTreeNode() != null) {
			final Course courseToDelete = (Course) contentTree.getSelectedTreeNode().getData();
			final String courseToDeleteName = courseToDelete.getName();

			try {
				courseBusiness.deleteCourse(courseToDelete, getCurrentUser());
				contentTree.removeCourseFromTree(courseToDelete);
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "startContentTabView.courseRemoved",
						"startContentTabView.courseRemovedMsg", courseToDeleteName);
				searchStrings.remove(courseToDelete.getId());
			} catch (final ActionNotAllowedException e) {
				handleExceptionMessageForDeleteCourse(e);
			}
			updateTree();
		}
	}

	public void deleteExercise() {
		if (contentTree.getSelectedTreeNode() != null) {
			final Exercise exercise = (Exercise) contentTree.getSelectedTreeNode().getData();
			final String cachedExerciseName = exercise.getName();

			try {
				exerciseBusiness.deleteExercise(exercise, getCurrentUser());
				contentTree.removeExerciseFromTree(exercise);
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "startContentTabView.exerciseRemoved",
						"startContentTabView.exerciseRemovedMsg", cachedExerciseName);
				searchStrings.remove(exercise.getId());
			} catch (final ActionNotAllowedException e) {
				handleExceptionMessageForDeleteExercise(e);
			}
			updateTree();
		}
	}

	public void onNodeCollapse(final NodeCollapseEvent event) {
		contentTree.collapseNode(event.getTreeNode());
	}

	public void onNodeExpand(final NodeExpandEvent event) {
		contentTree.expandNode(event.getTreeNode());
	}

	public StreamedContent exportContentFolder() throws IOException, ActionNotAllowedException {
		final ContentFolder contentFolder = (ContentFolder) contentTree.getSelectedTreeNode().getData();

		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), contentFolder)) {
			getLogger().error("User " + getCurrentUser().getLoginName() + "tried to export folder " + contentFolder
					+ " without sufficent rights!");
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}
		
		// Abort if no exercises are exportable
		if (!hasFolderExercises(contentTree.getSelectedTreeNode())) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "startContentTabView.exportFailed",
					"startContentTabView.exportFailed.noExercises");
			return null;
		}

		final Path tmpDir = Files.createTempDirectory(urlEncode(contentFolder.getName()));
		Files.createDirectory(Paths.get(tmpDir.toString(), urlEncode(contentFolder.getName())));

		final Path localRoot = tmpDir.resolve(Paths.get(urlEncode(contentFolder.getName())));
		serDeBusiness.exportContentFolderToDir(contentFolder, localRoot, false);

		addJack3IndicatorFile(tmpDir);

		final String folderNameFiltered = filterNonAlphNumChars(contentFolder.getName());
		final DefaultStreamedContent zipAsStreamedContent = DefaultStreamedContent //
				.builder() //
				.name("export_" + folderNameFiltered + ".zip").contentType("application/zip") //
				.stream(() -> zipFolder(tmpDir)) //
				.build();

		// Since we write the directory structure to disk and stream it to the user as a zip file, we can't instantantly
		// delete it from disk while we stream it to the user, so we create a TempDir-Entity that gets cleaned up via
		// a cronjob
		final TempDir dirToDelete = new TempDir(tmpDir, LocalDateTime.now());
		baseService.persist(dirToDelete);

		return zipAsStreamedContent;
	}

	/**
	 * Checks if the passed folder node contains any Exercise as children.
	 */
	private boolean hasFolderExercises(TreeNode folderNode) {
		// Data is already stored in the tree, no need to call business methods such as get...withLazyData
		return TreeNodeUtils.getAllNodes(folderNode).stream().anyMatch(node -> node.getData() instanceof Exercise);
	}

	public void duplicateExercise() {
		final TreeNode selectedNode = contentTree.getSelectedTreeNode();
		final Exercise exercise = (Exercise) selectedNode.getData();
		final ContentFolder parent = exercise.getFolder();

		final Optional<String> failureMessage = validateName(parent, duplicateExerciseName, EFolderChildType.EXERCISE);
		if (failureMessage.isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", failureMessage.get());
			duplicateExerciseName = "";
			return;
		}

		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), parent)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			PrimeFaces.current().executeScript("PF('duplicateExerciseDialog').hide()");
			duplicateExerciseName = "";
			return;
		}

		try {
			final Exercise newExercise = exerciseBusiness.duplicateExercise(exercise, exercise.getFolder(),
					duplicateExerciseName, getCurrentUser());
			contentTree.addExerciseNodeToTree(newExercise.getFolder(), newExercise);
			updateSingleSearchString(newExercise);

		} catch (final ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}

		duplicateExerciseName = "";
	}

	/**
	 * Prepares the course duplication.
	 * Checks if the user has edit rights on the course and gets all folders, on which the user has editrights.
	 *
	 */
	public void prepareDuplicateCourse() {
		courseDuplicateName = "";
		courseDuplicateWarning = "";
		if (contentTree.getSelectedTreeNode() != null) {
			final Course course = (Course) contentTree.getSelectedTreeNode().getData();
			authorizationBusiness.hasRightsOnAllCourseExercises(getCurrentUser(), course);
			//check if exercises exists there the user has no rights on. If so display corresponding warning
			if(!authorizationBusiness.hasRightsOnAllCourseExercises(getCurrentUser(), course)) {
				courseDuplicateWarning = formatLocalizedMessage("startContentTabView.duplicateCourse.noRightsForEveryExercise",
										new Object[] { course.getName() });
			}
		}
	}

	/**
	 * Duplicates a course.
	 * Shows error messages, if the user has no rights on the course-folder
	 * or the corresponding exercises,
	 * or the inserted coursename is not valid.
	 * The course is duplicated with the name, the user has inserted in the folder, the user has selected.
	 *
	 */
	public void duplicateCourse() {
		final Course course = (Course) contentTree.getSelectedTreeNode().getData();
		final ContentFolder parent = course.getFolder();

		final Optional<String> nameFailureMessage = validateName(parent, courseDuplicateName, EFolderChildType.COURSE);
		if (nameFailureMessage.isPresent()) {
			this.addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.invalidName", nameFailureMessage.get());
			return;
		}

		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), parent)) {
			this.addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			PrimeFaces.current().executeScript("PF('duplicateCourseDialog').hide()");
			return;
		}

		try {
			Course duplicatedCourse = courseBusiness
					.duplicateCourse(course, courseDuplicateName, getCurrentUser(), course.getFolder());
			contentTree.addCourseNodeToTree(duplicatedCourse.getFolder(), duplicatedCourse);
			updateSingleSearchString(duplicatedCourse);
		} catch (final ActionNotAllowedException e) {
			this.addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
			PrimeFaces.current().executeScript("PF('duplicateCourseDialog').hide()");
			return;
		}
		reloadSite(); // Otherwise the tree will be broken due to incorrect element order
	}

	public void onTreeNodeSelect(final NodeSelectEvent event) {
		contentTree.selectTreeNode(event.getTreeNode());
		//expand the treeNode only if the data is an AbstractEntity. Other elements can't be expanded (see #1244)
		if(event.getTreeNode().getData() instanceof AbstractEntity) {
			contentTree.expandTreeToNode(event.getTreeNode());
		}
		updateLocationId();

	}

	public void onTreeContextMenu(final NodeSelectEvent event) {
		contentTree.selectTreeNode(event.getTreeNode());
	}

	// REVIEW bo: brauchen wir das hier überhaupt noch? Duplizieren funktioniert schon über das Kontextmenü.
	// Zusätztlich find ich den Methodennamen etwas verwirrend
	// REVIEW kk: momentan wird das verwendet, wenn bei dem verschieben von Kursen der besitzer des Kurses
	// wechselt. Dann hat der User die option statt zu verschieben eine Kopie des Kurses zu erstellen. Bei Aufgaben
	// funktioniert das momentan genauso. Die Methodennamen habe ich etwas angepasst. Hoffe die sind jetzt besser
	public void duplicateCourseAfterDragAndDrop() {
		try {
			final Course newCourse = courseBusiness.duplicateCourse(courseToDrag, courseToDrag.getName(),
					getCurrentUser(), folderToDrop);
			contentTree.addCourseNodeToTree(newCourse.getFolder(), newCourse);
			undoDragDrop();
			updateSingleSearchString(newCourse);
		} catch (final ActionNotAllowedException e) {
			//TODO
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
		clearDragDropVariables();
		updateTree();
	}

	public void duplicateExerciseAfterDragAndDrop() {
		try {
			final Exercise newExercise = exerciseBusiness.duplicateExercise(exerciseToDrag, folderToDrop,
					exerciseToDrag.getName(), getCurrentUser());
			contentTree.addExerciseNodeToTree(newExercise.getFolder(), newExercise);
			undoDragDrop();
			updateSingleSearchString(newExercise);
		} catch (final ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
		}
		clearDragDropVariables();
		updateTree();
	}

	///////////////////////////////		EVENT:HELPER	////////////////////////

	/**
	 * TODO
	 *
	 * @param dragNode
	 * @param dropNode
	 * @throws ActionNotAllowedException
	 */
	private void handleDragDropOnExercise(final TreeNode dragNode, final TreeNode dropNode)
			throws ActionNotAllowedException {
		if ((dropNode.getData() instanceof ContentFolder)) {
			folderToDrop = (ContentFolder) dropNode.getData();
		} else {
			//The Target to drop into has to be a Folder
			throw new DragDropException(EType.TARGET_IS_NOT_FOLDER);
		}

		exerciseToDrag = (Exercise) dragNode.getData();

		// NOTE: This implements a fail-fast strategy. If the move operation is not allowed, this method throws an
		//       Eexception, which is then handled immediately by the caller. The warning message will be skipped.
		//       Without this line, the warning would be displayed even if the operation is not allowed.
		authorizationBusiness.ensureIsAllowedToMoveElement(getCurrentUser(), exerciseToDrag.getFolder(), folderToDrop);

		final var warning = getMoveWarning(exerciseToDrag.getFolder(), folderToDrop);
		if (warning.isEmpty()) {
			moveExercise(folderToDrop, exerciseToDrag);
			updateTree();
		} else {
			// Drag & Drop event produces a warning
			moveOperationConfirmText = warning.get();
			PrimeFaces.current().executeScript("PF('moveExercise').show()");
		}
	}

	/**
	 * TODO
	 *
	 * @param dragNode
	 * @param dropNode
	 * @throws ActionNotAllowedException
	 */
	private void handleDragDropOnCourse(final TreeNode dragNode, final TreeNode dropNode)
			throws ActionNotAllowedException {
		if ((dropNode.getData() instanceof ContentFolder)) {
			folderToDrop = (ContentFolder) dropNode.getData();
		} else {
			//The Target to drop into has to be a Folder
			throw new DragDropException(EType.TARGET_IS_NOT_FOLDER);
		}

		courseToDrag = (Course) dragNode.getData();

		// NOTE: This implements a fail-fast strategy. If the move operation is not allowed, this method throws an
		//       Eexception, which is then handled immediately by the caller. The warning message will be skipped.
		//       Without this line, the warning would be displayed even if the operation is not allowed.
		authorizationBusiness.ensureIsAllowedToMoveElement(getCurrentUser(), courseToDrag.getFolder(), folderToDrop);

		final var warning = getMoveWarning(courseToDrag.getFolder(), folderToDrop);
		if (warning.isEmpty()) {
			// Move Course, Exceptions are handled by the caller
			moveCourse(folderToDrop, courseToDrag);
			updateTree();
		} else {
			// Drag & Drop event produces a warning
			moveOperationConfirmText = warning.get();
			PrimeFaces.current().executeScript("PF('moveCourse').show()");
		}
	}

	/**
	 * TODO
	 *
	 * @param dragNode
	 * @param dropNode
	 * @throws ActionNotAllowedException
	 */
	private void handleDragDropOnContentFolder(final TreeNode dragNode, final TreeNode dropNode)
			throws ActionNotAllowedException {
		if ((dropNode.getData() instanceof ContentFolder)) {
			folderToDrop = (ContentFolder) dropNode.getData();
		} else {
			//The Target to drop into has to be a Folder
			throw new DragDropException(EType.TARGET_IS_NOT_FOLDER);
		}
		folderToDrag = (ContentFolder) dragNode.getData();

		// NOTE: This implements a fail-fast strategy. If the move operation is not allowed, this method throws an
		//       Eexception, which is then handled immediately by the caller. The warning message will be skipped.
		//       Without this line, the warning would be displayed even if the operation is not allowed.
		authorizationBusiness.ensureIsAllowedToMoveFolder(getCurrentUser(), folderToDrag, folderToDrop);

		final var warning = getMoveWarning((ContentFolder) folderToDrag.getParentFolder(), folderToDrop);
		if (warning.isEmpty()) {
			// Move Folder, Exceptions are handled by the caller
			moveFolder(folderToDrop, folderToDrag);
		} else {
			// Drag & Drop event produces a warning
			moveOperationConfirmText = warning.get();
			PrimeFaces.current().executeScript("PF('moveFolder').show()");
		}
	}

	private Optional<String> getMoveWarning(final ContentFolder source, final ContentFolder target) {
		// NOTE: We don't use "authorizationBusiness.canManage(source)" because "source" is already the parent folder!
		boolean wouldChangeOwner = !folderBusiness.foldersHaveTheSameOwner(source, target);
		boolean wouldLoseManageRights = authorizationBusiness.getMaximumRightForUser(getCurrentUser(), source)
				.isManage() && !authorizationBusiness.getMaximumRightForUser(getCurrentUser(), target).isManage();
		if (wouldChangeOwner && wouldLoseManageRights) {
			return Optional.of(getLocalizedMessage("start.confirmMoveOperation.loseManageRightAndOwner"));
		} else if (wouldChangeOwner) {
			return Optional.of(getLocalizedMessage("start.confirmMoveOperation.ownerChanges"));
		} else if (wouldLoseManageRights) {
			return Optional.of(getLocalizedMessage("start.confirmMoveOperation.loseManageRight"));
		}
		return Optional.empty();
	}

	private void clearDragDropVariables() {
		folderToDrop = null;
		folderToDrag = null;
		exerciseToDrag = null;
		courseToDrag = null;
	}

	public void openImportExerciseDialog() {
		Folder folder = null;
		if ((contentTree.getSelectedTreeNode() != null)
				&& (contentTree.getSelectedTreeNode().getData() instanceof ContentFolder)) {
			folder = (Folder) contentTree.getSelectedTreeNode().getData();
		} else {
			return;
		}
		importExerciseDialog.setCurrentFolder((ContentFolder) folder);
	}

	private void addJack3IndicatorFile(final Path baseTmpDir) throws IOException {
		Path jack3IndicatorFile = baseTmpDir.resolve(SerDeBusiness.JACK3_INDICATOR_FILE_NAME);
		jack3IndicatorFile = Files.createFile(jack3IndicatorFile);
		final String fileContent = "Please leave this file unedited. It is used to detect what type of import has to be used!";
		Files.write(jack3IndicatorFile, fileContent.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * This method updates the latest visited folder id in the userSession. The
	 * id can be used to restore the view as it was when the user returns to the
	 * start page.
	 */
	private void updateLocationId() {
		if (contentTree.getSelectedTreeNode().getData() instanceof ContentFolder) {
			final ContentFolder folder = (ContentFolder) contentTree.getSelectedTreeNode().getData();
			userSession.setLatestContentLocationId(folder.getId());
		} else if (contentTree.getSelectedTreeNode().getData() instanceof Course) {
			final Course course = (Course) contentTree.getSelectedTreeNode().getData();
			userSession.setLatestContentLocationId(course.getFolder().getId());
		} else if (contentTree.getSelectedTreeNode().getData() instanceof Exercise) {
			Exercise exercise = (Exercise) contentTree.getSelectedTreeNode().getData();
			userSession.setLatestContentLocationId(exercise.getFolder().getId());
		}
	}

	public void undoDragDrop() {
		if (folderToDrag != null) {
			contentTree.moveContentFolderToFolder((ContentFolder) folderToDrag.getParentFolder(), folderToDrag);
		} else if (courseToDrag != null) {
			contentTree.moveCourseToFolder(courseToDrag.getFolder(), courseToDrag);
		} else if (exerciseToDrag != null) {
			contentTree.moveExerciseToFolder(exerciseToDrag.getFolder(), exerciseToDrag);
		} else {
			//If this is reached smth. went horribly wrong and a correct State is not guaranteed anymore.
			throw new IllegalStateException("Undoing of the movement was not possible.");
		}
		clearDragDropVariables();
	}

	////////////////////////////////	EVENT:EXECUTE_CHANGE	////////////////

	/**
	 * Moves a given Exercise to the given Folder in the ContentTree
	 *
	 * @param folderToDrop
	 * @param exerciseToDrag
	 * @throws DragDropException
	 */
	private void moveExercise(final ContentFolder folderToDrop, final Exercise exerciseToDrag)
			throws ActionNotAllowedException {

		exerciseBusiness.moveExercise(exerciseToDrag, folderToDrop, getCurrentUser());
		contentTree.moveExerciseToFolder(folderToDrop, exerciseToDrag);

		//Clearing variables
		clearDragDropVariables();
	}

	/**
	 * Moves a given Course to the given Folder in the ContentTree
	 *
	 * @param folderToDrop
	 * @param courseToDrag
	 * @throws ActionNotAllowedException
	 */
	private void moveCourse(final ContentFolder folderToDrop, final Course courseToDrag)
			throws ActionNotAllowedException {

		courseBusiness.moveCourse(courseToDrag, folderToDrop, getCurrentUser());

		contentTree.moveCourseToFolder(folderToDrop, courseToDrag);
		//Clearing variables
		clearDragDropVariables();
	}

	private void moveFolder(ContentFolder folderToDrop, ContentFolder folderToDrag)
			throws ActionNotAllowedException {

		folderBusiness.moveContentFolder(getCurrentUser(), folderToDrag, folderToDrop);
		//contentTree.moveContentFolderToFolder(folderToDrop, folderToDrag);// unnecessary step, the Site will be reloaded anyway.
		folderBusiness.resetFolderRights(folderToDrag);
		//Clearing variables
		clearDragDropVariables();
		
		//In this case a reload is recommended to avoid data inconsistencies. Otherwise use {@link #updateTree()}
		reloadSite();
	}

	////////////////////////////////	HANDLE EXEPTION	////////////////////////

	private void handleExceptionMessageForDragAndDrop(final ActionNotAllowedException e) {
		final var msgSeverity = FacesMessage.SEVERITY_ERROR;
		final var msgTitle = "start.dragNotAllowed";
		// TODO Wenn wir auf Java 17 umgestiegen sind, sollte dieser Teil mit Pattern Matching für switch und instanceof umgeschrieben werden.
		if ((e instanceof FolderException)) {
			final FolderException fExc = FolderException.class.cast(e);
			switch (fExc.getType()) {
			case RECURSION:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.recursion");
				return;
			case PERSONAL_FOLDER:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.personalFolder");
				return;
			case ROOT:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.virtualDropNode");
				return;
			default:
				addGlobalFacesMessage(msgSeverity, msgTitle, null);
				getLogger().error("Unhandled FolderException", fExc);
				return;
			}
		} else if ((e instanceof AuthorizationException)) {
			final AuthorizationException aExc = AuthorizationException.class.cast(e);
			switch (aExc.getType()) {
			case RIGHTS_WILL_CHANGE:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.rightsWillChange");
				return;
			case DRAG_TARGET_RIGHT_IS_NOT_WRITE:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.noSourceRights");
				return;
			case DROP_TARGET_RIGHT_IS_NOT_WRITE:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.noTargetRights");
				return;
			default:
				addGlobalFacesMessage(msgSeverity, msgTitle, null);
				getLogger().error("Unhandled AuthorizationException", aExc);
				return;
			}
		} else if ((e instanceof DragDropException)) {
			final DragDropException dExc = DragDropException.class.cast(e);
			switch (dExc.getType()) {
			case TARGET_IS_NOT_FOLDER:
				addGlobalFacesMessage(msgSeverity, msgTitle, "start.dragNotAllowed.noFolder");
				return;
			default:
				addGlobalFacesMessage(msgSeverity, msgTitle, null);
				getLogger().error("Unhandled DragDropException", dExc);
				return;
			}
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.dragNotAllowed", null);
			getLogger().error("Unhandled ActionNotAllowedException.", e);
		}
	}
	
	private void handleExceptionMessageForCreateNewFolder(ActionNotAllowedException e) {
		// REVIEW lg - Hier und in den folgenden Methoden gibt es zwei Sachen zu klären:
		// 1. Bekommen wir den Code einfacher hin als mit den break-Labeln?
		// 2. "handleUnhandledException" wurde als deprecated markiert.
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.createFolder","startContentTabView.actionNotAllowed.createFolder.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}
	
	private void handleExceptionMessageForCreateNewCourse(ActionNotAllowedException e) {
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.createCourse","startContentTabView.actionNotAllowed.createCourse.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}
	
	private void handleExceptionMessageForCreateNewExercise(ActionNotAllowedException e) {
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.createExercise","startContentTabView.actionNotAllowed.createExercise.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}

	private void handleDragDropException(final ActionNotAllowedException e, final TreeNode dropNode,
			final TreeNode dragNode) {
		//Undo Movement
		final AbstractEntity abstractEntity = AbstractEntity.class.cast(dragNode.getData());
		if ((abstractEntity instanceof ContentFolder)) {
			final ContentFolder contentFolder = ContentFolder.class.cast(abstractEntity);
			contentTree.moveContentFolderToFolder((ContentFolder) contentFolder.getParentFolder(), contentFolder);
		} else if ((abstractEntity instanceof Course)) {
			final Course course = Course.class.cast(abstractEntity);
			contentTree.moveCourseToFolder(course.getFolder(), course);
		} else if ((abstractEntity instanceof Exercise)) {
			final Exercise exercise = Exercise.class.cast(abstractEntity);
			contentTree.moveExerciseToFolder(exercise.getFolder(), exercise);
		} else {
			//If this is reached smth. went horrible wrong and a correct State is not guaranteed anymore.
			throw new IllegalStateException("Undoing of the movement was not possible.");
		}
		handleExceptionMessageForDragAndDrop(e);
		clearDragDropVariables();
	}

	private void handleDragDropException(final ActionNotAllowedException e) {
		//Undo Movement
		undoDragDrop();
		handleExceptionMessageForDragAndDrop(e);
	}

	/**
	 * @return Failure message for validation of a new / renamed object
	 */
	private Optional<String> validateName(final ContentFolder parent, final String name,
			final EFolderChildType childType) {
		if (JackStringUtils.isBlank(name)) {
			return Optional.of("global.invalidName.empty");
		}

		return Optional.empty();
	}
	
	private void handleExceptionMessageForDeleteContentFolder(ActionNotAllowedException e) {
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.deleteFolder","startContentTabView.actionNotAllowed.deleteFolder.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}
	
	private void handleExceptionMessageForDeleteCourse(ActionNotAllowedException e) {
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.deleteFolder","startContentTabView.actionNotAllowed.deleteFolder.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}
	
	private void handleExceptionMessageForDeleteExercise(ActionNotAllowedException e) {
		breakIf: if(e instanceof AuthorizationException) {
			AuthorizationException ae = (AuthorizationException) e;
			switch(ae.getType()) {
			case INSUFFICIENT_RIGHT:
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,"startContentTabView.actionNotAllowed.deleteExercise","startContentTabView.actionNotAllowed.deleteExercise.insufficientRights");
				break breakIf;
			default:
				handleUnhandledException(ae);
				break breakIf;
			}
		}else {
			handleUnhandledException(e);
			break breakIf;
		}
	}

	/**
	 * Fallback handling for ActionNotAllowed Exceptions.
	 *
	 * @param e
	 */
	@Deprecated
	private void handleUnhandledException(final ActionNotAllowedException e) {
		addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.actionNotAllowed.unhandled.general", "start.actionNotAllowed.unhandled.notSpecified");
		getLogger().error("Unhandled ActionNotAllowedException ", e);
	}

	////////////	SEARCH	///////////////////////////////////////////////////

	@Override
	protected List<AbstractEntity> computeEntitiesInTree() {
		return contentTree.getAllSavedEntities();
	}

	/**
	 * Computes a search string:
	 * <ul>
	 * <li>Course: name (inherited), ID, internal description (if shown)</li>
	 * <li>Exercise: name (inherited), ID, internal description (if shown), tags (if shown)</li>
	 * <li>Content Folder: name (inherited)</li>
	 * </ul>
	 */
	@Override
	protected void generateSearchString(AbstractEntity entity, StringJoiner joiner) {
		super.generateSearchString(entity, joiner);
		if (entity instanceof Exercise) {
			joiner.add(Long.toString(entity.getId()));
			if (userSession.isShowContentInternalDescriptions()) {
				joiner.add(((Exercise) entity).getInternalNotes());
			}
			if (userSession.isShowContentTags()) {
				exerciseBusiness.getTagsForExerciseAsString((Exercise) entity).forEach(joiner::add);
			}
		}
		if (entity instanceof Course) {
			joiner.add(Long.toString(entity.getId()));
			if (userSession.isShowContentInternalDescriptions()) {
				joiner.add(((Course) entity).getInternalDescription());
			}
		}
	}

	@Override
	protected String getRealNameOfEntity(Namable nameableEntity) {
		if (nameableEntity instanceof ContentFolder) {
			ContentFolder folder = (ContentFolder) nameableEntity;
			if (!folder.isRoot() && folder.getParentFolder().isRoot()) {
				return userBusiness
						.getUserOwningThisFolder((ContentFolder) nameableEntity)
						.map(User::getLoginName)
						.orElse(nameableEntity.getName()); // Use folder name if user is not available
			}
		}
		return super.getRealNameOfEntity(nameableEntity);
	}

	////////////	IN-DEV	///////////////////////////////////////////////////
	
	public void prepareTreeContentDeletionDialog() {
		treeContentDeletionDialogView.prepareDialogView();
		PrimeFaces.current().executeScript("PF('treeContentDeletionDialog').show()");
	}
	
}