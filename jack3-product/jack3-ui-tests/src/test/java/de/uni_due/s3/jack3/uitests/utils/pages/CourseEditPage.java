package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithAjax;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithRedirect;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Misc.waitUntilPageHasLoaded;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitPresent;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.openqa.selenium.By;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Time;

public class CourseEditPage {

	private static final String SAVE_BUTTON_ID = "courseEditMainForm:saveCourse";
	private static final String TEST_BUTTON_ID = "courseEditMainForm:testCourse";
	private static final String TITLE_ID = "courseEditMainForm:courseName_display";
	private static final String TITLE_INPUT = "courseEditMainForm:courseNameInput";
	private static final String LANGUAGE_LABEL_ID = "courseEditMainForm:language_label";
	private static final String INTERNAL_NOTES_ID = "courseEditMainForm:inputInternalDescription";

	private static final String REVISIONS_ID = "courseEditMainForm:rev";

	private static final String SCORING_TYPE_LABEL = "courseEditMainForm:courseSettingsScoringSelectOne_label";

	private static final String CONTENT_SELECTION_LABEL = "courseEditMainForm:contentSelection_label";
	private static final String CHANGE_CONTENT_SELECTION_LABEL = "courseEditMainForm:cbConfirmYes";

	private static final String FROZEN_REVISIONS_DROPDOWN = "courseEditMainForm:frozenRevisionsDropDown_label";
	private static final String FROZEN_REVISIONS_ITEMS = "courseEditMainForm:frozenRevisionsDropDown_items";

	private static final String OPEN_STATISTICS = "courseEditMainForm:toMoreCourseStatistics";

	private static final String SUBMISSION_COUNT_ID = "courseEditMainForm:courseSubmissions";
	private static final String TESTSUBMISSION_COUNT_ID = "courseEditMainForm:testSubmissionsHint";

	public static void saveCourse() {
		waitClickable(By.id(SAVE_BUTTON_ID));
		int oldRevisionNumber = getRevisionNumber();
		find(SAVE_BUTTON_ID).click();
		//wait until saving is completed
		String newRevisionNumber = Integer.toString(oldRevisionNumber + 1);
		Time.wait(ExpectedConditions.textToBe(By.id(REVISIONS_ID), newRevisionNumber),
				"The Revision Count doesn't show the right number");
	}

	public static void testCourse() {
		waitClickable(By.id(TEST_BUTTON_ID));
		clickWithRedirect(By.id(TEST_BUTTON_ID));
	}

	public static void openFrozenRevision(int frozenRevisionNumber) {
		waitClickable(By.id(FROZEN_REVISIONS_DROPDOWN));
		find(FROZEN_REVISIONS_DROPDOWN).click();
		waitClickable(By.id(FROZEN_REVISIONS_ITEMS));
		find(FROZEN_REVISIONS_ITEMS.replace("items", "" + frozenRevisionNumber)).click();
	}

	public static void setCourseTitle(String title) {
		waitClickable(By.id(TITLE_ID));
		find(TITLE_ID).click();
		waitClickable(By.id(TITLE_INPUT));
		find(TITLE_INPUT).sendKeys(title + Keys.ENTER);
	}

	public static String getTitle() {
		waitClickable(By.id(TITLE_ID));
		return find(TITLE_ID).getText();
	}

	/**
	 * Changes the Language of the Course
	 * 
	 * @param language
	 *            use "de" for german and "en" for english
	 */
	public static void changeLanguage(String language) {
		waitClickable(By.id(LANGUAGE_LABEL_ID));
		find(LANGUAGE_LABEL_ID).click();
		waitClickable(By.id(LANGUAGE_LABEL_ID.replace("label", "0")));

		switch (language) {
		case "de":
			find(LANGUAGE_LABEL_ID.replace("label", "0")).click();
			break;
		case "en":
			find(LANGUAGE_LABEL_ID.replace("label", "1")).click();
			break;
		default:
			throw new InvalidArgumentException(
					"The given String '" + language + "' could not be recognized. Please use 'de' or 'en'");
		}
	}

	public static void setInternalNotes(String internalNotes) {
		waitClickable(By.id(INTERNAL_NOTES_ID));
		find(INTERNAL_NOTES_ID).sendKeys(internalNotes);
	}

	public static void setExternalDescription(String description) {
		final WebDriver driver = Driver.get();
		driver.switchTo().frame(find(By.tagName("iframe")));
		driver.findElement(By.cssSelector("body")).sendKeys(description);
		driver.switchTo().defaultContent();
	}

	public static int getRevisionNumber() {
		waitClickable(By.id(REVISIONS_ID));
		return Integer.parseInt(find(REVISIONS_ID).getText());
	}

	public static void openRevisions() {
		waitClickable(By.id(REVISIONS_ID));
		find(REVISIONS_ID).click();
	}

	public static void setScoringType(ECourseScoring scoringType) {
		waitClickable(By.id(SCORING_TYPE_LABEL));
		find(SCORING_TYPE_LABEL).click();
		waitClickable(By.id(SCORING_TYPE_LABEL.replace("label", "0")));

		switch (scoringType) {
		case LAST:
			find(By.id(SCORING_TYPE_LABEL.replace("label", "0"))).click();
			break;
		case BEST:
			find(By.id(SCORING_TYPE_LABEL.replace("label", "1"))).click();
			break;
		}
	}

	/**
	 * Changes the ContentType for the Course to FixedListExerciseProvider or FolderBasedExerciseProvider. Use this
	 * method if Course already have an provider but you want to change it. Don't use it if the Course doesn't have a
	 * Provider yet. In this case you want to use @setContentType
	 * 
	 * @param providerType
	 *            The type of the Provider (e.g FolderExerciseProvider.class)
	 */
	public static void changeContentType(Class<? extends AbstractExerciseProvider> providerType) {
		setContentType(providerType);
		waitClickable(By.id(CHANGE_CONTENT_SELECTION_LABEL));
		find(CHANGE_CONTENT_SELECTION_LABEL).click();
	}

	/**
	 * Set the ContentType for the Course to FixedListExerciseProvider or FolderExerciseProvider. Use this method
	 * only if the course doesn't have a provider. If you want to change the provider use @changeContentType
	 * 
	 * @param providerType
	 *            The type of the Provider (e.g FolderExerciseProvider.class)
	 */
	public static void setContentType(Class<? extends AbstractExerciseProvider> providerType) {
		waitClickable(By.id(CONTENT_SELECTION_LABEL));
		find(CONTENT_SELECTION_LABEL).click();
		waitClickable(By.id(CONTENT_SELECTION_LABEL.replace("label", "0")));

		if (providerType.equals(FixedListExerciseProvider.class)) {
			find(By.id(CONTENT_SELECTION_LABEL.replace("label", "1"))).click();
		} else if (providerType.equals(FolderExerciseProvider.class)) {
			find(By.id(CONTENT_SELECTION_LABEL.replace("label", "2"))).click();
		} else {
			find(By.id(CONTENT_SELECTION_LABEL.replace("label", "0"))).click();
		}
		
		waitUntilActionBarIsNotActive();
	}

	public static void openStatistics() {
		waitClickable(By.id(OPEN_STATISTICS));
		click(find(OPEN_STATISTICS));

		// At least the headerPart div element should be present when the page has been loaded
		waitPresent(By.id("courseStatisticsMainForm:headerPart"));
	}

	public static class RevisionTable {
		private static final String REVISION_LIST = "courseEditMainForm:dtRevisionList:";
		private static final String FREEZE_REVISION = ":cbfreezeRevision";
		private static final String OPEN_REVISION = ":cbSearchCourseRevision";

		public static void openRevision(int revisionNumber) {
			waitClickable(By.id(REVISION_LIST + revisionNumber + OPEN_REVISION));
			find(REVISION_LIST + revisionNumber + OPEN_REVISION).click();
		}

		public static void freezeRevision(int revisionNumber) {
			waitClickable(By.id(REVISION_LIST + revisionNumber + FREEZE_REVISION));
			WebElement freezeRevision = find(REVISION_LIST + revisionNumber + FREEZE_REVISION);
			clickWithAjax(freezeRevision);
		}
	}

	public static class RevisionPage {
		private static final String ACCEPT_AS_NEW = "courseEditMainForm:resetRevision";
		private static final String TO_LATEST_REVISION = "courseEditMainForm:jumpToCurrentRevision";

		private static final String FROZEN_TITLE = "courseEditMainForm:titelFrozenVersion";
		private static final String ACCEPT_FROZEN_TITLE_BUTTON = "courseEditMainForm:ajax";

		public static void acceptAsNew() {
			waitClickable(By.id(ACCEPT_AS_NEW));
			find(ACCEPT_AS_NEW).click();
			waitClickable(By.id(TEST_BUTTON_ID));
			waitUntilPageHasLoaded();
		}

		public static void GoToLatestRevision() {
			waitClickable(By.id(TO_LATEST_REVISION));
			find(TO_LATEST_REVISION).click();
		}

		public static void setFrozenTitle(String frozenTitle) {
			waitClickable(By.id(FROZEN_TITLE));
			find(FROZEN_TITLE).sendKeys(frozenTitle);
			find(ACCEPT_FROZEN_TITLE_BUTTON).click();
		}

		public static void checkThatUiIsDisabled() {
			waitNotClickable(By.id(INTERNAL_NOTES_ID));
			assertFalse(find(("courseEditMainForm:courseSettingsScoringSelectOne_input")).isEnabled());
			assertFalse(find("courseEditMainForm:language_input").isEnabled());
		}
	}

	public static int getNumberOfStudentSubmissions() {
		waitClickable(By.id(SUBMISSION_COUNT_ID));
		return Integer.parseInt(find(SUBMISSION_COUNT_ID).getText());
	}

	public static int getNumberOfTestSubmissions() {
		try {
			waitClickable(By.id(TESTSUBMISSION_COUNT_ID));
			return Integer.parseInt(find(TESTSUBMISSION_COUNT_ID).getText().replaceAll("[^0-9]", ""));
		} catch (TimeoutException e) {
			// This field is not visible if the counter is 0
			return 0;
		}
	}

}