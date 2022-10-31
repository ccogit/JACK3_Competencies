package de.uni_due.s3.jack3.business.stagetypes;

import static de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderConstants.EXERCISE_IDENTIFIER;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.openmath.OMA;
import org.openmath.OMEL;
import org.openmath.OMF;
import org.openmath.OMI;
import org.openmath.OMOBJ;
import org.openmath.OMS;
import org.openmath.OMSTR;

import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.SubmissionField;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionLogEntryService;
import de.uni_due.s3.openobject.OpenObject;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class FillInStageBusiness extends AbstractStageBusiness implements Serializable {

	// REVIEW: in eigene Datei auslagern?
	private static final class FeedbackJoiner {

		final StringJoiner stringJoiner;

		private FeedbackJoiner() {
			stringJoiner = new StringJoiner("</li><li>", "<ul><li>", "</li></ul>");
			stringJoiner.setEmptyValue("");
		}

		private void add(final String feedback) {
			if ((feedback != null) && !feedback.isBlank()) {
				stringJoiner.add(feedback);
			}
		}

		@Override
		public String toString() {
			return stringJoiner.toString();
		}
	}

	private static final long serialVersionUID = 6062746824056036828L;

	private static final String NAME = "name";
	public static final String HTML_INPUT = "<input";
	public static final String HTML_SELECT = "<select";
	public static final String TASK_DESCRIPTION_DIVIDER_PATTERN = "<input(.+?)/>|<select(.+?)</select>";

	private static final Pattern NUMBER_PATTERN = Pattern
			.compile("^(?<operator>[\\+\\-])?(?:(?<integer>\\d+)|(?<predecimal>\\d*)[\\.\\,](?<postdecimal>\\d+))$");

	private static final String OPENMATH_INPUT_BOX = "<OMOBJ xmlns='http://www.openmath.org/OpenMath' version='2.0' cdbase='http://www.openmath.org/cd'>"
			+ "<OMS cd='editor1' name='input_box'/></OMOBJ>";

	public static final int CORRECT_ANSWER_POINTS = 100;

	@Inject
	private ResultService resultService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private SubmissionLogEntryService submissionLogEntryService;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stagesubmission) {
		assureCorrectClassUsage(FillInSubmission.class, stagesubmission);

		final FillInSubmission fillInSubmission = (FillInSubmission) stagesubmission;

		FillInStage fillInStage = (FillInStage) stage;

		try {
			List<SubmissionField> submissionFields = getSubmissionFields(submission, stagesubmission, fillInStage);
			fillInSubmission.addSubmissionFields(submissionFields);
		} catch (IllegalStateException ise) {
			final SubmissionLogEntry logEntry = submissionLogEntryService.persistSubmissionLogEntryWithText(
					ESubmissionLogEntryType.FAIL, "Failed to prepare stage submission: " + ise.getMessage());
			submission.addSubmissionLogEntry(logEntry);
			submission.setHasInternalErrors(true);
		}

		return fillInSubmission;
	}

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(FillInSubmission.class, stagesubmission);

		final FillInSubmission fillInSubmission = (FillInSubmission) stagesubmission;

		int points = 0;
		final FeedbackJoiner feedback = new FeedbackJoiner();

		FillInStage fillInStage = (FillInStage) stage;
		// Correct Answer Feedback
		boolean correctAnswerMatched = false;
		for (Rule correctAnswerRule : fillInStage.getCorrectAnswerRulesAsList()) {
			if (correctAnswerRule.getValidationExpression().isEmpty()) {
				continue;
			}
			try {
				if (evaluatorBusiness.calculateToBoolean(correctAnswerRule.getValidationExpression(), evaluatorMaps)) {
					points = CORRECT_ANSWER_POINTS;
					feedback.add(fillInStage.getCorrectAnswerFeedback());
					correctAnswerMatched = true;
					break;
				}
			} catch (InternalErrorEvaluatorException e) {
				return getStageSubmissionForEvaluationException(e, e.getMessage(), fillInSubmission, submission,
						correctAnswerRule.getValidationExpression(), fillInStage);
			}
		}

		// if correct answer not matched check if other feedback matches else user get default feedback
		if (!correctAnswerMatched) {
			boolean userGetsDefaultFeedback = true;

			// Iterate over rules to generate feedback and points
			for (Rule rule : fillInStage.getFeedbackRulesAsList()) {

				EvaluatorExpression ruleExpressionEvaluatorNotation = rule.getValidationExpression();
				try {
					// In FillIn-Feedback an empty Evaluator Expression is interpreted as False and will be skipped
					// See also #438
					if (!ruleExpressionEvaluatorNotation.isEmpty()
							&& evaluatorBusiness.calculateToBoolean(ruleExpressionEvaluatorNotation, evaluatorMaps)) {
						userGetsDefaultFeedback = false;
						points += rule.getPoints();
						feedback.add(rule.getFeedbackText());

						if (rule.isTerminal()) {
							break;
						}
					}
				} catch (InternalErrorEvaluatorException e) {
					return getStageSubmissionForEvaluationException(e, e.getMessage(), fillInSubmission, submission,
							ruleExpressionEvaluatorNotation, fillInStage);
				}
			}

			if (userGetsDefaultFeedback) {
				feedback.add(fillInStage.getDefaultFeedback());
				points = fillInStage.getDefaultResult();
			}
		}

		// Score must be between 0 and 100!
		points = Math.min(100, Math.max(0, points));

		// Report result
		// 1. Create result object
		Result result = new Result(fillInSubmission);

		// 2. Add additional data to result object
		result.setPoints(points);
		result.setPublicComment(feedback.toString());

		// 3. Store additional data
		resultService.persistResult(result);

		// 4. Add result to stagesubmission
		return exercisePlayerBusiness.addResultToSubmission(submission, fillInSubmission, stage, result);
	}

	private StageSubmission getStageSubmissionForEvaluationException(Exception e, String enMessage,
			FillInSubmission fillInSubmission, Submission submission,
			EvaluatorExpression ruleExpressionEvaluatorNotation, FillInStage fillInStage) {
		getLogger().warnf(e,
				"Cannot evaluate validation expression %s in stage with id %s. Submission result may thus be incorrect!",
				ruleExpressionEvaluatorNotation, fillInSubmission.getStageId());

		return exercisePlayerBusiness.addFailureToSubmission(submission, fillInSubmission,
				"Cannot evaluate validation expression " + ruleExpressionEvaluatorNotation.getCode() + " in stage "
						+ fillInStage.getInternalName() + ": " + enMessage,
				e.getCause());
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stagesubmission, Submission submission) {
		if (!stagesubmission.getResults().isEmpty()) {
			stagesubmission.setHasPendingChecks(false);

			if (stagesubmission.getResults().size() > 1) {
				stagesubmission.setHasInternalErrors(true);
			} else {
				stagesubmission.setHasInternalErrors(false);
				stagesubmission.setPoints(stagesubmission.getResults().iterator().next().getPoints());
			}
		} else {
			stagesubmission.setHasPendingChecks(true);
			stagesubmission.setHasInternalErrors(false);
			stagesubmission.setPoints(0);
		}
		return stageSubmissionService.mergeStageSubmission(stagesubmission);
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps) throws InternalErrorEvaluatorException {
		assureCorrectClassUsage(FillInSubmission.class, stagesubmission);
		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	public void removeFeedbackFromStage(int feedbackRuleOrderIndex, FillInStage stage) {
		stage.removeFeedbackRule(feedbackRuleOrderIndex);
		List<Rule> rules = stage.getFeedbackRulesAsList();
		setNewRuleOrderOnStage(stage, rules);
	}

	public void reorderFeedbackRules(FillInStage stage, int fromIndex, int toIndex) {
		List<Rule> rules = stage.getFeedbackRulesAsList();
		final Rule ruleToReorder = rules.remove(fromIndex);
		rules.add(toIndex,ruleToReorder);
		setNewRuleOrderOnStage(stage, rules);
	}

	private void setNewRuleOrderOnStage(FillInStage stage, List<Rule> rules) {
		int i = 0;
		for (Rule rule : rules) {
			rule.setOrderIndex(i);
			i++;
		}
		Collections.sort(rules);
		stage.addFeedbackRules(rules);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission stageSubmission) {
		HashMap<String, VariableValue> inputs = new HashMap<>();

		FillInSubmission fillInSubmission = ((FillInSubmission) stageSubmission);
		for (SubmissionField submissionField : fillInSubmission.getOrderedSubmissionFields()) {
			if (submissionField instanceof DropDownSubmissionField) {
				DropDownSubmissionField dropDownSubmissionField = (DropDownSubmissionField) submissionField;
				int chosenDropDown = getSelectedDropDownPosition(dropDownSubmissionField);
				VariableValue dropdownPosition = VariableValueFactory
						.createVariableValueForOpenMathInteger(chosenDropDown);
				inputs.put(submissionField.getFieldName(), dropdownPosition);
			}
			if (submissionField instanceof FillInSubmissionField) {
				FillInSubmissionField fillInSubmissionField = (FillInSubmissionField) submissionField;
				VariableValue inputValue = new VariableValue();
				inputValue.setContent(getFillInFieldUserInput(fillInSubmissionField));
				inputs.put(submissionField.getFieldName(), inputValue);
			}
		}

		return inputs;
	}

	/**
	 * Split task description in text, fillIn and dropDown fields The fillIn and dropDown fields are replaced by
	 * primefaces Elements. Variables are replaced in first Step.
	 */
	private List<SubmissionField> getSubmissionFields(Submission submission, StageSubmission stagesubmission,
			FillInStage fillInStage) {
		String htmlText = fillInStage.getTaskDescription();
		String text = exercisePlayerBusiness.resolvePlaceholders(htmlText, submission, stagesubmission, fillInStage,
				true);

		Pattern patternInputOrSelect = Pattern.compile(TASK_DESCRIPTION_DIVIDER_PATTERN);
		Matcher matcherInputOrSelect = patternInputOrSelect.matcher(text);

		List<SubmissionField> submissionFields = new ArrayList<>();

		while (matcherInputOrSelect.find()) {
			String taskDescriptionPart = matcherInputOrSelect.group();
			if (HTML_INPUT.equals(taskDescriptionPart.substring(0, 6))) {
				submissionFields.add(getFillInSubmissionField(taskDescriptionPart, fillInStage));
			} else if (HTML_SELECT.equals(taskDescriptionPart.substring(0, 7))) {
				submissionFields
						.add(getDropDownSubmissionField(taskDescriptionPart, submission, stagesubmission, fillInStage));
			}
		}
		return submissionFields;
	}

	/**
	 * Extracts the Attribute Value
	 *
	 * @param attributeName
	 *            name of the attribute
	 * @param htmlElementCode
	 * @return value of the attribute
	 */
	public String getAttributeValue(String attributeName, String htmlElementCode) {
		Pattern attributPattern = Pattern.compile(attributeName + "=[\"]([^\"]*)[\"]");
		Matcher attributMatcher = attributPattern.matcher(htmlElementCode);
		if (!attributMatcher.find(0)) {
			return null;
		}

		return attributMatcher.group().substring(attributeName.length() + 2, attributMatcher.group().length() - 1);
	}

	private FillInSubmissionField getFillInSubmissionField(String inputFieldHtmlCode, FillInStage fillInStage) {
		String inputFieldName = getAttributeValue(NAME, inputFieldHtmlCode);
		if (inputFieldName == null) {
			throw new IllegalArgumentException("FillIn field without name:" + inputFieldHtmlCode);
		}
		FillInField fillInField = getFillInFieldForName(inputFieldName, fillInStage);
		FillInSubmissionField fillInSubmissionField = null;
		switch (fillInField.getFormularEditorEnumType()) {
		case NONE:
		case TEXT:
		case NUMBER:
			fillInSubmissionField = new FillInSubmissionField(fillInField.getName(),
					EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD, fillInField.getFormularEditorEnumType(),
					fillInField.getSize());
			fillInSubmissionField.setUserInput("");
			break;
		default:
			fillInSubmissionField = new FillInSubmissionField(fillInField.getName(),
					EFillInSubmissionFieldType.TYPE_MATHDOX_FIELD, fillInField.getFormularEditorEnumType(),
					fillInField.getSize());
			fillInSubmissionField.setUserInput(OPENMATH_INPUT_BOX);
		}
		return fillInSubmissionField;
	}

	private FillInField getFillInFieldForName(String name, FillInStage fillInStage) {
		FillInField fillInField = null;
		for (FillInField field : fillInStage.getFillInFields()) {
			if (field.getName().equals(name)) {
				fillInField = field;
				break;
			}
		}
		if (fillInField == null) {
			throw new IllegalStateException("FillInField " + name + " not found");
		}
		return fillInField;
	}

	private DropDownSubmissionField getDropDownSubmissionField(String inputFieldHtmlCode, Submission submission,
			StageSubmission stagesubmission, FillInStage fillInStage) {
		String inputFieldName = getAttributeValue(NAME, inputFieldHtmlCode);
		if (inputFieldName == null) {
			throw new IllegalArgumentException("DropDown field without name:" + inputFieldHtmlCode);
		}
		DropDownField dropDownField = getDropDownFieldForName(inputFieldName, fillInStage);

		List<String> items = new ArrayList<>();

		// If the only answer option is a variable with a list, the drop-down items are replaced with the list.
		boolean answerOptionsProcessed = false;
		if ((dropDownField.getItems().size() == 1)
				&& dropDownField.getItems().get(0).matches("\\$?\\[" + EXERCISE_IDENTIFIER + "=.+\\]\\$?")) {
			String variableName = dropDownField.getItems().get(0);
			boolean renderInLatex = variableName.startsWith("$") && variableName.endsWith("$");
			variableName = variableName.replace("$", "");
			variableName = variableName.substring(5, variableName.length() - 1);

			try {
				VariableValue varValue = stagesubmission.getVariableValues().get(variableName);
				for (VariableValue variableListItem : converterBusiness.convertToList(varValue)) {
					if (renderInLatex) {
						items.add("$" + converterBusiness.convertToLaTeX(variableListItem) + "$");
					} else {
						items.add(converterBusiness.convertToString(variableListItem));
					}
				}
				answerOptionsProcessed = true;
			} catch (Exception e) {
				// If the list could not be extracted from the variable, don't replace the items and reset items
				items.clear();
				getLogger().warnf(e, "Cannot extract answer option list from variable '%s' for field '%s'.",
						variableName, inputFieldName, e);
			}
		}

		// If there was an error while replacing the drop-down items with the variable or the field does not have
		// dynamic answer options, just process the saved drop-down items and replace variables
		if (!answerOptionsProcessed) {
			for (String dropDownItem : dropDownField.getItems()) {
				String itemText = exercisePlayerBusiness.resolvePlaceholders(dropDownItem, submission,
						stagesubmission,
						fillInStage, true);
				items.add(itemText);
			}
		}

		// Save original order and reorder items if randomize is checked
		List<String> itemsWithoutRandomizedOrder = new ArrayList<>();

		if (dropDownField.getRandomize()) {
			itemsWithoutRandomizedOrder.addAll(items);
			Collections.shuffle(items);
		}

		return new DropDownSubmissionField(dropDownField.getName(), EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD,
				items, dropDownField.getRandomize(), itemsWithoutRandomizedOrder);
	}

	private DropDownField getDropDownFieldForName(String name, FillInStage fillInStage) {
		DropDownField dropDownField = null;
		for (DropDownField field : fillInStage.getDropDownFields()) {
			if (field.getName().equals(name)) {
				dropDownField = field;
				break;
			}
		}
		if (dropDownField == null) {
			throw new IllegalStateException("DropDownField " + name + " not found");
		}
		return dropDownField;
	}

	public Integer getSelectedDropDownPosition(DropDownSubmissionField dropDownSubmissionField) {
		String userInput = dropDownSubmissionField.getUserInput();
		if (userInput == null) {
			return DropDownSubmissionField.NO_ITEM_SELECTED;
		}
		int position = 0;
		boolean found = false;
		List<String> items = null;
		if (dropDownSubmissionField.isDropDownRandomized()) {
			items = dropDownSubmissionField.getItemsWithoutRandomizedOrder();
		} else {
			items = dropDownSubmissionField.getItems();
		}
		for (String item : items) {
			if (item.equals(userInput)) {
				found = true;
				break;
			}
			position++;
		}
		if (found) {
			return position;
		}
		return DropDownSubmissionField.NO_ITEM_SELECTED;
	}

	/**
	 * Returns the input to a stagesubmission field in string representation. Currently, any input is transformed into
	 * an
	 * OpenObject and than marshalled into XML.
	 *
	 * @param field
	 *            fillin stagesubmission field
	 * @return String
	 */
	public String getFillInFieldUserInput(FillInSubmissionField field) {
		String userInput = field.getUserInput();

		if (field.getFieldType() == EFillInSubmissionFieldType.TYPE_MATHDOX_FIELD) {
			OpenObject interpreted = OpenObjectConverter.fromXmlString(userInput);
			return OpenObjectConverter.toXmlString(interpreted);
		}

		OMEL omel = null;

		if (field.getEditorType() == EFillInEditorType.NONE) {
			omel = parseNumber(userInput);
		}

		if (field.getEditorType() == EFillInEditorType.TEXT) {
			omel = OMSTR.of(userInput);
		} else {
			omel = parseNumber(userInput);
		}

		return OpenObjectConverter.toXmlString(OpenObject.of(OMOBJ.of(omel)));
	}

	private OMEL parseNumber(String input) {
		if (input != null) {
			OMEL result = null;
			Matcher matcher = NUMBER_PATTERN.matcher(input.strip());

			if (!matcher.matches()) {
				return OMSTR.of(input);
			}
			if (matcher.group("integer") != null) {
				result = OMI.of(new BigInteger(matcher.group("integer")));
			} else if ((matcher.group("predecimal") != null) && (matcher.group("postdecimal") != null)) {
				// input is a double
				double value = Double.parseDouble(matcher.group("predecimal") + "." + matcher.group("postdecimal"));
				if (value == Double.POSITIVE_INFINITY) {
					result = OMS.of("nums1", "infinity");
				} else if (String.valueOf(value).contains("E")) {
					// We want to save bigfloats not as MEFloat
					// we use MEBigFloat instead
					String[] number = String.valueOf(value).split("E");
					double base = Double.parseDouble(number[0]);

					result = OMA.of(OMS.of("bigfloat1", "bigfloat"),
							Arrays.asList(OMF.of(base), OMI.of(10), OMI.of(new BigInteger(number[1]))));
				} else {
					result = OMF.of(value);
				}
			} else {
				throw new JackRuntimeException("RegexException: Regex supports only integer or double value.");
			}

			// 2. check if unary operator
			if (matcher.group("operator") != null) {
				OMEL child = result;
				if ("+".equals(matcher.group("operator"))) {
					result = OMA.of(OMS.of("arith1", "unary_plus"), Arrays.asList(child));
				} else if ("-".equals(matcher.group("operator"))) {
					result = OMA.of(OMS.of("arith1", "unary_minus"), Arrays.asList(child));
				} else {
					throw new JackRuntimeException("RegexException: Regex supports only - or + as unary operator.");
				}
			}

			if (result == null) {
				throw new JackRuntimeException("RegexException: Regex should not match if is no integer or double.");
			}
			return result;
		}
		return OMSTR.of("");
	}

	public OpenObject getSelectedDropDownPositionAsOpenObject(DropDownSubmissionField dropDownSubmissionField) {
		Integer itemPos = getSelectedDropDownPosition(dropDownSubmissionField);
		OMEL itemPosOMEL = OMI.of(itemPos);
		return OpenObject.of(OMOBJ.of(itemPosOMEL));
	}

	public String getFillInFieldHtmlCode(String name, int size) {
		StringBuilder fillInFieldHTML = new StringBuilder(100);
		fillInFieldHTML.append("&nbsp;<input ");
		fillInFieldHTML.append("name=");
		fillInFieldHTML.append("\"" + name + "\"");
		fillInFieldHTML.append(" size=");
		fillInFieldHTML.append("\"" + size + "\"");
		fillInFieldHTML.append(" type=\"text\"");
		fillInFieldHTML.append(" value=");
		fillInFieldHTML.append("\"" + name + "\"");
		fillInFieldHTML.append(" />&nbsp;");
		return fillInFieldHTML.toString();
	}

	public String getDropDownFieldHtmlCode(String name) {
		StringBuilder dropDownFieldHTML = new StringBuilder(100);
		dropDownFieldHTML.append("&nbsp;<select ");
		dropDownFieldHTML.append("name=");
		dropDownFieldHTML.append("\"" + name + "\"");
		dropDownFieldHTML.append("><option value= \"0\" >");
		dropDownFieldHTML.append(name);
		dropDownFieldHTML.append("</option></select>&nbsp;");
		return dropDownFieldHTML.toString();
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(FillInSubmission.class, stagesubmission);

		return new HashMap<>();
	}
}
