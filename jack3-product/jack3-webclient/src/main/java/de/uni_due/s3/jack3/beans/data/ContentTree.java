package de.uni_due.s3.jack3.beans.data;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.util.TreeUtils;

import de.uni_due.s3.jack3.beans.UserSession;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.comparators.ContentTreeOrder;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.enums.ETreeNodeType;
import de.uni_due.s3.jack3.util.TreeNodeUtils;

public class ContentTree {

	/**
	 * ToC sketch:
	 * * Common Methods
	 * ** Non-Entity-Specific Node Methods
	 * ** Other Methods
	 * * Node Methods
	 * * Folder Methods
	 * * Course Methods
	 * * Exercise Methods
	 * * Getter and Setter
	 */

	// Injected fields
	// REVIEW sw/lg - It may be better to split this class into a Business class that generates the tree and one data
	//	              class that holds the tree data. Passing the injected fields in the constructor (see below) works
	//                but is not best-practice. See also JACK/jack3-core#993.
	//                Weitere Idee: Hilfsmethoden, die NICHT auf die Attribute zugreifen müssen, in "TreeNodeUtils"
	//                auslagern, damit im Bedarfsfall auch "in der Mitte" angefangen werden kann.
	private final FolderBusiness folderBusiness;
	private final UserSession userSession;

	private ContentFolder contentRoot;

	// Data for Building the Tree
	private Map<ContentFolder, AccessRight> contentFolderRightsMap;
	private List<Course> courseList;
	private List<Exercise> exerciseList;
	private List<Folder> expandedFolderList;
	private User currentUser;

	// Class Variables for caching processed Entities
	private Map<User, TreeNode> userNodesMap;
	private Map<ContentFolder, TreeNode> contentFolderNodeMap;
	private Map<Course, TreeNode> courseNodeMap;
	private Map<Exercise, TreeNode> exerciseNodeMap;

	private TreeNode treeRootNode;
	private TreeNode selectedTreeNode;

	private final Queue<ContentFolder> dueForDeletion = new ArrayDeque<>();

	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private final String searchText = "";

	private Consumer<TreeNode> storeExpandedNode;
	private Consumer<TreeNode> removeExpandedNode;

	///Common Methods --- Non-Entity-Specific Node Methods///

	public ContentTree(final UserSession userSession, final FolderBusiness folderBusiness) {
		this.userSession = userSession;
		this.folderBusiness = folderBusiness;
	}

	/**
	 * Builds the Tree from new
	 */
	public void buildTree() {
		//check if Data is available InvalidState
		if (!isDataSetForBuild()) {
			throw new NullPointerException("Required Variables are not set.");
		}

		//clear variables
		clearVariables();
		//add Folders
		addFoldersToTree();
		addCoursesToTree();
		addExercisesToTree();
		updateFolderNodeTypes();
		sortContentTree();
		expandTreeForFolders();

		// If we know in which folder the user was at his/her latest interaction, we expand the tree up to this point
		final Long locationFolderId = userSession.getLatestContentLocationId();
		if (locationFolderId != null) {
			for (final Folder folder : contentFolderNodeMap.keySet()) {
				if (folder.getId() == locationFolderId) {
					expandTreeToLocationFolder(folder);
					break;
				}
			}
		}
	}

	/**
	 * Returns all nodes <strong>with</strong> the root and user nodes.
	 */
	public List<TreeNode> getAllNodes() {
		List<TreeNode> result = new ArrayList<>(
				contentFolderNodeMap.size() + courseNodeMap.size() + exerciseNodeMap.size() + userNodesMap.size() + 1);
		result.addAll(contentFolderNodeMap.values());
		result.addAll(courseNodeMap.values());
		result.addAll(exerciseNodeMap.values());
		result.addAll(userNodesMap.values());
		result.add(treeRootNode);
		return result;
	}

	/**
	 * Returns all nodes <strong>without</strong> the root and <strong>without</strong> user nodes. This is equivalent
	 * to a list of all nodes representing a content (ContentFolder, Course, Exercise).
	 */
	public List<TreeNode> getContentNodes() {
		List<TreeNode> result = new ArrayList<>(
				contentFolderNodeMap.size() + courseNodeMap.size() + exerciseNodeMap.size());
		result.addAll(contentFolderNodeMap.values());
		result.addAll(courseNodeMap.values());
		result.addAll(exerciseNodeMap.values());
		return result;
	}

	/**
	 * Returns all JACK Entities that are saved in this content tree.
	 */
	public List<AbstractEntity> getAllSavedEntities() {
		List<AbstractEntity> result = new ArrayList<>(
				contentFolderNodeMap.size() + courseNodeMap.size() + exerciseNodeMap.size());
		result.addAll(contentFolderNodeMap.keySet());
		result.addAll(courseNodeMap.keySet());
		result.addAll(exerciseNodeMap.keySet());
		return result;
	}

	/**
	 * Clears the Caching Variables and the TreeRootNode
	 */
	private void clearVariables() {
		userNodesMap = new HashMap<>();
		contentFolderNodeMap = new HashMap<>();
		courseNodeMap = new HashMap<>();
		exerciseNodeMap = new HashMap<>();
		final ContentFolder rootContentFolder = folderBusiness.getContentRoot();
		setTreeRootNode(new DefaultTreeNode(rootContentFolder));
		contentFolderNodeMap.put(rootContentFolder, getTreeRootNode());
	}

	public boolean isDataSetForBuild() {
		if ((getContentFolderRightsMap() != null) && (getCourseList() != null) && (getExerciseList() != null)
				&& (getCurrentUser() != null) && (folderBusiness != null) && (getExpandedFolderList() != null)
				&& (getStoreExpandedNode() != null) && (getRemoveExpandedNode() != null)) {
			return true;
		}
		return false;
	}

	/**
	 * Sorts the ContentTree.<br>
	 * Requires the following Variable to be properly set:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain every Pair of Folder and
	 * Node for the current Tree
	 * </ul>
	 */
	private void sortContentTree() {
		final Comparator<TreeNode> compi = new ContentTreeOrder(folderBusiness);
		for (final TreeNode node : contentFolderNodeMap.values()) {
			TreeUtils.sortNode(node, compi);
		}
	}

	/**
	 * Removes every empty Folder <b>Node</b> from the Tree.
	 * <br>Requires the following Variables to be set or alters them:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain every Pair of Folder and
	 * Node for the current Tree
	 * <li>{@link #dueForDeletion} inherited from {@link #partialRemovalOfEmptyFolderNode(Entry)} and
	 * {@link #finalzeRemovalOfFolderNodes()}
	 * </ul>
	 */
	public void trimmTree() {
		contentFolderNodeMap.entrySet().stream().filter(e -> e.getValue().getChildCount() == 0)
				.forEach(this::partialRemovalOfEmptyFolderNode);
		finalzeRemovalOfFolderNodes();
	}

	/**
	 * Expands the Tree to the given Folder.
	 *
	 * @param folder
	 */
	private void expandTreeToLocationFolder(final Folder folder) {
		if ((folder != null) && (contentFolderNodeMap.containsKey(folder))) {
			TreeNode currentNode = contentFolderNodeMap.get(folder);
			do {
				expandNode(currentNode);
				currentNode = currentNode.getParent();
			} while ((currentNode != null) && (currentNode != treeRootNode));

		}

	}

	private void expandTreeForFolders() {
		// "expandedFolderList" is modified by the method below (because the consumer may access the same list)
		// We need a copy to prevent a ConcurrentModificationException
		final var expandedFolderListCopy = new ArrayList<>(expandedFolderList);
		for (final Folder folder : expandedFolderListCopy) {
			expandTreeToLocationFolder(folder);
		}
	}

	public void expandNode(final TreeNode nodeToExpand) {
		nodeToExpand.setExpanded(true);
		if (!expandedFolderList.contains(nodeToExpand.getData())) {
			// The consumer adds the passed folder (in form of a TreeNode) to the current list of expanded elements 
			storeExpandedNode.accept(nodeToExpand);
		}

	}

	public void collapseNode(final TreeNode nodeToCollapse) {
		nodeToCollapse.setExpanded(false);
		removeExpandedNode.accept(nodeToCollapse);
	}

	/**
	 * Sets for every FolderNode the right NodeType.
	 */
	private void updateFolderNodeTypes() {
		contentFolderNodeMap.forEach((k, v) -> setNodeType(v, k));
	}

	public boolean isBuilded() {
		return treeRootNode != null;
	}

	/// Node Methods

	/**
	 * Adds a new Child Node to a Parent Node <b>unchecked</b>.
	 *
	 * @param
	 * parentNode
	 *
	 * @param
	 * childNode
	 *
	 */
	private void addChildNodeToParentNode(final TreeNode parentNode, final TreeNode childNode) {
		parentNode.getChildren().add(childNode);
		childNode.setParent(parentNode);
	}

	private void setNodeType(final TreeNode currentNode, final AbstractEntity currentEntity) {
		if ((currentEntity instanceof ContentFolder)) {
			setNodeType(currentNode, (ContentFolder) currentEntity);
		} else if ((currentEntity instanceof Course)) {
			setNodeType(currentNode, (Course) currentEntity);
		} else if ((currentEntity instanceof Exercise)) {
			setNodeType(currentNode, (Exercise) currentEntity);
		} else {
			throw new IllegalArgumentException("Argument is an unexpected entity: " + currentEntity);
		}
	}

	/**
	 * <pre>
	 * Decides and Sets the NodeType for given Folder.
	 * Requires the following Variable to be properly set:
	 * </pre>
	 *
	 * <ul>
	 * <li> {@link #contentFolderRightsMap} should contain the Pair (Folder, Right) for every Folder the current User
	 * has access to.
	 * </ul>
	 *
	 *
	 * @param currentNode
	 * @param currentFolder
	 */
	private void setNodeType(final TreeNode currentNode, final ContentFolder currentFolder) {
		if (folderBusiness.isPersonalFolder(currentFolder)) {
			if (currentNode.equals(userNodesMap.getOrDefault(getCurrentUser(), null))) {
				// currentFolder is the root folder of the current user (= personal folder)
				currentNode.setType(ETreeNodeType.PERSONAL_FOLDER_TYPE.getName());
			} else {
				// currentFolder is the (virtual) root folder of another user who shares content with the current user
				currentNode.setType(ETreeNodeType.SHARED_FOLDER_TYPE.getName());
			}

		} else if (folderBusiness.getContentRoot().equals(currentFolder)) {
			// Noone has rights on the root
			// The root is not shown in the view but processed due to the hierarchy
			currentNode.setType(ETreeNodeType.NO_ACTION_FOLDER_TYPE.getName());
		} else {
			if (!getContentFolderRightsMap().containsKey(currentFolder)) {
				// At this state, every folder should either be a personal folder, root or already be cached.
				throw new IllegalStateException(
						"No AccessRight for Folder with ID:" + currentFolder.getId() + " Cached.");
			}
			final AccessRight userRight = getContentFolderRightsMap().get(currentFolder);
			final AccessRight userRightOnParent = getContentFolderRightsMap()
					.getOrDefault(currentFolder.getParentFolder(), AccessRight.getNone());
			// The type of the folder node depends on the right that the user has on the folder
			if (userRight.isWrite()) {
				if (!userRightOnParent.isWrite()) {
					// The folder was directly shared, so the user can neither delete/rename it nor edit rights
					currentNode.setType(ETreeNodeType.NO_CHANGE_FOLDER_TYPE.getName());
				} else if (currentNode.getChildCount() == 0) {
					// Empty folders with WRITE right can be deleted but not exported
					currentNode.setType(ETreeNodeType.EMPTY_FOLDER_TYPE.getName());
				} else {
					// Non-empty folders with WRITE right cannot be deleted but exported (due to READ right)
					currentNode.setType(ETreeNodeType.PLAIN_FOLDER_TYPE.getName());
				}
			} else if (userRight.isRead()) {
				currentNode.setType(ETreeNodeType.READ_RIGHTS_FOLDER.getName());
			} else {
				currentNode.setType(ETreeNodeType.NO_ACTION_FOLDER_TYPE.getName());
			}
		}
	}

	/**
	 * Sets the Type of the TreeNode.
	 * Requires the following Variable to be properly set:
	 *
	 * <ul>
	 * <li> {@link #contentFolderRightsMap} should contain the Pair (Folder, Right) for every Folder the current User
	 * has access to.
	 * </ul>
	 *
	 * @param
	 * currentNode
	 *            the Node for which the Node Type should be Set
	 * @param
	 * currentCourse
	 *            the to the Node associated Course
	 */
	private void setNodeType(final TreeNode currentNode, final Course currentCourse) {
		if (getContentFolderRightsMap().get(currentCourse.getFolder()).isWrite()) {
			currentNode.setType(ETreeNodeType.COURSE_TYPE.getName());
		} else {
			// The User doesn't have Edit Rights
			currentNode.setType(ETreeNodeType.NO_DELETE_COURSE_TYPE.getName());
		}
	}

	/**
	 * <pre>
	 * Sets the Type of the TreeNode.
	 * Requires the following Variable to be properly set:
	 * </pre>
	 *
	 * <ul>
	 * <li> {@link #contentFolderRightsMap} should contain the Pair (Folder, Right) for every Folder the current User
	 * has access to.
	 * </ul>
	 *
	 * @param
	 * currentNode
	 *            the Node for which the Node Type should be Set
	 * @param
	 * currentExercise
	 *            the to the Node associated Exercise
	 */
	private void setNodeType(final TreeNode currentNode, final Exercise currentExercise) {
		if (getContentFolderRightsMap().get(currentExercise.getFolder()).isWrite()) {
			currentNode.setType(ETreeNodeType.EXERCISE_TYPE.getName());
		} else {
			// The User doesn't have Edit Rights
			currentNode.setType(ETreeNodeType.NO_DELETE_EXERCISE_TYPE.getName());
		}
	}

	/**
	 * Select the Node for the given Folder.
	 * <br> Requires the following Variable to be set:
	 * <ul>
	 * <li> {@link #contentFolderNodeMap} should contain the Pair (ContentFolder, TreeNode) for the given Folder.
	 * </ul>
	 *
	 * @param contentFolder
	 */
	public void selectFolder(final ContentFolder contentFolder) {
		contentFolderNodeMap.get(contentFolder).setSelected(true);
	}

	public void selectParentFolder(final ContentFolder contentFolder) {
		final ContentFolder parentFolder = (ContentFolder) contentFolder.getParentFolder();
		if (contentFolderNodeMap.containsKey(parentFolder)) {
			selectFolder(parentFolder);
		} else {
			userNodesMap.get(folderBusiness.getOwnerOfContentFolder(contentFolder)).setSelected(true);
		}
	}

	/**
	 * Removes a Child Node from am Parent Node.
	 *
	 * @param parentNode
	 * @param childNode
	 */
	private void removeChildNodeFromParentNode(final TreeNode parentNode, final TreeNode childNode) {
		childNode.clearParent();
		parentNode.getChildren().remove(childNode);
	}

	@Deprecated(forRemoval = false) // Only used for old search
	private void collapseTree() {
		for (final Entry<ContentFolder, TreeNode> e : contentFolderNodeMap.entrySet()) {
			e.getValue().setExpanded(false);
		}
	}

	/**
	 * @deprecated This fragment was part of the old search where found elements are selected and not filtered. With <a
	 *             href="https://s3gitlab.paluno.uni-due.de/JACK/jack3-core/-/issues/1097">issue #1097</a> this
	 *             algorithm is obsolete, but it may be used for the fulltext search.
	 */
	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private void searchForText(final String searchString) {
		collapseTree();
		final List<TreeNode> toExpand = new ArrayList<>();

		for (TreeNode node : getContentNodes()) {
			if (isNodeMatchForSearch(node, searchString)) {
				toExpand.add(node);
				node.setSelected(true);
			} else {
				node.setSelected(false);
			}
		}
		for (TreeNode treeNode : toExpand) {
			TreeNodeUtils.expandUpToNode(treeNode);
		}
	}

	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private boolean isNodeMatchForSearch(final TreeNode node, final String searchString) {
		Boolean match;
		if (NumberUtils.isParsable(searchString)) {
			match = getIdForTreeNode(node) == Long.parseLong(searchString);
		} else {
			match = getNameForTreeNode(node).toLowerCase().contains(searchString.toLowerCase())
					|| ((userSession.isShowContentTags()
							&& getLowerCaseTagsForTreeNode(node).contains(searchString.toLowerCase()))
							|| (node.getType().contentEquals(ETreeNodeType.PERSONAL_FOLDER_TYPE.getName())
									&& getNameForPersonalFolder(node).contains(searchString.toLowerCase())));
		}
		return match;
	}

	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private String getNameForPersonalFolder(final TreeNode node) {
		if ((node.getData() instanceof Folder) && folderBusiness.isPersonalFolder(contentRoot)) {
			return folderBusiness.getOwnerOfContentFolder(contentRoot).getLoginName();
		}
		return "";
	}

	/**
	 * Copy from {@link de.uni_due.s3.jack3.beans.AbstractView}
	 *
	 * @param node
	 * @return
	 */
	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private String getNameForTreeNode(final TreeNode node) {
		if (node.getData() instanceof CourseOffer) {
			return ((CourseOffer) node.getData()).getName();
		}
		if (node.getData() instanceof PresentationFolder) {
			return ((PresentationFolder) node.getData()).getName();
		}
		if (node.getData() instanceof ContentFolder) {
			return ((ContentFolder) node.getData()).getName();
		}
		if (node.getData() instanceof Course) {
			return ((Course) node.getData()).getName();
		}
		if (node.getData() instanceof Exercise) {
			return ((Exercise) node.getData()).getName();
		}
		return "";
	}

	/**
	 * Copy from {@link de.uni_due.s3.jack3.beans.AbstractView}
	 *
	 * @param node
	 * @return
	 */
	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	private long getIdForTreeNode(final TreeNode node) {
		if (node.getData() instanceof CourseOffer) {
			return ((CourseOffer) node.getData()).getId();
		}
		if (node.getData() instanceof Course) {
			return ((Course) node.getData()).getId();
		}
		if (node.getData() instanceof Exercise) {
			return ((Exercise) node.getData()).getId();
		}
		return -1;
	}

	/**
	 * Copy from {@link de.uni_due.s3.jack3.beans.AbstractView}
	 *
	 * @param node
	 * @return
	 */
	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	protected List<String> getLowerCaseTagsForTreeNode(final TreeNode node) {
		if (node.getData() instanceof Exercise) {
			return ((Exercise) node.getData()).getTagsAsStrings().stream().map(String::toLowerCase)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	public void selectTreeNode(final TreeNode newSelectedTreeNode) {
		if (selectedTreeNode != null) {
			selectedTreeNode.setSelected(false);
		}
		selectedTreeNode = newSelectedTreeNode;
		selectedTreeNode.setSelected(true);
	}

	private void deselectAllNodes() {
		for (TreeNode node : getAllNodes()) {
			node.setSelected(false);
		}
	}

	/**
	 * Adds the given Child Node at the top of the Childrenlist from the given Parent Node.
	 *
	 * @param parentNode
	 * @param childNode
	 */
	private void addChildNodeToParentNodeAtTop(final TreeNode parentNode, final TreeNode childNode) {
		parentNode.getChildren().add(0, childNode);
		childNode.setParent(parentNode);
	}

	public void moveChildNodeToParentNode(final TreeNode parentNode, final TreeNode childNode) {
		childNode.getParent().getChildren().remove(childNode);
		parentNode.getChildren().add(childNode);
		TreeUtils.sortNode(childNode, new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Removes a given temporary Node.<br><br>
	 *
	 * Adding with {@link #addTemporaryNodeForNewFolder(ContentFolder)}
	 *
	 * @param tempNode
	 */
	public void removeTemporaryNodeForNewObject(final TreeNode tempNode) {
		if (contentFolderNodeMap.containsValue(tempNode) || courseNodeMap.containsValue(tempNode)
				|| exerciseNodeMap.containsValue(tempNode)) {
			throw new IllegalArgumentException("Object" + tempNode.toString() + " is not a temporary Node.");
		}
		removeChildNodeFromParentNode(tempNode.getParent(), tempNode);
	}

	public void expandTreeToNode(final TreeNode node) {
		final AbstractEntity entity = (AbstractEntity) node.getData();

		if (entity instanceof Folder) {
			expandTreeToLocationFolder((Folder) entity);
		} else if (entity instanceof Exercise) {
			expandTreeToLocationFolder(((Exercise) entity).getFolder());
		} else if (entity instanceof Course) {
			expandTreeToLocationFolder(((Course) entity).getFolder());
		} else {
			throw new IllegalArgumentException("Unsuported Entity in Node");
		}
	}

	///FolderMethods

	/**
	 * Adds all ContentFolders
	 *
	 * <ul>
	 * <li> {@link #contentFolderNodeMap} should be an empty HashMap
	 * <li> {@link #contentFolderRightsMap} should contain the Pair (Folder, Right) for every Folder the current User
	 * has access to.
	 * <li> {@link #userNodesMap} inherited from {@link #createNewUserRootNodeForTree(ContentFolder)} and
	 * {@link #iterateParentFolders(ContentFolder)}
	 *
	 * </ul>
	 */
	private void addFoldersToTree() {

		// Node for Current User Always always Exists
		getOrCreateUserRootNode(getCurrentUser().getPersonalFolder());

		for (final ContentFolder contentFolder : getContentFolderRightsMap().keySet()) {
			if (getContentFolderRightsMap().get(contentFolder).isNone()) {
				break;
			}
			if (!contentFolderNodeMap.containsKey(contentFolder) && !folderBusiness.isPersonalFolder(contentFolder)) {
				final TreeNode tempFolderNode = new DefaultTreeNode(contentFolder);
				contentFolderNodeMap.put(contentFolder, tempFolderNode);
				//We now walk up the tree until we have reached the root node, which is the folder that has no parent folder.
				iterateParentFolders(contentFolder);
			}

		}
	}

	/**
	 * <pre>
	 * Iterate through the parent folders (if necessary) and adds nodes and folders for them.
	 * Requires the following variables to be set and/or alters them:
	 * </pre>
	 *
	 * <ul>
	 * <li> {@link #contentFolderRightsMap} should contain the pair (folder, right) for every folder the current user
	 * has access to.
	 * <li> {@link #userNodesMap} inherited from {@link #getOrCreateUserRootNode(ContentFolder)}
	 * <li> {@link #contentFolderNodeMap} inherited from {@link #getOrCreateUserRootNode(ContentFolder)},
	 * {@link #addFolderToTreeNodeUnsorted(TreeNode, ContentFolder)} and
	 * {@link #getOrCreateOrdinaryNodeForTree(ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 *
	 */
	private void iterateParentFolders(ContentFolder currentFolder) {
		while (currentFolder != null) {
			Folder parent = currentFolder.getParentFolder();
			AccessRight parentAccessRight = getContentFolderRightsMap().get(parent);

			// If we don't have access to the parent folder, we hook the current folder into the corresponding personal
			// folder of the owner of the current folder. The user then sees the folder as a direct child of the person
			// sharing this folder with us. Fixes #1015
			if ((parentAccessRight == null) || parentAccessRight.isNone()) {
				final TreeNode tmpNode = getOrCreateUserRootNode((ContentFolder) getPersonalFolderFor(currentFolder));
				addFolderToTreeNodeUnsorted(tmpNode, currentFolder);
				return;
			}

			if (folderBusiness.isPersonalFolder(parent)) {
				final TreeNode tmpNode = getOrCreateUserRootNode((ContentFolder) parent);
				addFolderToTreeNodeUnsorted(tmpNode, currentFolder);
				return;
			}

			final TreeNode tmpNode = getOrCreateOrdinaryNodeForTree((ContentFolder) parent);
			addFolderToTreeNodeUnsorted(tmpNode, currentFolder);
			currentFolder = (ContentFolder) parent;
		}
	}

	/**
	 * Recursively traverses the tree to the personal folder of the owner of the given contentFolder.
	 *
	 * @param contentFolder
	 * @return The personal folder of the owner of the given folder
	 */
	private Folder getPersonalFolderFor(Folder contentFolder) {
		if (folderBusiness.isPersonalFolder(contentFolder)) {
			return contentFolder;
		}
		Folder parentFolder = contentFolder.getParentFolder();

		if (parentFolder == null) {
			throw new IllegalStateException(
					"Traversed to root folder while looking for a personal folder, this should not happen!");
		}
		return getPersonalFolderFor(parentFolder);
	}

	/**
	 * <pre>
	 * Adds the to the given Folder associated Node to the given Node as Child.
	 * Requires the following Variable to be properly set:
	 * </pre>
	 *
	 * <ul>
	 * <li> {@link #contentFolderNodeMap} should already contain the Pair (Folder,Node) for the given Folder.
	 * </ul>
	 *
	 * @param parentNode
	 * @param currentFolder
	 */
	private void addFolderToTreeNodeUnsorted(final TreeNode parentNode, final ContentFolder currentFolder) {
		addChildNodeToParentNode(parentNode, contentFolderNodeMap.get(currentFolder));
	}

	/**
	 * Adds a Folder to the Tree.
	 * <br>Requires the following Variables and alters them:
	 * <ul>
	 * <li>Into {@link #contentFolderNodeMap} the new Pair (ContentFolder, TreeNode) is inserted and should already
	 * contain the Pair associated to the parent Folder.
	 * </ul>
	 *
	 * @param currentFolder
	 */
	public void addFolderToTree(final ContentFolder currentFolder, final AccessRight rightOnFolder) {
		final TreeNode tmpFolderNode = new DefaultTreeNode(currentFolder);
		final ContentFolder parentFolder = (ContentFolder) currentFolder.getParentFolder();
		final TreeNode parentNode = contentFolderNodeMap.get(parentFolder);
		addChildNodeToParentNode(parentNode, tmpFolderNode);
		contentFolderNodeMap.put(currentFolder, tmpFolderNode);
		getContentFolderRightsMap().put(currentFolder, rightOnFolder);
		setNodeType(tmpFolderNode, currentFolder);
		setNodeType(parentNode, parentFolder);
		parentNode.getChildren().sort(new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Used to get (or if not available create) the Node for the given Folder (Root or not).
	 * <br>Requires the following Variables to be set and/or alters them:
	 *
	 * <ul>
	 * <li> {@link #userNodesMap} inherited from {@link #getOrCreateUserRootNode(ContentFolder)}
	 * <li> {@link #contentFolderNodeMap} inherited from {@link #getOrCreateUserRootNode(ContentFolder)} and
	 * {@link #getOrCreateOrdinaryNodeForTree(ContentFolder)}
	 * <li> {@link #contentFolderRightsMap} inherited from {@link #getOrCreateUserRootNode(ContentFolder)} and
	 * {@link #getOrCreateOrdinaryNodeForTree(ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 * @return
	 */
	private TreeNode getOrCreateParentFolderNodeForTree(final ContentFolder currentFolder) {
		if (folderBusiness.isPersonalFolder(currentFolder)) {
			return getOrCreateUserRootNode(currentFolder);
		}
		return getOrCreateOrdinaryNodeForTree(currentFolder);
	}

	/**
	 * Used to get (or if not available create) the Node for the given <b>non-User-Root</b> Folder. For User
	 * Root-Folder, use {@link #getOrCreateUserRootNode(ContentFolder)}.
	 * <br>Requires the following Variables to be set and/or alters them:
	 *
	 * <ul>
	 * <li> {@link #contentFolderNodeMap} should contain every already inscribed Pair (Folder,Node). If the Pair to the
	 * given Folder is not found, a new Pair will be inscribed in {@link #createNewOrdinaryNodeForTree(ContentFolder)}
	 * and returned.
	 * <li> {@link #contentFolderRightsMap} inherited from {@link #createNewOrdinaryNodeForTree(ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 * @return
	 */
	private TreeNode getOrCreateOrdinaryNodeForTree(final ContentFolder currentFolder) {
		return contentFolderNodeMap.computeIfAbsent(currentFolder, this::createNewOrdinaryNodeForTree);
	}

	/**
	 * Creates a new normal Node for the given Folder <b>unchecked</b>. If possible
	 * {@link #getOrCreateOrdinaryNodeForTree(ContentFolder)} should be used.
	 * <br>Requires the following Variables to be set and/or alters them:
	 *
	 * <ul>
	 * <li> To {@link #contentFolderNodeMap} the new Pair of given Folder and new Node will be added
	 * <li> {@link #contentFolderRightsMap} inherited from {@link #setNodeType(TreeNode, ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 * @return
	 */
	private TreeNode createNewOrdinaryNodeForTree(final ContentFolder currentFolder) {
		return new DefaultTreeNode(currentFolder);
	}

	/**
	 * Requires the following Variables to be Set and/or alters them:
	 *
	 * <ul>
	 * <li> {@link #userNodesMap} should contain every already created Pair of User and Node. If the User Root Node is
	 * not found, the Pair will be added in {@link #createNewUserRootNodeForTree(ContentFolder)} and returned.
	 * <li> {@link #contentFolderNodeMap} inherited from {@link #createNewUserRootNodeForTree(ContentFolder)}
	 * <li> {@link #contentFolderRightsMap} inherited from {@link #createNewUserRootNodeForTree(ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 * @return
	 */
	private TreeNode getOrCreateUserRootNode(final ContentFolder currentFolder) {
		final User tmpUser = folderBusiness.getOwnerOfContentFolder(currentFolder);
		return userNodesMap.computeIfAbsent(tmpUser, u -> createNewUserRootNodeForTree(currentFolder));
	}

	/**
	 * Creates a new User Root Node <b>unchecked</b> and adds him to the associated Maps. If possible
	 * {@link #getOrCreateUserRootNode(ContentFolder)} should be used.
	 * <br>Requires the following Variables to be set and/or alters them:
	 *
	 * <ul>
	 * <li> To {@link #userNodesMap} will the new pair of user and node be added
	 * <li> To {@link #contentFolderNodeMap} will the new pair of folder and node be added
	 * <li> {@link #contentFolderRightsMap} is inherited from {@link #setNodeType(TreeNode, ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 * @return
	 */
	private TreeNode createNewUserRootNodeForTree(final ContentFolder currentFolder) {
		final TreeNode tmpFolderNode = new DefaultTreeNode(currentFolder);
		addChildNodeToParentNode(getTreeRootNode(), tmpFolderNode);
		contentFolderNodeMap.put(currentFolder, tmpFolderNode);
		return tmpFolderNode;
	}

	/**
	 * Move a ContentFolder to another Parent Folder
	 *
	 * @param folderToMove
	 * @param targetFolder
	 */
	public void moveContentFolderToFolder(final ContentFolder targetFolder, final ContentFolder folderToMove) {
		final TreeNode folderToMoveNode = contentFolderNodeMap.get(folderToMove);
		final TreeNode oldParentEntityNode = folderToMoveNode.getParent();
		final AbstractEntity oldParentEntity = AbstractEntity.class.cast(oldParentEntityNode.getData());
		final TreeNode targetFolderNode = contentFolderNodeMap.get(targetFolder);

		removeChildNodeFromParentNode(oldParentEntityNode, folderToMoveNode);
		setNodeType(oldParentEntityNode, oldParentEntity);
		addChildNodeToParentNode(targetFolderNode, folderToMoveNode);
		setNodeType(targetFolderNode, targetFolder);
		TreeUtils.sortNode(targetFolderNode, new ContentTreeOrder(folderBusiness));
		//For simplicity-sake, the cached data won't be updated. Instead the site will be reloaded.
	}

	/**
	 * Partial removes a empty Folder <b>Node</b> from the Tree incl. the patent Folder, if he is the empty. Finalize
	 * with {@link #finalzeRemovalOfFolderNodes()}.
	 * <br> Requires the following Variables to be set or alters them:
	 * <ul>
	 * <li>{@link #dueForDeletion} inherited from {@link #partialRemovalOfFolderNode(Entry)}
	 * </ul>
	 *
	 * @param entry
	 */
	private void partialRemovalOfEmptyFolderNode(final Entry<ContentFolder, TreeNode> entry) {
		final TreeNode parentNode = entry.getValue().getParent();
		partialRemovalOfFolderNode(entry);
		if (parentNode.getChildCount() == 0) {
			parentNode.setType(ETreeNodeType.EMPTY_FOLDER_TYPE.getName());
			partialRemovalOfEmptyFolderNode(new AbstractMap.SimpleEntry<ContentFolder, TreeNode>(
					(ContentFolder) parentNode.getData(), parentNode));
		}
	}

	/**
	 * Partial removes a FolderNode. Finalize with {@link #finalzeRemovalOfFolderNodes()}.
	 * <br>Alters the following Variable:
	 * <ul>
	 * <li>{@link #dueForDeletion} A Queue of Folders which should be removed from {@link #contentFolderNodeMap} in
	 * {@link #finalzeRemovalOfFolderNodes()}
	 * </ul>
	 *
	 * @param entry
	 *            Entry (ContentFolder,TreeNode) which should be removed
	 */
	private void partialRemovalOfFolderNode(final Entry<ContentFolder, TreeNode> entry) {
		// Folder is not removed from contentFolderRightsMap, bc. the right is not revoked
		removeChildNodeFromParentNode(entry.getValue().getParent(), entry.getValue());
		dueForDeletion.add(entry.getKey());
	}

	/**
	 * Finalizes the Removal of FolderNodes
	 * <br>Alters the following Variables
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain every Pair of Folder and
	 * Node for the current Tree
	 * <li>{@link #dueForDeletion} The Queue containing every folder that should be removed from
	 * {@link #contentFolderNodeMap}
	 * </ul>
	 */
	private void finalzeRemovalOfFolderNodes() {
		while (!dueForDeletion.isEmpty()) {
			contentFolderNodeMap.remove(dueForDeletion.poll());
		}
	}

	/**
	 * Removes a Folder from the Tree before he gets deleted.
	 * <br>Requires the following Variables and alters them:
	 * <ul>
	 * <li>From {@link #contentFolderNodeMap}, the Pair associated to the Folder gets removed. Should also contain the
	 * Pair for parent Folder
	 * <li>From {@link #contentFolderRightsMap}, the Pair associated to the Folder gets removed and inherited from
	 * {@link #setNodeType(TreeNode, ContentFolder)}
	 * </ul>
	 *
	 * @param currentFolder
	 */
	public void removeFolderFromTree(final ContentFolder currentFolder) {
		final ContentFolder parentFolder = (ContentFolder) currentFolder.getParentFolder();
		final TreeNode parentFolderNode = contentFolderNodeMap.get(parentFolder);
		removeChildNodeFromParentNode(parentFolderNode, contentFolderNodeMap.get(currentFolder));

		contentFolderNodeMap.remove(currentFolder);
		getContentFolderRightsMap().remove(currentFolder);

		selectTreeNode(parentFolderNode);

		setNodeType(parentFolderNode, parentFolder);
		if (parentFolderNode.getChildCount() == 0) {
			parentFolderNode.setExpanded(false);
		}
	}

	/**
	 * Adds a Temporary Folder-Node in the given Folder.<br>
	 * <br>
	 * Removed with {@link #removeTemporaryNodeForNewObject(TreeNode)}
	 *
	 * @param parentFolder
	 * @return
	 */
	public TreeNode addTemporaryNodeForNewFolder(final ContentFolder parentFolder) {
		final TreeNode tempNode = new DefaultTreeNode("");
		tempNode.setType(ETreeNodeType.NEW_FOLDER_TYPE.getName());

		addChildNodeToParentNodeAtTop(contentFolderNodeMap.get(parentFolder), tempNode);
		expandTreeToNode(tempNode.getParent());
		return tempNode;
	}

	/**
	 * Returns the corresponding node for a given Folder
	 */
	public TreeNode getTreeNodeForFolder(ContentFolder folder) {
		return contentFolderNodeMap.getOrDefault(folder, null);
	}

	//CourseMethods///

	/**
	 * Adds every Course for the available Folders for the current User and inscribes them properly in the associated
	 * Objects.
	 * Requires the following Variables to be Set and/or alters them:
	 *
	 * <ul>
	 * <li> {@link #courseNodeMap} should contain every already inscribed Course, in this case most probably an empty
	 * HashMap
	 * <li> {@link #contentFolderNodeMap} should contain every Folder, the (current) User has access to
	 * <li> {@link #contentFolderRightsMap} inherited from
	 * {@link #addCourseNodeToTreeUnsorted(ContentFolder, Course)}
	 * </ul>
	 */
	private void addCoursesToTree() {
		for (final Course c : getCourseList()) {
			if (!courseNodeMap.containsKey(c)) {
				addCourseNodeToTreeUnsorted(c.getFolder(), c);
			}
		}
	}

	/**
	 * Adds a Course to the Tree <b>unchecked</b>.
	 * <br> Requires the following Variables to be set or alters them:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain the Pair (ContentFolder, TreeNode) for the given Folder.
	 * <li>Into {@link #courseNodeMap} the new Pair (Course, TreeNode) will be saved.
	 * <li>{@link #contentFolderRightsMap} inherited from {@link #setNodeType(TreeNode, ContentFolder)} and
	 * {@link #setNodeType(TreeNode, Course)}.
	 * </ul>
	 *
	 * @param parentFolder
	 * @param course
	 */
	private void addCourseNodeToTreeUnsorted(final ContentFolder parentFolder, final Course course) {
		final TreeNode tempCourseNode = new DefaultTreeNode(course);
		setNodeType(tempCourseNode, course);
		final TreeNode parent = getOrCreateParentFolderNodeForTree(parentFolder);
		addChildNodeToParentNode(parent, tempCourseNode);
		courseNodeMap.put(course, tempCourseNode);
	}

	/**
	 * Adds a Course to the Tree <b>unchecked</b> and sorts the Children of the Node from the given Folder.
	 * <br> Requires the following Variables to be set:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain the Pair (ContentFolder, TreeNode) for the given Folder.
	 * <li>Into {@link #courseNodeMap} the new Pair (Course, TreeNode) will be saved( inherited from
	 * {@link #addCourseNodeToTreeUnsorted(ContentFolder, Course)}).
	 * <li>{@link #contentFolderRightsMap} inherited from {@link #addCourseNodeToTreeUnsorted(ContentFolder, Course)}
	 * </ul>
	 *
	 * @param parentFolder
	 *            the Folder containing the Exercise
	 * @param course
	 *            the Course, which should get a new Node as Child for the given Folder
	 */
	public void addCourseNodeToTree(final ContentFolder parentFolder, final Course course) {
		addCourseNodeToTreeUnsorted(parentFolder, course);
		setNodeType(contentFolderNodeMap.get(parentFolder), parentFolder);
		getCourseList().add(course);
		contentFolderNodeMap.get(parentFolder).getChildren().sort(new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Moves the Node from the given Course to the Node for the given Folder. Expects to be executed <b>before</b> the
	 * actual Change in the File-System.
	 * <br>Requires the following Variables to be set:
	 * <ul>
	 * <li> {@link #courseNodeMap} should contain the Pair (Course,TreeNode) for the given Course
	 * <li> {@link #contentFolderNodeMap} should contain the Pair (ContentFolder,TreeNode) for the given Folder
	 * <b>and</b> the (old) parent Folder.
	 * </ul>
	 *
	 * @param targetFolder
	 * @param course
	 */
	public void moveCourseToFolder(final ContentFolder targetFolder, final Course course) {
		final TreeNode courseNode = courseNodeMap.get(course);
		final TreeNode sourceEntityNode = courseNode.getParent();
		final AbstractEntity sourceEntity = AbstractEntity.class.cast(sourceEntityNode.getData());
		final TreeNode targetFolderNode = contentFolderNodeMap.get(targetFolder);

		removeChildNodeFromParentNode(sourceEntityNode, courseNode);
		setNodeType(sourceEntityNode, sourceEntity);
		addChildNodeToParentNode(targetFolderNode, courseNode);
		setNodeType(targetFolderNode, targetFolder);
		TreeUtils.sortNode(targetFolderNode, new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Removes the given Course from the Tree.
	 * <br> Requires the following Variables to be set:
	 * <ul>
	 * <li> {@link #courseNodeMap} should contain the Pair (Course, TreeNode) for the given Course
	 * <li> {@link #contentFolderRightsMap} inherit from {@link #setNodeType(TreeNode, ContentFolder)}
	 * </ul>
	 *
	 * @param course
	 */
	public void removeCourseFromTree(final Course course) {
		final ContentFolder parentFolder = course.getFolder();
		final TreeNode parentFolderNode = contentFolderNodeMap.get(parentFolder);

		removeChildNodeFromParentNode(parentFolderNode, courseNodeMap.get(course));
		courseNodeMap.remove(course);
		getCourseList().remove(course);

		setNodeType(parentFolderNode, parentFolder);
		if (parentFolderNode.getChildCount() == 0) {
			parentFolderNode.setExpanded(false);
		}
	}

	/**
	 * Adds a Temporary Course-Node in the given Folder.<br>
	 * <br>
	 * Removed with {@link #removeTemporaryNodeForNewObject(TreeNode)}
	 *
	 * @param parentFolder
	 * @return
	 */
	public TreeNode addTemporaryNodeForNewCourse(final ContentFolder parentFolder) {
		final TreeNode tempNode = new DefaultTreeNode("");
		tempNode.setType(ETreeNodeType.NEW_COURSE.getName());

		addChildNodeToParentNodeAtTop(contentFolderNodeMap.get(parentFolder), tempNode);
		expandTreeToNode(tempNode.getParent());
		return tempNode;
	}

	/**
	 * Returns the corresponding node for a given Course
	 */
	public TreeNode getTreeNodeForCourse(Course course) {
		return courseNodeMap.getOrDefault(course, null);
	}

	///Exercise Methods///

	/**
	 * Gets every Exercise for the available Folders for the current User and inscribes them properly in the associated
	 * Objects.
	 * <br>Requires the following Variables to be Set and/or alters them:
	 *
	 * <ul>
	 * <li> {@link #exerciseNodeMap} should contain every already inscribed Exercise, in this case most probably an
	 * empty HashMap
	 * <li> {@link #contentFolderNodeMap} should contain every Folder, the (current) User has access to
	 * <li> {@link #contentFolderRightsMap} inherited from
	 * {@link #addExerciseNodeToTreeUnsorted(ContentFolder, Exercise)}
	 * </ul>
	 */
	private void addExercisesToTree() {
		for (final Exercise exercise : exerciseList) {
			if (!exerciseNodeMap.containsKey(exercise)) {
				addExerciseNodeToTreeUnsorted(exercise.getFolder(), exercise);
			}
		}
	}

	/**
	 * Adds an Exercise to the Tree <b>unchecked</b>.
	 * <br> Requires the following Variables to be set or alters them:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain the Pair (ContentFolder, TreeNode) for the given Folder.
	 * <li>Into {@link #exerciseNodeMap} the new Pair (Exercise, TreeNode) will be saved.
	 * <li>{@link #contentFolderRightsMap} inherited from {@link #setNodeType(TreeNode, ContentFolder)} and
	 * {@link #setNodeType(TreeNode, Exercise)}.
	 * </ul>
	 *
	 * @param parentFolder
	 *            the Folder containing the Exercise
	 * @param exercise
	 *            the Exercise, which should get a new Node as Child for the given Folder
	 */
	private void addExerciseNodeToTreeUnsorted(final ContentFolder parentFolder, final Exercise exercise) {
		final TreeNode tempExerciseNode = new DefaultTreeNode(exercise);
		setNodeType(tempExerciseNode, exercise);
		final TreeNode parent = getOrCreateParentFolderNodeForTree(parentFolder);
		addChildNodeToParentNode(parent, tempExerciseNode);
		exerciseNodeMap.put(exercise, tempExerciseNode);
	}

	/**
	 * Adds an Exercise to the Tree <b>unchecked</b> and sorts the Children of the Node from the given Folder.
	 * <br> Requires the following Variables to be set:
	 * <ul>
	 * <li>{@link #contentFolderNodeMap} should contain the Pair (ContentFolder, TreeNode) for the given Folder.
	 * <li>Into {@link #exerciseNodeMap} the new Pair (Exercise, TreeNode) will be saved( inherited from
	 * {@link #addExerciseNodeToTreeUnsorted(ContentFolder, Exercise)}).
	 * <li>{@link #contentFolderRightsMap} inherited from
	 * {@link #addExerciseNodeToTreeUnsorted(ContentFolder, Exercise)}
	 * </ul>
	 *
	 * @param parentFolder
	 *            the Folder containing the Exercise
	 * @param exercise
	 *            the Exercise, which should get a new Node as Child for the given Folder
	 */
	public void addExerciseNodeToTree(final ContentFolder parentFolder, final Exercise exercise) {
		addExerciseNodeToTreeUnsorted(parentFolder, exercise);
		setNodeType(contentFolderNodeMap.get(parentFolder), parentFolder);
		getExerciseList().add(exercise);
		contentFolderNodeMap.get(parentFolder).getChildren().sort(new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Moves the Node from the given Exercise to the Node for the given Folder. Expects to be executed <b>before</b> the
	 * actual Change in the File-System.
	 * <br>Requires the following Variables to be set:
	 * <ul>
	 * <li> {@link #exerciseNodeMap} should contain the Pair (Exercise,TreeNode) for the given Exercise
	 * <li> {@link #contentFolderNodeMap} should contain the Pair (ContentFolder,TreeNode) for the given Folder
	 * <b>and</b> the (old) parent Folder.
	 * </ul>
	 *
	 * @param targetFolder
	 * @param exercise
	 */
	public void moveExerciseToFolder(final ContentFolder targetFolder, final Exercise exercise) {
		final TreeNode exerciseNode = exerciseNodeMap.get(exercise);
		final TreeNode sourceEntityNode = exerciseNode.getParent();
		final AbstractEntity sourceEntity = AbstractEntity.class.cast(sourceEntityNode.getData());
		final TreeNode targetFolderNode = contentFolderNodeMap.get(targetFolder);

		removeChildNodeFromParentNode(sourceEntityNode, exerciseNode);
		setNodeType(sourceEntityNode, sourceEntity);
		addChildNodeToParentNode(targetFolderNode, exerciseNode);
		setNodeType(targetFolderNode, targetFolder);
		TreeUtils.sortNode(targetFolderNode, new ContentTreeOrder(folderBusiness));
	}

	/**
	 * Removes the given Exercise from the Tree.
	 * <br> Requires the following Variables to be set:
	 * <ul>
	 * <li> {@link #exerciseNodeMap} should contain the Pair (Exercise, TreeNode) for the given Exercise
	 * <li> {@link #contentFolderRightsMap} inherit from {@link #setNodeType(TreeNode, ContentFolder)}
	 * </ul>
	 *
	 * @param exercise
	 */
	public void removeExerciseFromTree(final Exercise exercise) {
		final ContentFolder parentFolder = exercise.getFolder();
		final TreeNode parentFolderNode = contentFolderNodeMap.get(parentFolder);

		removeChildNodeFromParentNode(parentFolderNode, exerciseNodeMap.get(exercise));
		exerciseNodeMap.remove(exercise);

		setNodeType(parentFolderNode, parentFolder);
		if (parentFolderNode.getChildCount() == 0) {
			parentFolderNode.setExpanded(false);
		}
	}

	/**
	 * Adds a Temporary Exercise-Node in the given Folder.<br>
	 * <br>
	 * Removed with {@link #removeTemporaryNodeForNewObject(TreeNode)}
	 *
	 * @param parentFolder
	 * @return
	 */
	public TreeNode addTemporaryNodeForNewExercise(final ContentFolder parentFolder) {
		final TreeNode tempNode = new DefaultTreeNode("");
		tempNode.setType(ETreeNodeType.NEW_EXERCISE.getName());

		addChildNodeToParentNodeAtTop(contentFolderNodeMap.get(parentFolder), tempNode);
		expandTreeToNode(tempNode.getParent());
		return tempNode;
	}

	/**
	 * Returns the corresponding node for a given Exercise
	 */
	public TreeNode getTreeNodeForExercise(Exercise exercise) {
		return exerciseNodeMap.getOrDefault(exercise, null);
	}

	//Getter and Setter

	/**
	 * @return the treeRootNode
	 */
	public TreeNode getTreeRootNode() {
		if ((treeRootNode == null)) {
			buildTree();
		}
		return treeRootNode;

	}

	/**
	 * @param treeRootNode
	 *            the treeRootNode to set
	 */
	public void setTreeRootNode(final TreeNode treeRootNode) {
		this.treeRootNode = treeRootNode;
	}

	/**
	 * @return the contentFolderRightsMap
	 */
	public Map<ContentFolder, AccessRight> getContentFolderRightsMap() {
		return contentFolderRightsMap;
	}

	/**
	 * @param contentFolderRightsMap
	 *            the contentFolderRightsMap to set
	 */
	public void setContentFolderRightsMap(final Map<ContentFolder, AccessRight> contentFolderRightsMap) {
		this.contentFolderRightsMap = contentFolderRightsMap;
	}

	/**
	 * @return the courseList
	 */
	public List<Course> getCourseList() {
		return courseList;
	}

	/**
	 * @param courseList
	 *            the courseList to set
	 */
	public void setCourseList(final List<Course> courseList) {
		this.courseList = courseList;
	}

	/**
	 * @return the exerciseList
	 */
	public List<Exercise> getExerciseList() {
		return exerciseList;
	}

	/**
	 * @param list
	 *            the exerciseList to set
	 */
	public void setExerciseList(final List<Exercise> list) {
		exerciseList = list;
	}

	/**
	 * @return the currentUser
	 */
	public User getCurrentUser() {
		return currentUser;
	}

	/**
	 * @param currentUser
	 *            the currentUser to set
	 */
	public void setCurrentUser(final User currentUser) {
		this.currentUser = currentUser;
	}

	/**
	 * @return the contentRoot
	 */
	public ContentFolder getContentRoot() {
		return contentRoot;
	}

	/**
	 * @param contentRoot
	 *            the contentRoot to set
	 */
	public void setContentRoot(final ContentFolder contentRoot) {
		this.contentRoot = contentRoot;
	}

	/**
	 * @return the expandedFolder
	 */
	public List<Folder> getExpandedFolderList() {
		return expandedFolderList;
	}

	/**
	 * @param expandedFolder
	 *            the expandedFolder to set
	 */
	public void setExpandedFolderList(final List<Folder> expandedFolder) {
		expandedFolderList = expandedFolder;
	}

	public TreeNode getSelectedTreeNode() {
		return selectedTreeNode;
	}

	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	public String getSearchText() {
		return searchText;
	}

	@Deprecated(forRemoval = false) // Only used by old search, may be used for #1095
	public void setSearchText(final String searchString) {
		if (searchString != null && !searchString.isBlank()) {
			searchForText(searchString);
		} else {
			deselectAllNodes();
		}
	}

	public Consumer<TreeNode> getStoreExpandedNode() {
		return storeExpandedNode;
	}

	public void setStoreExpandedNode(final Consumer<TreeNode> storeExpandedNode) {
		this.storeExpandedNode = storeExpandedNode;
	}

	public Consumer<TreeNode> getRemoveExpandedNode() {
		return removeExpandedNode;
	}

	public void setRemoveExpandedNode(final Consumer<TreeNode> removeExpandedNode) {
		this.removeExpandedNode = removeExpandedNode;
	}

	////////////////////////	IN-DEV	///////////////////////////////////////

}
