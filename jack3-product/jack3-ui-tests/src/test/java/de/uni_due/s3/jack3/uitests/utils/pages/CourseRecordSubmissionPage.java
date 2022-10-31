package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;
public class CourseRecordSubmissionPage {

	private static final String USER_ID = "courseRecordSubmissionsMainForm:student";
	private static final String RESULT_ID = "courseRecordSubmissionsMainForm:result";
	private static final String START_TIME = "courseRecordSubmissionsMainForm:startTime";
	private static final String LAST_VISIT = "courseRecordSubmissionsMainForm:lastVisit";

	private static final String AVAILABLE_SUBMISSIONS = "courseRecordSubmissionsMainForm:availableSubmissions_data";

	private static final String OPEN_SUBMISSION_BUTTON = "btnViewSubmissionId";

	private static final String BREADCRUMB_COURSE = "courseRecordSubmissionsMainForm:course";
	private static final String BREADCRUMB_COURSEOFFER = "courseRecordSubmissionsMainForm:courseOffer";

	public static String getUserName() {
		waitClickable(By.id(USER_ID));
		return find(USER_ID).getText();
	}

	public static int getResultPoints() {
		waitClickable(By.id(RESULT_ID));
		return Integer.parseInt(find(RESULT_ID).getText().replace(" %", ""));
	}

	public static String getStartTime() {
		waitClickable(By.id(START_TIME));
		return find(START_TIME).getText();
	}

	public static String getLastVisitTime() {
		waitClickable(By.id(LAST_VISIT));
		return find(LAST_VISIT).getText();
	}

	public static WebElement getSubmissionByExerciseName(String exerciseName) {
		waitClickable(By.id(AVAILABLE_SUBMISSIONS));
		List<WebElement> submissions = findChildren(find(AVAILABLE_SUBMISSIONS)).stream()
				.filter(ele -> "tr".equals(ele.getTagName())).collect(Collectors.toList());

		for (WebElement row : submissions) {
			WebElement title = findChildren(row).stream().filter(ele -> "td".equals(ele.getTagName())).findFirst()
					.orElseThrow(AssertionFailedError::new);
			if (title.getText().contains(exerciseName)) {
				return row;
			}
		}

		throw new AssertionFailedError("The Submission for the Exercise '"+exerciseName+"' could not be found");
	}

	public static int getPointsForSubmission(WebElement submissionRow) {
		WebElement points = findChildren(submissionRow).stream().filter(ele -> "td".equals(ele.getTagName()))
				.collect(Collectors.toList()).get(2);

		return Integer.parseInt(points.getText().replace(" %", ""));
	}

	public static int getPointsForSubmission(String exerciseName) {
		return getPointsForSubmission(getSubmissionByExerciseName(exerciseName));
	}

	public static void openSubmission(WebElement submissionRow) {
		int rowNumber = Integer.parseInt(submissionRow.getAttribute("data-ri"));

		click(find(AVAILABLE_SUBMISSIONS.replace("_data", "") + ":" + rowNumber + ":" + OPEN_SUBMISSION_BUTTON));

		//wait until the page has been loaded
		SubmissionDetailsPage.getStartTime();
	}

	public static void openSubmission(String exerciseName) {
		openSubmission(getSubmissionByExerciseName(exerciseName));
	}

	public static boolean checkIfBreadCrumbToCourseIsShown() {
		waitClickable(By.id(USER_ID));
		try {
			find(BREADCRUMB_COURSE);
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	public static boolean checkIfBreadCrumbToCourseOfferIsShown() {
		waitClickable(By.id(USER_ID));
		try {
			find(BREADCRUMB_COURSEOFFER);
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	public static void checkThatCourseBreadCrumbOnlyContainsGivenStrings(String... strings) {
		waitClickable(By.id(BREADCRUMB_COURSE));
		List<WebElement> listElements = find(BREADCRUMB_COURSE).findElements(By.tagName("li"));

		assertEquals(strings.length, 1 + (listElements.size() / 2),
				"The Breadcrump of the Course has not the expected length");

		int listCounter = 0;
		for (int i = 0; listCounter < listElements.size(); i++) {
			assertEquals(listElements.get(listCounter).getText(), strings[i]);
			listCounter += 2; // we skip the "/" symbol
		}
	}

	public static void checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings(String... strings) {
		waitClickable(By.id(BREADCRUMB_COURSEOFFER));
		List<WebElement> listElements = find(BREADCRUMB_COURSEOFFER).findElements(By.tagName("li"));

		assertEquals(strings.length, 1 + (listElements.size() / 2),
				"The Breadcrump of the CourseOffer has not the expected length");

		int listCounter = 0;
		for (int i = 0; listCounter < listElements.size(); i++) {
			assertEquals(listElements.get(listCounter).getText(), strings[i]);
			listCounter += 2; // we skip the "/" symbol
		}
	}

	/**
	 * @param map the map contains the strings of the breadcrumb which shall be checked.
	 * 	If a string is mapped to true the element should be enabled in the breadcrumb to the course.
	 * 	If a string is mapped to false the element should be disabled in the breadcrumb to the course.
	 */
	public static void checkThatCourseBreadCrumbHasTheCorrectElementsEnabled(Map<String,Boolean> map) {
		waitClickable(By.id(BREADCRUMB_COURSE));
		List<WebElement> listElements = find(BREADCRUMB_COURSE).findElements(By.tagName("li"));
		List<String> elementTexts = listElements.stream().map(ele -> ele.getText().strip().toLowerCase())
				.collect(Collectors.toList());

		for (String string : map.keySet()) {
			assertTrue(elementTexts.contains(string.toLowerCase().strip()),
					"the Element '" + string + "' couldn't be found in the breadcrumb.");

			if(map.get(string)) {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					throw new AssertionError("the element '"+string+"' should be enabled in the breadcrumb but wasn't");
				}
			}else {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					//we shouldn't find the link element.
					continue;
				}
				throw new AssertionError("the element '"+string+"' should be disabled in the breadcrumb but wasn't");

			}

		}
	}

	/**
	 * @param map the map contains the strings of the breadcrumb which shall be checked.
	 * 	If a string is mapped to true the element should be enabled in the breadcrumb to the courseOffer.
	 * 	If a string is mapped to false the element should be disabled in the breadcrumb to the courseOffer.
	 */
	public static void checkThatCourseOfferBreadCrumbHasTheCorrectElementsEnabled(Map<String,Boolean> map) {
		waitClickable(By.id(BREADCRUMB_COURSEOFFER));
		List<WebElement> listElements = find(BREADCRUMB_COURSEOFFER).findElements(By.tagName("li"));
		List<String> elementTexts = listElements.stream().map(ele -> ele.getText().strip().toLowerCase())
				.collect(Collectors.toList());

		for (String string : map.keySet()) {
			assertTrue(elementTexts.contains(string.toLowerCase().strip()),
					"the Element '" + string + "' couldn't be found in the breadcrumb.");

			if(map.get(string)) {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					throw new AssertionError("the element '"+string+"' should be enabled in the breadcrumb but wasn't");
				}
			}else {
				WebElement element = listElements.get(elementTexts.indexOf(string.toLowerCase().strip()));
				try {
					element.findElement(By.tagName("a"));
				}catch (NoSuchElementException e) {
					//we shouldn't find the link element.
					continue;
				}
				throw new AssertionError("the element '"+string+"' should be disabled in the breadcrumb but wasn't");

			}

		}
	}

	public static void openCourse() {
		waitClickable(By.id(BREADCRUMB_COURSE));
		List<WebElement> listElements = find(BREADCRUMB_COURSE).findElements(By.tagName("li"));

		listElements.get(listElements.size() - 1).click();

		//wait until the course page has been loaded
		CourseEditPage.getTitle();
	}

}
