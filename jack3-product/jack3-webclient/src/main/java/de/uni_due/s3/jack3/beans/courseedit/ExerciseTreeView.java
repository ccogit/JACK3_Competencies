package de.uni_due.s3.jack3.beans.courseedit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.NodeCollapseEvent;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.NodeUnselectEvent;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.CourseEditView;
import de.uni_due.s3.jack3.beans.UserSession;
import de.uni_due.s3.jack3.beans.data.ContentTree;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.enums.ETreeNodeType;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.util.TreeNodeUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 * This view controls the tree with Exercises and Folders in the Course Edit View with the possibility to (un)select
 * nodes.
 */
@ViewScoped
@Named
public class ExerciseTreeView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 6187575131394916473L;

	public static final String TYPE_EXERCISE = ETreeNodeType.EXERCISE_TYPE.getName();
	public static final String TYPE_PERSONAL_FOLDER = ETreeNodeType.PERSONAL_FOLDER_TYPE.getName();
	public static final String TYPE_SHARED_FOLDER = ETreeNodeType.SHARED_FOLDER_TYPE.getName();
	public static final String TYPE_NORMAL_FOLDER = ETreeNodeType.PLAIN_FOLDER_TYPE.getName();

	private ContentTree tree;
	private final Map<Long, String> searchStrings = new HashMap<>();
	private final Map<Long, List<String>> tagsForExercises = new HashMap<>();

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserSession userSession;

	@Inject
	private CourseEditView courseEditView;

	@PostConstruct
	public void initialize() {

		// Build the tree
		if (tree == null || !tree.isBuilded()) {
			tree = new ContentTree(userSession, folderBusiness);

			final var folderRightsMap = folderBusiness.getContentFoldersWithAtLeastReadRightForUser(getCurrentUser());
			final var folders = new ArrayList<>(folderRightsMap.keySet());
			final var exercises = exerciseBusiness.getAllExercisesForContentFolderList(folders);

			tree.setContentFolderRightsMap(folderRightsMap);
			tree.setCurrentUser(getCurrentUser());
			tree.setCourseList(Collections.emptyList()); // Courses are not shown in this view
			tree.setExerciseList(exercises);
			tree.setContentRoot(folderBusiness.getContentRoot());
			tree.setExpandedFolderList(userSession.getExpandedFoldersCourseEdit());
			tree.setStoreExpandedNode(userSession::addExpandedFolderCourseEdit);
			tree.setRemoveExpandedNode(userSession::removeExpandedFolderCourseEdit);

			final var watch = new StopWatch().start();
			tree.buildTree();
			TreeNodeUtils.forEach(getRoot(), this::performPostProcessing);
			getLogger().debugf("Loading the exercise tree took %s", watch.stop().getElapsedMilliseconds());
		}
	}

	/**
	 * Changes some initial values for a tree node, because the {@link ContentTree} is not intended for checkboxes.
	 */
	private void performPostProcessing(TreeNode node) {
		if (!isSelectableAtAll(node)) {
			node.setSelectable(false);
		}

		// The tree node types are reduced to:
		// 1) "exercise" for all exercises
		// 2) "personalFolder" for the user's personal folder, already set by ContentTree
		// 3) "sharedFolder" for another user's personal folder, already set by ContentTree
		// 4) "folder" for any other folder
		if (node.getData() instanceof Exercise) {
			node.setType(TYPE_EXERCISE);
		} else if (node.getData() instanceof ContentFolder &&
				!(TYPE_SHARED_FOLDER.equals(node.getType()) || TYPE_PERSONAL_FOLDER.equals(node.getType()))) {
			node.setType(TYPE_NORMAL_FOLDER);
		}

		if (node.getData() instanceof AbstractEntity) {
			processTagsAndNames((AbstractEntity) node.getData());
		}
	}

	/**
	 * Computes a search string for node data. For all named entities the string contains the name. For Exercises, it
	 * also contains a list of all tags.
	 */
	private void processTagsAndNames(AbstractEntity entity) {
		StringBuilder searchStringBuilder = new StringBuilder();
		if (entity instanceof Namable) {
			searchStringBuilder.append(((Namable) entity).getName());
		}
		if (entity instanceof Exercise) {
			final var tagNames = exerciseBusiness.getTagsForExerciseAsString((Exercise) entity);
			tagNames.forEach(searchStringBuilder::append);
			if (!tagNames.isEmpty()) {
				tagsForExercises.put(entity.getId(), tagNames);
			}
		}
		searchStrings.put(entity.getId(), searchStringBuilder.toString());
	}

	/**
	 * Returns the search String for an object that was computed by {@link #processTagsAndNames(AbstractEntity)}
	 * previously.
	 */
	public String getSearchString(Object nodeData) {
		if (nodeData instanceof TreeNode) {
			nodeData = ((TreeNode) nodeData).getData();
		}
		if (!(nodeData instanceof AbstractEntity)) {
			return null;
		}

		final AbstractEntity entity = (AbstractEntity) nodeData;
		return searchStrings.get(entity.getId());
	}

	public List<String> getTagsForExercise(Exercise exercise) {
		return tagsForExercises.getOrDefault(exercise.getId(), Collections.emptyList());
	}

	public void clearSelectionState() {
		TreeNodeUtils.forEach(getRoot(), n -> n.setSelected(false));
	}

	/**
	 * This method is called whenever a node is expanded.
	 */
	public void onExpand(final NodeExpandEvent event) {
		tree.expandNode(event.getTreeNode());
	}

	/**
	 * This method is called whenever a node is collapsed.
	 */
	public void onCollapse(final NodeCollapseEvent event) {
		tree.collapseNode(event.getTreeNode());
	}

	/**
	 * This method is called whenever the checkbox of a selectable node is selected.
	 */
	public void onSelect(NodeSelectEvent event) {
		final var selectionHandler = getCurrentTreeProviderView();
		if (selectionHandler != null) {
			TreeNode node = event.getTreeNode();
			if (node.getData() instanceof Exercise) {
				selectionHandler.onExerciseSelect(node, (Exercise) node.getData());
			} else if (node.getData() instanceof ContentFolder) {
				selectionHandler.onFolderSelect(node, (ContentFolder) node.getData());
			}
		}
	}

	/**
	 * This method is called whenever the checkbox of a selectable node is unselected.
	 */
	public void onUnselect(NodeUnselectEvent event) {
		final var selectionHandler = getCurrentTreeProviderView();
		if (selectionHandler != null) {
			TreeNode node = event.getTreeNode();
			if (node.getData() instanceof Exercise) {
				selectionHandler.onExerciseUnselect(node, (Exercise) node.getData());
			} else if (node.getData() instanceof ContentFolder) {
				selectionHandler.onFolderUnselect(node, (ContentFolder) node.getData());
			}
		}
	}

	/**
	 * For each node in the tree, the selectable property is set to the value, which the current
	 * {@link AbstractExerciseTreeView} specifies.
	 * 
	 * @see AbstractExerciseTreeView#isNodeSelectable(TreeNode)
	 */
	public void updateSelectableProperty() {
		final var selectionHandler = getCurrentTreeProviderView();
		if (selectionHandler != null) {
			TreeNodeUtils.forEach(getRoot(), node -> {
				if (isSelectableAtAll(node)) {
					node.setSelectable(selectionHandler.isNodeSelectable(node));
				} else {
					node.setSelectable(false);
				}
			});
		}
	}

	/**
	 * Returns {@code true} if the passed node is not a top-level Folder and if we are not looking on an old revision.
	 */
	private boolean isSelectableAtAll(TreeNode node) {
		boolean isTopLevelFolder = node.getData() instanceof ContentFolder &&
				(node.getParent() == null || node.getParent().getParent() == null);
		return !isTopLevelFolder && courseEditView.isNewestRevision();
	}

	/**
	 * Lookups the folder's owner and returns her/his name.
	 */
	public String getPersonalFolderName(final ContentFolder personalFolder) {
		return folderBusiness.getOwnerOfContentFolder(personalFolder).getLoginName();
	}

	public ContentTree getContentTree() {
		return tree;
	}

	public TreeNode getRoot() {
		return tree.getTreeRootNode();
	}

	/**
	 * Returns the {@link AbstractExerciseTreeView} implementation that fits to the current active content provider.
	 * Returns {@code null} if no content provider is active or it doesn't support a selection.
	 */
	@CheckForNull
	public AbstractExerciseTreeView<?> getCurrentTreeProviderView() {
		if (courseEditView.isChooseFolder())
			return courseEditView.getChooseFolderView();
		if (courseEditView.isFixedAllocation())
			return courseEditView.getFixedAllocationView();
		return null;
	}

}
