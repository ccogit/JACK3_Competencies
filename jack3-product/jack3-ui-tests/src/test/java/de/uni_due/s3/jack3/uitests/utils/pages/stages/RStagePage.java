package de.uni_due.s3.jack3.uitests.utils.pages.stages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.entities.stagetypes.r.ETestCasePointsMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestcaseRuleMode;
import de.uni_due.s3.jack3.uitests.utils.Driver;

public class RStagePage {

	private final String STAGE_INDEX;
	private final String TABS = "exerciseEdit:tabs0";

	/* ----- Content Tab ----- */
	private final String EXTERNAL_NAME = "exerciseEdit:tabs0:stageExternalNameInput";
	private final String EXERCISE_DESCRIPTION = "cke_exerciseEdit:tabs0:editor_0";
	private final String INITIAL_STAGE_CODE = "exerciseEdit:tabs0:rStageInitialCode";

	/* ----- Feedback Tab ----- */
	private final String STAGE_WEIGHT = "exerciseEdit:tabs0:stageWeight_input";
	private final String RESULT_COMPUTATION_STRING = "exerciseEdit:tabs0:resultComputationString";

	private final String ALLOW_SKIP = "exerciseEdit:tabs0:allowSkip";
	private final String SKIP_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:load_skipFeedback_InRawEditor";
	private final String SKIP_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:rawEditor_skipFeedback";

	/* ----- Checker Tab ----- */

	private final String ADD_NEW_TEST_CASE_GROUP  ="exerciseEdit:tabs0:addNewTestCaseGroup";
	private final String TEST_CASE_GROUPS_PANEL = "exerciseEdit:tabs0:testCaseGroupsPanel";
	private final String REMOVE_TEST_CASE_GROUP = "exerciseEdit:tabs0:deleteTestCase0";

	//dynamic
	private final String ADD_DYNAMIC_TEST_CASE = "exerciseEdit:tabs0:addDynamicTestCaseBtn0";
	private final String TEST_CASE_NAME = "exerciseEdit:tabs0:testCaseName";
	private final String TEST_CASE_POSTCODE = "exerciseEdit:tabs0:testCasePostCode";
	private final String TEST_CASE_POST_PROCESSING_FUNCTION = "exerciseEdit:tabs0:testCasePostprocessingFunction";
	private final String TEST_CASE_EXPECTED_RESULT = "exerciseEdit:tabs0:testCaseExpectedOutput";
	private final String TEST_CASE_TOLERANCE = "exerciseEdit:tabs0:testCaseTolerance";
	private final String TEST_CASE_RULE_MODE = "exerciseEdit:tabs0:testCaseStaticRuleMode";
	private final String TEST_CASE_POINTS_MODE = "exerciseEdit:tabs0:testCastePointsMode";
	private final String TEST_CASE_POINTS = "exerciseEdit:tabs0:testCasePoints";
	private final String TEST_CASE_FEEDBACK = "exerciseEdit:tabs0:testCaseFeebackIfFailed";
	private final String TEST_CASE_ADD_DYNAMIC = "exerciseEdit:tabs0:addDynamicTestcase";

	//static
	private final String STATIC_TEST_CASE_ADD = "exerciseEdit:tabs0:addStaticTestCaseBtn0";
	private final String STATIC_TEST_CASE_NAME = "exerciseEdit:tabs0:addStaticTestCaseOverlayNameInput";
	private final String STATIC_TEST_CASE_ADD_QUERY_BUTTON = "exerciseEdit:tabs0:addStaticTestCaseOverlayAddRuleButton";
	private final String STATIC_TEST_CASE_QUERY = "exerciseEdit:tabs0:addStaticTestCaseOverlayQueryTable:0:addStaticTestCaseOverlayQueryInput";
	private final String STATIC_TEST_CASE_RULE_MODE = "exerciseEdit:tabs0:addStaticTestCaseOverlayRuleMode";
	private final String STATIC_TEST_CASE_POINTS = "exerciseEdit:tabs0:addStaticTestCaseOverlayPointsInput";
	private final String STATIC_TEST_CASE_POINTS_MODE = "exerciseEdit:tabs0:addStaticTestCaseOverlayPointsMode";
	private final String STATIC_TEST_CASE_FEEDBACK = "exerciseEdit:tabs0:addStaticTestCaseOverlayFeedbackInput";
	private final String STATIC_TEST_CASE_ADD_BUTTON = "exerciseEdit:tabs0:addStaticTestCaseOverlayAddBtn";

	public RStagePage(String stageIndex) {
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
		driver.switchTo().frame(find(EXERCISE_DESCRIPTION.replace("0", STAGE_INDEX)).findElement(By.tagName("iframe")));
		driver.findElement(By.cssSelector("body")).sendKeys(text);
		driver.switchTo().defaultContent();

		waitUntilActionBarIsNotActive();
	}

	public void setInitialStageCode(String code) {
		String id = INITIAL_STAGE_CODE.replace("0", STAGE_INDEX);

		waitClickable(By.id(id));
		find(id).sendKeys(code);
	}

	public void setStageWeight(int weight) {
		WebElement stageWeight = find(STAGE_WEIGHT.replace("0", STAGE_INDEX));
		stageWeight.sendKeys(Keys.BACK_SPACE +""+ Keys.BACK_SPACE + Keys.BACK_SPACE + weight); //use backspaces to be sure the previous input is deleted (.clear() doesnt work)
	}

	public void setEvaluationRule(String evaluationRule) {
		String id = RESULT_COMPUTATION_STRING.replace("0", STAGE_INDEX);

		waitClickable(By.id(id));
		find(id).sendKeys(evaluationRule);
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

	public void addNewTestCaseGroup() {
		String id = ADD_NEW_TEST_CASE_GROUP.replace("0", STAGE_INDEX);

		waitClickable(By.id(id));
		find(id).click();
		waitUntilActionBarIsNotActive();
	}

	public long getNumberOfTestCaseGroups() {
		return find(TEST_CASE_GROUPS_PANEL.replace("0", STAGE_INDEX)).findElements(By.tagName("div")).stream().filter(ele -> ele!=null && ele.getAttribute("data-widget")!=null).count();
	}

	public void removeTestCaseGroup(int index) {
		String id = REMOVE_TEST_CASE_GROUP.replace("tabs0", "tabs"+STAGE_INDEX).replace("case0", "case"+index);

		waitClickable(By.id(id));
		find(id).click();
		waitUntilActionBarIsNotActive();
	}

	public void createDynamicTestCase(int testCaseGroupIndex, String name, String postCode, String postProcessingFunction, String expectedResult, int tolerance, ETestcaseRuleMode ruleMode, ETestCasePointsMode pointsMode, int points, String feedback) {
		//create new dynamic test case
		String id = ADD_DYNAMIC_TEST_CASE.replace("tabs0", "tabs"+STAGE_INDEX).replace("Btn0", "Btn"+testCaseGroupIndex);
		waitClickable(By.id(id));
		find(id).click();
		waitUntilActionBarIsNotActive();
		waitClickable(By.id(TEST_CASE_NAME.replace("0", STAGE_INDEX)));

		//set all of the settings of the test case
		find(TEST_CASE_NAME.replace("0", STAGE_INDEX)).sendKeys(name);
		find(TEST_CASE_POSTCODE.replace("0", STAGE_INDEX)).sendKeys(postCode);
		find(TEST_CASE_POST_PROCESSING_FUNCTION.replace("0", STAGE_INDEX)).sendKeys(postProcessingFunction);
		find(TEST_CASE_EXPECTED_RESULT.replace("0", STAGE_INDEX)).sendKeys(expectedResult);
		find(TEST_CASE_TOLERANCE.replace("0", STAGE_INDEX)).clear();
		find(TEST_CASE_TOLERANCE.replace("0", STAGE_INDEX)).sendKeys(tolerance+"");

		find(TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)).click();
		if(ruleMode.equals(ETestcaseRuleMode.PRESENCE)) {
			findChildElementByTagAndText(find(TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)), "option", "Presence").click();
		}else {
			findChildElementByTagAndText(find(TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)), "option", "Absence").click();
		}

		find(TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)).click();
		if(pointsMode.equals(ETestCasePointsMode.GAIN)) {
			findChildElementByTagAndText(find(TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)), "option", "Gain").click();
		}else {
			findChildElementByTagAndText(find(TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)), "option", "Deduction").click();
		}

		find(TEST_CASE_POINTS.replace("0", STAGE_INDEX)).clear();
		find(TEST_CASE_POINTS.replace("0", STAGE_INDEX)).sendKeys(points+"");
		find(TEST_CASE_FEEDBACK.replace("0", STAGE_INDEX)).sendKeys(feedback);

		find(TEST_CASE_ADD_DYNAMIC.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
		waitNotClickable(By.id(TEST_CASE_TOLERANCE.replace("0", STAGE_INDEX)));

	}

	public void createStaticTestCase(int testCaseGroupIndex, String name, String GReQLQuery, ETestcaseRuleMode ruleMode, int points, ETestCasePointsMode pointsMode, String feedback) {
		//create new dynamic test case
		String id = STATIC_TEST_CASE_ADD.replace("tabs0", "tabs"+STAGE_INDEX).replace("Btn0", "Btn"+testCaseGroupIndex);
		waitClickable(By.id(id));
		find(id).click();
		waitUntilActionBarIsNotActive();
		find(STATIC_TEST_CASE_ADD_QUERY_BUTTON.replace("tabs0", "tabs" + STAGE_INDEX)).click();

		//set all of the settings of the test case
		find(STATIC_TEST_CASE_NAME.replace("0", STAGE_INDEX)).clear();
		find(STATIC_TEST_CASE_NAME.replace("0", STAGE_INDEX)).sendKeys(name);
		find(STATIC_TEST_CASE_QUERY.replace("0", STAGE_INDEX)).clear();
		find(STATIC_TEST_CASE_QUERY.replace("0", STAGE_INDEX)).sendKeys(GReQLQuery);
		find(STATIC_TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)).click();
		if(ruleMode.equals(ETestcaseRuleMode.PRESENCE)) {
			findChildElementByTagAndText(find(STATIC_TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)), "option", "Presence").click();
		}else {
			findChildElementByTagAndText(find(STATIC_TEST_CASE_RULE_MODE.replace("0", STAGE_INDEX)), "option", "Absence").click();
		}
		find(STATIC_TEST_CASE_POINTS.replace("0", STAGE_INDEX)).clear();
		find(STATIC_TEST_CASE_POINTS.replace("0", STAGE_INDEX)).sendKeys(points+"");
		find(STATIC_TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)).click();
		if(pointsMode.equals(ETestCasePointsMode.GAIN)) {
			findChildElementByTagAndText(find(STATIC_TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)), "option", "Gain").click();
		}else {
			findChildElementByTagAndText(find(STATIC_TEST_CASE_POINTS_MODE.replace("0", STAGE_INDEX)), "option", "Deduction").click();
		}
		find(STATIC_TEST_CASE_FEEDBACK.replace("0", STAGE_INDEX)).clear();
		find(STATIC_TEST_CASE_FEEDBACK.replace("0", STAGE_INDEX)).sendKeys(feedback);

		find(STATIC_TEST_CASE_ADD_BUTTON.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
		waitNotClickable(By.id(STATIC_TEST_CASE_NAME.replace("0", STAGE_INDEX)));
	}
}