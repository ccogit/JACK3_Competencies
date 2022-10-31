package de.uni_due.s3.jack3.uitests.utils;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.navigate;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.opentest4j.AssertionFailedError;

/**
 * Helpers for operations from the student's point of view.
 */
public final class Student {

	/**
	 * Open a course offer (only works with course offers on the first level of a folder) from overview
	 * 
	 * @param folderIndex
	 *            Index of the folder
	 * @param courseOfferIndex
	 *            Index of the course offer in the folder
	 */
	public static void openCourseOffer(int folderIndex, int courseOfferIndex) {

		navigate(JackUrl.AVAILABLE_COURSES);

		Time.waitVisible(By.id("treeForm:courseOfferTree"));
		final String folderID = String.format("treeForm:courseOfferTree:%s", folderIndex);
		final String courseOfferID = String.format("treeForm:courseOfferTree:%s_%s:studentOfferName",
				folderIndex, courseOfferIndex);

		try {
			// Open course offer details page
			WebElement courseOffer = Driver.get().findElement(By.id(courseOfferID));
			courseOffer.click();

		} catch (Exception e) {

			// Open folder
			WebElement folderElement = Driver.get().findElement(By.id(folderID)).findElements(Find.ALL).get(0);
			Click.clickWithAjax(folderElement);

			try {
				// Open course offer details page
				WebElement courseOffer = Driver.get().findElement(By.id(courseOfferID));
				Click.clickWithRedirect(courseOffer);
			} catch (Exception e2) {
				String screenshot = Misc.makeScreenshot("open-course-offer");
				throw new Error(String.format("Course offer %s_%s was not present in overview. " + screenshot,
						folderIndex, courseOfferIndex), e2);
			}

		}
	}

	/**
	 * Start an exercise from course overview
	 * 
	 * @param exerciseIndex
	 *            Exercise index as presented in the exercises table
	 */
	public static void openExercise(int exerciseIndex) {
		String exerciseID = String.format("showCourseRecordMainForm:dtExercises:%s:linkCourseExercise", exerciseIndex);
		try {
			// Open the exercise with the given id
			Click.clickWithRedirect(By.id(exerciseID));
		} catch (org.openqa.selenium.NoSuchElementException e) {
			throw new AssertionFailedError(String.format("Exercise %s was not present.", exerciseIndex), e);
		}
	}

	/**
	 * Restart an exercise from course overview
	 *
	 * @param exerciseIndex
	 *            Exercise index as presented in the exercises table
	 */
	public static void restartExercise(int exerciseIndex) {
		String id = String.format("showCourseRecordMainForm:dtExercises:%s:linkCourseExerciseRestartCourse",
				exerciseIndex);
		try {
			// Restart the exercise with the given id
			Click.clickWithRedirect(By.id(id));
		} catch (org.openqa.selenium.NoSuchElementException e) {
			throw new AssertionFailedError(String.format("Exercise %s was not present.", exerciseIndex), e);
		}
	}

	/**
	 * Get all stages from exercise player view
	 * 
	 * @return List of found elements, sorted as shown
	 */
	public static List<WebElement> getStages() {
		return Driver.get()
				.findElement(By.id("showCourseRecordMainForm:stages"))
				.findElements(By.xpath("./div"));
	}

	/**
	 * Get all stages from submission view
	 * 
	 * @return List of found elements, sorted as shown
	 */
	public static List<WebElement> getSubmissionStages() {
		return Driver.get()
				.findElement(By.id("submissionDetails:stages"))
				.findElements(By.xpath("./div"));
	}

	/**
	 * Leave the course
	 */
	public static void leaveCourse() {
		// Click on "Quit course"
		find(By.id("showCourseRecordMainForm:cbExitSubmission")).click();

		// Click on "Yes, quit course and go back to main page"
		waitVisible(By.id("showCourseRecordMainForm:globalConfirmDialog"), "Close dialog was not visible!");
		Click.clickWithRedirect(By.id("showCourseRecordMainForm:confirmOk"));
	}

	/**
	 * Select a mc answer option with the given text (exakt match!)
	 */
	public static void mcSelectAnswer(WebElement stage, String text) {
		Find.findChildElementByTagAndText(stage, "label", text).click();
	}

	/**
	 * Scroll to the page's end
	 */
	public static void scrollDown() {
		((JavascriptExecutor) Driver.get()).executeScript("window.scrollTo(0, document.body.scrollHeight)");
	}

	/**
	 * Scroll to a web element with the passed id
	 */
	public static void scrollToElement(String id) {
		// Scrolls to the element's bottom
		((JavascriptExecutor) Driver.get())
				.executeScript("document.getElementById('" + id + "').scrollIntoView(false)");
	}

	/**
	 * Submit a stage.
	 */
	public static void submitAndWait(WebElement stage) {
		// Submit stage
		Find.findButtonByText(stage, I18nHelper.SUBMIT).click();
		// Wait until stage is submitted (this could take a while)
		Time.wait(
				ExpectedConditions
						.not(ExpectedConditions.elementToBeClickable(Find.findButtonByText(stage, I18nHelper.SUBMIT))),
				10, "Stage was not successfully submitted.");

	}

	/**
	 * Erase the current stage submission
	 */
	public static void eraseStageAndWait(WebElement stage) {
		// Submit stage
		Find.findButtonByText(stage, I18nHelper.ERASE_SUBMISSION).click();
		// Wait until stage is submitted
		waitClickable(Find.findButtonByText(stage, I18nHelper.SUBMIT));
	}

	/**
	 * Request a hint for a given stage
	 */
	public static void requestHint(WebElement stage) {
		try {
			Find.findButtonByText(stage, I18nHelper.HINT).click();
		} catch (NoSuchElementException e) {
			fail("Requesting a hint was not possible!");
		}
	}

	/**
	 * Fill a formula editor field
	 */
	public static void fillFormulaEditorField(WebElement stage, int fieldIndex, CharSequence keys) {
		final WebElement formulaEditorField = stage.findElements(By.tagName("canvas")).get(fieldIndex);
		new Actions(Driver.get())
				.moveToElement(stage)
				.moveToElement(formulaEditorField)
				// Double-click is performed to open and close the formula editor field
				.click()
				.click()
				.sendKeys(keys)
				.build().perform();
	}

	public static void fillInputField(WebElement stage, int fieldIndex, CharSequence keys) {
		stage.findElements(By.className("ui-inputfield"))
			.get(fieldIndex)
			.sendKeys(keys);
	}
}
