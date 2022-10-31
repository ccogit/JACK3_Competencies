package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.uitests.utils.Driver;
public class CoursePlayerPage {

	private static final String LEAVE_COURSE = "showCourseRecordMainForm:cbExitSubmission";
	private static final String CONFIRM_QUITE = "showCourseRecordMainForm:confirmOk";

	private static final String TEST_DESCRIPTION = "courseTestForm:panelDescription_content";
	private static final String STUDENT_DESCRIPTION = "showCourseRecordMainForm:panelDescription_content";

	private static final String BACK_TO_COURSE = "courseTestForm:backToCourse";
	private static final String BACK_TO_COURSEOFFER = "showCourseRecordMainForm:cbToCourseOffer";

	private static final String EXERCISE_TEST_TABLE = "courseTestForm:dtExercises_data";
	private static final String EXERCISE_STUDENT_TABLE = "showCourseRecordMainForm:dtExercises_data";
	private static final String EXERCISE_NAME = "linkCourseExercise";

	private static boolean isTestedCourse() {
		waitClickable(By.id("menubar"));
		String url = Driver.get().getCurrentUrl();
		return url.contains("courseTest");
	}

	private static String getTableId() {
		return isTestedCourse() ? EXERCISE_TEST_TABLE : EXERCISE_STUDENT_TABLE;
	}

	public static void goBack() {
		if (isTestedCourse()) {
			waitClickable(By.id(BACK_TO_COURSE));
			click(find(BACK_TO_COURSE));
		} else {
			waitClickable(By.id(BACK_TO_COURSEOFFER));
			click(find(BACK_TO_COURSEOFFER));
		}
	}

	public static void quiteCourse() {
		waitClickable(By.id(LEAVE_COURSE));
		click(find(LEAVE_COURSE));
		waitClickable(By.id(CONFIRM_QUITE));
		click(find(CONFIRM_QUITE));
	}

	public static String getDescription() {
		final String DESCRIPTION = isTestedCourse() ? TEST_DESCRIPTION : STUDENT_DESCRIPTION;
		waitClickable(By.id(DESCRIPTION));
		return find(DESCRIPTION).getText();
	}

	public static WebElement getExerciseRow(String exerciseName) {
		final String TABLE = getTableId();

		waitClickable(By.id(TABLE));
		List<WebElement> rows = findChildren(find(TABLE)).stream().filter(ele -> "tr".equals(ele.getTagName()))
				.collect(Collectors.toList());
		for (int i = 0; i < rows.size(); i++) {
			if (find(TABLE.replace("_data", "") + ":" + i + ":" + EXERCISE_NAME).getText()
					.equals(exerciseName)) {
				return rows.get(i);
			}
		}

		throw new AssertionFailedError("The Exercise '" + exerciseName + "' could not be found");
	}

	public static void openExercise(WebElement exerciseRow) {
		int rowNumber = Integer.parseInt(exerciseRow.getAttribute("data-ri"));
		final String ID = getTableId().replace("_data", "") + ":" + rowNumber + ":" + EXERCISE_NAME;
		waitClickable(By.id(ID));
		click(find(ID));
	}

	public static void openExercise(String exerciseName) {
		openExercise(getExerciseRow(exerciseName));
	}

	public static String getScoreOfExercise(WebElement exerciseRow) {
		List<WebElement> gridcells = findChildren(exerciseRow).stream().filter(ele -> "td".equals(ele.getTagName()))
				.collect(Collectors.toList());
		return gridcells.get(gridcells.size() - 2).getText();
	}

	public static String getScoreOfExercise(String exerciseName) {
		return getScoreOfExercise(getExerciseRow(exerciseName));
	}

	public static List<String> getShownExerciseNames() {
		final String TABLE = getTableId();

		waitClickable(By.id(TABLE));
		return findChildren(find(TABLE)).stream() //Create a Stream of all WebElements in the ExerciseTable
				.filter(ele -> "tr".equals(ele.getTagName())) //filter all rows
				.map(ele -> find(TABLE.replace("_data", ":") + ele.getAttribute("data-ri") + ":" + EXERCISE_NAME)
						.getText()) //map every row to the corresponding exercise name
				.collect(Collectors.toList());//
	}
}
