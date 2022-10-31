package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

public class SubmissionDetailsPage {

	private final static String AUTHOR_ID = "submissionDetails:student";
	private final static String START_TIME_ID = "submissionDetails:startTime";
	private final static String RESULT_POINTS_ID = "submissionDetails:result";

	private final static String LOG_ENTRIES_ID = "submissionDetails:logView:dtSubmissionLogEntries";
	private final static String LOG_ENTRIES_TABLE = "submissionDetails:logView:dtSubmissionLogEntries_data";

	private final static String COURSE_BREADCRUMB = "submissionDetails:courseBreadCrumb";
	private final static String EXERCISE_BREADCRUMB = "submissionDetails:exerciseBreadCrumb";
	private final static String COURSEOFFER_BREADCRUMB = "submissionDetails:courseOfferBreadCrumb";

	public static String getAuthor() {
		waitClickable(By.id(AUTHOR_ID));
		return find(AUTHOR_ID).getText();
	}

	public static String getStartTime() {
		waitClickable(By.id(START_TIME_ID));
		return find(START_TIME_ID).getText();
	}

	public static int getResultPoints() {
		waitClickable(By.id(RESULT_POINTS_ID));
		return Integer.parseInt(find(RESULT_POINTS_ID).getText().replace(" %", ""));
	}

	public static List<WebElement> getLogEntryRows() {
		waitClickable(By.id(LOG_ENTRIES_TABLE));

		return findChildren(find(LOG_ENTRIES_TABLE)).stream()
				.filter(Objects::nonNull)
				.filter(element -> {
					try {
					return "tr".equals(element.getTagName()) && "row".equals(element.getAttribute("role"));
					} catch (RuntimeException e) {
						// Some Elements causes the following Exception:
						// java.lang.RuntimeException: unexpected invocation exception during invocation of org.openqa.selenium.WebElement#getTagName(), on target 'null': null
						// If this happens we simply return false
						return false;
					}
				})
				.collect(Collectors.toList());
	}

	public static int getNumberOfLogEntries() {
		return getLogEntryRows().size();
	}

	public static void checkThatCourseBreadCrumbOnlyContainsGivenStrings(String... strings) {
		waitClickable(By.id(COURSE_BREADCRUMB));
		List<WebElement> listElements = find(COURSE_BREADCRUMB).findElements(By.tagName("li"));

		assertEquals(strings.length, 1 + (listElements.size() / 2),
				"The Breadcrumb of the Course has not the expected length");

		int listCounter = 0;
		for (int i = 0; listCounter < listElements.size(); i++) {
			assertEquals(listElements.get(listCounter).getText(), strings[i]);
			listCounter += 2; // we skip the "/" symbol
		}
	}
	
	public static void checkThatExerciseBreadCrumbOnlyContainsGivenStrings(String... strings) {
		waitClickable(By.id(EXERCISE_BREADCRUMB));
		List<WebElement> listElements = find(EXERCISE_BREADCRUMB).findElements(By.tagName("li"));

		assertEquals(strings.length, 1 + (listElements.size() / 2),
				"The Breadcrumb of the Course has not the expected length");

		int listCounter = 0;
		for (int i = 0; listCounter < listElements.size(); i++) {
			assertEquals(listElements.get(listCounter).getText(), strings[i]);
			listCounter += 2; // we skip the "/" symbol
		}
	}

	public static void checkThatCourseOfferBreadCrumbOnlyContainsGivenStrings(String... strings) {
		waitClickable(By.id(COURSEOFFER_BREADCRUMB));
		List<WebElement> listElements = find(COURSEOFFER_BREADCRUMB).findElements(By.tagName("li"));

		assertEquals(strings.length, 1 + (listElements.size() / 2),
				"The Breadcrumb of the CourseOffer has not the expected length");

		int listCounter = 0;
		for (int i = 0; listCounter < listElements.size(); i++) {
			assertEquals(listElements.get(listCounter).getText(), strings[i]);
			listCounter += 2; // we skip the "/" symbol
		}
	}
	
	/**
	 * @param map the map contains the strings of the breadcrumb which shall be checked.
	 * 	If a string is mapped to true the element should be enabled in the breadcrumb to the courseOffer.
	 * 	If a string is mapped to false the element should be disabled in the breadcrumb to the courseOffer.
	 */
	public static void checkThatCourseOfferBreadCrumbHasTheCorrectElementsEnabled(Map<String,Boolean> map) {
		waitClickable(By.id(COURSEOFFER_BREADCRUMB));
		List<WebElement> listElements = find(COURSEOFFER_BREADCRUMB).findElements(By.tagName("li"));
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
	 * 	If a string is mapped to true the element should be enabled in the breadcrumb to the course.
	 * 	If a string is mapped to false the element should be disabled in the breadcrumb to the course.
	 */
	public static void checkThatCourseBreadCrumbHasTheCorrectElementsEnabled(Map<String,Boolean> map) {
		waitClickable(By.id(COURSE_BREADCRUMB));
		List<WebElement> listElements = find(COURSE_BREADCRUMB).findElements(By.tagName("li"));
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
	

}
