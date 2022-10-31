package de.uni_due.s3.jack3.uitests.utils.pages.stages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildElementByTagAndText;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Find;
public class MCStagePage {
	
	private final String STAGE_INDEX;
	private final String TABS = "exerciseEdit:tabs0";
	
		/* ----- Content Tab ----- */
	private final String EXTERNAL_NAME = "exerciseEdit:tabs0:stageExternalNameInput";
	private final String EXERCISE_DESCRIPTION = "cke_exerciseEdit:tabs0:editor_0";
	
	private final String ADD_ANSWER_OPTION = "exerciseEdit:tabs0:cbAddMcAnswerOption";
	private final String ANSWERS_DATA = "exerciseEdit:tabs0:answers_data";
	private final String ANSWER_OPTION_EDITOR = "exerciseEdit:tabs0:answers:0:load_answerTextRow_InRawEditor";
	private final String ANSWER_OPTION_INPUT = "exerciseEdit:tabs0:answers:0:rawEditor_answerTextRow";
	
	private final String IS_ANSWER_CORRECT_BUTTON_FALSE = "exerciseEdit:tabs0:answers:0:mcCorrectSelectOneButton:0";
	private final String IS_ANSWER_CORRECT_BUTTON_TRUE = "exerciseEdit:tabs0:answers:0:mcCorrectSelectOneButton:1";
	private final String RANDOMIZE = "exerciseEdit:tabs0:randomize";
	private final String SINGLE_CHOICE = "exerciseEdit:tabs0:singleChoice";
	
		/* ----- Feedback Tab ----- */
	private final String STAGE_WEIGHT = "exerciseEdit:tabs0:stageWeight_input";
	private final String FEEDBACK_TEXT_FOR_CORRECT_ANSWER_BUTTON = "exerciseEdit:tabs0:load_correctAnswerFeedbackText_InRawEditor";
	private final String FEEDBACK_TEXT_FOR_CORRECT_ANSWER_INPUT = "exerciseEdit:tabs0:rawEditor_correctAnswerFeedbackText";
	private final String FEEDBACK_TEXT_FOR_WRONG_ANSWER_BUTTON = "exerciseEdit:tabs0:load_defaultAnswerFeedbackText_InRawEditor";
	private final String FEEDBACK_TEXT_FOR_WRONG_ANSWER_INPUT = "exerciseEdit:tabs0:rawEditor_defaultAnswerFeedbackText";
	private final String FEEDBACK_POINTS_FOR_WRONG_ANSWER = "exerciseEdit:tabs0:defaultResult_input";
	
	private final String CREATE_ADDITIONAL_FEEDBACK_BUTTON = "exerciseEdit:tabs0:feedbackPattern_toggler";
	private final String FEEDBACK_PATTERN_CONTENT = "exerciseEdit:tabs0:feedbackPattern_content";
	private final String FEEDBACK_PATTERN_SELECT = "exerciseEdit:tabs0:feedbackTable:0:feedbackSelectPattern_label";
	private final String FEEDBACK_PATTERN_SELECT_ITEMS = "exerciseEdit:tabs0:feedbackTable:0:feedbackSelectPattern_items";
	private final String ADDITIONAL_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:feedbacks:0:load_extraFeedbackText_InRawEditor";
	private final String ADDITIONAL_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:feedbacks:0:rawEditor_extraFeedbackText";
	private final String ADDITIONAL_FEEDBACK_POINTS = "exerciseEdit:tabs0:feedbacks:0:editorGradePoints_input";
	private final String ADDITIONAL_FEEDBACK_DATA = "exerciseEdit:tabs0:feedbacks_data";
	
	private final String ALLOW_SKIP = "exerciseEdit:tabs0:allowSkip";
	private final String SKIP_FEEDBACK_TEXT_BUTTON = "exerciseEdit:tabs0:load_skipFeedback_InRawEditor";
	private final String SKIP_FEEDBACK_TEXT_INPUT = "exerciseEdit:tabs0:rawEditor_skipFeedback";
	
		/* ----- Hint Tab ----- */
	private final String CREATE_HINT_BUTTON = "exerciseEdit:tabs0:cbAddHint";
	private final String HINT_TEXT_BUTTON = "exerciseEdit:tabs0:hintList:0:load_facetHintTextOutput_InRawEditor";
	private final String HINT_TEXT_INPUT = "exerciseEdit:tabs0:hintList:0:rawEditor_facetHintTextOutput";
	private final String HINT_LIST_DATA = "exerciseEdit:tabs0:hintList_data";
	private final String HINT_MALUS_INPUT = "exerciseEdit:tabs0:hintList:0:malusValue_input";
	private final String REMOVE_HINT = "exerciseEdit:tabs0:hintList:0:cbRemoveHint";
	
	/* ----- Transition Tab ----- */
	private final String DEFAULT_TRANSITION = "exerciseEdit:tabs0:defaultTransition";
	private final String ADD_SKIP_TRANSITION_BUTTON = "exerciseEdit:tabs0:cbAddNewSkipTransition";
	private final String SKIP_TRANSITION_DATA = "exerciseEdit:tabs0:skipTransitions_data";
	private final String SKIP_CODE_INPUT = "exerciseEdit:tabs0:skipTransitions:0:editTransitCondition_initCodeInput";
	private final String SKIP_DOMAIN_LABEL = "exerciseEdit:tabs0:skipTransitions:0:editTransitCondition_selectDomainInput_label";
	private final String SKIP_DOMAIN_MATH = "exerciseEdit:tabs0:skipTransitions:0:editTransitCondition_selectDomainInput_0";
	private final String SKIP_DOMAIN_CHEM = "exerciseEdit:tabs0:skipTransitions:0:editTransitCondition_selectDomainInput_1";
	private final String SKIP_TARGET = "exerciseEdit:tabs0:skipTransitions:0:skipTarget";
	private final String REMOVE_SKIP_TRANSITION_BUTTON = "exerciseEdit:tabs0:skipTransitions:0:cbRemoveSkipTransition";
	
	/* ----- UpdateTab ----- */
	private final String BEFORECHECK_LABEL = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateBeforeCheck_label";
	private final String BEFORECHECK_ITEMS = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateBeforeCheck_items";
	private final String BEFORECHECK_ADD_UPDATE = "exerciseEdit:tabs0:cbVarUpdateAddVariableUpdateBeforeCheck";
	private final String BEFORECHECK_CODE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesBeforeCheck:0:variableUpdateBeforeCheckCode_initCodeInput";
	private final String BEFORECHECK_SELECT_DOMAIN = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesBeforeCheck:0:variableUpdateBeforeCheckCode_selectDomainInput_label";
	private final String BEFORECHECK_DOMAIN_ITEMS = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesBeforeCheck:0:variableUpdateBeforeCheckCode_selectDomainInput_items";
	private final String BEFORECHECK_DELETE_UPDATE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesBeforeCheck:0:cbVarUpdateRemoveVariable";
	private final String BEFORECHECK_DATA = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesBeforeCheck_data";
	
	private final String AFTERCHECK_LABEL = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateAfterCheck_label";
	private final String AFTERCHECK_ITEMS = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateAfterCheck_items";
	private final String AFTERCHECK_ADD_UPDATE = "exerciseEdit:tabs0:cbVarUpdateAddVariableUpdateAfterCheck";
	private final String AFTERCHECK_CODE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesAfterCheck:0:variableUpdateAfterCheckCode_initCodeInput";
	private final String AFTERCHECK_SELECT_DOMAIN = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesAfterCheck:0:variableUpdateAfterCheckCode_selectDomainInput_label";
	private final String AFTERCHECK_DOMAIN_ITEMS = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesAfterCheck:0:variableUpdateAfterCheckCode_selectDomainInput_items";
	private final String AFTERCHECK_DELETE_UPDATE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesAfterCheck:0:cbVarUpdateRemoveVariable";
	private final String AFTERCHECK_DATA = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesAfterCheck_data";
	
	private final String ON_NORMAL_EXIT_Label = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnNormalExit_label";
	private final String ON_NORMAL_EXIT_ITEMS = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnNormalExit_items";
	private final String ON_NORMAL_EXIT_ADD_UPDATE = "exerciseEdit:tabs0:cbVarUpdateAddVariableUpdateOnNormalExit";
	private final String ON_NORMAL_EXIT_CODE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnNormalExit:0:variableUpdateOnNormalExitCode_initCodeInput";
	private final String ON_NORMAL_EXIT_SELECT_DOMAIN = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnNormalExit:0:variableUpdateOnNormalExitCode_selectDomainInput_label";
	private final String ON_NORMAL_EXIT_DOMAIN_ITEMS = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnNormalExit:0:variableUpdateOnNormalExitCode_selectDomainInput_items";
	private final String ON_NORMAL_EXIT_DELETE_UPDATE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnNormalExit:0:cbVarUpdateRemoveVariable";
	private final String ON_NORMAL_EXIT_DATA = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnNormalExit_data";
	
	private final String ON_REPEAT_LABEL = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnRepeat_label";
	private final String ON_REPEAT_ITEMS = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnRepeat_items";
	private final String ON_REPEAT_ADD_UPDATE = "exerciseEdit:tabs0:cbVarUpdateAddVariableUpdateOnRepeat";
	private final String ON_REPEAT_CODE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnRepeat:0:variableUpdateOnRepeatCode_initCodeInput";
	private final String ON_REPEAT_SELECT_DOMAIN = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnRepeat:0:variableUpdateOnRepeatCode_selectDomainInput_label";
	private final String ON_REPEAT_DOMAIN_ITEMS = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnRepeat:0:variableUpdateOnRepeatCode_selectDomainInput_items";
	private final String ON_REPEAT_DELETE_UPDATE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnRepeat:0:cbVarUpdateRemoveVariable";
	private final String ON_REPEAT_DATA = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnRepeat_data";
	
	private final String ON_SKIP_LABEL = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnSkip_label";
	private final String ON_SKIP_ITEMS = "exerciseEdit:tabs0:somVarUpdateAddVariableUpdateOnSkip_items";
	private final String ON_SKIP_ADD_UPDATE = "exerciseEdit:tabs0:cbVarUpdateAddVariableUpdateOnSkip";
	private final String ON_SKIP_CODE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnSkip:0:variableUpdateOnSkipCode_initCodeInput";
	private final String ON_SKIP_SELECT_DOMAIN = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnSkip:0:variableUpdateOnSkipCode_selectDomainInput_label";
	private final String ON_SKIP_DOMAIN_ITEMS = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnSkip:0:variableUpdateOnSkipCode_selectDomainInput_items";
	private final String ON_SKIP_DELETE_UPDATE = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnSkip:0:cbVarUpdateRemoveVariable";
	private final String ON_SKIP_DATA = "exerciseEdit:tabs0:dtStageConfigVariableUpdatesOnSkip_data";
	
	public MCStagePage(String stageIndex) {
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
	
	public void setExerciseDescription(String description) {
		WebDriver driver = Driver.get();
		driver.switchTo().frame(find(EXERCISE_DESCRIPTION.replace("0", STAGE_INDEX)).findElement(By.tagName("iframe")));
		driver.findElement(By.cssSelector("body")).sendKeys(description);
		driver.switchTo().defaultContent();
		
		waitUntilActionBarIsNotActive();
	}
	
	public void addNewAnswerOption(String answerOption, boolean isCorrect) {
		long index = getNumberOfAnswerOptions();
		find(ADD_ANSWER_OPTION.replace("0", STAGE_INDEX)).click();
		waitUntilActionBarIsNotActive();
		
		//set the text of the answer option
		WebElement openAnswerInput = find(ANSWER_OPTION_EDITOR.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index));
		openAnswerInput.click();
		WebElement answerInput = find(ANSWER_OPTION_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index));
		waitClickable(answerInput);
		answerInput.clear();
		answerInput.sendKeys(answerOption);
		openAnswerInput.click();
		waitUntilActionBarIsNotActive();
		
		//set isCorrect
		if(isCorrect) {
			find(IS_ANSWER_CORRECT_BUTTON_TRUE.replace("tabs0", "tabs"+STAGE_INDEX).replace("answers:0", "answers:"+index)).findElement(By.xpath("./..")).click();
		}else {
			find(IS_ANSWER_CORRECT_BUTTON_FALSE.replace("tabs0", "tabs"+STAGE_INDEX).replace("answers:0", "answers:"+index)).findElement(By.xpath("./..")).click();
		}
		
		waitUntilActionBarIsNotActive();
		
	}
	
	public long getNumberOfAnswerOptions() {
		return find(ANSWERS_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	private WebElement getAnswerOptionWithIndex(int index) {
		return find(ANSWERS_DATA.replace("0", STAGE_INDEX))
				.findElements(By.tagName("tr")) //get all answer option rows
				.stream()
				.filter(row -> (""+index) == row.getAttribute("data-ri")) //check if row has the correct index
				.findFirst() //get the correct row
				.orElseThrow(() -> new AssertionError("Answeroption could not be found"));
	}
	
	public void setRandomizeAnswerOptions(boolean randomizeAnswers) {
		WebElement checkBox = find(RANDOMIZE.replace("0", STAGE_INDEX));
		if(checkBox.isSelected() != randomizeAnswers) {
			checkBox.click();
		}
	}
	
	public void setSingleChoiceOption(boolean singleChoice) {
		WebElement checkBox = find(SINGLE_CHOICE.replace("0", STAGE_INDEX));
		if(checkBox.isSelected() != singleChoice) {
			checkBox.click();
		}
	}
	
	public void setStageWeight(int weight) {
		WebElement stageWeight = find(STAGE_WEIGHT.replace("0", STAGE_INDEX));
		stageWeight.sendKeys(Keys.BACK_SPACE +""+ Keys.BACK_SPACE + Keys.BACK_SPACE + weight); //use backspaces to be sure the previous input is deleted (.clear() doesnt work)
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
	
	public void createAdditionalFeedback(String feedbackText, int points, EMCRuleType... rulePattern) {
		waitClickable(By.id(CREATE_ADDITIONAL_FEEDBACK_BUTTON.replace("0", STAGE_INDEX)));
		find(CREATE_ADDITIONAL_FEEDBACK_BUTTON.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		for(int i=0; i<rulePattern.length;i++) {
			find(FEEDBACK_PATTERN_SELECT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+i)).click();
			
			WebElement items = find(FEEDBACK_PATTERN_SELECT_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+i));
			switch(rulePattern[i]) {
				case CORRECT: 
					Find.findChildElementByTagAndText(items, "li", "Ja").click();			
					break;
				case WRONG: 
					Find.findChildElementByTagAndText(items, "li", "Nein").click();	
					break;
				case VARIABLE: 
					Find.findChildElementByTagAndText(items, "li", "variable").click();	
					break;
				case NO_MATTER:
					Find.findChildElementByTagAndText(items, "li", "Egal").click();	
					break;
			}
		}
		
		find(FEEDBACK_PATTERN_CONTENT.replace("0", STAGE_INDEX)).findElement(By.tagName("button")).click();
		waitUntilActionBarIsNotActive();
		
		long rowIndex = getNumberOfAdditionalFeedbacks() - 1; // -1 to get the index instead of the count
		
		WebElement button = find(ADDITIONAL_FEEDBACK_TEXT_BUTTON.replace("tabs0", "tabs" + STAGE_INDEX).replace(":0", ":" + rowIndex));
		button.click();
		waitClickable(By.id(ADDITIONAL_FEEDBACK_TEXT_INPUT.replace("tabs0", "tabs" + STAGE_INDEX).replace(":0", ":" + rowIndex)));
		find(ADDITIONAL_FEEDBACK_TEXT_INPUT.replace("tabs0", "tabs" + STAGE_INDEX).replace(":0", ":" + rowIndex)).sendKeys(feedbackText);
		button.click();
		
		waitUntilActionBarIsNotActive();
		
		find(ADDITIONAL_FEEDBACK_POINTS.replace("tabs0", "tabs" + STAGE_INDEX).replace(":0", ":" + rowIndex)).sendKeys("" + points);
		
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
	
	public void createHint(String hintText, int malusPoints) {
		long newHintIndex = getNumberOfHints();
		
		waitClickable(By.id(CREATE_HINT_BUTTON));
		find(CREATE_HINT_BUTTON).click();
		waitUntilActionBarIsNotActive();
		
		final WebElement hintTextButton = find(HINT_TEXT_BUTTON.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newHintIndex));
		waitClickable(hintTextButton);
		hintTextButton.click();
		final WebElement hintTextInput = find(HINT_TEXT_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newHintIndex));
		waitClickable(hintTextInput);
		hintTextInput.sendKeys(hintText);
		hintTextButton.click();
		waitUntilActionBarIsNotActive();
		
		List<WebElement> hintMalusInput = Driver.get().findElements(By.id(HINT_MALUS_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newHintIndex)));
		if(!hintMalusInput.isEmpty()) {
			hintMalusInput.get(0).sendKeys(Keys.LEFT + "" + malusPoints + Keys.ENTER);
		}else if(malusPoints>0) {
			throw new AssertionError("malus points for hint couldn't be set");
		}
	}
	
	public void removeHint(int hintIndex) {
		WebElement removeHintButton = find(REMOVE_HINT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+hintIndex));
		waitClickable(removeHintButton);
		removeHintButton.click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfHints() {
		return find(HINT_LIST_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void setDefaultTransition (String option) {
		WebElement defaultTransitionSelect = find(DEFAULT_TRANSITION.replace("0", STAGE_INDEX));
		waitClickable(defaultTransitionSelect);
		defaultTransitionSelect.click();
		
		WebElement transitionOption = findChildElementByTagAndText(defaultTransitionSelect, "option", option);
		waitClickable(transitionOption);
		transitionOption.click();
		
		waitUntilActionBarIsNotActive();
	}
	
	public void addSkipTransition(String evaluatorExpression, EDomain domain, String target) {
		long newSkipTransitionIndex = getNumberOfSkipTransitions();
		waitClickable(By.id(ADD_SKIP_TRANSITION_BUTTON.replace("0", STAGE_INDEX)));
		find(ADD_SKIP_TRANSITION_BUTTON.replace("0", STAGE_INDEX)).click();
		
		waitUntilActionBarIsNotActive();
		
		final WebElement codeInput = find(SKIP_CODE_INPUT.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newSkipTransitionIndex));
		final WebElement domainLabel = find(SKIP_DOMAIN_LABEL.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newSkipTransitionIndex));
		final WebElement transitionTarget = find(SKIP_TARGET.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newSkipTransitionIndex));
		
		waitClickable(codeInput);
		codeInput.sendKeys(evaluatorExpression);
		domainLabel.click();
		String domainId = domain==EDomain.MATH ? SKIP_DOMAIN_MATH : SKIP_DOMAIN_CHEM;
		find(domainId.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+newSkipTransitionIndex)).click();
		
		transitionTarget.click();
		final WebElement transitionOption = findChildElementByTagAndText(transitionTarget, "option", target);
		waitClickable(transitionOption);
		transitionOption.click();
	}
	
	public void removeSkipTransition (int index) {
		WebElement removeSkipTransitionButton = find(REMOVE_SKIP_TRANSITION_BUTTON.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index));
		waitClickable(removeSkipTransitionButton);
		removeSkipTransitionButton.click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfSkipTransitions() {
		return find(SKIP_TRANSITION_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addVariableUpdateBeforeCheck(String variableName, String code, EDomain domain) {
		waitClickable(By.id(BEFORECHECK_LABEL.replace("0", STAGE_INDEX)));
		final long updateIndex = getNumberOfVariableUpadtesBeforeCheck();
		find(BEFORECHECK_LABEL.replace("0", STAGE_INDEX)).click();
		waitClickable(By.id(BEFORECHECK_ITEMS.replace("0", STAGE_INDEX)));
		findChildElementByTagAndText(find(BEFORECHECK_ITEMS.replace("0", STAGE_INDEX)), "li", variableName).click();
		
		find(BEFORECHECK_ADD_UPDATE.replace("0", STAGE_INDEX)).click();	
		waitClickable(By.id(BEFORECHECK_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		find(BEFORECHECK_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).click();
		waitClickable(By.id(BEFORECHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		if(domain==EDomain.MATH) {
			findChildElementByTagAndText(find(BEFORECHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "MATH").click();
		}else {
			findChildElementByTagAndText(find(BEFORECHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "CHEM").click();
		}
		
		find(BEFORECHECK_CODE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).sendKeys(code);
	}
	
	public void removeVariableUpdateBeforeCheck(int index) {
		find(BEFORECHECK_DELETE_UPDATE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfVariableUpadtesBeforeCheck() {
		return find(BEFORECHECK_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addVariableUpdateAfterCheck(String variableName, String code, EDomain domain) {
		waitClickable(By.id(AFTERCHECK_LABEL.replace("0", STAGE_INDEX)));
		final long updateIndex = getNumberOfVariableUpadtesAfterCheck();
		find(AFTERCHECK_LABEL.replace("0", STAGE_INDEX)).click();
		waitClickable(By.id(AFTERCHECK_ITEMS.replace("0", STAGE_INDEX)));
		findChildElementByTagAndText(find(AFTERCHECK_ITEMS.replace("0", STAGE_INDEX)), "li", variableName).click();
		
		find(AFTERCHECK_ADD_UPDATE.replace("0", STAGE_INDEX)).click();	
		waitClickable(By.id(AFTERCHECK_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		find(AFTERCHECK_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).click();
		waitClickable(By.id(AFTERCHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		if(domain==EDomain.MATH) {
			findChildElementByTagAndText(find(AFTERCHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "MATH").click();
		}else {
			findChildElementByTagAndText(find(AFTERCHECK_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "CHEM").click();
		}
		
		find(AFTERCHECK_CODE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).sendKeys(code);
	}
	
	public void removeVariableUpdateAfterCheck(int index) {
		find(AFTERCHECK_DELETE_UPDATE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfVariableUpadtesAfterCheck() {
		return find(AFTERCHECK_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addVariableUpdateOnNormalExit(String variableName, String code, EDomain domain) {
		waitClickable(By.id(ON_NORMAL_EXIT_Label.replace("0", STAGE_INDEX)));
		final long updateIndex = getNumberOfVariableUpadtesOnNormalExit();
		find(ON_NORMAL_EXIT_Label.replace("0", STAGE_INDEX)).click();
		waitClickable(By.id(ON_NORMAL_EXIT_ITEMS.replace("0", STAGE_INDEX)));
		findChildElementByTagAndText(find(ON_NORMAL_EXIT_ITEMS.replace("0", STAGE_INDEX)), "li", variableName).click();
		
		find(ON_NORMAL_EXIT_ADD_UPDATE.replace("0", STAGE_INDEX)).click();	
		waitClickable(By.id(ON_NORMAL_EXIT_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		find(ON_NORMAL_EXIT_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).click();
		waitClickable(By.id(ON_NORMAL_EXIT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		if(domain==EDomain.MATH) {
			findChildElementByTagAndText(find(ON_NORMAL_EXIT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "MATH").click();
		}else {
			findChildElementByTagAndText(find(ON_NORMAL_EXIT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "CHEM").click();
		}
		
		find(ON_NORMAL_EXIT_CODE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).sendKeys(code);
	}
	
	public void removeVariableUpdateOnNormalExit(int index) {
		find(ON_NORMAL_EXIT_DELETE_UPDATE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfVariableUpadtesOnNormalExit() {
		return find(ON_NORMAL_EXIT_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addVariableUpdateOnRepeat(String variableName, String code, EDomain domain) {
		waitClickable(By.id(ON_REPEAT_LABEL.replace("0", STAGE_INDEX)));
		final long updateIndex = getNumberOfVariableUpadtesOnRepeat();
		find(ON_REPEAT_LABEL.replace("0", STAGE_INDEX)).click();
		waitClickable(By.id(ON_REPEAT_ITEMS.replace("0", STAGE_INDEX)));
		findChildElementByTagAndText(find(ON_REPEAT_ITEMS.replace("0", STAGE_INDEX)), "li", variableName).click();
		
		find(ON_REPEAT_ADD_UPDATE.replace("0", STAGE_INDEX)).click();	
		waitClickable(By.id(ON_REPEAT_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		find(ON_REPEAT_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).click();
		waitClickable(By.id(ON_REPEAT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		if(domain==EDomain.MATH) {
			findChildElementByTagAndText(find(ON_REPEAT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "MATH").click();
		}else {
			findChildElementByTagAndText(find(ON_REPEAT_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "CHEM").click();
		}
		
		find(ON_REPEAT_CODE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).sendKeys(code);
	}
	
	public void removeVariableUpdateOnRepeat(int index) {
		find(ON_REPEAT_DELETE_UPDATE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfVariableUpadtesOnRepeat() {
		return find(ON_REPEAT_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}
	
	public void addVariableUpdateOnSkip(String variableName, String code, EDomain domain) {
		waitClickable(By.id(ON_SKIP_LABEL.replace("0", STAGE_INDEX)));
		final long updateIndex = getNumberOfVariableUpadtesOnSkipt();
		find(ON_SKIP_LABEL.replace("0", STAGE_INDEX)).click();
		waitClickable(By.id(ON_SKIP_ITEMS.replace("0", STAGE_INDEX)));
		findChildElementByTagAndText(find(ON_SKIP_ITEMS.replace("0", STAGE_INDEX)), "li", variableName).click();
		
		find(ON_SKIP_ADD_UPDATE.replace("0", STAGE_INDEX)).click();	
		waitClickable(By.id(ON_SKIP_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		find(ON_SKIP_SELECT_DOMAIN.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).click();
		waitClickable(By.id(ON_SKIP_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)));
		if(domain==EDomain.MATH) {
			findChildElementByTagAndText(find(ON_SKIP_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "MATH").click();
		}else {
			findChildElementByTagAndText(find(ON_SKIP_DOMAIN_ITEMS.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)), "li", "CHEM").click();
		}
		
		find(ON_SKIP_CODE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+updateIndex)).sendKeys(code);
	}
	
	public void removeVariableUpdateOnSkip(int index) {
		find(ON_SKIP_DELETE_UPDATE.replace("tabs0", "tabs"+STAGE_INDEX).replace(":0", ":"+index)).click();
		waitUntilActionBarIsNotActive();
	}
	
	private long getNumberOfVariableUpadtesOnSkipt() {
		return find(ON_SKIP_DATA.replace("0", STAGE_INDEX)).findElements(By.tagName("tr")).stream().filter(ele -> ele.getAttribute("role")!=null && ele.getAttribute("data-ri")!=null).count();
	}

}
