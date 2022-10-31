package de.uni_due.s3.jack3.beans.courseedit;

import static de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider.ALL_EXERCISES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.Dependent;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;

import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.util.TreeNodeUtils;

/**
 * Handles a {@link FolderExerciseProvider}.
 */
@Dependent
public class ChooseFolderView extends AbstractExerciseTreeView<FolderExerciseProvider> {

	private static final long serialVersionUID = 8050852345370867867L;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExerciseTreeView exerciseTreeView;

	// Saves exercises that are in a selected Content Folder without access
	private Map<ContentFolder, List<Exercise>> noRightExercises = new HashMap<>();

	/**
	 * This method loads the content. For each choosen folder, the corresponding tree node is selected (and all children
	 * are also selected) and expanded.
	 */
	@Override
	public void loadView(FolderExerciseProvider contentProvider, AbstractCourse course) {
		super.loadView(contentProvider, course);

		final var contentTree = exerciseTreeView.getContentTree();
		for (final ContentFolder contentFolder : contentProvider.getFolders()) {

			// Start with the content folder from the provider
			final TreeNode folderNode = contentTree.getTreeNodeForFolder(contentFolder);
			if (folderNode == null) {
				loadNotAvailableFolder(contentFolder);
			} else {
				// Iterate over the folder and all (in)direct children and select each folder
				TreeNodeUtils.propagateSelectionDown(folderNode, true);
				TreeNodeUtils.expandUpToNode(folderNode);
			}
		}
	}

	private void loadNotAvailableFolder(ContentFolder contentFolder) {
		// There are two special cases to get in here:
		// 1) When "contentProvider" is from envers, there may be selected Folders that were meanwhile deleted
		//    We just skip those these elements. They are shown with a trash icon in the view.
		// 2) When the user does not have rights on the folder. We add the Exercises manually and show a warning in the
		//    view.
		if (folderBusiness.getContentFolderById(contentFolder.getId()).isPresent()) {
			// Folder is present, but not shown in the view --> This is only the case if the user doesn't have rights
			List<Exercise> exercises = exerciseBusiness.getAllExercisesForContentFolderRecursive(contentFolder);
			noRightExercises.put(contentFolder, exercises);
		}
	}

	// -----------------------------------
	// ---------- Tree handlers ----------
	// -----------------------------------

	/**
	 * All content folders are selectable in this view. Exercises are not selectable.
	 */
	@Override
	boolean isNodeSelectable(TreeNode node) {
		return node.getData() instanceof ContentFolder;
	}

	@Override
	void onExerciseSelect(TreeNode node, Exercise exercise) {
		// This should not be possible due to the "selectable" property, but if something went wrong and the element was clickable after all, we still show a warning.
		addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "courseEditExerciseTree.error.noFolder");
	}

	@Override
	void onExerciseUnselect(TreeNode node, Exercise exercise) {
		// This should not be possible due to the "selectable" property, but if something went wrong and the element was clickable after all, we still show a warning.
		addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "courseEditExerciseTree.error.noFolder");
	}

	/**
	 * This method is triggered when a user selects a Folder node.
	 * 
	 * If the selected node is a content folder, all exercises in the folder and each exercise in any of the subfolders
	 * will be part of the course. Thus all these exercises and subfolders will appear as selected in the view and the
	 * selected folder will be added to the provider.
	 */
	@Override
	void onFolderSelect(TreeNode node, ContentFolder folder) {

		if (node.getParent() == null || node.getParent().getParent() == null) {
			// This should not be possible due to the "selectable" property, but if something went wrong and the element was clickable after all, we still show a warning.
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "courseEditExerciseTree.error.rootFolder");
			return;
		}

		if (!getContentProvider().getFolders().contains(folder)) {
			// All children folders are now included in the provider via the parent folder and can be removed
			removeAllChildrenFromProvider(node);
			// But the folder itself is added
			getContentProvider().addFolder(folder);
		}

		// Select all direct and indirect children to indicate that they are now included
		TreeNodeUtils.propagateSelectionDown(node, true);
		TreeNodeUtils.expandUpToNode(node);
		node.setExpanded(true);
	}

	/**
	 * This method is triggered when a user unselects a Folder node.
	 * 
	 * However, when a parent folder (or a folder in the hierarchy) is still selected, this action is not allowed
	 * because by selecting a certain folder all nodes in the tree downwards from there are automatically selected.
	 * Thus, the user sees a message explaining this to him. If the action is allowed, all nodes in the tree downwards
	 * from here will be unselected and the folder will be closed.
	 */
	@Override
	void onFolderUnselect(TreeNode node, ContentFolder folder) {

		if (node.getParent() == null || node.getParent().getParent() == null) {
			// Root folders cannot be selected
			// This should not be possible due to the "selectable" property, but if something went wrong and the element was clickable after all, we still show a warning.
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "courseEditExerciseTree.error.rootFolder");
			return;
		}

		if (node.getParent().isSelected()) {
			// Folders where a parent folder is selected cannot be unselected individually
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "courseEditExerciseTree.error.parentSelected");
			return;
		}

		if (getContentProvider().getFolders().contains(folder)) {
			getContentProvider().removeFolder(folder);
			// Different from above, we don't have to update the children in the provider because they were removed
			// from the provider when the current folder was added.
		}

		// Unselect all direct and indirect children because they are no longer included
		TreeNodeUtils.propagateSelectionDown(node, false);
		TreeNodeUtils.expandUpToNode(node);
		node.setExpanded(false);
	}

	/**
	 * Removes all Folders downwards from the node from the Content Provider.
	 */
	private void removeAllChildrenFromProvider(TreeNode node) {
		TreeNodeUtils.forEachOfType(node, ContentFolder.class, getContentProvider()::removeFolder);
	}

	/**
	 * Removes the passed Exercise's Folder from the provider and unchecks the corresponding Folder node in the Exercise
	 * Tree. This is called by the "minus" button.
	 */
	public void removeFolder(Exercise exercise) {
		final ContentFolder folder = getFolderFromProviderContainingExercise(exercise);
		getContentProvider().removeFolder(folder);
		if (!isMissingRightForFolder(folder)) {
			onFolderUnselect(exerciseTreeView.getContentTree().getTreeNodeForFolder(folder), folder);
		}
	}

	// ---------------------------------------
	// ---------- Exercise handlers ----------
	// ---------------------------------------

	/**
	 * Returns a list with all exercises in the given content folder. This method works recursively and also fetches
	 * exercises in sub directories.
	 */
	public List<Exercise> getExercisesForFolder(ContentFolder folder) {

		final var contentTree = exerciseTreeView.getContentTree();
		var node = contentTree.getTreeNodeForFolder(folder);
		if (node == null) {
			// Special cases:
			if (isMissingRightForFolder(folder)) {
				// 1) When the user does not have rights on the folder, the folder is still present, but not in the
				//    view, so we cannot use the tree to find the exercises
				return getExerciseListOrNoExerciseList(noRightExercises.get(folder), folder);
			} else {
				// 2) When "contentProvider" is from envers, the folder maybe was deleted meanwhile.
				return getExercisesForDeletedFolder(folder);
			}
		}

		final List<Exercise> exerciseList = new ArrayList<>();
		TreeNodeUtils.forEachOfType(node, Exercise.class, exerciseList::add);
		return getExerciseListOrNoExerciseList(exerciseList, folder);
	}

	/**
	 * This method transforms an empty exercise list into a one-element list with a dummy exercise.
	 */
	private List<Exercise> getExerciseListOrNoExerciseList(List<Exercise> list, ContentFolder folder) {
		if (!list.isEmpty()) {
			return list;
		} else {
			// Otherwise, the folder won't be shown in the view. This would prevent the user from unselecting it.
			Exercise dummy = new Exercise();
			dummy.setName(getLocalizedMessage("courseEditProvider.noExercises"));
			dummy.setFolder(folder);
			return List.of(dummy);
		}
	}

	private List<Exercise> getExercisesForDeletedFolder(ContentFolder folder) {
		// Since we cannot fetch an Exercise list for the folder, a dummy Exercise refers to the problem its title.
		// This dummy is shown as the only exercise in the folder
		Exercise dummy = new Exercise();
		dummy.setName(getLocalizedMessage("global.exercises") + " " + getLocalizedMessage("global.notAvailable.long"));
		dummy.setFolder(folder);
		return List.of(dummy);
	}

	/**
	 * Returns a list with all exercises that belong to the course. The listet is sorted according to the set
	 * {@link ECourseExercisesOrder} property.
	 */
	public List<Exercise> getAllExercisesSorted() {
		if (getContentProvider() == null) {
			return Collections.emptyList();
		}

		List<Exercise> allExercises = new ArrayList<>();
		for (ContentFolder folder : getContentProvider().getFolders()) {
			allExercises.addAll(getExercisesForFolder(folder));
		}
		// 1. We sort by the exercise order
		allExercises.sort(courseBusiness.getExerciseComparatorInCourse(getCourse(), null));
		// 2. We sort by the group key, this is necessary for folder groups
		// Note that we cannot use Comparator.thenComparing... because (1) returns a Comparator<AbstractExercise> and (2) uses a Comparator<Exercise>
		allExercises.sort(Comparator.comparing(this::getGroupingKeyForExercise));
		return allExercises;
	}

	/**
	 * Switches the usage of all Exercises and the usage of a concrete number of Exercises in the folder that belongs to
	 * the passed Exercise.
	 */
	public void switchAllExerciseSetting(Exercise exercise) {
		final ContentFolder folder = getFolderFromProviderContainingExercise(exercise);
		getContentProvider().switchAllExerciseSetting(folder, getExercisesForFolder(folder).size());
	}

	/**
	 * Returns a map of all contentFolders in the contentProvider. If a folder should use all childExercises map it with
	 * true. If a folder should only use some of the childExercises map it with false.
	 */
	public Map<ContentFolder, Boolean> getContentFoldersUsingAllChildExercises() {
		final var result = new HashMap<ContentFolder, Boolean>();
		for (Map.Entry<ContentFolder, Integer> folderEntry : getContentProvider().getFoldersMap().entrySet()) {
			result.put(folderEntry.getKey(), folderEntry.getValue() == ALL_EXERCISES);
		}
		return result;
	}

	/**
	 * Returns the folder to which the passed Exercise belongs. The folder does not have to be the direct parent folder
	 * of the Exercise but due to the tree hierarchy it must be in the Exercise breadcrumb.
	 */
	public ContentFolder getFolderFromProviderContainingExercise(Exercise exercise) {
		if (exercise.isTransient()) {
			// Special case: The Folder of the Exercise was deleted
			return exercise.getFolder();
		}

		for (ContentFolder folder : getContentProvider().getFolders()) {
			// TODO This solution may be somewhat slow. Another approach is to iterate over the Exercise's breadcrumb 
			// and check if the content provider contains any of the breadcrumb folders.
			List<Exercise> exercises = getExercisesForFolder(folder);
			if (exercises.contains(exercise)) {
				return folder;
			}
		}

		throw new IllegalStateException("All exercises in the course should have a corresponding folder selected.");
	}

	public String getGroupingKeyForExercise(Exercise exercise) {
		// We cannot take only the name because folder names may not be unique (see JACK/jack3-core#978)
		final ContentFolder folder = getFolderFromProviderContainingExercise(exercise);
		return folder.getName() + folder.getId();
	}

	// ------------------------------------
	// ---------- Computed Values ----------
	// -------------------------------------

	/**
	 * Counts all exercises that are currently part of the course.
	 */
	public long getExerciseCount() {
		if (getContentProvider() == null)
			return 0;
		return getContentProvider().getFoldersMap().entrySet().stream()
				.mapToInt(this::getExerciseCountForFolderEntry)
				.sum();
	}

	private int getExerciseCountForFolderEntry(Map.Entry<ContentFolder, Integer> entry) {
		if (entry.getValue() == ALL_EXERCISES) {
			List<Exercise> exercisesInThisFolder = getExercisesForFolder(entry.getKey());
			if (exercisesInThisFolder.stream().anyMatch(Exercise::isTransient)) {
				// Folders with transient exercises were meanwhile deleted and are ignored
				return 0;
			}
			return exercisesInThisFolder.size();
		} else {
			return entry.getValue();
		}
	}

	/**
	 * Counts the currently selected folders.
	 */
	public long getFolderCount() {
		if (getContentProvider() == null)
			return 0;
		return getContentProvider().getFoldersMap().size();
	}

	/**
	 * Returns "All Exercises" if the Folder that belongs to the Exercise currently uses all exercises, otherwise "n
	 * Exercises" where 'n' is the number of selected exercises.
	 */
	public String getChangeNumberOfExercisesButtonText(final Exercise exercise) {
		if (getContentProvider() == null)
			return null;
		final ContentFolder folder = getFolderFromProviderContainingExercise(exercise);
		int exerciseCount = getContentProvider().getFoldersMap().get(folder);
		switch (exerciseCount) {
		case ALL_EXERCISES:
			return getLocalizedMessage("courseEdit.allExercises");
		case 1:
			return "1 " + getLocalizedMessage("global.exercise");
		default:
			return exerciseCount + " " + getLocalizedMessage("global.exercises");
		}
	}

	public int getMaxExercises(final Exercise exercise) {
		return getExercisesForFolder(getFolderFromProviderContainingExercise(exercise)).size();
	}

	public boolean isSpinnerRendered(final Exercise exercise) {
		final ContentFolder folder = getFolderFromProviderContainingExercise(exercise);
		return getContentProvider().getFoldersMap().get(folder) != ALL_EXERCISES;
	}

	public String getFolderName(final Exercise exercise) {
		return getFolderFromProviderContainingExercise(exercise).getName();
	}

	public boolean isMissingRightForFolder(final ContentFolder folder) {
		return noRightExercises.containsKey(folder);
	}

	public boolean isMissingRightForExercise(final Exercise exercise) {
		return noRightExercises.containsKey(getFolderFromProviderContainingExercise(exercise));
	}

	/**
	 * Evaluates if and what breadcrumb is shown at an exercise in the right view.
	 */
	public String getShownExerciseBreadcrumb(Exercise exercise) {
		if (exercise.isTransient()) {
			// Special case: The Folder of the Exercise was deleted
			return null;
		}

		final var folderFromProvider = getFolderFromProviderContainingExercise(exercise);
		final var folderFromExercise = exercise.getFolder();
		if (folderFromProvider.equals(folderFromExercise)) {
			// The exercise is directly in a selected folder
			return null;
		} else {
			// The exercise is NOT directly in a selected folder
			// We show the user in which sub folder (relative to the selected folder) the exercise is
			final var breadcrumbFromProvider = getPathAsString(folderFromProvider);
			final var breadcrumbFromExercise = getPathAsString(folderFromExercise);

			Pattern pattern = Pattern.compile("\\ (.*)$");
			Matcher matcher = pattern.matcher(breadcrumbFromProvider);
			String result = "";
			if (matcher.find()) {
				result = matcher.group(0);
				result = result.stripLeading();
				result = result.replace("\\", "\\\\");
			} else {
				throw new IllegalStateException("Something went wrong with the breadcrumb generation.");
			}

			// Add ... to indicate that this path is relative
			return "... " + breadcrumbFromExercise.replaceAll("(.*) " + result, "");
		}
	}
}
