package de.uni_due.s3.jack3.business.stagetypes;

import static de.uni_due.s3.jack.dto.generated.DynamicRCheckerJobData.RTransport.Testcase.TestcaseType.ABSENCE;
import static de.uni_due.s3.jack.dto.generated.DynamicRCheckerJobData.RTransport.Testcase.TestcaseType.PRESENCE;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Hibernate;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack.dto.generated.ConsoleEvaluationRequest.ConsoleEvalRequest;
import de.uni_due.s3.jack.dto.generated.DynamicRCheckerJobData.RTransport;
import de.uni_due.s3.jack.dto.generated.DynamicRCheckerJobData.RTransport.Testcase;
import de.uni_due.s3.jack.dto.generated.DynamicRCheckerJobData.RTransport.Testcase.Builder;
import de.uni_due.s3.jack.dto.generated.JobMetaInformation.JobMetaInfo;
import de.uni_due.s3.jack.dto.generated.StaticRCheckerDTO.Rule;
import de.uni_due.s3.jack.dto.generated.StaticRCheckerDTO.StaticRCheckerData;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.messaging.MessageBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterException;
import de.uni_due.s3.jack3.business.microservices.converterutils.InternalErrorConverterException;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.stagetypes.r.AbstractTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.DynamicRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestcaseRuleMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.RSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTupleResult;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.ConsoleResult;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.RStageJob;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class RStageBusiness extends AbstractStageBusiness implements Serializable {

	private static final String R_STUDENT_INPUT_VARIABLE_KEY = "rStudentInputSetBySystem";

	private static final long serialVersionUID = -4622776789423734393L;

	public static final String DYNAMIC_R_CHECKER = "de.uni_due.s3.jack3.dynamicrchecker";
	public static final String STATIC_R_CHECKER = "de.uni_due.s3.jack3.staticrchecker";
	public static final String R_CONSOLE_SERVICE = "de.uni_due.s3.jack.backend.r_console_service";
	private static final int DEFAULT_MIN_POINTS = 0;

	private static final int DEFAULT_MAX_POINTS = 100;

	@Inject
	private ConverterBusiness converterBusiness;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private CalculatorBusiness calculatorBusiness;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private MessageBusiness messageBusiness;

	@Inject
	@ConfigProperty(name = "mp.messaging.incoming.console-results.topic")
	private String consoleResultsTopic;

	@Inject
	@ConfigProperty(name = "mp.messaging.incoming.checker-results.topic")
	private String checkerResultsTopic;

	@Override
	public boolean hasCodeTestCapability() {
		return true;
	}

	@Override
	public long testCode(StageSubmission stageSubmission, String initiatingUserString) {
		RSubmission rSubmission = (RSubmission) stageSubmission;
		String studentInput = rSubmission.getStudentInput();

		ConsoleResult consoleResult = new ConsoleResult();
		consoleResult.setInitiatingUser(initiatingUserString);
		consoleResult.setInput(studentInput);
		consoleResult.setStartedAt(LocalDateTime.now());
		consoleResult.setHandlerStageType("RStage");
		baseService.persist(consoleResult);

		long resultId = consoleResult.getId();

		ConsoleEvalRequest consoleEvalRequest = ConsoleEvalRequest.newBuilder()//
				.setCodeToEvaluate(studentInput) //
				.setId(String.valueOf(resultId)) //
				.setResultTopic(consoleResultsTopic) //
				.build();

		messageBusiness.sendSerializedDtoToKafka(consoleEvalRequest.toByteArray(), R_CONSOLE_SERVICE);

		return resultId;
	}

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stagesubmission) {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		assureCorrectClassUsage(RStage.class, stage);
		// This is called after a new plain stage submission is created and before it is displayed to the user.
		// At the moment there is nothing we need to do here
		return stagesubmission;
	}

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		assureCorrectClassUsage(RStage.class, stage);

		RSubmission rstageSubmission = (RSubmission) stagesubmission;
		RStage rStage = (RStage) stage;
		List<TestCaseTuple> testCasetuples = rStage.getTestCasetuples();

		boolean atLeastOneJobCreated = false;
		for (TestCaseTuple testCasetuple : testCasetuples) {
			initializeAndPersistTestCaseTupleResults(rstageSubmission, testCasetuple);
			atLeastOneJobCreated = sendJobsToKafka(submission, rstageSubmission, atLeastOneJobCreated, testCasetuple);
		}

		if (!atLeastOneJobCreated) {
			// No Job was created, setting result to 0
			setResultNothingWasTested(submission, rstageSubmission, rStage);
		} else {
			rstageSubmission.setHasPendingChecks(true);
		}
		baseService.merge(rstageSubmission);
		submission = submissionService.mergeSubmission(submission);
		// WTF? We need to actually flush here, because if we don't, we will get a unique constraint violation in the
		// KafkaReciver from the constraint "submission_submissionlogentry_pkey"!
		// This might have to do with this transaction still being open while the kafka-receiver tries to also write the
		// submission.
		baseService.flushEntityManager();

		if (atLeastOneJobCreated) {
			// Otherwise we already called expandSubmissionAfterSubmit() through addResultToSubmission()
			exercisePlayerBusiness.expandSubmissionAfterSubmit(submission, stage, rstageSubmission);
		}
		baseService.flushEntityManager();
		return rstageSubmission;
	}

	private boolean sendJobsToKafka(Submission submission, RSubmission rSubmission, boolean atLeastOneJobCreated,
			TestCaseTuple testCasetuple) {
		for (AbstractTestCase testCase : testCasetuple.getTestCases()) {
			boolean jobCreated = sendJobToKafka(submission, rSubmission, testCase);
			atLeastOneJobCreated = atLeastOneJobCreated || jobCreated;
		}
		return atLeastOneJobCreated;
	}

	private void initializeAndPersistTestCaseTupleResults(RSubmission rSubmission, TestCaseTuple testCasetuple) {
		rSubmission.initializeTestCaseTupleResult(testCasetuple);
		for (TestCaseTupleResult testCaseTupleResult : rSubmission.getTestCaseTupleResults()) {
			baseService.persist(testCaseTupleResult);
		}
	}

	private void setResultNothingWasTested(Submission submission, RSubmission rSubmission, RStage rStage) {
		getLogger().warn("No Jobs created, setting rStage result to 0!");
		rSubmission.setHasPendingChecks(false);
		submission.setHasPendingStageChecks(false);

		Result result = new Result(rSubmission);
		result.setPoints(0);
		result.setPublicComment("Nothing was tested, please contact the exercise-creator"); // TODO internat.
		baseService.persist(result);
		exercisePlayerBusiness.addResultToSubmission(submission, rSubmission, rStage, result);
	}

	private boolean sendJobToKafka(Submission submission, RSubmission rSubmission, AbstractTestCase testCase) {
		byte[] transportBytes;
		RStageJob job;
		String topic;
		if (testCase.isDynamic()) {
			topic = DYNAMIC_R_CHECKER;
			job = generateRJob(submission, rSubmission, testCase, topic);
			DynamicRTestCase dynamicRTestCase = (DynamicRTestCase) testCase;
			RTransport dto = generateDynamicDTO(rSubmission, dynamicRTestCase, job.getId(), checkerResultsTopic);
			transportBytes = dto.toByteArray();
			getLogger().info("Sending dynamic R job to topic '" + topic + "', Message: '" + dto + "'");
		} else if (testCase.isStatic()) {
			topic = STATIC_R_CHECKER;
			job = generateRJob(submission, rSubmission, testCase, topic);
			StaticRTestCase staticRTestCase = (StaticRTestCase) testCase;
			StaticRCheckerData dto = generateStaticDTO(rSubmission, staticRTestCase, job.getId(), checkerResultsTopic);
			transportBytes = dto.toByteArray();
			getLogger().info("Sending static R job to topic '" + topic + "', Message: '" + dto + "'");
		} else {
			throw new UnsupportedOperationException("Type of testcase not (yet) supported: " + testCase);
		}

		messageBusiness.sendSerializedDtoToKafka(transportBytes, topic);

		job.setStarted();
		baseService.persist(job);
		return true;
	}

	private StaticRCheckerData generateStaticDTO(RSubmission rSubmission, StaticRTestCase testCase, long jobId,
			String kafkaResultTopic) {
		Rule rule = convertToRuleListDTO(testCase, rSubmission);

		return StaticRCheckerData.newBuilder() //
				.setRule(rule) //
				.setUserInput(rSubmission.getStudentInput()) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
						.setResultTopic(kafkaResultTopic) //
						.setCheckerType(STATIC_R_CHECKER).setJobId(jobId)) //
				.build();
	}

	private RStageJob generateRJob(Submission submission, RSubmission rSubmission, AbstractTestCase testCase,
			String kafkaTopic) {

		RStageJob job = new RStageJob(submission, rSubmission, testCase, kafkaTopic);
		baseService.persist(job);
		return job;
	}

	private RTransport generateDynamicDTO(RSubmission rSubmission, DynamicRTestCase testCase, long jobId,
			String kafkaResultTopic) {
		Testcase transportTestCase = convertToDTO(testCase, rSubmission);

		return RTransport.newBuilder() //
				.setTestcase(transportTestCase) //
				.setUserInput(rSubmission.getStudentInput()) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
						.setResultTopic(kafkaResultTopic) //
						.setCheckerType(DYNAMIC_R_CHECKER) //
						.setJobId(jobId)) //
				.build();
	}

	/**
	 * Converts the JACK internal DynamicTestCase object to a DTO testcase object to be sent to Kafka
	 *
	 * @return
	 */
	private Testcase convertToDTO(DynamicRTestCase testCase, RSubmission rSubmission) {
		Builder testCaseBuilder = Testcase.newBuilder();

		String expectedOutput = testCase.getExpectedOutput();
		if (expectedOutput != null) {
			expectedOutput = replaceVariables(expectedOutput, rSubmission);
			testCaseBuilder.setExpectedOutput(expectedOutput);
		}

		String name = testCase.getName();
		if (name != null) {
			testCaseBuilder.setName(name);
		}

		String postCode = testCase.getPostCode();
		if (postCode != null) {
			testCaseBuilder.setPostcode(postCode);
		}

		String postprocessingFunction = testCase.getPostprocessingFunction();
		if (postprocessingFunction != null) {
			testCaseBuilder.setPostprocessingFunction(postprocessingFunction);
		}

		if (ETestcaseRuleMode.ABSENCE.equals(testCase.getRuleMode())) {
			testCaseBuilder.setTestcaseType(ABSENCE);
		} else if (ETestcaseRuleMode.PRESENCE.equals(testCase.getRuleMode())) {
			testCaseBuilder.setTestcaseType(PRESENCE);
		} else {
			throw new UnsupportedOperationException("Ruletype unknown: " + testCase.getRuleMode());
		}

		testCaseBuilder.setTolerance(testCase.getTolerance());

		return testCaseBuilder.build();
	}

	/**
	 * Converts the JACK internal DynamicTestCase object to a DTO testcase object to be sent to Kafka
	 *
	 * @return
	 */
	private Rule convertToRuleListDTO(StaticRTestCase testCase, RSubmission rSubmission) {
		de.uni_due.s3.jack.dto.generated.StaticRCheckerDTO.Rule.Builder rule = Rule.newBuilder();

		// Make sure to work on migrated data
		testCase.migrateSingleQueries();

		List<String> queries = testCase.getQueries();
		if (queries != null && !queries.isEmpty()) {
			for (String query : queries) {
				query = replaceVariables(query, rSubmission);
				rule.addQuery(query);
			}
		}

		String feedbackIfFailed = testCase.getFeedbackIfFailed();
		if (feedbackIfFailed != null) {
			rule.setFeedback(feedbackIfFailed);
		}

		rule.setId(String.valueOf(System.nanoTime())); // TODO

		Rule.Type type;
		if (ETestcaseRuleMode.ABSENCE.equals(testCase.getRuleMode())) {
			type = Rule.Type.ABSENCE;
		} else if (ETestcaseRuleMode.PRESENCE.equals(testCase.getRuleMode())) {
			type = Rule.Type.PRESENCE;
		} else {
			throw new UnsupportedOperationException("Ruletype unknown: " + testCase.getRuleMode());
		}
		rule.setType(type);

		return rule.build();
	}

	private String replaceVariables(String inputText, StageSubmission stageSubmission) {
		EvaluatorMaps evaluatorMaps = new EvaluatorMaps();
		stageSubmission = baseService.findById(StageSubmission.class, stageSubmission.getId(), true).orElseThrow();
		evaluatorMaps.setExerciseVariableMap(stageSubmission.getVariableValues());

		return converterBusiness.replaceVariablesByVariableName(inputText, evaluatorMaps);
	}

	public void removeTestCase(TestCaseTuple testCasetuple, AbstractTestCase testCase) {
		testCasetuple.removeTestCase(testCase);
	}

	@Override
	public void setViewDefaults(Stage stage) {
		addNewTestCasetuple(stage);
	}

	public void addNewTestCasetuple(Stage stage) {
		assureCorrectClassUsage(RStage.class, stage);

		TestCaseTuple newTestCasetuple = new TestCaseTuple();
		CheckerConfiguration checkerConfiguration = new CheckerConfiguration();

		setCheckerConfigDefaults(checkerConfiguration);

		newTestCasetuple.setCheckerConfiguration(checkerConfiguration);

		RStage rStage = (RStage) stage;
		if (rStage.getTestCasetuples() == null) {
			rStage.setTestCasetuples(new ArrayList<>());
		}

		rStage.getTestCasetuples().add(newTestCasetuple);
	}

	private void setCheckerConfigDefaults(CheckerConfiguration checkerConfiguration) {
		checkerConfiguration.setActive(true);
		checkerConfiguration.setAsync(true);
	}

	public void removeTestCasetuple(Stage stage, TestCaseTuple testCasetuple) {
		assureCorrectClassUsage(RStage.class, stage);

		List<TestCaseTuple> testCaseUnits = ((RStage) stage).getTestCasetuples();
		testCaseUnits.remove(testCasetuple);
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps) throws InternalErrorEvaluatorException {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		assureCorrectClassUsage(RStage.class, stage);
		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		RSubmission rSubmission = (RSubmission) stagesubmission;

		HashMap<String, VariableValue> input = new HashMap<>();
		VariableValue studentInput = VariableValueFactory
				.createVariableValueForOpenMathString(rSubmission.getStudentInput());
		input.put(R_STUDENT_INPUT_VARIABLE_KEY, studentInput);
		return input;
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		return new HashMap<>();
	}

	@Override
	public String formatConsoleResponse(String consoleResponse) {
		return Arrays.asList(consoleResponse.split("\n")).stream() //
				.map(line -> line.replace("$value", "")) //
				.filter(line -> !line.isBlank()) //
				.map(line -> line.replaceAll("\\[\\[(.*)\\]\\]", "$1:")) //
				.takeWhile(line -> !"$visible".equals(line)) //
				.collect(Collectors.joining("\n"));
	}

	@Override
	public void handleAsyncCheckerResult(BackendResult backendResult, Job job) {
		RStageJob rStageJob = (RStageJob) job;

		String checkerType = backendResult.getJobMetaInfo().getCheckerType();
		if (!RStageBusiness.DYNAMIC_R_CHECKER.equals(checkerType)
				&& !RStageBusiness.STATIC_R_CHECKER.equals(checkerType)) {
			throw new UnsupportedOperationException(
					"Unsupported checker type: '" + checkerType + "' for " + getClass().getSimpleName());
		}
		handleRCheckerResult(backendResult, rStageJob, rStageJob.getTestCase());
	}

	private void handleRCheckerResult(BackendResult backendResult, RStageJob job, AbstractTestCase testCase) {
		RSubmission rSubmission = (RSubmission) Hibernate.unproxy(job.getStageSubmission());
		Set<TestCaseTupleResult> testCaseTupleResults = rSubmission.getTestCaseTupleResults();

		job.setGraderId(backendResult.getJobMetaInfo().getGraderId());
		job.setStageSubmission(rSubmission);
		baseService.merge(job);

		Result result = new Result(rSubmission);
		result.setCheckerLog(backendResult.getBackendLog());

		TestCaseTupleResult relevantTestcaseTupleResult = rSubmission.getTestCaseTupleResultContaining(testCase);

		calculateResult(backendResult, testCase, rSubmission, testCaseTupleResults, result,
				relevantTestcaseTupleResult);

		result.setFromKafkaTopic(backendResult.getJobMetaInfo().getCheckerType());

		Submission submission = job.getSubmission();
		boolean hasPendingChecks = hasPendingChecks(testCaseTupleResults);
		submission.setHasPendingStageChecks(hasPendingChecks);
		rSubmission.setHasPendingChecks(false);

		getLogger().info("Calculated new async R result: " + result);
		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, rSubmission, result);
	}

	private void calculateResult(BackendResult backendResult, AbstractTestCase testCase, RSubmission rSubmission,
			Set<TestCaseTupleResult> testCaseTupleResults, Result result,
			TestCaseTupleResult relevantTestcaseTupleResult) {

		// We are only interested if this rule matched or not. The backend encodes false as 0 and true as 100, because
		// we use a unified BackendResult Object that returns points as results.
		// The calculation how many points a rule is worth is made in the testcasetupel.
		boolean ruleMatched = backendResult.getResult() == 100;

		TestCaseTupleResult testCaseTupleResult = testCaseTupleResults.stream()
				.filter(tupleResult -> tupleResult.getTestCaseTuple().getTestCases().contains(testCase)) //
				.findAny() //
				.orElseThrow();
		testCaseTupleResult.setRuleMatched(testCase, ruleMatched);

		setResultPointsForTestcase(testCase, result, ruleMatched);

		if ((!ruleMatched && testCase.isPresenceMode()) || (ruleMatched && testCase.isAbsenceMode())) {
			String feedBack = relevantTestcaseTupleResult.getFailureFeedback(testCase);
			result.setPublicComment(replaceVariables(feedBack, rSubmission));
		}
	}

	private void setResultPointsForTestcase(AbstractTestCase testCase, Result result, boolean ruleMatched) {
		if (testCase.isDeductionMode()) {
			if ((ruleMatched && testCase.isAbsenceMode()) || (!ruleMatched && testCase.isPresenceMode())) {
				result.setPoints(DEFAULT_MAX_POINTS - testCase.getPoints());
			} else {
				result.setPoints(DEFAULT_MAX_POINTS);
			}
		} else if (testCase.isGainMode()) {
			if ((ruleMatched && testCase.isPresenceMode()) || (!ruleMatched && testCase.isAbsenceMode())) {
				result.setPoints(testCase.getPoints());
			} else {
				result.setPoints(DEFAULT_MIN_POINTS);
			}
		} else {
			throw new IllegalStateException("We should't be able to get here: " //
					+ "getPointsMode(): " + testCase.getPointsMode() //
					+ ", getRuleMode(): " + testCase.getRuleMode() //
					+ ", ruleMatched: " + ruleMatched);
		}
	}

	private boolean hasPendingChecks(Set<TestCaseTupleResult> testCaseTupleResults) {
		return testCaseTupleResults.stream().anyMatch(TestCaseTupleResult::hasPendingChecks);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stagesubmission, Submission submission) {
		assureCorrectClassUsage(RSubmission.class, stagesubmission);
		RSubmission rstagesubmission = (RSubmission) stagesubmission;

		if (hasPendingChecks(rstagesubmission)) {
			return rstagesubmission;
		}
		getLogger().info("All Jobs for " + rstagesubmission + " have been received, calculating final result...");
		rstagesubmission.setPoints(calculateFinalResult(rstagesubmission, submission));
		return stageSubmissionService.mergeStageSubmission(rstagesubmission);
	}

	private Integer calculateFinalResult(RSubmission stagesubmission, Submission submission) {
		if (hasPendingChecks(stagesubmission)) {
			throw new VerifyException("Calculation of final Results was triggered, but there are still pending results!"
					+ stagesubmission);
		}

		RStage stage = baseService.findById(RStage.class, stagesubmission.getStageId(), false)
				.orElseThrow(VerifyException::new);

		String finalResultComputationString = stage.getFinalResultComputationString();

		if ((finalResultComputationString == null) || finalResultComputationString.isEmpty()) {
			return calculateFinalResultDefaultCase(stagesubmission);
		}

		finalResultComputationString = replaceCheckerConfigVarsWithResults(stage, stagesubmission);
		Integer result = evaluateFinalResultString(stagesubmission, finalResultComputationString, submission);
		return ensureResultBorders(result);
	}

	private Integer evaluateFinalResultString(RSubmission rSubmission, String finalResultComputationString,
			Submission submission) {
		Integer result = 0;
		try {
			EvaluatorExpression expression = new EvaluatorExpression(finalResultComputationString);
			VariableValue evaluatorResult = calculatorBusiness.calculateToVariableValue(expression,
					new EvaluatorMaps());
			// REVIEW bo: kann man einen Int-Wert auch direkt aus dem Evaluator zurückgeben lassen?
			Double resultAsDouble = Double.valueOf(converterBusiness.convertToString(evaluatorResult));
			result = ensureResultBorders(Math.toIntExact(Math.round(resultAsDouble)));
		} catch (InternalErrorEvaluatorException | ConverterException | InternalErrorConverterException e) {
			rSubmission.setHasInternalErrors(true);

			exercisePlayerBusiness.addFailureToSubmission(submission, rSubmission,
					"Final results of an RStage could not be calculated, please check the 'Evaluationrule' at the 'Feedback' page!",
					e);
			baseService.merge(rSubmission);
		}
		return result;
	}

	private String replaceCheckerConfigVarsWithResults(RStage stage, RSubmission stagesubmission) {
		String result = stage.getFinalResultComputationString();
		for (TestCaseTupleResult tupleResult : stagesubmission.getTestCaseTupleResults()) {
			long checkerConfigId = tupleResult.getTestCaseTuple().getCheckerConfiguration().getId();
			int points = calculateTupleResult(tupleResult)
					.orElseThrow(() -> new VerifyException("There should be no pending checks anymore!"));
			result = result.replace("#{c" + checkerConfigId + "}", String.valueOf(points));
		}
		return result;
	}

	private boolean hasPendingChecks(RSubmission stagesubmission) {
		return stagesubmission.getTestCaseTupleResults() //
				.stream() //
				.map(TestCaseTupleResult::hasPendingChecks) //
				.reduce(false, (partialHasPending, hasPending) -> partialHasPending || hasPending);
	}

	private Integer calculateFinalResultDefaultCase(RSubmission stagesubmission) {
		Integer finalResult = stagesubmission.getTestCaseTupleResults() //
				.stream() //
				.map(this::calculateTupleResult) //
				.map(Optional::orElseThrow) // We already checked all results are present
				.reduce(0, (partialResult, result) -> partialResult + result);
		return ensureResultBorders(finalResult);
	}

	private Optional<Integer> calculateTupleResult(TestCaseTupleResult ttr) {
		if (ttr.hasPendingChecks()) {
			return Optional.empty();
		}
		int result = 0;

		for (Entry<AbstractTestCase, String> entry : ttr.getTestCaseResultsMap().entrySet()) {
			AbstractTestCase testCase = entry.getKey();

			boolean testCaseWasSuccessful = Boolean.parseBoolean(entry.getValue());
			if (!testCaseWasSuccessful) {
				continue;
			}
			result += testCase.getPoints();
		}
		return Optional.of(ensureResultBorders(result));
	}

	private int ensureResultBorders(int points) {
		int result = Math.min(Math.max(0, points), 100);
		if (points != result) {
			getLogger().debug("Warn: Cutting result points " + points + " to be between 0 and 100!");
		}
		return result;
	}

}