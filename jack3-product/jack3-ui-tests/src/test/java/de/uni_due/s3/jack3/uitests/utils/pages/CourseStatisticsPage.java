package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;

public class CourseStatisticsPage {

	private static final String NUMBER_OF_PARTICIPANTS = "courseStatisticsMainForm:participants";
	private static final String NUMBER_OF_SUBMISSIONS = "courseStatisticsMainForm:courseSubmissions";
	private static final String NUMBER_OF_TESTSUBMISSIONS = "courseStatisticsMainForm:testSubmissionsHint";
	private static final String BREADCRUMB_ID = "courseStatisticsMainForm:breadCrumbCourseStatistics";
	private static final String DELETE_TEST_SUBMISSIONS = "courseStatisticsMainForm:details:courseRecordTable:deleteTestSubmissions";
	private static final String COURSE_RECORDS_DATA = "courseStatisticsMainForm:details:courseRecordTable_data";

	public static int getNumberOfParticipants() {
		waitClickable(By.id(NUMBER_OF_PARTICIPANTS));
		return Integer.parseInt(find(NUMBER_OF_PARTICIPANTS).getText());
	}

	public static int getNumberOfStudentSubmissions() {
		waitClickable(By.id(NUMBER_OF_SUBMISSIONS));
		return Integer.parseInt(find(NUMBER_OF_SUBMISSIONS).getText());
	}

	public static int getNumberOfTestSubmissions() {
		try {
			waitClickable(By.id(NUMBER_OF_TESTSUBMISSIONS));
			return Integer.parseInt(find(NUMBER_OF_TESTSUBMISSIONS).getText().replaceAll("[^0-9]", ""));
		} catch (TimeoutException e) {
			// This field is not visible if the counter is 0
			return 0;
		}
	}

	public static void goBackToCourse() {
		waitClickable(By.id(BREADCRUMB_ID));

		//Filter the Breadcrump
		List<WebElement> childs = findChildren(find(BREADCRUMB_ID)).stream()
				.filter(element -> element.getTagName().equals("li") && "menuitem".equals(element.getAttribute("role")))
				.collect(Collectors.toList());

		click(childs.get(childs.size() - 2));
	}

	public static List<WebElement> getAllEntriesOfUser(String userName) {
		waitClickable(By.id(COURSE_RECORDS_DATA));

		List<WebElement> rows = findChildren(find(COURSE_RECORDS_DATA)).stream()
				.filter(element -> "row".equals(element.getAttribute("role")) && element.getText().contains(userName))
				.collect(Collectors.toList());

		return rows;
	}

	public static WebElement getFirstEntryOfUser(String userName) {
		return getAllEntriesOfUser(userName).get(0);
	}

	public static void openCourseRecordSubmission(WebElement row) {
		waitClickable(By.id(COURSE_RECORDS_DATA));
		row.findElement(By.className("ui-linkbutton")).click();
		//wait until the page has been loaded
		CourseRecordSubmissionPage.getUserName();
	}

	private static final Pattern pattern = Pattern.compile("^\\d+ %$");

	public static int getPointsOfEntry(WebElement row) {
		waitClickable(By.id(COURSE_RECORDS_DATA));

		for (WebElement element : findChildren(row)) {
			String text = element.getText();
			if (!text.isEmpty() && pattern.matcher(text).matches()) {
				System.out.println("kilian: " + text);
				return Integer.parseInt(text.replace(" %", ""));
			}
		}

		throw new AssertionFailedError("It was not possible to find the Points for ths CourseRecord");
	}

	public static void rempoveAllTestSubmissions() {
		waitClickable(By.id(DELETE_TEST_SUBMISSIONS));
		click(find(DELETE_TEST_SUBMISSIONS));
	}


}
