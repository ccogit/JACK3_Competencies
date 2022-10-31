package de.uni_due.s3.jack3.uitests.utils.pages.stages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.uitests.utils.Driver;

public class JavaStagePage {

	private final String STAGE_INDEX;
	private final String TABS = "exerciseEdit:tabs0";

	/* ----- Content Tab ----- */
	private final String EXTERNAL_NAME = "exerciseEdit:tabs0:stageExternalNameInput";
	private final String EXERCISE_DESCRIPTION = "cke_exerciseEdit:tabs0:editor_0";
	private final String FILE_NAMES_TABLE = "exerciseEdit:tabs0:pgFileNames";
	private final String MIN_FILE_COUNT = "exerciseEdit:tabs0:minFileCount_input";
	private final String MAX_FILE_COUNT = "exerciseEdit:tabs0:maxFileCount_input";

	/* ----- Feedback Tab ----- */
	private final String STAGE_WEIGHT = "exerciseEdit:tabs0:stageWeight_input";
	private final String REPEAT_ON_MISSING_UPLOAD = "exerciseEdit:tabs0:repeatOnMissingUpload";
	private final String PROPAGATE_ON_INTERNAL_ERRORS = "exerciseEdit:tabs0:propagateOnInternalErrors";
	
	private final String ADD_STATIC_CHECKER = "exerciseEdit:tabs0:cdAddGreqlConfig";
	private final String STATIC_CHECKER_WEIGHT = "exerciseEdit:tabs0:checkerConfigurations:stepWeight_0_16_input"; //why do we have the 16?
	private final String ADD_DYNAMIC_CHECKER = "exerciseEdit:tabs0:cdAddTracingConfig";
	private final String ADD_METRICS_CONFIG = "exerciseEdit:tabs0:cdAddMetricsConfig";

	private final String ALLOW_SKIP = "exerciseEdit:tabs0:allowSkip";
	private final String SKIP_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:load_skipFeedback_InRawEditor";
	private final String SKIP_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:rawEditor_skipFeedback";

	public JavaStagePage(String stageIndex) {
		this.STAGE_INDEX = stageIndex;
	}

	public void navigateToContentTab() {
		navigateToTab("Aufgabeninhalt");
	}

	public void navigateToFeedbackTab() {
		navigateToTab("Feedback");
	}

	public void navigateToHintTab() {
		navigateToTab("Hinweise");
	}

	public void navigateToTransitionsTab() {
		navigateToTab("Verknüpfungen");
	}

	public void navigateToUpdateTab() {
		navigateToTab("Variablenupdates");
	}

	public void navigateToCheckerTab() {
		navigateToTab("Checker");
	}

	private void navigateToTab (String tabName) {
		WebElement contentTab = findChildElementByTagAndText(find(TABS.replace("0", STAGE_INDEX)), "a", tabName);
		waitClickable(contentTab);
		contentTab.click();

		waitUntilActionBarIsNotActive();
	}

	public void setExternalTitel(String titel) {
		String id = EXTERNAL_NAME.replace("0", STAGE_INDEX);

		waitClickable(By.id(id));
		find(id).sendKeys(titel);
	}

	public void setExerciseDescritptionText(String text) {
		WebDriver driver = Driver.get();
		driver.switchTo().frame(find(EXERCISE_DESCRIPTION.replaceAll("0", STAGE_INDEX)).findElement(By.tagName("iframe")));
		driver.findElement(By.cssSelector("body")).sendKeys(text);
		driver.switchTo().defaultContent();

		waitUntilActionBarIsNotActive();
	}
	
	public void setNeccessaryFileNames(String... textInput) {
		waitClickable(By.id(FILE_NAMES_TABLE.replaceAll("0", STAGE_INDEX)));
		WebElement textArea = find(FILE_NAMES_TABLE.replaceAll("0", STAGE_INDEX)).findElement(By.tagName("textarea"));
		for(String fileName : textInput) {
			textArea.sendKeys(fileName);
			textArea.sendKeys(Keys.ENTER);
		}
	}
	
	public void setAllowedFileNames(String... textInput) {
		waitClickable(By.id(FILE_NAMES_TABLE.replaceAll("0", STAGE_INDEX)));
		WebElement textArea = find(FILE_NAMES_TABLE.replaceAll("0", STAGE_INDEX)).findElements(By.tagName("textarea")).get(1);
		for(String fileName : textInput) {
			textArea.sendKeys(fileName);
			textArea.sendKeys(Keys.ENTER);
		}
	}
	
	public void setMinFileCount(int minFileCount) {
		waitClickable(By.id(MIN_FILE_COUNT.replace("0", STAGE_INDEX)));
		find(MIN_FILE_COUNT.replace("0", STAGE_INDEX)).sendKeys("" + Keys.BACK_SPACE + Keys.BACK_SPACE + minFileCount);
	}
	
	public void setMaxFileCount(int maxFileCount) {
		waitClickable(By.id(MAX_FILE_COUNT.replace("0", STAGE_INDEX)));
		find(MAX_FILE_COUNT.replace("0", STAGE_INDEX)).sendKeys("" + Keys.BACK_SPACE + Keys.BACK_SPACE + maxFileCount);
	}

	public void setStageWeight(int weight) {
		WebElement stageWeight = find(STAGE_WEIGHT.replace("0", STAGE_INDEX));
		stageWeight.sendKeys(Keys.BACK_SPACE +""+ Keys.BACK_SPACE + Keys.BACK_SPACE + weight); //use backspaces to be sure the previous input is deleted (.clear() doesnt work)
	}

	public void setRepeatOnMissingUpload(boolean repeatOnMissingUpload) {
		waitClickable(By.id(REPEAT_ON_MISSING_UPLOAD.replace("0", STAGE_INDEX)));
		if(repeatOnMissingUpload) {
			find(REPEAT_ON_MISSING_UPLOAD.replace("0", STAGE_INDEX)).findElement(By.tagName("span")).click();
		}else {
			find(REPEAT_ON_MISSING_UPLOAD.replace("0", STAGE_INDEX)).findElements(By.tagName("span")).get(1).click();
		}
	}
	
	public void setPropagateInternalErrors(boolean propagateInternalErrors) {
		waitClickable(By.id(PROPAGATE_ON_INTERNAL_ERRORS.replace("0", STAGE_INDEX)));
		if(propagateInternalErrors) {
			find(PROPAGATE_ON_INTERNAL_ERRORS.replace("0", STAGE_INDEX)).findElement(By.tagName("span")).click();
		}else {
			find(PROPAGATE_ON_INTERNAL_ERRORS.replace("0", STAGE_INDEX)).findElements(By.tagName("span")).get(1).click();
		}
	}
	
	public void addStaticChecker() {
		waitClickable(By.id(ADD_STATIC_CHECKER.replace("0", STAGE_INDEX)));
		find(ADD_STATIC_CHECKER.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
	}
	
	public void addDynamicChecker() {
		waitClickable(By.id(ADD_DYNAMIC_CHECKER.replace("0", STAGE_INDEX)));
		find(ADD_DYNAMIC_CHECKER.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
	}
	
	public void addMetricsConfig() {
		waitClickable(By.id(ADD_METRICS_CONFIG.replace("0", STAGE_INDEX)));
		find(ADD_METRICS_CONFIG.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
	}

	public void setAllowSkip (boolean allowSkip) {
		WebElement checkBox = find(ALLOW_SKIP.replace("0", STAGE_INDEX));

		if(allowSkip != checkBox.isSelected()) {
			checkBox.click();
		}
		waitUntilActionBarIsNotActive();
	}

	public void setSkipFeedbackText(String feedbackText) {
		WebElement button = find(SKIP_FEEDBACK_TEXT_BUTTON.replace("0", STAGE_INDEX));
		button.click();
		waitClickable(By.id(SKIP_FEEDBACK_TEXT_INPUT.replace("0", STAGE_INDEX)));
		find(SKIP_FEEDBACK_TEXT_INPUT.replace("0", STAGE_INDEX)).sendKeys(feedbackText);
		button.click();

		waitUntilActionBarIsNotActive();
	}
}