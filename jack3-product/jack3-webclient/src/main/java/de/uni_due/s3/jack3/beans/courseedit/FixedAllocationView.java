package de.uni_due.s3.jack3.beans.courseedit;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.primefaces.event.ReorderEvent;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.util.TreeNodeUtils;

/**
 * Handles a {@link FixedListExerciseProvider}.
 */
@Dependent
public class FixedAllocationView extends AbstractExerciseTreeView<FixedListExerciseProvider> {

	private static final long serialVersionUID = -6321474348716706014L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private ExerciseTreeView exerciseTreeView;
	
	// Saves available frozen revisions for Exercises that are in use
	private Map<Exercise, List<FrozenExercise>> frozenExercisesForExercise = new HashMap<>();
	// Saves exercises without access
	private Set<Exercise> noRightExercises = new HashSet<>();

	/**
	 * This method loads the content. For each choosen exercise, the corresponding tree node is selected and the tree
	 * will be expanded to these exercises.
	 */
	@Override
	public void loadView(FixedListExerciseProvider contentProvider, AbstractCourse course) {
		super.loadView(contentProvider, course);

		final var contentTree = exerciseTreeView.getContentTree();
		for (final CourseEntry courseEntry : contentProvider.getCourseEntries()) {

			final Exercise exercise = courseEntry.getExercise();
			final TreeNode exerciseNode = contentTree.getTreeNodeForExercise(exercise);
			if (exerciseNode == null) {
				loadNotAvailableExercise(exercise);
			} else {
				exerciseNode.setSelected(true);
				TreeNodeUtils.expandUpToNode(exerciseNode);
				TreeNodeUtils.passSelectionStatusToParents(exerciseNode);
			}

			frozenExercisesForExercise.put(exercise, loadFrozenExercisesFor(exercise));
		}
	}

	private void loadNotAvailableExercise(Exercise exercise) {
		// There are two special cases to get in here:
		// 1) When "contentProvider" is from envers, there may be selected Exercises that were meanwhile deleted  
		//    We just skip those these elements. They are shown with a trash icon in the view.
		// 2) When the user does not have rights on the Exercise. We add it manually and show a warning in the view.
		if (exerciseBusiness.getExerciseById(exercise.getId()).isPresent()) {
			// Exercise is present, but not shown in the view --> This is only the case if the user doesn't have rights
			noRightExercises.add(exercise);
		}
	}
	
	// -----------------------------------
	// ---------- Tree handlers ----------
	// -----------------------------------

	/**
	 * All content folders and Exercises are selectable in this view.
	 */
	@Override
	public boolean isNodeSelectable(TreeNode node) {
		return node.getData() instanceof ContentFolder || node.getData() instanceof Exercise;
	}

	/**
	 * This method is triggered when a user selects an Exercise node.
	 * 
	 * The Exercise will be added to the provider.
	 */
	@Override
	public void onExerciseSelect(TreeNode node, Exercise exercise) {
		getContentProvider().addExerciseIfNotPresent(exercise);
		node.setSelected(true);
		TreeNodeUtils.passSelectionStatusToParents(node);
		frozenExercisesForExercise.putIfAbsent(exercise, loadFrozenExercisesFor(exercise));
	}

	/**
	 * This method is triggered when a user unselects an Exercise node.
	 * 
	 * The Exercise will be removed from the provider
	 */
	@Override
	public void onExerciseUnselect(TreeNode node, Exercise exercise) {
		getContentProvider().removeExercise(exercise);
		node.setSelected(false);
		TreeNodeUtils.passSelectionStatusToParents(node);
	}

	/**
	 * This method is triggered when a user selects a Folder node.
	 * 
	 * All Exercises in the folder will be added to the provider, as well as all exercises in sub folders.
	 */
	@Override
	public void onFolderSelect(TreeNode node, ContentFolder folder) {
		TreeNodeUtils.forEach(node, n -> {
			n.setSelected(true);
			if (n.getData() instanceof Exercise) {
				final Exercise exercise = (Exercise) n.getData();
				getContentProvider().addExerciseIfNotPresent(exercise);
				frozenExercisesForExercise.putIfAbsent(exercise, loadFrozenExercisesFor(exercise));
			}
		});
		TreeNodeUtils.passSelectionStatusToParents(node);
	}

	/**
	 * This method is triggered when a user unselects a Folder node.
	 * 
	 * All Exercises in the folder will be removed from the provider, as well as all exercises in sub folders.
	 */
	@Override
	public void onFolderUnselect(TreeNode node, ContentFolder folder) {
		TreeNodeUtils.forEach(node, n -> {
			n.setSelected(false);
			if (n.getData() instanceof Exercise) {
				getContentProvider().removeExercise((Exercise) n.getData());
			}
		});
		TreeNodeUtils.passSelectionStatusToParents(node);
	}
	
	// ---------------------------------------
	// ---------- Exercise handlers ----------
	// ---------------------------------------

	/**
	 * If the current sorting mode is {@link ECourseExercisesOrder#MANUAL}, the sortable Course Entry list is returned.
	 * Otherwise, an unmodifiable Course Entry list is returned that is sorted by the current sorting mode.
	 */
	public List<CourseEntry> getSortedCourseEntriesMaybeForReorder() {
		if (getContentProvider() == null) {
			return Collections.emptyList();
		}

		if (getCourse().getExerciseOrder() == ECourseExercisesOrder.MANUAL) {
			return getContentProvider().getCourseEntriesForReorder();
		}

		// Return a list that is ordered by the ECourseExercisesOrder, but not modifiable
		if (getCourse().getExerciseOrder() != null) {
			getContentProvider().sortCourseEntries(Comparator.comparing(
					CourseEntry::getExerciseOrFrozenExercise,
					courseBusiness.getExerciseComparatorInCourse(getCourse(), null)));
		}

		return getContentProvider().getCourseEntries();
	}

	/**
	 * Removes the Course Entry from the provider and unchecks the corresponding Exercise node in the Exercise Tree.
	 * This is called by the "minus" button.
	 */
	public void removeCourseEntry(CourseEntry courseEntry) {
		final Exercise exercise = courseEntry.getExercise();
		getContentProvider().removeExercise(exercise);
		if (!isMissingRightForExercise(exercise)) {
			onExerciseUnselect(exerciseTreeView.getContentTree().getTreeNodeForExercise(exercise), exercise);
		}
	}

	public void onExerciseReorder(ReorderEvent event) {
		getContentProvider().reorderCourseEntry(event.getFromIndex(), event.getToIndex());
	}

	private List<FrozenExercise> loadFrozenExercisesFor(Exercise exercise) {
		List<FrozenExercise> frozenRevisionsForExercise = exerciseBusiness.getFrozenRevisionsForExercise(exercise);
		Collections.sort(frozenRevisionsForExercise);
		return frozenRevisionsForExercise;
	}

	public List<FrozenExercise> getFrozenExercisesFor(Exercise exercise) {
		return frozenExercisesForExercise.computeIfAbsent(exercise, this::loadFrozenExercisesFor);
	}

	// ------------------------------------
	// ---------- Computed Values ----------
	// -------------------------------------

	public int getExerciseCount() {
		if (getContentProvider() == null)
			return 0;
		return getContentProvider().getCourseEntries().size();
	}

	/**
	 * Returns the average difficulty over all exercises.
	 */
	public String getAverageDifficulty() {
		if (getContentProvider() == null)
			return null;
		OptionalDouble avgDifficulty = getContentProvider().getAverageDifficulty();
		if (avgDifficulty.isEmpty()) {
			return getLocalizedMessage("global.notAvailable.short");
		} else {
			NumberFormat format = NumberFormat.getInstance(getResponseLocale());
			format.setMaximumFractionDigits(2);
			return format.format(avgDifficulty.getAsDouble());
		}
	}

	/**
	 * Returns the sum of all points
	 */
	public String getPointSum() {
		if (getContentProvider() == null)
			return null;
		OptionalInt pointSum = getContentProvider().getPointSum();
		if (pointSum.isEmpty()) {
			return getLocalizedMessage("global.notAvailable.short");
		} else {
			return Integer.toString(pointSum.getAsInt());
		}
	}

	public int getRevisionIndexForExerciseRevisionId(FrozenExercise frozenExercise) {
		AbstractExercise tmpExercise = exerciseBusiness
				.getExerciseById(frozenExercise.getProxiedOrRegularExerciseId())
				.orElseThrow(AssertionError::new);
		return exerciseBusiness.getRevisionIndexForRevisionId(tmpExercise,
				frozenExercise.getProxiedExerciseRevisionId());
	}

	public boolean isMissingRightForExercise(final Exercise exercise) {
		return noRightExercises.contains(exercise);
	}
}
