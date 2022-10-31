package de.uni_due.s3.jack3.beans.courseedit;

import java.io.Serializable;
import java.util.Objects;

import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;

/**
 * Abstract class for selecting Exercises and Folders from a Tree ({@link ExerciseTreeView}). Implementations handle a
 * specific provider and react to select and unselect events.
 *
 * @param <T>
 *            Type of the provider that is handled by this view.
 */
public abstract class AbstractExerciseTreeView<T extends AbstractExerciseProvider>
		extends AbstractView implements Serializable {

	private static final long serialVersionUID = 2175192110309582394L;

	private T contentProvider;
	private AbstractCourse course;

	/**
	 * Called whenever the view is loaded after another provider (or no at all) was active.
	 */
	public void loadView(T contentProvider, AbstractCourse course) {
		updateFields(contentProvider, course);
	}

	public void updateFields(T contentProvider, AbstractCourse course) {
		this.contentProvider = Objects.requireNonNull(contentProvider);
		this.course = Objects.requireNonNull(course);
	}

	/**
	 * Should return wether the Provider supports selecting the passed node. This is overridden by
	 * {@link ExerciseTreeView#isSelectableAtAll(TreeNode)}.
	 */
	abstract boolean isNodeSelectable(TreeNode node);

	/**
	 * Should handle the selection of an Exercise.
	 */
	abstract void onExerciseSelect(TreeNode node, Exercise exercise);

	/**
	 * Should handle the unselection of an Exercise.
	 */
	abstract void onExerciseUnselect(TreeNode node, Exercise exercise);

	/**
	 * Should handle the selection of a Folder
	 */
	abstract void onFolderSelect(TreeNode node, ContentFolder folder);

	/**
	 * Should handle the unselection of a Folder
	 */
	abstract void onFolderUnselect(TreeNode node, ContentFolder folder);

	public final T getContentProvider() {
		return contentProvider;
	}

	protected final AbstractCourse getCourse() {
		return course;
	}

}
