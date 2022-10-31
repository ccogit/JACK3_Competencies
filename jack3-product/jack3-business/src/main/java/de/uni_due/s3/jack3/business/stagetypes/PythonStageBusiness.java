package de.uni_due.s3.jack3.business.stagetypes;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Hibernate;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult.Feedback;
import de.uni_due.s3.jack.dto.generated.JobMetaInformation.JobMetaInfo;
import de.uni_due.s3.jack.dto.generated.TracingPythonCheckerJobData.TracingPythonTransport;
import de.uni_due.s3.jack.dto.generated.TracingPythonCheckerJobData.TracingPythonTransport.File;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.messaging.MessageBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.stagetypes.python.AbstractPythonCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.python.GreqlPythonGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.python.TracingPythonGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.FeedbackMessage;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.JobService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class PythonStageBusiness extends AbstractStageBusiness implements Serializable {

	private static final long serialVersionUID = 174242065601670658L;

	private static final String GREQLPYTHONCHECKER = "de.uni_due.s3.jack.backend.greqlpythonchecker";
	private static final String TRACINGPYTHONCHECKER = "de.uni_due.s3.jack.backend.tracingpythonchecker";

	private static final Pattern FILEREFERENCE = Pattern.compile("#---#(.*)#---#");
	private static final Pattern JACK2LINK = Pattern.compile("<p><a(.*)jack2(.*)</a></p>");

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private JobService jobService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private MessageBusiness messageBusiness;

	@Inject
	@ConfigProperty(name = "mp.messaging.incoming.checker-results.topic")
	private String checkerResultsTopic;

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stageSubmission) {
		// Nothing to prepare
		return stageSubmission;
	}

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stageSubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(PythonSubmission.class, stageSubmission);
		assureCorrectClassUsage(PythonStage.class, stage);

		PythonSubmission pythonSubmission = (PythonSubmission) stageSubmission;
		PythonStage pythonStage = (PythonStage) stage;

		boolean jobsCreated = createAndSendJobs(submission, pythonSubmission, pythonStage);
		pythonSubmission.setHasPendingChecks(jobsCreated);

		// REVIEW: The following lines are quite similar to those from RStageBusiness
		baseService.merge(pythonSubmission);
		submission = submissionService.mergeSubmission(submission);
		baseService.flushEntityManager();

		exercisePlayerBusiness.expandSubmissionAfterSubmit(submission, stage, pythonSubmission);
		baseService.flushEntityManager();

		return pythonSubmission;
	}

	private boolean createAndSendJobs(Submission submission, PythonSubmission pythonSubmission,
			PythonStage pythonStage) {
		boolean jobsCreated = false;

		for (AbstractPythonCheckerConfiguration config : pythonStage.getGradingSteps()) {
			boolean newJobCreated = false;
			if (config instanceof TracingPythonGradingConfig) {
				newJobCreated = createTracingJob(submission, pythonSubmission, pythonStage,
						(TracingPythonGradingConfig) config);
			} else if (config instanceof GreqlPythonGradingConfig) {
				newJobCreated = createGreqlJob(submission, pythonSubmission, pythonStage,
						(GreqlPythonGradingConfig) config);
			}
			jobsCreated = jobsCreated || newJobCreated;
		}

		return jobsCreated;
	}

	private boolean createTracingJob(Submission submission, PythonSubmission pythonSubmission, PythonStage pythonStage,
			TracingPythonGradingConfig config) {

		Map<String, String> sourceFiles = new HashMap<>();
		if (pythonSubmission.getPythonCode() == null) {
			return false;
		}
		sourceFiles.put("__init__.py", pythonSubmission.getPythonCode());

		for (ExerciseResource resource : config.getSourceFiles()) {
			// TODO ms: Replace placeholders here?
			try {
				sourceFiles.put(resource.getFilename(), new String(resource.getContent(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		// TODO ms: Get sample traces

		String testDriver = exercisePlayerBusiness.resolvePlaceholders(config.getTestDriver(), submission,
				pythonSubmission,
				pythonStage, false);

		Job job = new Job(submission, pythonSubmission, "PythonStage", TRACINGPYTHONCHECKER);
		job.setCheckerConfiguration(config);
		baseService.persist(job);

		TracingPythonTransport.Builder transportBuilder = TracingPythonTransport.newBuilder() //
				.setTestDriverContents(testDriver) //
				.setTimeoutSeconds(config.getTimeoutSeconds()) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
								.setResultTopic(checkerResultsTopic)
								.setCheckerType(TRACINGPYTHONCHECKER) //
								.setJobId(job.getId()) //
								.setLocale(submission.getExercise().getLanguage()));

		for (Entry<String, String> sourceFile : sourceFiles.entrySet()) {
			transportBuilder.addSourceFiles(File.newBuilder().setName(sourceFile.getKey())
					.setFileContents(sourceFile.getValue()).setModulePath(config.getStudentModule()).build());
		}

		TracingPythonTransport transport = transportBuilder.build();

		getLogger().info("Sending Job to Topic '" + job.getKafkaTopic() + "'");

		messageBusiness.sendSerializedDtoToKafka(transport.toByteArray(), TRACINGPYTHONCHECKER);

		job.setStarted();
		baseService.merge(job);

		return true;
	}

	private boolean createGreqlJob(Submission submission, PythonSubmission pythonSubmission, PythonStage pythonStage,
			GreqlPythonGradingConfig config) {

		Map<String, String> sourceFiles = new HashMap<>();
		if (pythonSubmission.getPythonCode() == null) {
			return false;
		}
		sourceFiles.put("__init__.py", pythonSubmission.getPythonCode());

		for (ExerciseResource resource : config.getSourceFiles()) {
			sourceFiles.put(resource.getFilename(), new String(resource.getContent(), StandardCharsets.UTF_8));
		}

		// REVIEW: Unused
		String rules = exercisePlayerBusiness.resolvePlaceholders(config.getGreqlRules(), submission,
				pythonSubmission,
				pythonStage, false);

		Job job = new Job(submission, pythonSubmission, "PythonStage", GREQLPYTHONCHECKER);
		job.setCheckerConfiguration(config);
		baseService.persist(job);

		// TODO: Implement GReQL checker and transport
		return true;
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stageSubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps)
			throws InternalErrorEvaluatorException, CalculatorException {
		assureCorrectClassUsage(PythonSubmission.class, stageSubmission);

		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public void handleAsyncCheckerResult(BackendResult backendResult, Job job) {
		String checkerType = backendResult.getJobMetaInfo().getCheckerType();
		if (GREQLPYTHONCHECKER.equals(checkerType)) {
			handleGreqlPythonCheckerResult(backendResult,
					(PythonSubmission) Hibernate.unproxy(job.getStageSubmission()), job.getSubmission(),
					job.getCheckerConfiguration());
		} else if (TRACINGPYTHONCHECKER.equals(checkerType)) {
			handleTracingPythonCheckerResult(backendResult,
					(PythonSubmission) Hibernate.unproxy(job.getStageSubmission()), job.getSubmission(),
					job.getCheckerConfiguration());
		} else {
			throw new UnsupportedOperationException(
					"Unsupported checker type: '" + checkerType + "' for " + getClass().getSimpleName());
		}
	}

	private void handleGreqlPythonCheckerResult(BackendResult backendResult, PythonSubmission pythonSubmission,
			Submission submission, CheckerConfiguration config) {
		int jobResult = backendResult.getResult();

		Result result = new Result(pythonSubmission);
		result.setPoints(jobResult);
		result.setCheckerConfiguration(config);
		result.setCheckerLog(backendResult.getBackendLog());
		result.setFromKafkaTopic(GREQLPYTHONCHECKER);
		result.setErrorResult(!backendResult.getJobMetaInfo().getJobSuccessful());

		String publicComment = "";
		for (Feedback graderFeedbackMessage : backendResult.getFeedbackList()) {
			String category = graderFeedbackMessage.getCategory();
			if ("generalInformation".equals(category)) {
				publicComment = "<h4>" + graderFeedbackMessage.getTitle() + "</h4><p>"
						+ graderFeedbackMessage.getContent() + "</p>";
			} else if ("internalError".equals(category)) {
				publicComment = "<p><i>" + graderFeedbackMessage.getTitle() + "</i>: "
						+ graderFeedbackMessage.getContent() + "</p>";
			} else if ("errorMessage".equals(category)) {
				FeedbackMessage resultFeedbackMessage = new FeedbackMessage();
				resultFeedbackMessage
						.setText(graderFeedbackMessage.getTitle() + ": " + graderFeedbackMessage.getContent());
				result.addFeedbackMessage(resultFeedbackMessage);
			} else if ("reportValue".equals(category)) {
				pythonSubmission.addReportValue(graderFeedbackMessage.getTitle(), graderFeedbackMessage.getContent());
			}
		}

		result.setPublicComment(publicComment);

		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, pythonSubmission, result);
	}

	private void handleTracingPythonCheckerResult(BackendResult backendResult, PythonSubmission pythonSubmission,
			Submission submission, CheckerConfiguration config) {
		int jobResult = backendResult.getResult();

		Result result = new Result(pythonSubmission);
		result.setPoints(jobResult);
		result.setCheckerConfiguration(config);
		result.setCheckerLog(backendResult.getBackendLog());
		result.setFromKafkaTopic(TRACINGPYTHONCHECKER);
		result.setErrorResult(!backendResult.getJobMetaInfo().getJobSuccessful());

		Map<FeedbackMessage, String> messageToDetails = new HashMap<>();
		Map<String, String> detailContents = new HashMap<>();

		String publicComment = "";
		for (Feedback graderFeedbackMessage : backendResult.getFeedbackList()) {
			String category = graderFeedbackMessage.getCategory();
			String content = graderFeedbackMessage.getContent();

			// Extract and clean file references
			Matcher referenceMatcher = FILEREFERENCE.matcher(content);
			String matchResult = "";
			while (referenceMatcher.find()) {
				matchResult = referenceMatcher.group(1);
			}

			Matcher linkMatcher = JACK2LINK.matcher(content);
			content = linkMatcher.replaceAll("");

			if ("generalInformation".equals(category)) {
				publicComment = "<h4>" + graderFeedbackMessage.getTitle() + "</h4><p>" + content + "</p>";
			} else if ("internalError".equals(category)) {
				publicComment = "<p><i>" + graderFeedbackMessage.getTitle() + "</i>: " + content + "</p>";
			} else if ("errorMessage".equals(category)) {
				FeedbackMessage resultFeedbackMessage = new FeedbackMessage();
				resultFeedbackMessage.setText(graderFeedbackMessage.getTitle() + ": " + content);
				if (!matchResult.isEmpty()) {
					messageToDetails.put(resultFeedbackMessage, matchResult);
				}
				result.addFeedbackMessage(resultFeedbackMessage);
			} else if ("reportValue".equals(category)) {
				pythonSubmission.addReportValue(graderFeedbackMessage.getTitle(), graderFeedbackMessage.getContent());
			} else if ("checkerResource".equals(category)) {
				String resourceContent = graderFeedbackMessage.getContent();
				if (resourceContent.contains("<body>")) {
					resourceContent = resourceContent.substring(resourceContent.indexOf("<body>") + 6,
							resourceContent.indexOf("</body>"));
				}
				detailContents.put(graderFeedbackMessage.getTitle(), resourceContent);
			}
		}

		for (Entry<FeedbackMessage, String> detailedFeedback : messageToDetails.entrySet()) {
			if (detailContents.containsKey(detailedFeedback.getValue())) {
				detailedFeedback.getKey().setDetails(detailContents.get((detailedFeedback.getValue())));
			}
		}

		result.setPublicComment(publicComment);

		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, pythonSubmission, result);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stageSubmission, Submission submission) {
		assureCorrectClassUsage(PythonSubmission.class, stageSubmission);
		PythonSubmission pythonSubmission = (PythonSubmission) stageSubmission;

		PythonStage pythonStage = baseService.findById(PythonStage.class, pythonSubmission.getStageId(), false)
				.orElseThrow(VerifyException::new);

		long numberOfPendingJobs = jobService.countPendingJobsForStageSubmission(pythonSubmission);
		int numberOfErrorResults = 0;

		// Propagate pending checks and result points
		if (numberOfPendingJobs > 0) {
			pythonSubmission.setHasPendingChecks(true);
		} else {
			pythonSubmission.setHasPendingChecks(false);
			double points = 0;
			double weights = 0;

			Set<Result> existingResults = pythonSubmission.getResults();
			for (Result result : existingResults) {
				int weight = 1;

				if (result.getCheckerConfiguration() != null) {
					weight = result.getCheckerConfiguration().getWeight();
				} else {
					getLogger().warn("No weight for result " + result);
				}

				weights += weight;

				if (result.isErrorResult()) {
					numberOfErrorResults++;
				} else {
					points += result.getPoints() * weight;
				}
			}

			if (weights > 0) {
				pythonSubmission.setPoints((int) Math.round(points / weights));
			}
		}

		// Propagate internal errors
		if (pythonStage.isPropagateInternalErrors()) {
			pythonSubmission.setHasInternalErrors(numberOfErrorResults > 0);
		}

		return stageSubmissionService.mergeStageSubmission(pythonSubmission);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission submission) {
		assureCorrectClassUsage(PythonSubmission.class, submission);
		PythonSubmission pythonSubmission = (PythonSubmission) submission;

		Map<String, VariableValue> inputs = new HashMap<>();

		inputs.put("code", VariableValueFactory.createVariableValueForOpenMathString(pythonSubmission.getPythonCode()));

		for (Entry<String, String> reportValue : pythonSubmission.getReportValues().entrySet()) {
			inputs.put(reportValue.getKey(),
					VariableValueFactory.createVariableValueForOpenMathString(reportValue.getValue()));
		}

		return inputs;
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission submission) {
		return new HashMap<>();
	}
}
