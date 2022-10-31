package de.uni_due.s3.jack3.uitests.utils.pages.exerciseproviders;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.uitests.utils.fragments.CourseEditExerciseTree;

public class FixedListExerciseProviderPage {

	private static final String NUMBER_OF_ADDED_EXERCISES = "courseEditMainForm:numOfCourseExercises";
	private static final String AVERAGE_DIFFICULTY = "courseEditMainForm:averageDifficulty";
	private static final String POINT_SUM = "courseEditMainForm:pointSum";

	private static final String AVAILABLE_EXERCISES = "courseEditMainForm:fixedAllocation_tree";
	private static final String SELECTED_EXERCISES = "courseEditMainForm:fixedAllocation_selectedExercisesTable_data";

	private static final String POINTS_FOR_SELECTED_EXERCISE = ":inputPoints_input";
	private static final String REMOVE_SELECTED_EXERCISE = ":cbDeleteSelectedExercise";
	private static final String OPEN_FROZEN_REVISION_PANEL = ":cbEditFrozenRevision";

	private static final String FROZEN_REVISION_FOR_EXERCISE = ":frozenRevisionForExercise_label";

	public static int getNumberOfAddedExercises() {
		waitClickable(By.id(NUMBER_OF_ADDED_EXERCISES));
		return Integer.parseInt(find(NUMBER_OF_ADDED_EXERCISES).getText());
	}

	public static float getAverageDifficulty() {
		waitClickable(By.id(AVERAGE_DIFFICULTY));
		return Float.parseFloat(find(AVERAGE_DIFFICULTY).getText());
	}

	public static int getTotalPoints() {
		waitClickable(By.id(POINT_SUM));
		return Integer.parseInt(find(POINT_SUM).getText());
	}

	public static void addExerciseWithName(String... breadcrumb) {
		waitClickable(By.id(AVAILABLE_EXERCISES));
		WebElement foundExercise = CourseEditExerciseTree.expandUpToElement(find(AVAILABLE_EXERCISES), breadcrumb);
		if (foundExercise == null) {
			throw new AssertionFailedError("Exercise '" + breadcrumb[breadcrumb.length - 1] + "' not found");
		}
		CourseEditExerciseTree.selectNodeViaCheckbox(foundExercise, true);
		// Otherwise checkbox is already checked.
	}

	public static void setPointsForSelectedExericse(String exerciseName, int points) {
		waitClickable(By.id(SELECTED_EXERCISES));
		int rowNumber = getRowNumberForSelectedExercises(exerciseName);
		find(SELECTED_EXERCISES.replace("_data", ":" + rowNumber) + POINTS_FOR_SELECTED_EXERCISE).sendKeys("" + points);
	}

	public static void removeSelectedExercise(String exerciseName) {
		waitClickable(By.id(SELECTED_EXERCISES));
		int rowNumber = getRowNumberForSelectedExercises(exerciseName);
		find(SELECTED_EXERCISES.replace("_data", ":") + rowNumber + REMOVE_SELECTED_EXERCISE).click();
	}

	public static void chooseFrozenRevisionForSelectedExercise(String exerciseName, int frozenRevisionNumber) {
		int rowNumber = openFrozenRevisionPanelForSelectedExercise(exerciseName);
		waitClickable(By.id(SELECTED_EXERCISES.replace("_data", ":") + rowNumber + FROZEN_REVISION_FOR_EXERCISE));
		click(find(SELECTED_EXERCISES.replace("_data", ":") + rowNumber + FROZEN_REVISION_FOR_EXERCISE));
		waitClickable(By.id(SELECTED_EXERCISES.replace("_data", ":") + rowNumber
				+ FROZEN_REVISION_FOR_EXERCISE.replace("label", "" + frozenRevisionNumber)));
		find(SELECTED_EXERCISES.replace("_data", ":") + rowNumber
				+ FROZEN_REVISION_FOR_EXERCISE.replace("label", "" + frozenRevisionNumber)).click();
	}

	private static int openFrozenRevisionPanelForSelectedExercise(String exerciseName) {
		waitClickable(By.id(SELECTED_EXERCISES));
		int rowNumber = getRowNumberForSelectedExercises(exerciseName);
		find(SELECTED_EXERCISES.replace("_data", ":") + rowNumber + OPEN_FROZEN_REVISION_PANEL).click();
		return rowNumber;
	}

	private static int getRowNumberForSelectedExercises(String selectedExerciseName) {
		List<WebElement> rows = findChildren(find(SELECTED_EXERCISES)).stream()
				.filter(element -> "row".equals(element.getAttribute("role"))).collect(Collectors.toList());

		for (WebElement row : rows) {
			String rowName = findChildren(row).stream()
					.filter(element -> "gridcell".equals(element.getAttribute("role"))).findFirst()
					.orElseThrow(() -> new AssertionFailedError(
							"The Exercise with the Name '" + selectedExerciseName + "' could not be found"))
					.getText();
			if (rowName.contains(selectedExerciseName)) {
				return Integer.parseInt(row.getAttribute("data-ri"));
			}
		}

		throw new AssertionFailedError("The Exercise with the name '" + selectedExerciseName + "'could not be found");
	}

}