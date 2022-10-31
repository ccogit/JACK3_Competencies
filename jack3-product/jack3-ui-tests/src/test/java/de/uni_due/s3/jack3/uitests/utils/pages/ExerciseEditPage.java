package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitUntilActionBarIsNotActive;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Find;
import de.uni_due.s3.jack3.uitests.utils.Time;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.FillInStagePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.JavaStagePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.MCStagePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.PythonStagePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.RStagePage;
import de.uni_due.s3.jack3.uitests.utils.pages.stages.UmlStagePage;

public class ExerciseEditPage {

	private static final String SAVE_BUTTON_ID = "exerciseEdit:saveExercise";
	private static final String VALIDATE_BUTTON_ID = "exerciseEdit:validateExercise";
	private static final String TEST_EXERCISE_BUTTON_ID = "exerciseEdit:testExercise";
	private static final String DISCARD_CHANGES_ID = "exerciseEdit:discardChanges";

	private static final String SELECT_FROZEN_REVISION_ID = "exerciseEdit:frozenRevisionsDropDown";

	private static final String TITLE_ID = "exerciseEdit:exerciseName_display";
	private static final String TITLE_INPUT_ID = "exerciseEdit:exerciseNameInput";
	private static final String TITLE_EDITOR_ID = "exerciseEdit:exerciseName_editor";

	private static final String AUTHOR_LABEL_ID = "exerciseEdit:author";

	private static final String LANGUAGE_LABEL_ID = "exerciseEdit:language_label";
	private static final String LANGUAGE_OPTIONS_ID = "exerciseEdit:language_";

	private static final String REVISIONS_ID = "exerciseEdit:rev";

	private static final String DIFFICULTY_INPUT_ID = "exerciseEdit:difficulty_input";
	private static final String TAG_INPUT_ID = "exerciseEdit:newTagName_input";
	private static final String INTERNAL_NOTES_ID = "exerciseEdit:internalNotes";
	private static final String MALUS_TYPE_LABEL_ID = "exerciseEdit:malusTypes_label";
	private static final String MALUS_TYPE_ID = "exerciseEdit:malusTypes_";

	// Variables configuration tab
	private static final String VARIABLE_CONTENT_ID = "exerciseEdit:varConfig_content";
	private static final String ADD_VARIABLE_ID = "exerciseEdit:cbVarConfigAddVariable";
	private static final String VARIABLE_DECLARATION_ID = "exerciseEdit:dtVarConfigVariableDeclarations";
	private static final String VARIABLE_NAME_ID = ":editorVarConfig";
	private static final String VARIAVLE_RENAME_INPUT_ID = ":inputVarConfigVariableName";
	private static final String VARIABLE_CODE_ID = ":variableInitializationExpression_initCodeInput";
	private static final String VARIABLE_DOMAIN_ID = ":variableInitializationExpression_selectDomainInput";
	private static final String VARIABLE_REMOVE_ID = ":cbVarConfigRemoveVariable";

	private static final String SUBMISSION_COUNT_ID = "exerciseEdit:submissionCount";
	private static final String TESTSUBMISSION_COUNT_ID = "exerciseEdit:testSubmissionsHint";
	private static final String EXERCISE_SUBMISSIONS_ID = "exerciseEdit:toExerciseSubmissions";
	
	private static final String CREATE_STAGE = "exerciseEdit:cbCreateStage";
	private static final String SELECT_STAGE_TYPE = "exerciseEdit:addStageType_label";
	private static final String STAGE_TYPES_ITEMS = "exerciseEdit:addStageType_items";
	
	private static final String STAGE_CONTENT = "exerciseEdit:step_content";  
	private static final String STAGE_HEADER = "exerciseEdit:stage0_header";
	private static final String EXPAND_STAGE = "exerciseEdit:stage0_toggler";
	
	private static final String ACTIVITY_BAR = "activity-bar";
	

	public static void testExercise() {
		waitClickable(By.id(TEST_EXERCISE_BUTTON_ID));
		find(TEST_EXERCISE_BUTTON_ID).click();
		// wait until the new Page is loaded
		waitClickable(By.id("exerciseTest:backToExercise"));
	}

	public static void saveExercise() {
		waitClickable(By.id(SAVE_BUTTON_ID));
		int oldRevisionNumber = getNumberOfRevisions();
		find(SAVE_BUTTON_ID).click();
		//wait until saving is completed
		String newRevisionNumber = Integer.toString(oldRevisionNumber + 1);
		Time.wait(ExpectedConditions.textToBe(By.id(REVISIONS_ID), newRevisionNumber),
				"The Revision Count doesn't show the right number");
	}

	public static void discardChanges() {
		waitClickable(By.id(DISCARD_CHANGES_ID));
		find(DISCARD_CHANGES_ID).click();
	}

	public static RevisionPage selectFrozenRevision(int revisionNumber) {
		waitClickable(By.id(SELECT_FROZEN_REVISION_ID + "_label"));
		find(SELECT_FROZEN_REVISION_ID + "_label").click();
		waitClickable(By.id(SELECT_FROZEN_REVISION_ID + "_0"));

		List<WebElement> items = findChildren(find(SELECT_FROZEN_REVISION_ID + "_items"));

		for (WebElement item : items) {
			if (item.getText().contains(revisionNumber + ": ")) {
				item.click();
				return new RevisionPage();
			}
		}
		// No Revision is selected
		find(SELECT_FROZEN_REVISION_ID + "_0").click();
		return null;
	}

	public static String getTitle() {
		waitClickable(By.id(TITLE_ID));
		return find(TITLE_ID).getText();
	}

	// changes the title of the Exercise
	public static void changeTitle(String newTitle) {
		waitClickable(By.id(TITLE_ID));
		find(TITLE_ID).click();
		waitClickable(By.id(TITLE_INPUT_ID));
		find(TITLE_INPUT_ID).sendKeys(newTitle);
		findChildren(find(TITLE_EDITOR_ID)).get(0).click();
	}

	// returns the Name of the Author of the Exercise
	public static String getAuthor() {
		waitClickable(By.id(AUTHOR_LABEL_ID));
		return find(AUTHOR_LABEL_ID).getText();
	}

	/**
	 * Changes the Language of the Exercise
	 *
	 * @param language
	 *            use "de" for german and "en" for english
	 */
	public static void changeTheLanguageOfTheExercise(String language) {
		language = language.strip().toLowerCase();
		waitClickable(By.id(LANGUAGE_LABEL_ID));
		find(LANGUAGE_LABEL_ID).click();
		waitClickable(By.id(LANGUAGE_OPTIONS_ID + "0"));
		if (language.equals("de")) {
			find(LANGUAGE_OPTIONS_ID + "0").click();
		} else if (language.equals("en")) {
			find(LANGUAGE_OPTIONS_ID + "1").click();
		} else {
			throw new AssertionFailedError("The Language '" + language + "' is not supported");
		}

	}

	public static int getNumberOfRevisions() {
		waitClickable(By.id(REVISIONS_ID));
		return Integer.parseInt(find(REVISIONS_ID).getText());
	}

	public static RevisionDialogue openRevisions() {
		waitClickable(By.id(REVISIONS_ID));
		find(REVISIONS_ID).click();
		return new RevisionDialogue();
	}

	public static class RevisionDialogue {

		private final String REVISION_PANEL_ID = "exerciseEdit:dtRevisionPanel";
		private final String FREEZE_BUTTON_ID = ":cbfreezeRevision";
		private final String SHOW_REVISION_ID = ":cbShowRevision";

		public void freezeRevision(int revisionNumber) {
			waitClickable(By.id(REVISION_PANEL_ID + ":" + revisionNumber + FREEZE_BUTTON_ID));
			find(REVISION_PANEL_ID + ":" + revisionNumber + FREEZE_BUTTON_ID).click();
		}

		public RevisionPage openRevision(int revisionNumber) {
			waitClickable(By.id(REVISION_PANEL_ID + ":" + revisionNumber + SHOW_REVISION_ID));
			find(REVISION_PANEL_ID + ":" + revisionNumber + SHOW_REVISION_ID).click();
			RevisionPage revisionPage = new RevisionPage();
			waitClickable(By.id(revisionPage.JUMP_TO_CURRENT_REVISION_ID));
			return revisionPage;
		}
	}

	public static class RevisionPage {
		private final String RESET_TO_REVISION_ID = "exerciseEdit:resetToRevision";
		private final String JUMP_TO_CURRENT_REVISION_ID = "exerciseEdit:jumpToCurrentRevision";

		private final String FROZEN_TITLE_INPUT_ID = "exerciseEdit:titelFrozenVersion";
		private final String SAVE_FROZEN_TITLE_ID = "exerciseEdit:ajax";

		private final String EXERCISE_NAME_ID = "exerciseEdit:exerciseName_display";

		public void jumpToNewestRevision() {
			waitClickable(By.id(JUMP_TO_CURRENT_REVISION_ID));
			find(JUMP_TO_CURRENT_REVISION_ID).click();
		}

		public void acceptThisRevisionAsNew() {
			waitClickable(By.id(RESET_TO_REVISION_ID));
			find(RESET_TO_REVISION_ID).click();
		}

		public void changeFrozenTitle(String newTitle) {
			waitClickable(By.id(FROZEN_TITLE_INPUT_ID));
			find(FROZEN_TITLE_INPUT_ID).click();
			find(FROZEN_TITLE_INPUT_ID).sendKeys(newTitle);
			waitClickable(By.id(SAVE_FROZEN_TITLE_ID));
			Time.wait(ExpectedConditions.attributeToBe(find(FROZEN_TITLE_INPUT_ID), "value", newTitle), "new frozen title could not be set.");
			find(SAVE_FROZEN_TITLE_ID).click();

			//wait until the growl shows that the saving action was successful
			Time.wait(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#globalGrowl_container>*"), 0), "new frozen title could not be saved");
		}

		public String getExerciseName() {
			waitClickable(By.id(EXERCISE_NAME_ID));
			return find(EXERCISE_NAME_ID).getText();
		}

		public void checkThatUiIsDisabled() {
			waitNotClickable(By.id(DIFFICULTY_INPUT_ID));

			waitNotClickable(By.id(TAG_INPUT_ID));
			waitNotClickable(By.id(INTERNAL_NOTES_ID));
			assertFalse(find(MALUS_TYPE_ID + "input").isEnabled());
			assertFalse(find(TAG_INPUT_ID).isEnabled());
		}
	}

	public static void changeDifficulty(int newDifficulty) {
		waitClickable(By.id(DIFFICULTY_INPUT_ID));
		find(By.id(DIFFICULTY_INPUT_ID)).click();
		find(DIFFICULTY_INPUT_ID).sendKeys("" + newDifficulty);
	}

	/**
	 * Enter the tagName into the input field for tags. The Tag can not be confirmed yet, because the ui needs a moment
	 * to process the input and wouln't react immediately if enter would be pressed. BEcuase of that you habe to call
	 * the confirmTag() method later on
	 *
	 * @param tagName
	 */
	public static void enterTagName(String tagName) {
		waitClickable(By.id(TAG_INPUT_ID));
		find(TAG_INPUT_ID).sendKeys(tagName);

	}

	/**
	 * Call this method after calling enterTagName(tagName) and some time has passed
	 */
	public static void confirmTag() {
		waitClickable(By.id(TAG_INPUT_ID));
		find(TAG_INPUT_ID).sendKeys(Keys.ENTER);
	}

	public static void setInternalNotes(String notes) {
		waitClickable(By.id(INTERNAL_NOTES_ID));
		find(By.id(INTERNAL_NOTES_ID)).sendKeys(notes);
	}

	public static void setExternalNotes(String notes) {
		WebDriver driver = Driver.get();
		driver.switchTo().frame(driver.findElement(By.tagName("iframe")));
		driver.findElement(By.cssSelector("body")).sendKeys(notes);
		driver.switchTo().defaultContent();
	}

	public static void setMalusType(EStageHintMalus malusType) {
		waitClickable(By.id(MALUS_TYPE_LABEL_ID));
		find(MALUS_TYPE_LABEL_ID).click();
		waitClickable(By.id(MALUS_TYPE_ID + "0"));
		switch (malusType) {
		case CUT_MAXIMUM:
			find(MALUS_TYPE_ID + "1").click();
			break;
		case CUT_ACTUAL:
			find(MALUS_TYPE_ID + "2").click();
			break;
		default:
			find(MALUS_TYPE_ID + "0").click();
			break;
		}

	}

	/**
	 * Create a new Variable for the Exercise
	 *
	 * @return returns the name of the new Variable
	 */
	public static String createNewVariable() {
		waitClickable(By.id(ADD_VARIABLE_ID));
		find(ADD_VARIABLE_ID).click();
		waitClickable(By.id(VARIABLE_DECLARATION_ID + "_data"));
		List<WebElement> variables = findChildren(find(VARIABLE_DECLARATION_ID + "_data"));
		List<WebElement> rows = variables.stream().filter(element -> "row".equals(element.getAttribute("role")))
				.collect(Collectors.toList());

		if (rows.isEmpty()) {
			throw new AssertionFailedError("The new Variable could not be created");
		} else {
			WebElement newRow = rows.get(rows.size() - 1);
			return findChildren(newRow).get(0).getText();
		}

	}

	/**
	 *
	 * @param oldName
	 *            the originally Name of the Variable
	 * @param newName
	 *            the new Name of the Variable
	 * @return if the renaming was successful the newName of the Variable will be returned
	 */
	public static String renameVariable(String oldName, String newName) {
		int rowNumber = getRowNumberFromVariable(oldName);
		find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_NAME_ID).click();
		waitClickable(By.id(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIAVLE_RENAME_INPUT_ID));
		find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIAVLE_RENAME_INPUT_ID).sendKeys(newName);
		findChildren(find(VARIABLE_DECLARATION_ID + ":" + rowNumber + ":editorVarConfig_editor")).get(0).click();
		return newName;
	}

	public static void changeVariableCode(String variableName, String newCode) {
		int rowNumber = getRowNumberFromVariable(variableName);
		find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_CODE_ID).sendKeys(newCode);
	}

	public static void changeVariableDomain(String variableName, EvaluatorExpression.EDomain domain) {
		int rowNumber = getRowNumberFromVariable(variableName);
		find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_DOMAIN_ID + "_label").click();

		if (domain.equals(EDomain.MATH)) {
			find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_DOMAIN_ID + "_0").click();
		} else if (domain.equals(EDomain.CHEM)) {
			find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_DOMAIN_ID + "_1").click();
		} else {
			throw new AssertionFailedError("Domain '" + domain + "' didn't exist");
		}
	}

	public static void removeVariable(String variableName) {
		int rowNumber = getRowNumberFromVariable(variableName);
		find(VARIABLE_DECLARATION_ID + ":" + rowNumber + VARIABLE_REMOVE_ID).click();
	}

	public static void openVariablesTab() {
		find("exerciseEdit:varConfig_toggler").click();
	}

	private static int getRowNumberFromVariable(String variableName) {
		waitClickable(By.id(VARIABLE_DECLARATION_ID + "_data"));
		List<WebElement> variables = findChildren(find(VARIABLE_DECLARATION_ID + "_data"));
		List<WebElement> rows = variables.stream().filter(element -> "row".equals(element.getAttribute("role")))
				.collect(Collectors.toList());
		for (WebElement row : rows) {
			if (findChildren(row).get(0).getText().equals(variableName)) {
				return Integer.parseInt(row.getAttribute("data-ri"));
			}
		}

		return -1;
	}

	public static int getNumberOfStudentSubmissions() {
		waitClickable(By.id(SUBMISSION_COUNT_ID));
		return Integer.parseInt(find(SUBMISSION_COUNT_ID).getText());
	}

	public static int getNumberOfTestSubmissions() {
		try {
			waitClickable(By.id(TESTSUBMISSION_COUNT_ID));
			return Integer.parseInt(find(TESTSUBMISSION_COUNT_ID).getText().replaceAll("[^0-9]", ""));
		} catch (org.openqa.selenium.TimeoutException e) {
			// This field is not visible if the counter is 0
			return 0;
		}
	}

	public static void openSubmissions() {
		waitClickable(By.id(EXERCISE_SUBMISSIONS_ID));
		find(EXERCISE_SUBMISSIONS_ID).click();
		// Wait that the Submissions Page is loaded
		ExerciseSubmissionsPage.getNumberOfStudentSubmissions();
	}
	
	public static void createMCStage(String stageName) {
		createStage(stageName, "Multiple Choice");
	}
	
	public static void createRStage(String stageName) {
		createStage(stageName, "R");
	}
	
	public static void createJavaStage(String stageName) {
		createStage(stageName, "Java");
	}
	
	public static void createPythonStage(String stageName) {
		createStage(stageName, "Python");
	}
	
	public static void createUMLStage(String stageName) {
		createStage(stageName, "UML");
	}
	
	public static void createFillInStage(String stageName) {
		createStage(stageName, "FillIn");
	}
	
	private static void createStage(String stageName, String stageTyp) {
		waitClickable(By.id(SELECT_STAGE_TYPE));
		find(SELECT_STAGE_TYPE).click();
		try {
			waitClickable(By.id(STAGE_TYPES_ITEMS));
		}catch(TimeoutException e) {
			//if the drop down menu doesn't open try to click the label again
			find(SELECT_STAGE_TYPE).click();
			waitClickable(By.id(STAGE_TYPES_ITEMS));
		}
		
		WebElement stageType = findChildren(find(STAGE_TYPES_ITEMS)).stream().filter(item -> item.getAttribute("data-label").equals(stageTyp)).findAny().orElseThrow(() -> new AssertionError());
		stageType.click();
		Time.wait(ExpectedConditions.attributeToBe(stageType, "aria-selected", "true"), "the stageType '"+ stageType +"' could not be selected");
		
		//create stage
		waitClickable(By.id(CREATE_STAGE));
		find(CREATE_STAGE).click();
		waitUntilActionBarIsNotActive();
		
		//set the name of the stage
		WebElement header = find(STAGE_HEADER.replace("0", ""+(getNumberOfStages()-1)));
		findChildren(header).get(2).click();
		WebElement input = header.findElement(By.tagName("input"));
		waitClickable(input);
		input.sendKeys(stageName);
		header.findElement(By.tagName("button")).click();
		
		waitUntilActionBarIsNotActive();
	}
	
	public static void expandStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		waitClickable(By.id(EXPAND_STAGE.replace("0", stageIndex)));
		find(EXPAND_STAGE.replace("0", stageIndex)).click();
	}
	
	public static MCStagePage getMCStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new MCStagePage(stageIndex);
	}
	
	public static FillInStagePage getFillInStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new FillInStagePage(stageIndex);
	}
	
	public static RStagePage getRStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new RStagePage(stageIndex);
	}
	
	public static JavaStagePage getJavaStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new JavaStagePage(stageIndex);
	}
	
	public static PythonStagePage getPythonStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new PythonStagePage(stageIndex);
	}
	
	public static UmlStagePage getUmlStage(String stageName) {
		WebElement stageRoot = getStageByName(stageName);
		String stageIndex = stageRoot.getAttribute("id").replace("exerciseEdit:stage", "");
		
		return new UmlStagePage(stageIndex);
	}
	
	private static WebElement getStageByName (String stageName) {	
		//be sure we don't run into a StaleElementReferenceException (see also https://stackoverflow.com/questions/12967541/how-to-avoid-staleelementreferenceexception-in-selenium)
		new WebDriverWait(Driver.get(), Time.TIMEOUT_DEFAULT).ignoring(StaleElementReferenceException.class).until(ExpectedConditions.elementToBeClickable(By.id(STAGE_CONTENT)));
		waitClickable(By.id(STAGE_CONTENT));

		WebElement stageContents = find(By.id(STAGE_CONTENT));
		WebElement stageNameElement = Find.findChildElementByTagAndText(stageContents, "span", stageName);				

		//try multiple times to find the root of the stage. We have to do this again to prevent a StaleElementException (http://darrellgrainger.blogspot.com/2012/06/staleelementexception.html)
	    int attempts = 0;
	    while(attempts < 2) {
	        try {
	        	// go 3 elements up from 'stageNameElement' to get the root of the stage
	            return stageNameElement.findElement(By.xpath("./../../.."));
	        } catch(StaleElementReferenceException e) {
	        	if(attempts+1 >= 2) {
	        	    throw new AssertionError("The Stage '"+stageName+"' could not be found, because of a StaleElementReferenceException",e);
	        	}
	        	//ignore the exception silently and try to find the element again
	        }
	        attempts++;
	    }
	   
	    throw new AssertionError("The Stage '"+stageName+"' could not be found");		
	}
	
	public static int getNumberOfStages() {
		waitClickable(By.id(STAGE_CONTENT));
		List<WebElement> childElements = find(STAGE_CONTENT).findElements(By.xpath("./*"));
		if(childElements.size()<3) {
			return 0;
		}else {
			return childElements.size() - 2;
		}
	}

}