package de.uni_due.s3.jack3.uitests.utils.pages.stages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Misc.scrollElementIntoView;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Find;

public class FillInStagePage {
	
	private final String STAGE_INDEX;
	private final String TABS = "exerciseEdit:tabs0";
	
		/* ----- Content Tab ----- */
	private final String EXTERNAL_NAME = "exerciseEdit:tabs0:stageExternalNameInput";
	private final String EXERCISE_DESCRIPTION = "cke_exerciseEdit:tabs0:editor_0";
	private final String ADD_FILLIN_FIELD_BUTTON = "exerciseEdit:tabs0:cbAddFillInField";
	private final String ADD_DROPDOWN_BUTTON = "exerciseEdit:tabs0:cbAddDropDownField";
	private final String FILLINFIELDS_DATA = "exerciseEdit:tabs0:fillInFields_data";
	private final String FILLIN_FIELD_NAME = "exerciseEdit:tabs0:fillInFields:0:editorFillInFieldName_display";
	private final String FILLIN_FIELD_NAME_INPUT = "exerciseEdit:tabs0:fillInFields:0:inputFillInFieldName";
	private final String FILLIN_FIELD_SIZE = "exerciseEdit:tabs0:fillInFields:0:editorFillInFieldSize_display";
	private final String FILLIN_FIELD_SIZE_INPUT = "exerciseEdit:tabs0:fillInFields:0:inputFillInFieldSize_input";
	private final String FILLIN_FIELD_FORMELEDITOR = "exerciseEdit:tabs0:fillInFields:0:selectMenuFillInFieldFormularEditorType_label";
	private final String FILLIN_FIELD_FORMELEDITOR_ITEMS = "exerciseEdit:tabs0:fillInFields:0:selectMenuFillInFieldFormularEditorType_items";
	private final String REMOVE_FILLIN_FIELD = "exerciseEdit:tabs0:fillInFields:0:cbRemoveFillInField";
	private final String DROPDOWN_DATA = "exerciseEdit:tabs0:dropDownFields_data";
	private final String DROPDOWN_NAME = "exerciseEdit:tabs0:dropDownFields:0:editorDropDownFieldName_display";
	private final String DROPDOWN_NAME_INPUT = "exerciseEdit:tabs0:dropDownFields:0:inputDropDownFieldName";
	private final String DROPDOWN_RANDOMIZE = "exerciseEdit:tabs0:dropDownFields:0:dropDownFieldRandomize";
	private final String DROPDOWN_ANSWER_BUTTON = "exerciseEdit:tabs0:dropDownFields:0:cbAddAnswerAtDropDownField";
	private final String DROPDOWN_ANSWER_DISPLAY = "exerciseEdit:tabs0:dropDownFields:0:dropDownFieldAnswerOptions:0:editorInputDropDownAnswer_display";
	private final String DROPDOWN_ANSWER_INPUT = "exerciseEdit:tabs0:dropDownFields:0:dropDownFieldAnswerOptions:0:inputDropDownAnswer";
	private final String REMOVE_DROPDOWN = "exerciseEdit:tabs0:dropDownFields:0:cbRemoveDropDownField";
	
		/* ----- Feedback Tab ----- */
	private final String STAGE_WEIGHT = "exerciseEdit:tabs0:stageWeight_input";
	private final String CORRECT_ANSWER_RULES_DATA = "exerciseEdit:tabs0:correctAnswerRules_data";
	private final String ADD_CORRECT_ANSWER_RULE = "exerciseEdit:tabs0:cbAddCorrectAnswerRule";
	private final String CORRECT_ANSWER_RULE_EXPRESSION_INPUT = "exerciseEdit:tabs0:correctAnswerRules:0:correctAnswerRuleExpression_initCodeInput";
	private final String CORRECT_ANSWER_DOMAIN_LABEL = "exerciseEdit:tabs0:correctAnswerRules:0:correctAnswerRuleExpression_selectDomainInput_label";
	private final String CORRECT_ANSWER_DOMAIN_PANEL = "exerciseEdit:tabs0:correctAnswerRules:0:correctAnswerRuleExpression_selectDomainInput_panel";
	private final String FEEDBACK_TEXT_FOR_CORRECT_ANSWER_BUTTON = "exerciseEdit:tabs0:load_correctAnswerFeedbackText_InRawEditor";
	private final String FEEDBACK_TEXT_FOR_CORRECT_ANSWER_INPUT = "exerciseEdit:tabs0:rawEditor_correctAnswerFeedbackText";
	private final String FEEDBACK_TEXT_FOR_WRONG_ANSWER_BUTTON = "exerciseEdit:tabs0:load_defaultAnswerFeedbackText_InRawEditor";
	private final String FEEDBACK_TEXT_FOR_WRONG_ANSWER_INPUT = "exerciseEdit:tabs0:rawEditor_defaultAnswerFeedbackText";
	private final String FEEDBACK_POINTS_FOR_WRONG_ANSWER = "exerciseEdit:tabs0:defaultResult_input";
	private final String ADDITIONAL_FEEDBACK_CODE = "exerciseEdit:tabs0:feedbackRules:0:inputValidationExpression_initCodeInput";
	private final String ADDITIONAL_FEEDBACK_DOMAIN_LABEL = "exerciseEdit:tabs0:feedbackRules:0:inputValidationExpression_selectDomainInput_label";
	private final String ADDITIONAL_FEEDBACK_DOMAIN_MATH = "exerciseEdit:tabs0:feedbackRules:0:inputValidationExpression_selectDomainInput_0";
	private final String ADDITIONAL_FEEDBACK_DOMAIN_CHEM = "exerciseEdit:tabs0:feedbackRules:0:inputValidationExpression_selectDomainInput_1";
	private final String ADDITIONAL_FEEDBACK_POINTS = "exerciseEdit:tabs0:feedbackRules:0:inputFeedbackRulePoints_input";
	private final String ADDITIONAL_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:feedbackRules:0:load_inputFeedbackText_InRawEditor";
	private final String ADDITIONAL_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:feedbackRules:0:rawEditor_inputFeedbackText";
	private final String ADDITIONAL_FEEDBACK_TERMINAL_CHECKBOX = "exerciseEdit:tabs0:feedbackRules:0:inputFeedbackRuleTerminal";
	private final String CREATE_ADDITIONAL_FEEDBACK_BUTTON = "exerciseEdit:tabs0:cbAddFeedbackRule";
	private final String ADDITIONAL_FEEDBACK_DATA = "exerciseEdit:tabs0:feedbackRules_data";
	private final String ALLOW_SKIP = "exerciseEdit:tabs0:allowSkip";
	private final String SKIP_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:load_skipFeedback_InRawEditor";
	private final String SKIP_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:rawEditor_skipFeedback";
	
	
		/* ----- Hint Tab ----- */

	
		/* ----- Transition Tab ----- */

	
	public FillInStagePage(String stageIndex) {
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
	
	public void addFillInField(String fieldName, int fieldSize, String formelEditorOption) {
		waitClickable(By.id(ADD_FILLIN_FIELD_BUTTON.replace("0", STAGE_INDEX)));
		long inputFieldIndex = getNumberOfFillInFields();
		find(ADD_FILLIN_FIELD_BUTTON.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		final WebElement fieldNameDisplay = find(FILLIN_FIELD_NAME.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex));
		waitClickable(fieldNameDisplay);
		fieldNameDisplay.click();
		final WebElement fieldNameInput = find(FILLIN_FIELD_NAME_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex));
		waitClickable(fieldNameInput);
		fieldNameInput.sendKeys(fieldName + Keys.ENTER);
	
		waitUntilActionBarIsNotActive();
	
		final WebElement fieldSizeDisplay = find(FILLIN_FIELD_SIZE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex));
		waitClickable(fieldSizeDisplay);
		fieldSizeDisplay.click();
		final WebElement fieldSizeInput = find(FILLIN_FIELD_SIZE_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex));
		waitClickable(fieldSizeInput);
		fieldSizeInput.sendKeys(fieldSize + "" + Keys.ENTER);
		
		waitUntilActionBarIsNotActive();
		
		final WebElement formelEditor = find(FILLIN_FIELD_FORMELEDITOR.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex));
		waitClickable(formelEditor);
		formelEditor.click();
		
		final WebElement formelEditorOptionElement = findChildElementByTagAndText(find(FILLIN_FIELD_FORMELEDITOR_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+inputFieldIndex)), "li", formelEditorOption);
		waitClickable(formelEditorOptionElement);
		formelEditorOptionElement.click();
		
		waitUntilActionBarIsNotActive();
	}
	
	public void removeFillInField(int index) {
		waitClickable(By.id(REMOVE_FILLIN_FIELD.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)));
		find(REMOVE_FILLIN_FIELD.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfFillInFields() {
		return find(FILLINFIELDS_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addDropDownField(String fieldName, boolean randomized, String... answerOptions) {
		waitClickable(By.id(ADD_DROPDOWN_BUTTON.replace("0", STAGE_INDEX)));
		long dropDownIndex = getNumberOfDropDownFields();
		scrollElementIntoView(find(EXTERNAL_NAME.replace("0", STAGE_INDEX)));
		waitClickable(By.id(ADD_DROPDOWN_BUTTON.replace("0", STAGE_INDEX)));
		find(ADD_DROPDOWN_BUTTON.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		final WebElement dropDownNameDisplay = find(DROPDOWN_NAME.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+dropDownIndex));
		waitClickable(dropDownNameDisplay);
		dropDownNameDisplay.click();
		final WebElement dropDownNameInput = find(DROPDOWN_NAME_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+dropDownIndex));
		waitClickable(dropDownNameInput);
		dropDownNameInput.sendKeys(fieldName + Keys.ENTER);
		waitUntilActionBarIsNotActive();
		
		WebElement randomizeCheckBox = find(DROPDOWN_RANDOMIZE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+dropDownIndex));
		if(randomized != randomizeCheckBox.isSelected()) {
			randomizeCheckBox.click();
		}
		
		for(int i=0; i<answerOptions.length; i++) {
			//scroll element into view by moving to the element		
			find(DROPDOWN_ANSWER_BUTTON.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+dropDownIndex)).click();
			waitUntilActionBarIsNotActive();
			
			find(DROPDOWN_ANSWER_DISPLAY.replace("tabs0", "tabs"+STAGE_INDEX).replace("Fields:0", "Fields:"+dropDownIndex).replace("Options:0", "Options:"+i)).click();
			waitClickable(By.id(DROPDOWN_ANSWER_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace("Fields:0", "Fields:"+dropDownIndex).replace("Options:0", "Options:"+i)));
			find(DROPDOWN_ANSWER_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace("Fields:0", "Fields:"+dropDownIndex).replace("Options:0", "Options:"+i)).sendKeys(answerOptions[i] + Keys.ENTER);
			waitUntilActionBarIsNotActive();
		}
		
	}
	
	public void removeDropDownField(int index) {
		waitClickable(By.id(REMOVE_DROPDOWN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)));
		find(REMOVE_DROPDOWN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfDropDownFields() {
		return find(DROPDOWN_DATA.replace("0", STAGE_INDEX)).findElements(By.xpath("./*")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void setStageWeight(int weight) {
		WebElement stageWeight = find(STAGE_WEIGHT.replace("0", STAGE_INDEX));
		stageWeight.sendKeys(Keys.BACK_SPACE +""+ Keys.BACK_SPACE + Keys.BACK_SPACE + weight); //use backspaces to be sure the previous input is deleted (.clear() doesnt work)
	}
	
	public void addCorrectAnswerRule(String evaluatorCode, EDomain domain) {
		waitClickable(By.id(ADD_CORRECT_ANSWER_RULE.replace("0", STAGE_INDEX)));
		long ruleIndex = getNumberOfCorrectAnswerRules();
		find(ADD_CORRECT_ANSWER_RULE.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		WebElement input = find(CORRECT_ANSWER_RULE_EXPRESSION_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+ruleIndex));
		waitClickable(input);
		input.sendKeys(evaluatorCode);
		
		find(CORRECT_ANSWER_DOMAIN_LABEL.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+ruleIndex)).click();
		WebElement panel = find(CORRECT_ANSWER_DOMAIN_PANEL.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+ruleIndex));
		waitClickable(panel);
		
		if(domain == EDomain.MATH) {
			findChildElementByTagAndText(panel, "li", "MATH").click();
		}else {
			findChildElementByTagAndText(panel, "li", "CHEM").click();
		}
		
	}
	
	public void setFeedbackTextForCorrectAnswer(String feedbackText) {
		WebElement button = find(FEEDBACK_TEXT_FOR_CORRECT_ANSWER_BUTTON.replace("0", STAGE_INDEX));
		button.click();
		waitClickable(By.id(FEEDBACK_TEXT_FOR_CORRECT_ANSWER_INPUT.replace("0", STAGE_INDEX)));
		find(FEEDBACK_TEXT_FOR_CORRECT_ANSWER_INPUT.replace("0", STAGE_INDEX)).sendKeys(feedbackText);
		button.click();
		
		waitUntilActionBarIsNotActive();
	}
	
	public void setFeedbackTextForWrongAnswer(String feedbackText) {
		WebElement button = find(FEEDBACK_TEXT_FOR_WRONG_ANSWER_BUTTON.replace("0", STAGE_INDEX));
		button.click();
		waitClickable(By.id(FEEDBACK_TEXT_FOR_WRONG_ANSWER_INPUT.replace("0", STAGE_INDEX)));
		find(FEEDBACK_TEXT_FOR_WRONG_ANSWER_INPUT.replace("0", STAGE_INDEX)).sendKeys(feedbackText);
		button.click();
		
		waitUntilActionBarIsNotActive();
	}
	
	public void setPointsForWrongAnswer(int points) {
		find(FEEDBACK_POINTS_FOR_WRONG_ANSWER.replace("0", STAGE_INDEX)).sendKeys("" + points);
	}
	
	private long getNumberOfCorrectAnswerRules() {
		return find(CORRECT_ANSWER_RULES_DATA.replace("0", STAGE_INDEX)).findElements(By.xpath("./*")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void createAdditionalFeedback(String evaluatorCode, EDomain domain, String feedbackText, int points, boolean terminal) {
		waitClickable(By.id(CREATE_ADDITIONAL_FEEDBACK_BUTTON.replace("0", STAGE_INDEX)));
		long rowIndex = getNumberOfAdditionalFeedbacks();
		find(CREATE_ADDITIONAL_FEEDBACK_BUTTON.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		find(ADDITIONAL_FEEDBACK_CODE.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).sendKeys(evaluatorCode);
		find(ADDITIONAL_FEEDBACK_DOMAIN_LABEL.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).click();
		
		if(domain == EDomain.MATH) {
			find(ADDITIONAL_FEEDBACK_DOMAIN_MATH.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).click();
		}else {
			find(ADDITIONAL_FEEDBACK_DOMAIN_CHEM.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).click();
		}
		
		find(ADDITIONAL_FEEDBACK_POINTS).sendKeys(points + "" + Keys.ENTER);
		
		find(ADDITIONAL_FEEDBACK_TEXT_BUTTON.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).click();
		waitClickable(By.id(ADDITIONAL_FEEDBACK_TEXT_INPUT.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)));
		find(ADDITIONAL_FEEDBACK_TEXT_INPUT.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).sendKeys(feedbackText);
		find(ADDITIONAL_FEEDBACK_TEXT_BUTTON.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex)).click();
		
		waitUntilActionBarIsNotActive();
		
		WebElement terminalCheckBox = Find.findChildren(find(ADDITIONAL_FEEDBACK_TERMINAL_CHECKBOX.replace("0", STAGE_INDEX).replace(":0", ":"+rowIndex))).get(3);
		if(terminalCheckBox.isSelected() != terminal) {
			terminalCheckBox.click();
		}
		
	}
	
	private long getNumberOfAdditionalFeedbacks() {
		return find(ADDITIONAL_FEEDBACK_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();

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