package de.uni_due.s3.jack3.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.primefaces.model.TreeNode;

/**
 * Utility methods for PrimeFaces' {@link TreeNode} class.
 */
public final class TreeNodeUtils {

	private TreeNodeUtils() {
		throw new AssertionError("This class shouldn't be instantiated.");
	}

	/**
	 * Performs an action for each element in the tree, starting from the passed node downwards.
	 */
	public static void forEach(TreeNode start, Consumer<TreeNode> action) {
		Objects.requireNonNull(action);
		Objects.requireNonNull(start);

		final Queue<TreeNode> nodesToProcess = new LinkedList<>();
		TreeNode currentNode = start;
		do {
			action.accept(currentNode);
			nodesToProcess.addAll(currentNode.getChildren());
		} while ((currentNode = nodesToProcess.poll()) != null);
	}

	/**
	 * Performs an action for each element data in the tree matching the passed class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void forEachOfType(TreeNode start, Class<T> clazz, Consumer<T> action) {
		forEach(start, n -> {
			final Object data = n.getData();
			if (clazz.isInstance(data)) {
				action.accept((T) data);
			}
		});
	}

	/**
	 * Performs an action for each element in the tree that matches the predicate,
	 * starting from the passed node downwards.
	 */
	public static void forEachIf(TreeNode start, Predicate<TreeNode> predicate, Consumer<TreeNode> action) {
		forEach(start, n -> {
			if (predicate.test(n)) {
				action.accept(n);
			}
		});
	}

	/**
	 * Lists all nodes in the tree, starting from the passed node, then the children, and so on.
	 */
	public static List<TreeNode> getAllNodes(TreeNode start) {
		List<TreeNode> nodes = new LinkedList<>();
		forEach(start, nodes::add);
		return nodes;
	}

	/**
	 * Lists all nodes from the passed node to the root, excluding the root and the passed node.
	 */
	public static List<TreeNode> getBreadcrumb(TreeNode node) {
		List<TreeNode> nodes = new LinkedList<>();
		while (node.getParent() != null && node.getParent().getParent() != null) {
			node = node.getParent();
			nodes.add(node);
		}
		return nodes;
	}

	/**
	 * Expands the view up to the passed node
	 */
	public static void expandUpToNode(TreeNode node) {
		while (node.getParent() != null) {
			node = node.getParent();
			node.setExpanded(true);
		}
	}

	/**
	 * Selects / unselects the passed node and all items downwards from the node.
	 *
	 * @param start
	 *            The node to select / unselect
	 * @param selected
	 *            Wether the nodes should be selected ({@code true}) or unselected.
	 */
	public static void propagateSelectionDown(TreeNode start, boolean selected) {
		forEach(start, n -> n.setSelected(selected));
	}

	/**
	 * Passes the selection-state of the passed node to all nodes in the breadcrumb to set wether
	 * the parent nodes are selected or not.
	 */
	public static void passSelectionStatusToParents(TreeNode start) {
		for (TreeNode node : getBreadcrumb(start)) {
			// "partialSelected" is automatically managed by PrimeFaces
			node.setPartialSelected(false);
			node.setSelected(node.getChildren().stream().allMatch(TreeNode::isSelected));
		}
	}
}
