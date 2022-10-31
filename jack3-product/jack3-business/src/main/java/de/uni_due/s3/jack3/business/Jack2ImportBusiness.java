package de.uni_due.s3.jack3.business;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_due.s3.evaluatorconverter.EvaluatorConverter;
import de.uni_due.s3.evaluatorconverter.EvaluatorConverterException;
import de.uni_due.s3.jack2.backend.checkers.dynamicrchecker.checkerconfiguration.Checkerconfiguration;
import de.uni_due.s3.jack2.backend.checkers.dynamicrchecker.checkerconfiguration.Checkerconfiguration.MetaInf;
import de.uni_due.s3.jack2.backend.checkers.dynamicrchecker.checkerconfiguration.Checkerconfiguration.Testcases.Testcase;
import de.uni_due.s3.jack2import.Business.ImportBusiness;
import de.uni_due.s3.jack2import.Business.Jack2ExcerciseData;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Element;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Exercisechain;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Input;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Output;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Path;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Step;
import de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Variables;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Answers;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Choice;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Correctanswer;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Inputvalue;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Option;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Outputvalue;
import de.uni_due.s3.jack2import.jack2Objects.stageData.Ruleviolation;
import de.uni_due.s3.jack2import.textUtils.fillIn.TaskDescriptionReadFillInField;
import de.uni_due.s3.jack2import.textUtils.fillIn.TaskDescriptionReadFillInField.FillInFieldJack2Information;
import de.uni_due.s3.jack2import.textUtils.fillIn.TaskDescriptionReadFillInField.Jack2FillInFieldType;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.helpers.EFolderChildType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.business.stagetypes.FillInStageBusiness;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.enums.EMCRuleType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCAnswer;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCFeedback;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.DynamicRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestCasePointsMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestcaseRuleMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.JSXGraph;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class Jack2ImportBusiness extends AbstractBusiness {

	private static enum StageEvent {
		START,
		BEFORE_CHECK,
		SKIP,
		EXIT
	}

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private FillInStageBusiness fillInStageBusiness;

	// REVIEW lg - Does it make sense to store some kind of "state" in a business class?
	private Jack2ExcerciseData jack2Data;
	private Exercise jack3Exercise;
	private Map<String, StageContainer> stageMap;
	private EvaluatorConverter evaluatorConverterWithoutInput;
	private int exerciseFracNumCounter = 1;

	private List<String> converterErrorMessages = new ArrayList<>();

	private static final Pattern PATTERN_ONLY_NUMBERS = Pattern.compile("\\d+");
	private static final Pattern PATTERN_STARONEZERO_VAR = Pattern.compile("[\\*10]|\\[var=(.+?)\\]");
	private static final Pattern PATTERN_VAR = Pattern.compile(
			"\\[var=" + // The "[var=" prefix
					"(?<identifier>\\w+)" + // A named capturing group for the identifier
			"\\]"); // The closing bracket.


	private static final Pattern EVAL_PATTERN = Pattern
			.compile("\\[(" + "eval" + "|" + "symja" + "|" + "r" + "|" + "sage" + ")=\\\"([^\\\"]+)\\\"\\]");
	private static final Pattern PATTERN_FRAC_NUM = Pattern.compile("\\[frac num=(.+?) denom=(.+?)\"\\]");
	private static final Pattern PATTERN_ANYTHING_INSIDE_QOUTION_MARKS = Pattern.compile("\"(.+?)\"");
	private static final Pattern PATTERN_IMAGE = Pattern.compile("\\[img(.+?)/img\\]");
	private static final Pattern PATTERN_ANYTHING_INSIDE_CLOSING_AND_OPENING_SQUARE_BRACKET = Pattern
			.compile("\\](.+?)\\[");

	private static final Pattern PATTERN_JSX_GRAPH_START = Pattern.compile("\\[graph");
	private static final Pattern PATTERN_JSX_GRAPH_END = Pattern.compile("\\[/graph\\]");
	private static final Pattern PATTERN_JSX_GRAPH_INIT_BOX = Pattern.compile("\\[graph(.+?)\\]");
	private static final Pattern PATTERN_JSX_GRAPH_INIT_BOX_ID = Pattern.compile("id=\"(.+?)\"");
	private static final Pattern PATTERN_JSX_GRAPH_INIT_BOX_WIDTH = Pattern.compile("width=\"(.+?)\"");
	private static final Pattern PATTERN_JSX_GRAPH_INIT_BOX_HEIGHT = Pattern.compile("height=\"(.+?)\"");

	static final String URL_PATTERN = "/resource" + "?" + "resource" + "=";

	static final Pattern LATEX_ENVIRONMENT = Pattern.compile(
			"\\$" +// We start with a dollar sign,
					"([^$]|\\\\\\$)+" + // followed by anything but a dollar sign or an escaped dollar sign
			"\\$"); // and end with a final dollar sign.

	private static final Pattern PATTERN_EVAL_POLYNOMIAL = Pattern.compile("evalpolynomial(.+?)'\\)");
	private static final Pattern PATTERN_ANYTHING_INSIDE_SINGLE_QUOTAION_MARK = Pattern.compile("'(.+?)'");
	private static final Pattern PATTERN_FILLIN_BOX = Pattern.compile("\\[fillIn(.+?)(\"|&quot;)\\]");

	private static final String POS_BOX = "[pos=";
	private static final String INPUT_FIELD = "[input=field";
	private static final String EMPTY_STRING = "";
	private static final String FIELD_PREFIX = "field";
	private static final String FEEDBACK_PREFIX = "Feedback_";
	private static final int CORRECT_ANSWER_POINTS = 100;
	private static final String DEFAULT_FEEDBACK_KEYWORD = "andere";
	private static final String TARGET_END_OF_EXERCISE = "end";
	private static final String TARGET_REPEAT_STAGE = "repeat";
	private static final String VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE = "\"filled later by update\"";

	private String contextPath;

	@PostConstruct
	public void init() {
		stageMap = new LinkedHashMap<>();
		Map<Integer, String> emptyFillInMap = new HashMap<>(0);
		evaluatorConverterWithoutInput = new EvaluatorConverter(emptyFillInMap);
	}

	private class StageContainer {
		Step step;
		de.uni_due.s3.jack2import.jack2Objects.stageData.Exercise jack2stage;
		Stage jack3Stage;
		Map<String, String> inputVariableReplaceMap;
		Map<Integer, String> fillInPositionToFieldNameMap;
		int evalNumberCounter = 1;
		int fracNumCounter = 1;
		List<FillInField> fillinFieldList = new ArrayList<>();
		List<String> dropDownFieldList = new ArrayList<>();
	}

	public File getFileForByteArray(byte[] fileContent) {
		File dataDir = new File(System.getProperty("jboss.server.data.dir"));
		String filename = "jack2ExerciseImport" + Arrays.hashCode(fileContent) + ".zip";
		File file = null;
		try {
			file = new File(dataDir, filename);
			java.nio.file.Path path = file.toPath();
			Files.write(path, fileContent);
		} catch (Exception e) {
			throw new IllegalStateException("convert byte to file failed " + dataDir + filename + " ", e);
		}
		return file;
	}

	public Exercise importJack2Excercise(User user, String userLanguage, ContentFolder folder, byte[] fileContent,
			String contextPath) throws ActionNotAllowedException {
		ImportBusiness jack2Importer = new ImportBusiness();

		handleZipFile(fileContent, jack2Importer);
		handleCommonExerciseProperties(user, userLanguage, folder, contextPath);

		if ("R".equals(jack2Data.getExercise().getGeneral().getViewType())) {
			try {
				handleRStage();
			} catch (IOException | SAXException | ParserConfigurationException e) {
				throw new JackRuntimeException(e);
			}
			return jack3Exercise;
		}
		readExerciseChain();
		processFormBasedStages();
		processNextStep();

		return jack3Exercise;
	}

	private void handleRStage() throws ParserConfigurationException, SAXException, IOException {
		de.uni_due.s3.jack2import.rstage.Exercise rStageData = jack2Data.getrExercise();
		de.uni_due.s3.jack2import.rstage.Variables variables = jack2Data.getrExercise().getVariables();
		if(variables != null) {
			readVariables(variables);
		}

		RStage rstage = new RStage();
		rstage.setOrderIndex(0);
		rstage.setInternalName("#1");
		rstage.setTaskDescription(rStageData.getTask());
		for (String hint : rStageData.getAdvice().getAdviceOption()) {
			StageHint stageHint = new StageHint();
			stageHint.setText(hint);
			rstage.addHint(stageHint);
		}
		jack3Exercise.addStage(rstage);

		Map<String, String> finalResultVariableReplacement = new HashMap<>();

		for (Entry<String, String> currentStaticChecker : jack2Data.getStaticCheckers().entrySet()) {
			addStaticRTestcase(rstage, currentStaticChecker, finalResultVariableReplacement);
		}

		for (Entry<String, Checkerconfiguration> jack2CheckerConf : jack2Data.getDynamicCheckers().entrySet()) {
			addDynamicRTestcase(rstage, jack2CheckerConf, finalResultVariableReplacement);
		}

		if (jack2Data.getInitialCodes().size() > 1) {
			jack3Exercise.setInternalNotes(jack3Exercise.getInternalNotes()
					+ "\nMultiple initialcodes found, please check the original exercise!");
		}

		rstage.setInitialCode(jack2Data.getInitialCodes().stream().findAny().orElse(""));

		addFinalResultComputationString(rstage, finalResultVariableReplacement);

		jack3Exercise.setStartStage(rstage);
		jack3Exercise.generateSuffixWeights();
	}

	private void addStaticRTestcase(RStage rstage, Entry<String, String> currentStaticCheckerEntry,
			Map<String, String> finalResultVariableReplacement)
					throws ParserConfigurationException, SAXException, IOException {

		List<StaticRTestCase> staticTestcases = parseStaticRCheckerRules(currentStaticCheckerEntry.getValue());
		TestCaseTuple testCaseTuple = new TestCaseTuple();
		testCaseTuple.setCheckerConfiguration(new CheckerConfiguration());
		staticTestcases.stream().forEach(testCaseTuple::addTestCase);
		rstage.getTestCasetuples().add(testCaseTuple);
		finalResultVariableReplacement.put( //
				"{" + currentStaticCheckerEntry.getKey() + "}",
				"{c" + testCaseTuple.getCheckerConfiguration().getId() + "}");
	}

	private void addFinalResultComputationString(RStage rstage, Map<String, String> finalResultVariableReplacement) {
		String evaluationRule = jack2Data.getExercise().getGeneral().getEvaluationRule();

		for (Entry<String, String> replacement : finalResultVariableReplacement.entrySet()) {
			evaluationRule = evaluationRule.replace(replacement.getKey(), replacement.getValue());
		}

		rstage.setFinalResultComputationString(evaluationRule);
	}

	public List<StaticRTestCase> parseStaticRCheckerRules(String ruleFile)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		// Prevent XXE attacks
		documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

		Document document = documentBuilder.parse(new ByteArrayInputStream(ruleFile.getBytes()));

		org.w3c.dom.Element root = document.getDocumentElement();
		NodeList rules = root.getElementsByTagName("rule");
		if (rules.getLength() == 0) {
			getLogger().warn("Provided rule file does not contain rules!");
			return new ArrayList<>();
		}

		List<StaticRTestCase> result = new ArrayList<>();
		for (int rulesCounter = 0; rulesCounter < rules.getLength(); rulesCounter++) {
			org.w3c.dom.Element rule = (org.w3c.dom.Element) rules.item(rulesCounter);
			String ruleId = rule.getAttribute("id");
			ETestcaseRuleMode type = null;
			if ("presence".equals(rule.getAttribute("type"))) {
				type = ETestcaseRuleMode.PRESENCE;
			} else if ("absence".equals(rule.getAttribute("type"))) {
				type = ETestcaseRuleMode.ABSENCE;
			} else {
				getLogger().warn("Skipping rule with unknown type '" + rule.getAttribute("type") + "'!");
				continue;
			}

			NodeList queries = rule.getElementsByTagName("query");
			List<String> queryStrings = new LinkedList<>();
			if (queries.getLength() == 0) {
				getLogger().warn("Skipping rule that does not contain any queries!");
			}
			for (int queryCounter = 0; queryCounter < queries.getLength(); queryCounter++) {
				queryStrings.add(queries.item(queryCounter).getTextContent());
			}
			if (rule.getElementsByTagName("feedback").getLength() == 0) {
				getLogger().warn("Skipping rule that does not contain feedback!");
				continue;
			}

			// In static checkers the errorFeedback is just called "feedback"
			NodeList feedbacks = rule.getElementsByTagName("feedback");
			String feedback = null;
			for (int feedbackCounter = 0; feedbackCounter < feedbacks.getLength(); feedbackCounter++) {
				org.w3c.dom.Element currentFeedback = (org.w3c.dom.Element) feedbacks.item(feedbackCounter);
				feedback = currentFeedback.getTextContent();
			}

			addStaticTestcaseToResult(result, rule, ruleId, type, feedback, queryStrings);
		}
		return result;
	}

	private void addStaticTestcaseToResult(List<StaticRTestCase> result, org.w3c.dom.Element rule, String ruleId,
			ETestcaseRuleMode type, String feedback, List<String> queryStrings) {
		int points = Integer.parseInt(rule.getAttribute("points"));

		StaticRTestCase staticRTestcase = new StaticRTestCase();
		staticRTestcase.setName("".equals(ruleId) ? "imported testcase" : ruleId);
		staticRTestcase.setFeedbackIfFailed(feedback);
		for (String queryString : queryStrings) {
			staticRTestcase.addQuery(queryString);
		}
		staticRTestcase.setRuleMode(type);
		staticRTestcase.setPointsMode(ETestCasePointsMode.GAIN);
		staticRTestcase.setPoints(points);
		result.add(staticRTestcase);
	}

	private void addDynamicRTestcase(RStage rstage, Entry<String, Checkerconfiguration> jack2CheckerConfEntry,
			Map<String, String> finalResultVariableReplacement) {
		DynamicRTestCase dynamicRTestCase = new DynamicRTestCase();
		// There is in 99% of the time only 1 testcase per Checkerconfiguration
		Testcase jack2Testcase = jack2CheckerConfEntry.getValue().getTestcases().getTestcase().get(0);
		dynamicRTestCase.setName("imported testcase");
		dynamicRTestCase.setExpectedOutput(jack2Testcase.getExpectedOutput());
		// Jack2 Dynamic Testcases are always in deduction mode!
		dynamicRTestCase.setPointsMode(ETestCasePointsMode.DEDUCTION);
		// We want the rule to be present and if not: "100 - getPoints()"
		dynamicRTestCase.setRuleMode(ETestcaseRuleMode.PRESENCE);
		dynamicRTestCase.setPoints(Math.round(jack2Testcase.getPenalty()));
		dynamicRTestCase.setPostprocessingFunction(Objects.toString(jack2Testcase.getPostprocessingFunction(), ""));
		// In Jack2 this nobody has used this function, so setting it to default e^{-4} here
		dynamicRTestCase.setTolerance(-4);

		// errorFeedback is required
		dynamicRTestCase.setFeedbackIfFailed(jack2Testcase.getErrorFeedback());

		MetaInf metaInf = jack2CheckerConfEntry.getValue().getMetaInf();

		dynamicRTestCase
		.setPostCode(Objects.toString(metaInf.getPreCode(), "") + Objects.toString(metaInf.getPostCode(), ""));

		TestCaseTuple testCaseTuple = new TestCaseTuple();
		testCaseTuple.setCheckerConfiguration(new CheckerConfiguration());
		testCaseTuple.addTestCase(dynamicRTestCase);
		rstage.getTestCasetuples().add(testCaseTuple);

		finalResultVariableReplacement.put( //
				"{" + jack2CheckerConfEntry.getKey() + "}",
				"{c" + testCaseTuple.getCheckerConfiguration().getId() + "}");
	}

	private void handleCommonExerciseProperties(User user, String userLanguage, ContentFolder folder,
			String contextPath) throws ActionNotAllowedException {
		this.contextPath = contextPath;

		String exerciseName = jack2Data.getExercise().getGeneral().getTitle();
		exerciseName = getUniqueExerciseName(exerciseName, folder);
		// Exercise must be persisted before adding tags
		jack3Exercise = exerciseBusiness.createExercise(exerciseName, user, folder, userLanguage);
		fillExerciseMetaData();
		addResourcesToExercise(user);
	}

	private void handleZipFile(byte[] fileContent, ImportBusiness jack2Importer) {
		File file = getFileForByteArray(fileContent);
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
		} catch (IOException e) {
			throw new IllegalStateException("Convert to zip File failed ", e);
		}

		jack2Data = jack2Importer.readZip(zip);

		try {
			Files.delete(file.toPath());
		} catch (Exception e) {
			throw new IllegalStateException("Deleting import zip file failed ", e);
		}
	}

	private void processFormBasedStages() {
		for (StageContainer stageContainer : stageMap.values()) {
			fillInputVariabalReplaceMap(stageContainer);
			processStageName(stageContainer);
			processTaskDescription(stageContainer);
			processHints(stageContainer);
			processSkipMessage(stageContainer);
			if (stageContainer.jack3Stage instanceof FillInStage) {
				processFillInFeedback(stageContainer);
			}
			if (stageContainer.jack3Stage instanceof MCStage) {
				processMCAnswer(stageContainer);
				processMCFeedback(stageContainer);
			}
			processJack2OutputVariableUpdate(stageContainer);
		}
	}

	private void processMCAnswer(StageContainer stageContainer) {
		if (stageContainer.jack2stage.getCorrectanswer() == null) {
			return;
		}

		final Answers jack2Answers = stageContainer.jack2stage.getAnswers();
		if (jack2Answers != null) {
			MCStage mcStage = (MCStage) stageContainer.jack3Stage;

			mcStage.setRandomize(jack2Answers.isRandomize());
			final int numberOfAnswers = jack2Answers.getOption().size();

			// We add all the answer options.
			for (Option answerOption : jack2Answers.getOption()) {
				String answerText = extractJSXGraph(answerOption.getValue(), stageContainer);
				answerText = convertFracNumToRational(answerText,stageContainer,StageEvent.START);
				answerText = formatVarsInLatexEnvironment(answerText);
				mcStage.addAnswerOption(answerText);
			}

			// We parse the JACK 2 pattern into tokens.
			final String jack2pattern = stageContainer.jack2stage.getCorrectanswer().getChoice().getPattern();
			final List<String> patternTokens = parsePattern(jack2pattern);

			// If the pattern is a single variable containing the pattern we split it up.
			if (patternTokens.size() == 1 && numberOfAnswers > 1) {
				unwrapPatternTokenList(patternTokens,mcStage);
			}

			// We attempt to assign the pattern tokens to the answers.
			if (patternTokens.size() == numberOfAnswers) {
				for (int i = 0; i < numberOfAnswers; i++) {
					final String token = patternTokens.get(i);
					final EMCRuleType type = tokenToRuleType(token);
					MCAnswer answer = mcStage.getAnswerOptions().get(i);
					answer.setRule(type);
					if (type == EMCRuleType.VARIABLE) {
						answer.setVariableName(token.substring(5, token.length() - 1));
					}
				}
			} else {
				addPatternErrorMessage(jack2pattern);
			}
		}
	}

	private void addPatternErrorMessage(final String pattern) {
		converterErrorMessages.add(
				"Pattern \"" + pattern + "\" passt nicht zur Anzahl der Antworten. " +
				"Das Antwortmuster muss manuell gesetzt werden.");
	}

	private void unwrapPatternTokenList(final List<String> patternTokens, final MCStage mcStage) {
		final String var = patternTokens.get(0);
		final String baseName = var.substring(5, var.length() - 1);
		patternTokens.clear();

		final int numberOfAnswers = mcStage.getAnswerOptions().size();
		for (int i = 0; i < numberOfAnswers; i++) {
			final String varName = createVariableAndUpdateForMCPattern(mcStage, baseName, i);
			patternTokens.add(toVarReference(varName));
		}
	}

	private String createVariableAndUpdateForMCPattern(final MCStage stage,final String baseName,final int index) {
		final String varName = "stage_" + stage.getId() + "_" + baseName + "_" + index;

		// If the variable already exists we don't need to do a thing.
		for (final VariableDeclaration vd : jack3Exercise.getVariableDeclarations()) {
			if (varName.equals(vd.getName())) {
				return varName;
			}
		}

		// We create a new variable ...
		final VariableDeclaration declaration = new VariableDeclaration(varName);
		declaration.setInitializationCode(new EvaluatorExpression(VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE));
		jack3Exercise.addVariable(declaration);

		// ... and update it right before the relevant check happens.
		final String expr = "convertBinaryStringToInteger(charAt([var=" + baseName + "]," + index + "))";
		addVariableUpdateToStage(stage, declaration, expr, StageEvent.BEFORE_CHECK);

		return varName;
	}

	private List<String> parsePattern(final String pattern) {
		final Matcher m = PATTERN_STARONEZERO_VAR.matcher(pattern);
		final List<String> tokens = new ArrayList<String>();
		while (m.find()) {
			tokens.add(m.group());
		}
		return tokens;
	}

	private EMCRuleType tokenToRuleType(final String token) {
		switch (token) {
		case "0": return EMCRuleType.WRONG;
		case "1": return EMCRuleType.CORRECT;
		case "*": return EMCRuleType.NO_MATTER;
		default:
			if (PATTERN_VAR.matcher(token).matches()) {
				return EMCRuleType.VARIABLE;
			} else {
				throw new IllegalArgumentException("Invalid pattern token: " + token);
			}
		}
	}

	private void processMCFeedback(StageContainer stageContainer) {
		MCStage mcStage = (MCStage) stageContainer.jack3Stage;
		if ((stageContainer.jack2stage.getCorrectanswer() != null)
				&& (stageContainer.jack2stage.getCorrectanswer().getMessage() != null)) {
			mcStage.setCorrectAnswerFeedback(stageContainer.jack2stage.getCorrectanswer().getMessage());
		}
		if (stageContainer.jack2stage.getFeedback() != null) {
			for (Choice feedback : stageContainer.jack2stage.getFeedback().getChoice()) {
				handleCurrentMCFeedback(mcStage, feedback);
			}
		}
	}

	private void handleCurrentMCFeedback(MCStage mcStage, Choice feedback) {
		if (DEFAULT_FEEDBACK_KEYWORD.equals(feedback.getPattern())) {
			if (feedback.getValue() != null) {
				mcStage.setDefaultFeedback(feedback.getValue());
			}
			if (feedback.getPoints() != null) {
				mcStage.setDefaultResult(Integer.parseInt(feedback.getPoints()));
			}
		} else {
			EvaluatorExpression expression = new EvaluatorExpression();
			expression.setCode(convertEMCRuleTypeToExpression(feedback.getPattern(),mcStage));
			mcStage.addFeedbackOption(expression);
			MCFeedback freshMCFeedback = mcStage.getExtraFeedbacks().get(mcStage.getExtraFeedbacks().size() - 1);
			if (feedback.getValue() != null) {
				freshMCFeedback.setFeedbackText(feedback.getValue());
			}
			if (feedback.getPoints() != null) {
				freshMCFeedback.setResult(Integer.parseInt(feedback.getPoints()));
			}
		}
	}

	private String convertEMCRuleTypeToExpression(final String pattern, MCStage mcStage) {
		StringJoiner stringJoiner = new StringJoiner("&&");
		final int numberOfAnswers = mcStage.getAnswerOptions().size();

		final List<String> tokens = parsePattern(pattern);
		if (tokens.size() == 1 && tokens.size() != numberOfAnswers) {
			unwrapPatternTokenList(tokens, mcStage);
		}

		if (tokens.size() != numberOfAnswers) {
			addPatternErrorMessage(pattern);
			return "false()";
		}

		for (int i = 0; i < tokens.size(); i++) {
			final String token = tokens.get(i);
			final String inputVar = PlaceholderPatternProducer.forMcInputVariable(i);
			switch (tokenToRuleType(token)) {
			case CORRECT:   stringJoiner.add(inputVar); break;
			case WRONG:     stringJoiner.add("!" + inputVar); break;
			case NO_MATTER: stringJoiner.add("true()"); break;
			default:        stringJoiner.add(inputVar + "==" + token); break;
			}
		}

		return stringJoiner.toString();
	}

	private void processJack2OutputVariableUpdate(StageContainer stageContainer) {

		if (stageContainer.jack2stage.getOutput() != null) {
			EvaluatorConverter evalConverter = new EvaluatorConverter(stageContainer.fillInPositionToFieldNameMap);
			List<String> outputValues = stageContainer.step.getOutput().stream().map(Output::getName)
					.collect(Collectors.toList());
			int i = 0;
			for (Outputvalue outputvalue : stageContainer.jack2stage.getOutput().getOutputvalue()) {
				String nameOfVariable = outputValues.get(i++);
				VariableDeclaration jack3Variable = jack3Exercise.getVariableDeclarations().stream()
						.filter(jack3var -> jack3var.getName().equals(nameOfVariable))
						.findAny().get();

				EvaluatorExpression expr = getEvaluatorExprFromJack2Expr(outputvalue.getValue(), evalConverter, true);
				addVariableUpdateToStage(stageContainer.jack3Stage, jack3Variable, expr, StageEvent.EXIT);
			}
		}

		if (stageContainer.step.getSkip() != null) {
			EvaluatorConverter evalConverter = new EvaluatorConverter(stageContainer.fillInPositionToFieldNameMap);
			for (de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Outputvalue skipOutputvalue : stageContainer.step
					.getSkip().getOutputvalue()) {
				VariableDeclaration jack3Variable = jack3Exercise.getVariableDeclarations().stream()
						.filter(jack3var -> jack3var.getName().equals(skipOutputvalue.getName()))
						.findAny()
						.orElseThrow();

				EvaluatorExpression expr = getEvaluatorExprFromJack2Expr(skipOutputvalue.getValue(), evalConverter, true);
				addVariableUpdateToStage(stageContainer.jack3Stage, jack3Variable, expr, StageEvent.SKIP);
			}
		}
	}

	private void processNextStep() {
		for (StageContainer stageContainer : stageMap.values()) {
			if (stageContainer.step.getSkip() != null) {
				stageContainer.jack3Stage.setAllowSkip(true);
				if (stageContainer.step.getSkip().getTarget() != null) {
					String skipTarget = stageContainer.step.getSkip().getTarget();
					boolean skipTargetIsAnotherStage = true;
					if (skipTarget.contains(TARGET_END_OF_EXERCISE)) {
						StageTransition transitionToEnd = new StageTransition();
						stageContainer.jack3Stage.addSkipTransition(transitionToEnd);
						skipTargetIsAnotherStage = false;
					}
					if (skipTarget.contains(TARGET_REPEAT_STAGE)) {
						StageTransition transitionRepeatStage = new StageTransition(new RepeatStage());
						stageContainer.jack3Stage.addSkipTransition(transitionRepeatStage);
						skipTargetIsAnotherStage = false;
					}
					if (skipTargetIsAnotherStage) {
						StageContainer targetStageContainer = stageMap.get(skipTarget);
						StageTransition transitionToTargetStage = new StageTransition(targetStageContainer.jack3Stage);
						stageContainer.jack3Stage.addSkipTransition(transitionToTargetStage);
					}
				}
				if (stageContainer.step.getSkip().getPath() != null) {
					for (Path skipPath : stageContainer.step.getSkip().getPath()) {
						String skipTarget = skipPath.getTarget();
						EvaluatorExpression transitionCondition = null;
						if (skipPath.getCondition() != null) {
							transitionCondition = getEvaluatorExprFromJack2Expr(skipPath.getCondition(),
									evaluatorConverterWithoutInput, false);
						}
						boolean skipTargetIsAnotherStage = true;
						if (skipTarget.contains(TARGET_END_OF_EXERCISE)) {
							StageTransition transitionToEnd = new StageTransition();
							if (transitionCondition != null) {
								transitionToEnd.setConditionExpression(transitionCondition);
							}
							stageContainer.jack3Stage.addSkipTransition(transitionToEnd);
							skipTargetIsAnotherStage = false;
						}
						if (skipTarget.contains(TARGET_REPEAT_STAGE)) {
							StageTransition transitionRepeatStage = new StageTransition(new RepeatStage());
							if (transitionCondition != null) {
								transitionRepeatStage.setConditionExpression(transitionCondition);
							}
							stageContainer.jack3Stage.addSkipTransition(transitionRepeatStage);
							skipTargetIsAnotherStage = false;
						}
						if (skipTargetIsAnotherStage) {
							StageContainer targetStageContainer = stageMap.get(skipTarget);
							StageTransition transitionToTargetStage = new StageTransition(
									targetStageContainer.jack3Stage);
							if (transitionCondition != null) {
								transitionToTargetStage.setConditionExpression(transitionCondition);
							}
							stageContainer.jack3Stage.addSkipTransition(transitionToTargetStage);
						}
					}
				}
			}

			if (stageContainer.step.getNext() != null) {
				String defaultTarget = stageContainer.step.getNext().getDefault();
				if (defaultTarget != null) {
					switch (defaultTarget) {
					case TARGET_END_OF_EXERCISE:
						stageContainer.jack3Stage.setDefaultTransition(new StageTransition());
						break;
					case TARGET_REPEAT_STAGE:
						stageContainer.jack3Stage.setDefaultTransition(new StageTransition(new RepeatStage()));
						break;
					default:
						stageContainer.jack3Stage
						.setDefaultTransition(new StageTransition(stageMap.get(defaultTarget).jack3Stage));
					}
				}
				for (Path path : stageContainer.step.getNext().getPath()) {
					String pathTarget = path.getTarget();
					EvaluatorExpression transitionCondition = null;
					String transitionConditionCode = "";
					if (path.getCondition() != null) {
						transitionCondition = getEvaluatorExprFromJack2Expr(path.getCondition(),
								evaluatorConverterWithoutInput, false);
					}
					String transitionResultCode = "";
					if (path.getResult() != null) {
						int pointNeededForTransition = Integer.parseInt(path.getResult());
						transitionResultCode = "[meta=stageCurrentResult]>=" + pointNeededForTransition;
						transitionCondition = new EvaluatorExpression(transitionResultCode);
					}
					if (!transitionConditionCode.isEmpty() && !transitionResultCode.isEmpty()) {
						String combinedTransionCode = transitionConditionCode + "&&" + transitionResultCode;
						transitionCondition = new EvaluatorExpression(combinedTransionCode);
					}
					if (pathTarget != null) {
						StageTransition stageTransion = null;
						switch (pathTarget) {
						case TARGET_END_OF_EXERCISE:
							stageTransion = new StageTransition();
							break;
						case TARGET_REPEAT_STAGE:
							stageTransion = new StageTransition(new RepeatStage());
							break;
						default:
							if (stageMap.get(pathTarget) != null) {
								stageTransion = new StageTransition(stageMap.get(pathTarget).jack3Stage);
							}
						}
						if (stageTransion != null) {
							if (transitionCondition != null) {
								stageTransion.setStageExpression(transitionCondition);
							}
							stageContainer.jack3Stage.addStageTransition(stageTransion);
							break;
						}
					}
				}
			}
		}
	}

	private void processStageName(StageContainer stageContainer) {
		if (JackStringUtils.isNotBlank(stageContainer.step.getDisplayName())) {
			stageContainer.jack3Stage.setExternalName(stageContainer.step.getDisplayName());
		}
	}

	private void processSkipMessage(StageContainer stageContainer) {
		if (JackStringUtils.isNotBlank(stageContainer.jack2stage.getSkipmessage())) {
			stageContainer.jack3Stage.setSkipMessage(
					processText(stageContainer.jack2stage.getSkipmessage(), stageContainer, null, StageEvent.START));
		}
	}

	private void processFillInFeedback(StageContainer stageContainer) {
		FillInStage fillInStage = (FillInStage) stageContainer.jack3Stage;
		EvaluatorConverter evalConverter = new EvaluatorConverter(stageContainer.fillInPositionToFieldNameMap);
		int feedbackNumber = 1;

		List<String> ruleNamesOfStage = new ArrayList<>();

		if (stageContainer.jack2stage.getCorrectanswer() != null) {
			Correctanswer correctanswer = stageContainer.jack2stage.getCorrectanswer();
			fillInStage.setCorrectAnswerFeedback(
					processText(correctanswer.getMessage(), stageContainer, evalConverter, StageEvent.BEFORE_CHECK));
			int correctAnswerNumber = 1;
			for (Option jack2EvaluatorExpression : correctanswer.getOption()) {
				Rule rule = new Rule("CorrectAnswer" + correctAnswerNumber, correctAnswerNumber++);
				rule.setValidationExpression(
						getEvaluatorExprFromJack2Expr(jack2EvaluatorExpression.getResult(), evalConverter, false));
				fillInStage.addCorrectAnswerRule(rule);
			}
			for (de.uni_due.s3.jack2import.jack2Objects.stageData.Rule jack2Rule : correctanswer.getRule()) {
				String ruleVariableName = "Stage_" + stageContainer.step.getId() + "_Rule_" + jack2Rule.getId();
				ruleNamesOfStage.add(ruleVariableName);
				VariableDeclaration variableDeclaration = new VariableDeclaration(ruleVariableName);
				variableDeclaration
				.setInitializationCode(new EvaluatorExpression(VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE));
				jack3Exercise.addVariable(variableDeclaration);

				EvaluatorExpression expr = getEvaluatorExprFromJack2Expr(jack2Rule.getValue(), evalConverter, true);
				addVariableUpdateToStage(stageContainer.jack3Stage, variableDeclaration, expr, StageEvent.BEFORE_CHECK);
			}
			if (!ruleNamesOfStage.isEmpty()) {
				Rule rule = new Rule("CorrectAnswer" + correctAnswerNumber, correctAnswerNumber++);
				String ruleNamesConnected = "[var=";
				ruleNamesConnected += String.join("]&&[var=", ruleNamesOfStage);
				ruleNamesConnected += "]";
				rule.setValidationExpression(new EvaluatorExpression(ruleNamesConnected));
				fillInStage.addCorrectAnswerRule(rule);
			}
		}
		if (stageContainer.jack2stage.getFeedback() != null) {
			for (Option jack2Feedback : stageContainer.jack2stage.getFeedback().getOption()) {
				if (jack2Feedback.getResult().contains(DEFAULT_FEEDBACK_KEYWORD)) {
					fillInStage.setDefaultFeedback(
							processText(jack2Feedback.getValue(), stageContainer, evalConverter, StageEvent.BEFORE_CHECK));
					if (JackStringUtils.isNotBlank(jack2Feedback.getPoints())) {
						fillInStage.setDefaultResult(Integer.parseInt(jack2Feedback.getPoints()));
					} else {
						fillInStage.setDefaultResult(0);
					}
					continue;
				}
				Rule rule = new Rule(FEEDBACK_PREFIX + feedbackNumber, feedbackNumber++);
				rule.setFeedbackText(
						processText(jack2Feedback.getValue(), stageContainer, evalConverter, StageEvent.BEFORE_CHECK));
				if (jack2Feedback.getPoints() != null) {
					rule.setPoints(Integer.parseInt(jack2Feedback.getPoints()));
				} else {
					rule.setPoints(0);
				}
				rule.setValidationExpression(
						getEvaluatorExprFromJack2Expr(jack2Feedback.getResult(), evalConverter, false));
				fillInStage.addFeedbackRule(rule);
			}
			// Rulevaiolation has negative Points. In JACK3 we don't have Base 100 Points
			// therefor here is a rule that always give 100 Points and the following ruleviolations reduce this points.
			if (!stageContainer.jack2stage.getFeedback().getRuleviolation().isEmpty()) {
				Rule rule100Points = new Rule("Ruleviolation Base 100 Points", feedbackNumber);
				feedbackNumber++;
				rule100Points.setPoints(CORRECT_ANSWER_POINTS);
				rule100Points.setValidationExpression(new EvaluatorExpression("true()"));
				fillInStage.addFeedbackRule(rule100Points);

				for (Ruleviolation ruleviolation : stageContainer.jack2stage.getFeedback().getRuleviolation()) {
					String convertedRuleviolation = convertRuleviolationToJack3Notation(ruleviolation.getTest(),
							ruleNamesOfStage);
					Rule rule = new Rule(FEEDBACK_PREFIX + feedbackNumber, feedbackNumber++);
					if (ruleviolation.getPenalty() != null) {
						rule.setPoints(Integer.parseInt(ruleviolation.getPenalty()) * -1);
					}
					rule.setValidationExpression(new EvaluatorExpression(convertedRuleviolation));
					rule.setFeedbackText(
							processText(ruleviolation.getValue(), stageContainer, evalConverter, StageEvent.BEFORE_CHECK));
					fillInStage.addFeedbackRule(rule);
				}
			}
		}

	}

	private String convertRuleviolationToJack3Notation(String ruleviolationExpr, List<String> ruleNamesOfStage) {
		String convertedExpr = "";
		if (ruleviolationExpr.contains("condition")) {
			ruleviolationExpr = ruleviolationExpr.replace("condition", "");
			Matcher idMatcher = PATTERN_ONLY_NUMBERS.matcher(ruleviolationExpr);
			StringBuilder exprWithReplacedId = new StringBuilder();
			int beginIndex = 0;
			while (idMatcher.find()) {
				exprWithReplacedId.append(ruleviolationExpr.substring(beginIndex, idMatcher.start()));
				String idToReplace = idMatcher.group();
				exprWithReplacedId.append("![var=" + ruleNamesOfStage.get(Integer.parseInt(idToReplace) - 1) + "]");
				beginIndex = idMatcher.end();
			}
			exprWithReplacedId.append(ruleviolationExpr.substring(beginIndex));
			convertedExpr = exprWithReplacedId.toString();
		}
		if (ruleviolationExpr.contains("combined")) {
			ruleviolationExpr = ruleviolationExpr.replace("combined", "");
			Matcher idMatcher = PATTERN_ONLY_NUMBERS.matcher(ruleviolationExpr);
			StringBuilder exprWithReplacedId = new StringBuilder();
			int beginIndex = 0;
			while (idMatcher.find()) {
				exprWithReplacedId.append(ruleviolationExpr.substring(beginIndex, idMatcher.start()));
				String idToReplace = idMatcher.group();
				exprWithReplacedId.append("![var=" + ruleNamesOfStage.get(Integer.parseInt(idToReplace) - 1) + "]");
				beginIndex = idMatcher.end();
			}
			exprWithReplacedId.append(ruleviolationExpr.substring(beginIndex));
			convertedExpr = exprWithReplacedId.toString().replace(",", "&&");
		}

		if (ruleviolationExpr.contains("exactly") || ruleviolationExpr.contains("atleast")) {
			Matcher countMatcher = PATTERN_ONLY_NUMBERS.matcher(ruleviolationExpr);
			if (countMatcher.find()) {
				int countCountFalse = Integer.parseInt(countMatcher.group());
				int countTrue = ruleNamesOfStage.size() - countCountFalse;
				String ruleNamesConnected = "[var=";
				ruleNamesConnected += String.join("],[var=", ruleNamesOfStage);
				ruleNamesConnected += "]";
				if (ruleviolationExpr.contains("exactly")) {
					convertedExpr = "equals(countTrue(" + ruleNamesConnected + ")," + countTrue + ")";
				}
				if (ruleviolationExpr.contains("atleast")) {
					convertedExpr = "countTrue(" + ruleNamesConnected + ")<=" + countTrue;
				}
			}
		}
		return convertedExpr;
	}

	private EvaluatorExpression getEvaluatorExprFromJack2Expr(String jack2EvalString, EvaluatorConverter evalConverter,
			boolean isUpdate) {
		String convertedEvaluatorExpression;
		convertedEvaluatorExpression = convertEvalpolynomialToSageExpr(jack2EvalString, isUpdate);

		if(jack2EvalString.contains("evaluateInSage('")) {
			try {
				convertedEvaluatorExpression = convertedEvaluatorExpression.replace("\\\'", "\'");
				convertedEvaluatorExpression = evalConverter.convertMathExpression(convertedEvaluatorExpression);
			} catch (EvaluatorConverterException e) {
				convertedEvaluatorExpression = replacePosToInputField(convertedEvaluatorExpression);
			}
		} else {
			if (jack2EvalString.contains("\\") || jack2EvalString.contains("$") || jack2EvalString.contains(".")) {

				if (jack2EvalString.contains("[var=")) {
					jack2EvalString = getVarConcatedExpr(jack2EvalString);
				} else if(!jack2EvalString.contains("\"")) {
					jack2EvalString = "\"" + jack2EvalString + "\"";
				}
				convertedEvaluatorExpression = jack2EvalString;
				convertedEvaluatorExpression = replacePosToInputField(convertedEvaluatorExpression);
			} else {
				try {
					convertedEvaluatorExpression = evalConverter.convertMathExpression(convertedEvaluatorExpression);
				} catch (EvaluatorConverterException e) {
					if (!convertedEvaluatorExpression.contains("evaluateInSage")) {
						converterErrorMessages.add("Convert Expression fails on: " + convertedEvaluatorExpression);
						convertedEvaluatorExpression = jack2EvalString;
					} else {
						convertedEvaluatorExpression = replacePosToInputField(convertedEvaluatorExpression);
					}
				}
			}
		}
		return new EvaluatorExpression(convertedEvaluatorExpression);
	}

	public String getConverterErrorsAsString() {
		return converterErrorMessages.stream().collect(Collectors.joining(System.lineSeparator()));
	}

	private String getVarConcatedExpr(String jack2VarExpr) {
		String concatedExpression = jack2VarExpr;
		Set<String> varsReplaced = new HashSet<>();

		final Matcher matcher = PATTERN_VAR.matcher(jack2VarExpr);
		while (matcher.find()) {

			String varToReplace = matcher.group();
			if (varsReplaced.contains(varToReplace)) {
				continue;
			}
			varsReplaced.add(varToReplace);
			if(!varToReplace.contains("\"")) {
				varToReplace = "\"," + varToReplace + ",\"";
			}
			concatedExpression = concatedExpression.replace(matcher.group(), varToReplace);
		}
		concatedExpression = "concat(\"" + concatedExpression + "\")";
		if (concatedExpression.startsWith("concat(\"\",")) {
			concatedExpression = concatedExpression.replace("concat(\"\",", "concat(");
		}
		if (concatedExpression.endsWith(",\"\")")) {
			concatedExpression = concatedExpression.replace(",\"\")", ")");
		}

		return concatedExpression;
	}

	private void processHints(StageContainer stageContainer) {
		if (stageContainer.jack2stage.getAdvice() == null) {
			return;
		}

		for (Option jack2Hint : stageContainer.jack2stage.getAdvice().getOption()) {
			StageHint jack3hint = new StageHint();
			jack3hint.setText(processText(jack2Hint.getValue(), stageContainer, null, StageEvent.START));
			stageContainer.jack3Stage.addHint(jack3hint);
		}
	}

	private void fillInputVariabalReplaceMap(StageContainer stageContainer) {
		List<Input> variableNames = stageContainer.step.getInput();
		stageContainer.inputVariableReplaceMap = new LinkedHashMap<>(variableNames.size());
		if (variableNames.isEmpty()) {
			return;
		}

		List<Inputvalue> variableNamesOnStage = stageContainer.jack2stage.getInput().getInputvalue();
		for (int i = 0; i < variableNames.size(); i++) {
			String variableNameOnStage = toVarReference(variableNamesOnStage.get(i).getName());
			stageContainer.inputVariableReplaceMap.put(variableNames.get(i).getValue(), variableNameOnStage);
		}
	}

	private void processTaskDescription(StageContainer stageContainer) {
		String taskDescription = stageContainer.jack2stage.getTask();
		taskDescription = getReplaceVariableText(taskDescription, stageContainer.inputVariableReplaceMap);
		taskDescription = extractJSXGraph(taskDescription, stageContainer);
		taskDescription = convertEvalpolynomialToSageExpr(taskDescription, true);
		taskDescription = convertFracNumToRational(taskDescription, stageContainer, StageEvent.START);
		taskDescription = formatVarsInLatexEnvironment(taskDescription);
		taskDescription = convertImageInText(taskDescription);
		stageContainer.jack3Stage.setTaskDescription(taskDescription);

		if (stageContainer.jack3Stage instanceof FillInStage) {
			TaskDescriptionReadFillInField readFillInFields = new TaskDescriptionReadFillInField(taskDescription);
			List<FillInFieldJack2Information> jack2FillInFieldInformations = readFillInFields
					.getJack2FillInFieldsInformationFromTaskDesription();
			buildFillInPositionToFieldName(stageContainer, jack2FillInFieldInformations);
			buildFillInFieldsFromTaskDescription(stageContainer, jack2FillInFieldInformations);
			buildDropDownFieldsFromTaskDescription(stageContainer, jack2FillInFieldInformations);

			String taskDescriptionBeforeReplacement = stageContainer.jack3Stage.getTaskDescription();
			String taskDescriptionAfterReplacment = getTaskDescriptionWithReplacedDropDownAndFillInFields(
					taskDescriptionBeforeReplacement, stageContainer);
			stageContainer.jack3Stage.setTaskDescription(taskDescriptionAfterReplacment);
		}
	}

	private void buildFillInPositionToFieldName(StageContainer stageContainer,
			List<FillInFieldJack2Information> jack2FillInFieldInformation) {
		stageContainer.fillInPositionToFieldNameMap = new HashMap<>();
		for (FillInFieldJack2Information jack2InfoField : jack2FillInFieldInformation) {
			stageContainer.fillInPositionToFieldNameMap.put(jack2InfoField.pos, getFieldName(jack2InfoField.pos));
		}
	}

	private String getTaskDescriptionWithReplacedDropDownAndFillInFields(String text, StageContainer stageContainer) {
		StringBuilder textWithReplacedFields = new StringBuilder(text.length());
		Matcher fillinBoxMatcher = PATTERN_FILLIN_BOX.matcher(text);

		int beginIndex = 0;
		int dropDownListIndex = 0;
		int fillInListIndex = 0;
		while (fillinBoxMatcher.find()) {

			String textBeforeField = text.substring(beginIndex, fillinBoxMatcher.start());
			textWithReplacedFields.append(textBeforeField);

			String fillinBoxText = fillinBoxMatcher.group();
			if (fillinBoxText.contains("answerset")) {
				String dropDownFieldName = stageContainer.dropDownFieldList.get(dropDownListIndex);
				dropDownListIndex++;
				String jack3DropDownFieldHTMLRepresentation = fillInStageBusiness
						.getDropDownFieldHtmlCode(dropDownFieldName);
				textWithReplacedFields.append(jack3DropDownFieldHTMLRepresentation);
			} else {
				FillInField fillinField = stageContainer.fillinFieldList.get(fillInListIndex);
				fillInListIndex++;
				String jack3FillInFieldHTMLRepresentation = fillInStageBusiness
						.getFillInFieldHtmlCode(fillinField.getName(), fillinField.getSize());
				textWithReplacedFields.append(jack3FillInFieldHTMLRepresentation);
			}

			beginIndex = fillinBoxMatcher.end();
		}

		String textAfterLastField = text.substring(beginIndex);
		textWithReplacedFields.append(textAfterLastField);

		return textWithReplacedFields.toString();
	}

	private void buildDropDownFieldsFromTaskDescription(StageContainer stageContainer,
			List<FillInFieldJack2Information> jack2FillInFieldInformation) {
		FillInStage fillInStage = (FillInStage) stageContainer.jack3Stage;
		for (FillInFieldJack2Information jack2InfoField : jack2FillInFieldInformation) {
			if (jack2InfoField.fieldType == Jack2FillInFieldType.FILL_IN) {
				continue;
			}
			DropDownField dropDownField = new DropDownField(getFieldName(jack2InfoField.pos),
					fillInStage.getFillInFields().size());
			for (String dropDownItem : jack2InfoField.dropDownItems) {
				dropDownField.addItemPerImport(dropDownItem);
			}

			fillInStage.addDropDownField(dropDownField);
			stageContainer.dropDownFieldList.add(dropDownField.getName());
		}

	}

	private void buildFillInFieldsFromTaskDescription(StageContainer stageContainer,
			List<FillInFieldJack2Information> jack2FillInFieldInformation) {
		FillInStage fillInStage = (FillInStage) stageContainer.jack3Stage;
		boolean stageHasPalette = false;
		if (!EMPTY_STRING.equals(stageContainer.jack2stage.getEditor())) {
			stageHasPalette = true;
			String editorPalette = stageContainer.jack2stage.getEditor();
			fillInStage.setFormularEditorPalette(
					EFormularEditorPalette.getEFormularEditorPaletteForTypeLabel(editorPalette).toString());
		}
		for (FillInFieldJack2Information jack2InfoField : jack2FillInFieldInformation) {
			if (jack2InfoField.fieldType == Jack2FillInFieldType.DROP_DOWN) {
				continue;
			}
			FillInField fillInfield = new FillInField(getFieldName(jack2InfoField.pos),
					fillInStage.getFillInFields().size());
			fillInfield.setSize(jack2InfoField.fillInFieldSize);
			if (jack2InfoField.editor) {
				fillInfield.setFormularEditorType(EFillInEditorType.MATHDOX_FORMULAR_EDITOR.toString());
				// If not stage palette set but on a field is editor true than take palette 6 (jackwiki Formeleditor page)
				if (!stageHasPalette) {
					fillInStage.setFormularEditorPalette(
							EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_6.toString());
				}
			} else if (stageHasPalette) {
				fillInfield.setFormularEditorType(EFillInEditorType.MATHDOX_FORMULAR_EDITOR.toString());
			}

			fillInStage.addFillInField(fillInfield);
			stageContainer.fillinFieldList.add(fillInfield);
		}
	}

	private String processText(String text, StageContainer stageContainer, EvaluatorConverter converter,
			StageEvent event) {

		text = extractJSXGraph(text, stageContainer);

		if (stageContainer.jack3Stage instanceof FillInStage) {
			text = extractEvalExpressionAndAddAsVar(text, stageContainer, converter, event);
			text = replacePosToInputField(text);
		} else {
			text = extractEvalExpressionAndAddAsVar(text, stageContainer, event);
		}
		text = convertFracNumToRational(text, stageContainer, event);
		text = formatVarsInLatexEnvironment(text);
		return convertImageInText(text);
	}

	private String replacePosToInputField(String textWithPosFields) {
		return textWithPosFields.replace(POS_BOX, INPUT_FIELD);
	}

	private String getFieldName(int jack2PosNumber) {
		return FIELD_PREFIX + Integer.toString(jack2PosNumber);
	}

	private String getReplaceVariableText(String text, Map<String, String> inputVariableReplaceMap) {
		for (Entry<String, String> replaceTupel : inputVariableReplaceMap.entrySet()) {
			text = text.replace(replaceTupel.getValue(), replaceTupel.getKey());
		}
		return text;
	}

	private void fillExerciseMetaData() {
		de.uni_due.s3.jack2import.jack2Objects.exerciseConfig.Exercise jack2E = jack2Data.getExercise();
		jack3Exercise.setDifficulty(jack2E.getGeneral().getLevelOfDifficulty());
		jack3Exercise.setPublicDescription(jack2E.getGeneral().getExternalDescription());
		jack3Exercise.setInternalNotes(jack2E.getGeneral().getInternalDescription());
		String[] tags = jack2E.getGeneral().getExerciseTagList().split(";");
		for (String tag : tags) {
			if (JackStringUtils.isNotBlank(tag)) {
				exerciseBusiness.addTagToExercise(jack3Exercise, tag);
			}
		}
	}

	private void readExerciseChain() {
		Exercisechain chain = jack2Data.getChain();
		Variables variables = chain.getVariables();
		if (variables != null) {
			readVariables(variables);
		}
		if (chain.getStep() != null) {
			readOutputVariables(chain.getStep());
			fillStageMap(chain.getStep());
		}
	}

	private void fillStageMap(List<Step> steps) {
		for (Step step : steps) {
			StageContainer stageContainer = new StageContainer();
			stageContainer.step = step;
			stageContainer.jack2stage = jack2Data.getStages().get(step.getFile().toLowerCase());
			createEmptyJack3Stage(stageContainer);
			stageMap.put(step.getId(), stageContainer);
		}
	}

	private void createEmptyJack3Stage(StageContainer stageContainer) {
		String stageType = stageContainer.jack2stage.getType();
		switch (stageType.toLowerCase()) {
		case "fillin":
			stageContainer.jack3Stage = new FillInStage();
			break;
		case "mc":
			stageContainer.jack3Stage = new MCStage();
			break;
		case "sclist":
			stageContainer.jack3Stage = new FillInStage();
			break;
		}
		int currentSize = jack3Exercise.getStages().size();
		stageContainer.jack3Stage.setOrderIndex(currentSize);

		stageContainer.jack3Stage.setInternalName(stageContainer.step.getId());

		// Make it the start stage if it is the only one
		if (currentSize == 0) {
			jack3Exercise.setStartStage(stageContainer.jack3Stage);
		}
		jack3Exercise.addStage(stageContainer.jack3Stage);
	}

	private void readOutputVariables(List<Step> steps) {
		for (Step step : steps) {
			if (!step.getOutput().isEmpty()) {
				for (de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Output chainOutputVariable : step
						.getOutput()) {
					String outputVarName = chainOutputVariable.getName();
					if (!jack3Exercise.getVariableDeclarations().stream()
							.filter(jack3var -> jack3var.getName().equals(outputVarName)).findAny().isPresent()) {
						VariableDeclaration variableDeclaration = new VariableDeclaration(outputVarName);
						variableDeclaration
						.setInitializationCode(new EvaluatorExpression(VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE));
						jack3Exercise.addVariable(variableDeclaration);
					}
				}
			}
		}
	}

	/**
	 *
	 * @param variables
	 */
	private void readVariables(de.uni_due.s3.jack2import.rstage.Variables variables) {

		for (de.uni_due.s3.jack2import.rstage.Option variable : variables.getOption()) {

			String variableValue = null;

			if (variable.getValue() != null) {
				String varCode = convertFracNumToRational(variable.getValue(), null, StageEvent.START);

				variableValue = getEvaluatorExprFromJack2Expr(varCode, evaluatorConverterWithoutInput, false).getCode();
				if ("R".equalsIgnoreCase(variable.getCas())) {
					variableValue = "evaluateInR('" + variableValue + "')";
				}
			}
			if ((variableValue == null) && (variable.getMin() != null)) {
				variableValue = "randomIntegerBetween(" + variable.getMin() + "," + variable.getMax() + ")";
			}
			if ((variableValue == null) && (variable.getSet() != null)) {
				String setString = variable.getSet();
				if (setString.contains("{")) {
					setString = setString.substring(1, variable.getSet().length() - 1);
				}
				variableValue = "getRandomFromList(list(" + setString + "))";
			}
			if ((variableValue == null) && "set".equals(variable.getType())) {
				List<de.uni_due.s3.jack2import.rstage.Element> elements = variable.getElement();
				int counter = 1;
				StringBuilder elementVaribles = new StringBuilder();
				for (de.uni_due.s3.jack2import.rstage.Element e : elements) {
					String elementVariableName = variable.getName() + "set" + counter;
					elementVaribles.append(toVarReference(elementVariableName) + ",");
					VariableDeclaration elementVariable = new VariableDeclaration(elementVariableName);
					String varCode = convertFracNumToRational(e.getValue(), null, StageEvent.START);
					elementVariable.setInitializationCode(
							getEvaluatorExprFromJack2Expr(varCode, evaluatorConverterWithoutInput, false));
					jack3Exercise.addVariable(elementVariable);
					counter++;
				}
				String elementVariablesCommaCorrect = elementVaribles.toString().substring(0,
						elementVaribles.toString().length() - 1);
				variableValue = "list(" + elementVariablesCommaCorrect + ")";

			}
			VariableDeclaration variableDeclaration = new VariableDeclaration(variable.getName());
			variableDeclaration.setInitializationCode(new EvaluatorExpression(variableValue));
			jack3Exercise.addVariable(variableDeclaration);
		}
	}

	private void readVariables(Variables variables) {

		for (de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable : variables.getOption()) {
			String variableValue;
			variableValue = handleVariableValue(variable);
			variableValue = handleVariableFunction(variable, variableValue);
			variableValue = handleVariableMin(variable, variableValue);
			variableValue = handleVariableSet(variable, variableValue);
			variableValue = handleVariableTypeOfSet(variable, variableValue);
			variableValue = handleVariableTypeOfMcIndex(variable, variableValue, variables);
			VariableDeclaration variableDeclaration = new VariableDeclaration(variable.getName());
			variableDeclaration.setInitializationCode(new EvaluatorExpression(variableValue));
			jack3Exercise.addVariable(variableDeclaration);
		}
	}

	private String handleVariableValue(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable) {
		if (variable.getValue() != null) {
			if ("R".equalsIgnoreCase(variable.getCas())) {
				return "evaluateInR('" + variable.getValue() + "')";
			}

			if(variable.getValue().contains("chooseFromComplement")) {
				if(variable.getValue().substring(21, 22).equals("'")) {
					String varChoose = variable.getValue();
					varChoose = varChoose.replaceFirst("'", "list(");
					varChoose = varChoose.replaceFirst("'", ")");
					varChoose = varChoose.replaceFirst("'", "list(");
					varChoose = varChoose.replaceFirst("'", ")");
					varChoose = varChoose.replace(";", ",");
					return varChoose;
				}
			}

			if(variable.getValue().contains("getRandomFromList")) {
				if(variable.getValue().contains("chooseFromComplement")) {
					if(!variable.getValue().substring(18, 25).contains("list")) {
						String endString = variable.getValue().substring(18);
						String varRandomList = variable.getValue().substring(0, 18);
						varRandomList = varRandomList + "list(" + endString + ")";
						return varRandomList;
					}
				}
			}

			String varCode = convertFracNumToRational(variable.getValue(), null, StageEvent.START);
			return getEvaluatorExprFromJack2Expr(varCode, evaluatorConverterWithoutInput, false).getCode();
		}
		return null;
	}

	private String handleVariableFunction(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable,
			String variableValue) {
		if(variableValue == null) {
			if("R".equalsIgnoreCase(variable.getCas())) {
				if(variable.getFunction() != null) {
					return "evaluateInR('" + variable.getFunction().getName() + "')";
				}
			}
			if("Sage".equalsIgnoreCase(variable.getCas())) {
				if(variable.getFunction() != null) {
					return "evaluateInSage(\"" + variable.getFunction().getName() + "\")";
				}
			}
		}
		return variableValue;
	}

	private String handleVariableTypeOfSet(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable,
			String variableValue) {
		if ((variableValue == null) && "set".equals(variable.getType()) && !"R".equalsIgnoreCase(variable.getCas())) {
			List<Element> elements = variable.getElement();
			int counter = 1;
			StringJoiner elementVariables = new StringJoiner(",");
			for (Element e : elements) {
				String elementVariableName = variable.getName() + "set" + counter;
				elementVariables.add(toVarReference(elementVariableName));
				VariableDeclaration elementVariable = new VariableDeclaration(elementVariableName);
				String varCode = convertFracNumToRational(e.getValue(), null, StageEvent.START);
				elementVariable.setInitializationCode(
						getEvaluatorExprFromJack2Expr(varCode, evaluatorConverterWithoutInput, false));
				jack3Exercise.addVariable(elementVariable);
				counter++;
			}
			variableValue = "list(" + elementVariables.toString() + ")";

		}
		return variableValue;
	}

	private String handleVariableTypeOfMcIndex(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable,
			String variableValue, Variables variables) {
		if((variableValue == null) && "mcindex".equals(variable.getType())) {
			if(variable.getUpperbound() != null) {
				if(variable.getAnswerposition().equals("0")) {
					variableValue = "randomIntegerBetween(0, " + variable.getUpperbound() + ")";
				} else {
					String liste = "0";
					int varUpperbound = Integer.parseInt(variable.getUpperbound());
					for(int i=1; i<varUpperbound; i++) {
						liste += "," + i;
					}
					String varListe = "";
					int varAnswerposition = Integer.parseInt(variable.getAnswerposition());
					int index = 0;
					for(int i=0; i<varAnswerposition; i++) {
						while(variables.getOption().size() != index && index < varAnswerposition) {
							if(variables.getOption().get(index).getType().equals("mcindex")) {
								if(variables.getOption().get(index).getForstage().equals(variable.getForstage())) {
									int k = Integer.parseInt(variables.getOption().get(index).getAnswerposition());
									if(k == index) {
										final String varName = variables.getOption().get(index).getName();
										varListe += toVarReference(varName);
										if(index+1 != varAnswerposition) {
											varListe += ",";
										}
									}
								}
							}
							index++;
						}
					}
					variableValue = "chooseFromComplement(list(" + liste + "),list(" + varListe + "))";
				}
			}
		}
		return variableValue;
	}

	private String handleVariableSet(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable,
			String variableValue) {
		if ((variableValue == null) && (variable.getSet() != null)) {
			String setString = variable.getSet();
			if (setString.contains("{")) {
				setString = setString.substring(1, variable.getSet().length() - 1);
			}
			variableValue = "getRandomFromList(list(" + setString + "))";
		}
		return variableValue;
	}

	private String handleVariableMin(de.uni_due.s3.jack2import.jack2Objects.exerciseChain.Option variable,
			String variableValue) {
		if ((variableValue == null) && (variable.getMin() != null)) {
			int varMax = Integer.parseInt(variable.getMax()) + 1;
			variableValue = "randomIntegerBetween(" + variable.getMin() + "," + varMax + ")";
		}
		return variableValue;
	}

	private String formatVarsInLatexEnvironment(String text) {
		final Matcher texEnvMatcher = LATEX_ENVIRONMENT.matcher(text);

		// We look for all the TeX environments in the string.
		while (texEnvMatcher.find()) {
			String texEnvironment = texEnvMatcher.group();

			// Inside the TeX environment we reformat all variables.
			final Matcher varMatcher = PATTERN_VAR.matcher(texEnvironment);
			while (varMatcher.find()) {
				final String varName = varMatcher.group("identifier");
				final String formattedVar = "[var=" + varName + ",latex]";
				texEnvironment = texEnvironment.replace(varMatcher.group(),formattedVar);
			}
			text = text.replace(texEnvMatcher.group(),texEnvironment);
		}
		return text;
	}

	private String getUniqueExerciseName(String name, ContentFolder folder) {
		if (!folderBusiness.checkForDuplicateName(folder, name, EFolderChildType.EXERCISE)) {
			return name;
		}
		int counter = 1;
		while (folderBusiness.checkForDuplicateName(folder, name + counter, EFolderChildType.EXERCISE)) {
			counter++;
		}

		return name + counter;
	}

	private String extractEvalExpressionAndAddAsVar(String text, StageContainer stageContainer, StageEvent updateTime) {
		return extractEvalExpressionAndAddAsVar(text, stageContainer, null, updateTime);
	}

	private String extractEvalExpressionAndAddAsVar(String text, StageContainer stageContainer,
			EvaluatorConverter evaluatorConverter, StageEvent updateTime) {

		final Matcher m = EVAL_PATTERN.matcher(text);
		if (evaluatorConverter == null) {
			evaluatorConverter = evaluatorConverterWithoutInput;
		}

		String outputText = new String(text);

		while (m.find()) {
			String varName = "Stage_" + stageContainer.step.getId() + "_eval_" + stageContainer.evalNumberCounter++;
			String evalToReplace = m.group();
			String replaceVarExpression = toVarReference(varName);
			outputText = outputText.replace(evalToReplace, replaceVarExpression);

			String updateCode = m.group(2);
			updateCode = convertFracNumToRational(updateCode, stageContainer, updateTime);

			VariableDeclaration variableDeclaration = new VariableDeclaration(varName);
			variableDeclaration.setInitializationCode(new EvaluatorExpression(VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE));
			jack3Exercise.addVariable(variableDeclaration);
			EvaluatorExpression expr = getEvaluatorExprFromJack2Expr(updateCode, evaluatorConverter, true);
			addVariableUpdateToStage(stageContainer.jack3Stage, variableDeclaration, expr, updateTime);
		}

		return outputText;
	}

	private String toVarReference(final String varName) {
		return "[var=" + varName + "]";
	}

	private String convertFracNumToRational(String text, StageContainer stageContainer, StageEvent time) {
		String output = text;

		Set<String> processeStrings = new HashSet<>();
		if (text.contains("[frac num=")) {
			final Matcher matcher = PATTERN_FRAC_NUM.matcher(text);
			while (matcher.find()) {

				String fracNumToReplace = matcher.group();
				if (processeStrings.contains(fracNumToReplace)) {
					continue;
				}
				processeStrings.add(fracNumToReplace);
				final Matcher matcherValues = PATTERN_ANYTHING_INSIDE_QOUTION_MARKS.matcher(fracNumToReplace);
				matcherValues.find();
				String num = matcherValues.group().substring(1, matcherValues.group().length() - 1);
				if (!matcherValues.find()) {
					continue;
				}
				String den = matcherValues.group().substring(1, matcherValues.group().length() - 1);
				String rationalExpr = "rational(" + num + "," + den + ")";
				rationalExpr = rationalExpr.replace("var=$", "var=");
				String varName = "";
				if (stageContainer == null) {
					varName = "fraction_" + exerciseFracNumCounter++;
				} else {
					varName = "Stage_" + stageContainer.step.getId() + "_fraction_" + stageContainer.fracNumCounter++;
				}
				VariableDeclaration variableDeclaration = new VariableDeclaration(varName);
				variableDeclaration.setInitializationCode(
						new EvaluatorExpression(VARIABLE_SET_EXPRESSION_CODE_BY_UPDATE));
				jack3Exercise.addVariable(variableDeclaration);
				addVariableUpdateToStage(stageContainer.jack3Stage, variableDeclaration, rationalExpr, time);

				String replaceVarExpression = toVarReference(varName);
				output = output.replace(fracNumToReplace, replaceVarExpression);
			}
		}
		return output;
	}

	private String extractJSXGraph(String text, StageContainer stageContainer) {
		String output = text;

		if (text.contains("[graph")) {
			final Matcher matcherGraph = PATTERN_JSX_GRAPH_START.matcher(text);
			final Matcher matcherGraphEnd = PATTERN_JSX_GRAPH_END.matcher(text);
			Set<String> graphBoxes = new HashSet<>();
			while (matcherGraph.find()) {
				matcherGraphEnd.find();

				String fullGraphBox = text.substring(matcherGraph.start(), matcherGraphEnd.end());
				if (graphBoxes.contains(fullGraphBox)) {
					continue;
				}
				graphBoxes.add(fullGraphBox);

				final Matcher matcherGraphBox = PATTERN_JSX_GRAPH_INIT_BOX.matcher(fullGraphBox);
				matcherGraphBox.find();
				String graphInitBox = matcherGraphBox.group();

				String graphBoxValue = fullGraphBox.substring(graphInitBox.length(), fullGraphBox.length() - 8);

				final Matcher matcherGraphBoxId = PATTERN_JSX_GRAPH_INIT_BOX_ID.matcher(graphInitBox);
				matcherGraphBoxId.find();
				String graphInitBoxId = matcherGraphBoxId.group().substring(4, matcherGraphBoxId.group().length() - 1);

				final Matcher matcherGraphBoxWidht = PATTERN_JSX_GRAPH_INIT_BOX_WIDTH.matcher(graphInitBox);
				matcherGraphBoxWidht.find();
				String graphInitBoxWidth = matcherGraphBoxWidht.group().substring(7,
						matcherGraphBoxWidht.group().length() - 3);

				final Matcher matcherGraphBoxHeight = PATTERN_JSX_GRAPH_INIT_BOX_HEIGHT.matcher(graphInitBox);
				matcherGraphBoxHeight.find();
				String graphInitBoxHeight = matcherGraphBoxHeight.group().substring(8,
						matcherGraphBoxHeight.group().length() - 3);

				int countJSXGraphs = jack3Exercise.getJSXGraphs().size();
				JSXGraph jSXGraph = new JSXGraph(graphInitBoxId, countJSXGraphs);
				jSXGraph.setHeight(Integer.parseInt(graphInitBoxHeight));
				jSXGraph.setWidth(Integer.parseInt(graphInitBoxWidth));
				jSXGraph.setText(graphBoxValue);
				jack3Exercise.addJSXGraph(jSXGraph);

				String textGraphBoxReplacement = "[graph=" + graphInitBoxId + "]";

				output = output.replace(fullGraphBox, textGraphBoxReplacement);
			}
		}
		return output;
	}

	private String convertEvalpolynomialToSageExpr(String text, boolean isUpdate) {
		String output = text;
		Set<String> evalPolynomialReplaced = new HashSet<>();
		if (text.contains("evalpolynomial")) {
			final Matcher matcher = PATTERN_EVAL_POLYNOMIAL.matcher(text);
			while (matcher.find()) {

				String evalPolynomialExpr = matcher.group();
				if (evalPolynomialReplaced.contains(evalPolynomialExpr)) {
					continue;
				}
				if (!evalPolynomialExpr.contains("evalpolynomial('")) {
					converterErrorMessages.add("evalpolynomial with special syntax found");
					continue;
				}

				final Matcher matcherValues = PATTERN_ANYTHING_INSIDE_SINGLE_QUOTAION_MARK.matcher(evalPolynomialExpr);
				matcherValues.find();
				String firstValue = matcherValues.group().substring(1, matcherValues.group().length() - 1);
				matcherValues.find();
				String secondValue = matcherValues.group().substring(1, matcherValues.group().length() - 1);
				String evaluateInSageString = "evaluateInSage(\"var('x');f(x)=" + firstValue + ";f(" + secondValue
						+ ")\")";
				if (isUpdate) {
					evaluateInSageString = "evaluateInSage(\"var('x temp');f(x)=" + firstValue + ";temp=f("
							+ secondValue + ")\")";
				}
				output = output.replace(evalPolynomialExpr, evaluateInSageString);
			}
		}
		return output;
	}

	private String convertImageInText(String text) {
		String output = text;
		final Matcher matcherJACK2Img = PATTERN_IMAGE.matcher(text);
		while (matcherJACK2Img.find()) {
			String jack2Image = matcherJACK2Img.group();
			final Matcher matcherSquareBracket = PATTERN_ANYTHING_INSIDE_CLOSING_AND_OPENING_SQUARE_BRACKET
					.matcher(jack2Image);
			matcherSquareBracket.find();
			String jack2fileName = matcherSquareBracket.group().substring(1, matcherSquareBracket.group().length() - 1)
					.strip();

			ExerciseResource exerciseResource = jack3Exercise.getExerciseResources().stream()
					.filter(exRes -> exRes.getFilename().toLowerCase().equals(jack2fileName.toLowerCase())).findAny()
					.get();
			String resourceHTMLCode = getJack3ImageHTMLCode(exerciseResource);
			output = output.replace(jack2Image, resourceHTMLCode);
		}
		return output;
	}

	private String getJack3ImageHTMLCode(ExerciseResource exerciseResource) {
		String resourceURL = contextPath + URL_PATTERN.concat(Long.toString(exerciseResource.getId()));
		StringBuilder imgHTML = new StringBuilder(100);
		imgHTML.append("&nbsp;<img ");
		imgHTML.append("src=");
		imgHTML.append(resourceURL);
		imgHTML.append(" />&nbsp;");
		return imgHTML.toString();
	}

	private void addResourcesToExercise(final User lastEditor) {
		for (Entry<String, byte[]> resourceItem : jack2Data.getResources().entrySet()) {
			ExerciseResource exRes = new ExerciseResource(resourceItem.getKey(), resourceItem.getValue(), lastEditor,
					"", false);
			jack3Exercise.addExerciseResource(exRes);
		}
	}

	private void addVariableUpdateToStage(Stage stage,VariableDeclaration declaration, String code,
			StageEvent updateTime) {
		addVariableUpdateToStage(stage, declaration, new EvaluatorExpression(code), updateTime);
	}

	private void addVariableUpdateToStage(Stage stage,VariableDeclaration declaration, EvaluatorExpression expression,
			StageEvent updateTime) {
		final VariableUpdate variableUpdate = new VariableUpdate(declaration);
		variableUpdate.setUpdateCode(expression);
		switch (updateTime) {
		case START:  stage.addVariableUpdateOnEnter(variableUpdate);      break;
		case SKIP:         stage.addVariableUpdateOnSkip(variableUpdate);       break;
		case BEFORE_CHECK: stage.addVariableUpdateBeforeCheck(variableUpdate);  break;
		case EXIT:         stage.addVariableUpdateOnNormalExit(variableUpdate); break;
		default: throw new AssertionError("Unsupported update time: " + updateTime);
		}
	}
}
