package de.uni_due.s3.jack3.beans.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.MyWorkspaceView;
import de.uni_due.s3.jack3.beans.UserSession;
import de.uni_due.s3.jack3.beans.data.ContentTree;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.utils.StopWatch;

@ViewScoped
@Named
public class TreeContentDeletionDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -6465371278168254319L;
	private ContentTree tree;
	private TreeNode[] selectedNodes;
	//stores ids of already deleted content
	private Set<Long> alreadyDeletedContentIds = new HashSet<>();

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private UserSession userSession;

	@Inject
	private MyWorkspaceView myWorkspaceView;

	/**
	 * Creates a new content tree for deletion selection.
	 * Checks for each node in the tree, if the containing data can be deleted by the user.
	 * If it could be deleted, the node is set selectable.
	 * 
	 */
	public void prepareDialogView() {
		tree = new ContentTree(userSession, folderBusiness);
		final var watch = new StopWatch().start();
		setVariablesOfContentTree();
		tree.buildTree();

		this.setSelectableOfTree(this.tree.getTreeRootNode());
		getLogger().debugf("Building the content deletion tree took %s", watch.stop().getElapsedMilliseconds());

	}

	/**
	 * Iterates the tree recursively in postorder.
	 * Checks for each Node if the contained Data can be deleted by the user.
	 * Data can be of type Exercise, Course and ContentFolder.
	 * If the data can be deleted, the node is set selectable.
	 * <br>
	 * <strong>Exercise</strong> can be deleted if:
	 * <ul>
	 * <li> User has Edit-Rights</li>
	 * <li> there are none or only Testsubmissions</li>
	 * <li> the exercise is not referenced by a course</li>
	 * </ul>
	 * <strong>Courses</strong> can be deleted if:
	 * <ul>
	 * <li> User has Edit-Rights</li>
	 * <li> there are none or only Testsubmissions</li>
	 * <li> the course is not referenced by a courseOffer</li>
	 * </ul>
	 * <strong>Folders</strong> can be deleted if:
	 * <ul>
	 * <li> User has Edit-Rights</li>
	 * <li> the folder is empty or all elements in the folder can be deleted</li>
	 * <li> the folder is not the personal folder of a user</li>
	 * </ul>
	 * 
	 * @param node
	 *            rootNode of the tree
	 */
	private void setSelectableOfTree(TreeNode node) {
		boolean selectable = false;

		if (node.getData() instanceof Exercise) {
			selectable = exerciseBusiness.isExerciseDeletableByUser((Exercise) node.getData(), getCurrentUser());
		} else if (node.getData() instanceof Course) {
			selectable = courseBusiness.isCourseDeletableByUser((Course) node.getData(), getCurrentUser());
		} else if (node.getData() instanceof ContentFolder) {
			ContentFolder folder = (ContentFolder) node.getData();
			int selectableChildCount = 0;
			//go through all children
			for (TreeNode childNode : node.getChildren()) {
				setSelectableOfTree(childNode);
				if (childNode.isSelectable()) {
					selectableChildCount++;
				}

			}
			/**
			 * user has edit rights,
			 * all Children are selectable
			 * and it is not the personal Folder
			 */
			selectable = authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), (ContentFolder) node.getData())
					&& (node.getChildCount() == selectableChildCount) && !folderBusiness.isPersonalFolder(folder);

		}

		node.setSelectable(selectable);
	}

	/**
	 * Checks if the delete button should be enabled.
	 * Returns true if user selected selectable nodes for deletion
	 * and if all child nodes of a parentNode are selected.
	 * 
	 * @return
	 */
	public boolean isDeletionConfirm() {
		if (this.selectedNodes != null && this.selectedNodes.length > 0) {
			for (TreeNode node : this.selectedNodes) {
				if (!confirmDeletionOfNode(node)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Deletion is only confirm if the node and all childNodes are selectable.
	 * 
	 * @param node
	 * @return
	 */
	private boolean confirmDeletionOfNode(TreeNode node) {
		if (!node.isLeaf()) {
			for (TreeNode child : node.getChildren()) {
				if (!confirmDeletionOfNode(child)) {
					return false;
				}
			}
		}
		return (node.isSelectable() && node.isSelected());
	}

	/**
	 * Creates confirmation message and lists all selected Data.
	 * 
	 * @return
	 */
	public String getConfirmMessage() {
		String selectedNodeNames = this.getNamesOfAllSelectedNodes();
		return formatLocalizedMessage("startContentTabView.deleteTreeContents.confirmation",
				new Object[] { selectedNodeNames });
	}

	/**
	 * Returns the names and type of the content of all selected nodes as html-List
	 * 
	 * @return
	 */
	private String getNamesOfAllSelectedNodes() {
		StringBuilder builder = new StringBuilder();
		if (this.selectedNodes != null && this.selectedNodes.length > 0) {
			builder.append("<ul>");
			for (TreeNode node : this.selectedNodes) {
				String name = "";
				String type = "";
				if (node.getData() instanceof Exercise) {

					name = ((Exercise) node.getData()).getName();
					type = getLocalizedMessage("global.exercise");

				} else if (node.getData() instanceof Course) {

					name = ((Course) node.getData()).getName();
					type = getLocalizedMessage("global.course");

				} else if (node.getData() instanceof ContentFolder) {

					name = ((ContentFolder) node.getData()).getName();
					type = getLocalizedMessage("global.folder");

				}
				builder.append("<li>" + name + " (" + type + ") " + "</li>");
			}
			builder.append("</ul>");
		}
		return builder.toString();
	}

	/**
	 * Deletes the content of all selected nodes and closes dialog.
	 * Per node the content of the node and the content of its children are deleted.
	 * This makes sure, that a folder is only deleted, when all its content is deleted.
	 * <br>
	 * It could be the case that a selected Node (A) is also a child of another selected node (B).
	 * The content of A is deleted, when the childs of B are iterated.
	 * After that the system would try to delete the content of the A because it is in the selected Nodes.
	 * To prevent multiple deletions of the content of A, it is checked if the content was deleted before.
	 * <br>
	 * <strong>This method assumes these things:</strong>
	 * <ul>
	 * <li> Folders could only be deleted, if all children are deleted </li>
	 * <li> the content is only of type {@link Course}, {@link Exercise} and {@link ContentFolder}</li>
	 * </ul>
	 * 
	 */
	public void deleteContent() {
		for (TreeNode node : this.selectedNodes) {
			this.deleteContent(node);
		}
		this.alreadyDeletedContentIds.clear();
		
		PrimeFaces.current().executeScript("PF('treeContentDeletionDialog').hide()");
		myWorkspaceView.reloadSite();
	}
	
	/**
	 * Deletes the content of a node and its children recursively.
	 * Content is only deleted, if it was not deleted before.
	 * 
	 * @param node
	 *            TreeNode which contains content of type {@link Course}, {@link Exercise} and {@link ContentFolder}
	 */
	private void deleteContent(TreeNode node) {
		if (!node.isLeaf()) {
			for (TreeNode child : node.getChildren()) {
				this.deleteContent(child);
			}
		}
		this.deleteContentOfLeaf(node);
	}


	/**
	 * Deletes content of an node,
	 * which has no children or the content of the children is already deleted.
	 * <br>
	 * <strong>Only content of type {@link Course}, {@link Exercise} and {@link ContentFolder} is deleted.</strong>
	 * <br>
	 * Checks if the content was deleted before, to prevent a second deletion attempt.
	 * Necessary because content can be multiple times in the selected Nodes array:
	 * <ul>
	 * <li>as a child of a selected Node</li>
	 * <li>as a selected node</li>
	 * </ul>
	 * 
	 * @param leaf
	 *            node, which has no children or the content of the children is already deleted.
	 */
	private void deleteContentOfLeaf(TreeNode leaf) {
		if (leaf.getData() instanceof AbstractEntity) {
			//check if content is already deleted
			long id = ((AbstractEntity) leaf.getData()).getId();
			String name = "";
			if (!this.alreadyDeletedContentIds.contains(id)) {
				try {
					//delete content
					if (leaf.getData() instanceof Exercise) {
						Exercise exercise = (Exercise) leaf.getData();
						name = exercise.getName();
						exerciseBusiness.deleteExercise(exercise, getCurrentUser());

					} else if (leaf.getData() instanceof Course) {
						Course course = (Course) leaf.getData();
						name = course.getName();
						courseBusiness.deleteCourse(course, getCurrentUser());

					} else if (leaf.getData() instanceof ContentFolder) {
						ContentFolder folder = (ContentFolder) leaf.getData();
						name = folder.getName();
						folderBusiness.deleteFolder(getCurrentUser(), folder);
					}
					//add id to deleted content list
					this.addIdToAlreadyDeletedIds(id);
				} catch (ActionNotAllowedException e) {
					addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,
							"startContentTabView.deleteTreeContents.deletionError", null,
							name, e.getLocalizedMessage());
			}
		}
		}
	}
	


	/**
	 * Sets the for the Building of the ContentTree necessary Variables
	 */
	private void setVariablesOfContentTree() {
		final Map<ContentFolder, AccessRight> contentFolderRightMap = folderBusiness
				.getContentFoldersWithAtLeastReadRightForUser(getCurrentUser());
		tree.setContentFolderRightsMap(contentFolderRightMap);
		tree.setCurrentUser(getCurrentUser());
		tree.setCourseList(courseBusiness
				.getAllCoursesForContentFolderList(new ArrayList<>(contentFolderRightMap.keySet())));
		tree.setExerciseList(exerciseBusiness
				.getAllExercisesForContentFolderList(new ArrayList<>(contentFolderRightMap.keySet())));
		tree.setContentRoot(folderBusiness.getContentRoot());
		tree.setExpandedFolderList(userSession.getExpandedFolders());
		tree.setStoreExpandedNode(userSession::addExpandedFolder);
		tree.setRemoveExpandedNode(userSession::removeExpandedFolder);
	}

	public void setTree(ContentTree tree) {
		this.tree = tree;
	}

	public TreeNode[] getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(TreeNode[] selectedNodes) {
		this.selectedNodes = selectedNodes;
	}

	public TreeNode getContentTreeRoot() {
		return tree.getTreeRootNode();
	}

	public ContentTree getTree() {
		return tree;
	}

	private void addIdToAlreadyDeletedIds(long id) {
		this.alreadyDeletedContentIds.add(id);
	}

}
