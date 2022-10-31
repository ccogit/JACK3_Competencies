package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ExerciseSubmissionsPage {

	private static final String SUBMISSION_COUNT = "exerciseSubmissions:submissionCount";
	private static final String TESTSUBMISSION_COUNT = "exerciseSubmissions:testSubmissionCount";

	private static final String DELETE_ALL_SUBMISSIONS_BUTTON = "exerciseSubmissions:cbDeleteAllSubmissions";
	private static final String DELETE_ALL_SUBMISSIONS_YES = "exerciseSubmissions:btDeleteSubmissionsYes";

	private static final String CSV_DOWNLOAD_BUTTON = "exerciseSubmissions:csvDownloadButton";
	private static final String XLSX_DOWNLOAD_BUTTON = "exerciseSubmissions:excelDownloadButton";

	private static final String BREADCRUMP_ID = "exerciseSubmissions:breadCrumbExerciseSubmissions";

	private static final String EXERCISE_SUBMISSION_DATA = "exerciseSubmissions:dtExerciseSubmission_data";

	public static int getNumberOfStudentSubmissions() {
		waitClickable(By.id(SUBMISSION_COUNT));
		return Integer.parseInt(find(SUBMISSION_COUNT).getText());
	}

	public static int getNumberOfTestSubmissions() {
		waitClickable(By.id(TESTSUBMISSION_COUNT));
		return Integer.parseInt(find(TESTSUBMISSION_COUNT).getText());
	}

	public static void goBackToExercise() {
		waitClickable(By.id(BREADCRUMP_ID));

		//Filter the Breadcrump
		List<WebElement> childs = findChildren(find(BREADCRUMP_ID)).stream()
				.filter(element -> element.getTagName().equals("li") && "menuitem".equals(element.getAttribute("role")))
				.collect(Collectors.toList());
		
		waitClickable(childs.get(childs.size() - 2));
		click(childs.get(childs.size() - 2));
		
		waitUntilActionBarIsNotActive();

		//wait until the title of the exercise appears
		ExerciseEditPage.getTitle();
	}

	public static List<WebElement> getAllEntriesOfUser(String userName) {
		waitClickable(By.id(EXERCISE_SUBMISSION_DATA));

		List<WebElement> rows = findChildren(find(EXERCISE_SUBMISSION_DATA)).stream()
				.filter(element -> "row".equals(element.getAttribute("role")) && element.getText().contains(userName))
				.collect(Collectors.toList());

		return rows;
	}

	public static WebElement getFirstEntryOfUser(String userName) {
		return getAllEntriesOfUser(userName).get(0);
	}

	public static void openExercisesSubmissionDetails(WebElement row) {
		waitClickable(By.id(EXERCISE_SUBMISSION_DATA));
		row.findElement(By.className("ui-linkbutton")).click();
	}

	public static void removeSingleSubmission(WebElement row) {
		List<WebElement> elements = findChildren(row);
		for (int i = elements.size() - 1; i >= 0; i--) {
			if ("submit".equals(elements.get(i).getAttribute("type"))) {
				elements.get(i).click();
				return;
			}
		}
	}

	public static void removeAllTestSubmissions() {
		waitClickable(By.id(DELETE_ALL_SUBMISSIONS_BUTTON));
		click(find(DELETE_ALL_SUBMISSIONS_BUTTON));
		waitClickable(By.id(DELETE_ALL_SUBMISSIONS_YES));
		click(find(DELETE_ALL_SUBMISSIONS_YES));
	}
}
