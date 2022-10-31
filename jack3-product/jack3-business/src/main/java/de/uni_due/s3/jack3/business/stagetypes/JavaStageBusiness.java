package de.uni_due.s3.jack3.business.stagetypes;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import de.uni_due.s3.jack.dto.generated.GreqlJavaCheckerJobData.GreqlJavaTransport;
import de.uni_due.s3.jack.dto.generated.GreqlJavaCheckerJobData.GreqlJavaTransport.SourceFile;
import de.uni_due.s3.jack.dto.generated.JobMetaInformation.JobMetaInfo;
import de.uni_due.s3.jack.dto.generated.TracingJavaCheckerJobData.TracingJavaTransport;
import de.uni_due.s3.jack.dto.generated.TracingJavaCheckerJobData.TracingJavaTransport.File;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.messaging.MessageBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.stagetypes.java.AbstractJavaCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.java.GreqlGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.java.MetricsGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.TracingGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.FeedbackMessage;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.JobService;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class JavaStageBusiness extends AbstractStageBusiness implements Serializable {

	private static final long serialVersionUID = 174242065601670658L;

	private static final String GREQLJAVACHECKER = "de.uni_due.s3.jack2.backend.checkers.greqljavachecker";
	private static final String TRACINGJAVACHECKER = "de.uni_due.s3.jack2.backend.checkers.tracingjavachecker";
	private static final String METRICSJAVACHECKER = "de.uni_due.s3.jack2.backend.checkers.metricsjavachecker";

	private static final Pattern FILEREFERENCE = Pattern.compile("#---#(.*)#---#");
	private static final Pattern JACK2LINK = Pattern.compile("<p><a(.*)jack2(.*)</a></p>");

	@Inject
	private JobService jobService;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private ResultService resultService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

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
		assureCorrectClassUsage(JavaSubmission.class, stageSubmission);
		assureCorrectClassUsage(JavaStage.class, stage);

		JavaSubmission javaSubmission = (JavaSubmission) stageSubmission;
		JavaStage javaStage = (JavaStage) stage;

		if (validateFileUploads(javaSubmission, javaStage)) {
			boolean jobsCreated = createAndSendJobs(submission, javaSubmission, javaStage);
			javaSubmission.setHasPendingChecks(jobsCreated);
		} else {
			javaSubmission.setHasPendingChecks(false);

			// We must first create the REPEAT entry in the log, because otherwise the incoming result will cause the
			// exercise to END
			if (javaStage.isRepeatOnMissingUpload()) {
				exercisePlayerBusiness.handleRepeatTransition(submission, javaStage, javaSubmission);
			}

			Result result = new Result(javaSubmission);
			result.setPoints(0);

			// REVIEW ms: Where is a good place for such language-dependent strings, which are NOT part of the UI?
			String publicComment = "Your submission misses required files.";
			if ("de".equals(submission.getExercise().getLanguage())) {
				publicComment = "Die Einreichung enthält nicht alle erforderlichen Dateien.";
			}
			result.setPublicComment(publicComment);

			resultService.persistResult(result);
			javaSubmission = (JavaSubmission) exercisePlayerBusiness.addResultToSubmission(submission, javaSubmission, stage,
					result);

			if (javaStage.isRepeatOnMissingUpload()) {
				return javaSubmission;
			}
		}

		// REVIEW: The following lines are quite similar to those from RStageBusiness
		baseService.merge(javaSubmission);
		submission = submissionService.mergeSubmission(submission);
		baseService.flushEntityManager();

		exercisePlayerBusiness.expandSubmissionAfterSubmit(submission, stage, javaSubmission);
		baseService.flushEntityManager();

		return javaSubmission;
	}

	private boolean validateFileUploads(JavaSubmission javaSubmission, JavaStage javaStage) {
		Set<SubmissionResource> uploadedFiles = javaSubmission.getUploadedFiles();

		// Check number
		if ((uploadedFiles.size() < javaStage.getMinimumFileCount()) || (uploadedFiles.size() > javaStage.getMaximumFileCount())) {
			return false;
		}

		// Check names
		Set<String> expectedNames = new HashSet<>(javaStage.getMandatoryFileNamesAsSet());
		Set<String> allowedNames = new HashSet<>(javaStage.getAllowedFileNamesAsSet());
		allowedNames.addAll(expectedNames);

		for (SubmissionResource file : uploadedFiles) {
			if (!allowedNames.contains(file.getFilename())) {
				return false;
			}

			expectedNames.remove(file.getFilename());
		}

		if (!expectedNames.isEmpty()) {
			return false;
		}

		return true;
	}

	private boolean createAndSendJobs(Submission submission, JavaSubmission javaSubmission, JavaStage javaStage) {
		boolean jobsCreated = false;

		for (AbstractJavaCheckerConfiguration config : javaStage.getGradingSteps()) {
			boolean newJobCreated = false;
			if (config instanceof TracingGradingConfig) {
				newJobCreated = createTracingJob(submission, javaSubmission, javaStage, (TracingGradingConfig) config);
			} else if (config instanceof GreqlGradingConfig) {
				newJobCreated = createGreqlJob(submission, javaSubmission, javaStage, (GreqlGradingConfig) config);
			} else if (config instanceof MetricsGradingConfig) {
				newJobCreated = createMetricsJob(submission, javaSubmission, javaStage, (MetricsGradingConfig) config);
			}
			jobsCreated = jobsCreated || newJobCreated;
		}

		return jobsCreated;
	}

	private boolean createTracingJob(Submission submission, JavaSubmission javaSubmission, JavaStage javaStage,
			TracingGradingConfig config) {

		Map<String, String> sourceFiles = new HashMap<>();
		for (SubmissionResource resource : javaSubmission.getUploadedFiles()) {
			if (config.getFileNames().contains(resource.getFilename())) {
				try {
					sourceFiles.put(resource.getFilename(), new String(resource.getContent(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		for (ExerciseResource resource : config.getSourceFiles()) {
			try {
				final byte[] content = exerciseBusiness.getExerciseResourceContent(resource, submission, javaSubmission,
						javaStage);
				sourceFiles.put(resource.getFilename(), new String(content, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		Map<String, String> libraryFiles = new HashMap<>();
		for (ExerciseResource resource : config.getLibraryFiles()) {
			try {
				final byte[] content = exerciseBusiness.getExerciseResourceContent(resource, submission, javaSubmission,
						javaStage);
				libraryFiles.put(resource.getFilename(), new String(content, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		// TODO ms: Get sample traces
		String testDriver = exercisePlayerBusiness.resolvePlaceholders(config.getTestDriver(), submission,
				javaSubmission,
				javaStage, false);

		List<String> classesToTrace = new LinkedList<>();
		for (String classToTrace : config.getClassesToTrace()) {
			classesToTrace.add(
					exercisePlayerBusiness.resolvePlaceholders(classToTrace, submission, javaSubmission, javaStage,
							false));
		}

		Job job = new Job(submission, javaSubmission, "JavaStage", TRACINGJAVACHECKER);
		job.setCheckerConfiguration(config);
		baseService.persist(job);

		TracingJavaTransport.Builder transportBuilder = TracingJavaTransport.newBuilder() //
				.setTestDriverContents(testDriver) //
				.setTimeoutSeconds(config.getTimeoutSeconds()) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
								.setResultTopic(checkerResultsTopic) //
								.setCheckerType(TRACINGJAVACHECKER) //
								.setJobId(job.getId()) //
								.setLocale(submission.getExercise().getLanguage()));

		for (Entry<String, String> sourceFile : sourceFiles.entrySet()) {
			transportBuilder.addSourceFiles(
					File.newBuilder().setName(sourceFile.getKey()).setFileContents(sourceFile.getValue()).build());
		}

		for (Entry<String, String> libraryFile : libraryFiles.entrySet()) {
			transportBuilder.addLibraryFiles(
					File.newBuilder().setName(libraryFile.getKey()).setFileContents(libraryFile.getValue()).build());
		}

		classesToTrace.stream().forEach(transportBuilder::addClassesToTrace);

		TracingJavaTransport transport = transportBuilder.build();

		getLogger().info("Sending Job to Topic '" + job.getKafkaTopic() + "'");

		messageBusiness.sendSerializedDtoToKafka(transport.toByteArray(), TRACINGJAVACHECKER);

		job.setStarted();
		baseService.merge(job);

		return true;
	}

	private boolean createGreqlJob(Submission submission, JavaSubmission javaSubmission, JavaStage javaStage,
			GreqlGradingConfig config) {
		Map<String, String> sourceFiles = new HashMap<>();
		for (SubmissionResource resource : javaSubmission.getUploadedFiles()) {
			if (config.getFileNames().contains(resource.getFilename())) {
				try {
					sourceFiles.put(resource.getFilename(), new String(resource.getContent(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		for (ExerciseResource resource : config.getSourceFiles()) {
			try {
				final byte[] content = exerciseBusiness.getExerciseResourceContent(resource, submission, javaSubmission,
						javaStage);
				sourceFiles.put(resource.getFilename(), new String(content, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		String rules = exercisePlayerBusiness.resolvePlaceholders(config.getGreqlRules(), submission, javaSubmission,
				javaStage, false);

		Job job = new Job(submission, javaSubmission, "JavaStage", GREQLJAVACHECKER);
		job.setCheckerConfiguration(config);
		baseService.persist(job);

		GreqlJavaTransport.Builder transportBuilder = GreqlJavaTransport.newBuilder() //
				.setRuleFileContents(rules) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
								.setResultTopic(checkerResultsTopic) //
								.setCheckerType(GREQLJAVACHECKER) //
								.setJobId(job.getId()) //
								.setLocale(submission.getExercise().getLanguage()));

		for (Entry<String, String> sourceFile : sourceFiles.entrySet()) {
			transportBuilder.addSourceFiles(SourceFile.newBuilder().setName(sourceFile.getKey())
					.setFileContents(sourceFile.getValue()).build());
		}

		GreqlJavaTransport transport = transportBuilder.build();

		getLogger().info("Sending Job to Topic '" + job.getKafkaTopic() + "'");

		messageBusiness.sendSerializedDtoToKafka(transport.toByteArray(), GREQLJAVACHECKER);

		job.setStarted();
		baseService.merge(job);

		return true;
	}

	private boolean createMetricsJob(Submission submission, JavaSubmission javaSubmission, JavaStage javaStage,
			MetricsGradingConfig config) {
		Map<String, String> sourceFiles = new HashMap<>();
		for (SubmissionResource resource : javaSubmission.getUploadedFiles()) {
			if (config.getFileNames().contains(resource.getFilename())) {
				try {
					sourceFiles.put(resource.getFilename(), new String(resource.getContent(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		for (ExerciseResource resource : config.getSourceFiles()) {
			try {
				final byte[] content = exerciseBusiness.getExerciseResourceContent(resource, submission, javaSubmission,
						javaStage);
				sourceFiles.put(resource.getFilename(), new String(content, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}

		Job job = new Job(submission, javaSubmission, "JavaStage", METRICSJAVACHECKER);
		job.setCheckerConfiguration(config);
		baseService.persist(job);

		GreqlJavaTransport.Builder transportBuilder = GreqlJavaTransport.newBuilder() //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
								.setResultTopic(checkerResultsTopic) //
								.setCheckerType(METRICSJAVACHECKER) //
								.setJobId(job.getId()) //
								.setLocale(submission.getExercise().getLanguage()));
		for (Entry<String, String> sourceFile : sourceFiles.entrySet()) {
			transportBuilder.addSourceFiles(SourceFile.newBuilder().setName(sourceFile.getKey())
					.setFileContents(sourceFile.getValue()).build());
		}

		GreqlJavaTransport transport = transportBuilder.build();

		getLogger().info("Sending Job to Topic '" + job.getKafkaTopic() + "'");

		messageBusiness.sendSerializedDtoToKafka(transport.toByteArray(), METRICSJAVACHECKER);

		job.setStarted();
		baseService.merge(job);

		return true;
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stageSubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps)
			throws InternalErrorEvaluatorException, CalculatorException {
		assureCorrectClassUsage(JavaSubmission.class, stageSubmission);

		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission submission) {
		assureCorrectClassUsage(JavaSubmission.class, submission);
		JavaSubmission javaSubmission = (JavaSubmission) submission;

		Map<String, VariableValue> inputs = new HashMap<>();

		for (Entry<String, String> reportValue : javaSubmission.getReportValues().entrySet()) {
			inputs.put(reportValue.getKey(),
					VariableValueFactory.createVariableValueForOpenMathString(reportValue.getValue()));
		}

		return inputs;
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission submission) {
		return new HashMap<>();
	}

	@Override
	public void handleAsyncCheckerResult(BackendResult backendResult, Job job) {
		String checkerType = backendResult.getJobMetaInfo().getCheckerType();
		if (GREQLJAVACHECKER.equals(checkerType)) {
			handleGreqlJavaCheckerResult(backendResult, (JavaSubmission) Hibernate.unproxy(job.getStageSubmission()),
					job.getSubmission(), job.getCheckerConfiguration());
		} else if (TRACINGJAVACHECKER.equals(checkerType)) {
			handleTracingJavaCheckerResult(backendResult, (JavaSubmission) Hibernate.unproxy(job.getStageSubmission()),
					job.getSubmission(), job.getCheckerConfiguration());
		} else if (METRICSJAVACHECKER.equals(checkerType)) {
			handleMetricsJavaCheckerResult(backendResult, (JavaSubmission) Hibernate.unproxy(job.getStageSubmission()),
					job.getSubmission(), job.getCheckerConfiguration());
		} else {
			throw new UnsupportedOperationException(
					"Unsupported checker type: '" + checkerType + "' for " + getClass().getSimpleName());
		}
	}

	private void handleGreqlJavaCheckerResult(BackendResult backendResult, JavaSubmission javaSubmission,
			Submission submission, CheckerConfiguration config) {
		int jobResult = backendResult.getResult();

		Result result = new Result(javaSubmission);
		result.setPoints(jobResult);
		result.setCheckerConfiguration(config);
		result.setCheckerLog(backendResult.getBackendLog());
		result.setFromKafkaTopic(GREQLJAVACHECKER);
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
				javaSubmission.addReportValue(graderFeedbackMessage.getTitle(), graderFeedbackMessage.getContent());
			}
		}

		result.setPublicComment(publicComment);

		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, javaSubmission, result);
	}

	private void handleTracingJavaCheckerResult(BackendResult backendResult, JavaSubmission javaSubmission,
			Submission submission, CheckerConfiguration config) {
		int jobResult = backendResult.getResult();

		Result result = new Result(javaSubmission);
		result.setPoints(jobResult);
		result.setCheckerConfiguration(config);
		result.setCheckerLog(backendResult.getBackendLog());
		result.setFromKafkaTopic(TRACINGJAVACHECKER);
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
				javaSubmission.addReportValue(graderFeedbackMessage.getTitle(), graderFeedbackMessage.getContent());
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

		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, javaSubmission, result);
	}

	private void handleMetricsJavaCheckerResult(BackendResult backendResult, JavaSubmission javaSubmission,
			Submission submission, CheckerConfiguration config) {
		for (Feedback graderFeedbackMessage : backendResult.getFeedbackList()) {
			String category = graderFeedbackMessage.getCategory();
			if ("reportValue".equals(category)) {
				javaSubmission.addReportValue(graderFeedbackMessage.getTitle(), graderFeedbackMessage.getContent());
			}
		}
		baseService.merge(javaSubmission);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stageSubmission, Submission submission) {
		assureCorrectClassUsage(JavaSubmission.class, stageSubmission);
		JavaSubmission javaSubmission = (JavaSubmission) stageSubmission;

		JavaStage javaStage = baseService.findById(JavaStage.class, javaSubmission.getStageId(), false)
				.orElseThrow(VerifyException::new);

		long numberOfPendingJobs = jobService.countPendingJobsForStageSubmission(javaSubmission);
		int numberOfErrorResults = 0;

		// Propagate pending checks and result points
		if (numberOfPendingJobs > 0) {
			javaSubmission.setHasPendingChecks(true);
		} else {
			javaSubmission.setHasPendingChecks(false);
			double points = 0;
			double weights = 0;

			Set<Result> existingResults = javaSubmission.getResults();
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
				javaSubmission.setPoints((int) Math.round(points / weights));
			}
		}

		// Propagate internal errors
		if (javaStage.isPropagateInternalErrors()) {
			javaSubmission.setHasInternalErrors(numberOfErrorResults > 0);
		}

		return stageSubmissionService.mergeStageSubmission(javaSubmission);
	}

}
