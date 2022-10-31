package de.uni_due.s3.jack3.business.stagetypes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Hibernate;

import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult.Feedback;
import de.uni_due.s3.jack.dto.generated.GreqlUmlCheckerJobData.GreqlUmlTransport;
import de.uni_due.s3.jack.dto.generated.JobMetaInformation.JobMetaInfo;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.messaging.MessageBusiness;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlStage;
import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlSubmission;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class UmlStageBusiness extends AbstractStageBusiness implements Serializable {

	private static final long serialVersionUID = 174242065601670658L;

	private static final String GREQLUMLCHECKER = "de.uni_due.s3.jack.backend.greqlumlchecker";

	@Inject
	private SubmissionService submissionService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ResultService resultService;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private MessageBusiness messageBusiness;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	@ConfigProperty(name = "mp.messaging.incoming.checker-results.topic")
	private String checkerResultsTopic;

	@Override
	public StageSubmission prepareSubmission(Submission submission, Stage stage, StageSubmission stagesubmission) {
		return stagesubmission;
	}

	@Override
	public StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps) {
		assureCorrectClassUsage(UmlSubmission.class, stagesubmission);
		assureCorrectClassUsage(UmlStage.class, stage);

		UmlSubmission umlSubmission = (UmlSubmission) stagesubmission;
		UmlStage umlStage = (UmlStage) stage;

		if (!umlSubmission.getUploadedFiles().isEmpty()) {
			boolean jobCreated = createAndSendGreqlJob(submission, umlSubmission, umlStage);
			umlSubmission.setHasPendingChecks(jobCreated);
		} else if (umlStage.isRepeatOnMissingUpload()) {
			exercisePlayerBusiness.handleRepeatTransition(submission, umlStage, umlSubmission);
			return umlSubmission;
		} else {
			Result result = new Result(umlSubmission);
			result.setPoints(0);
			// TODO: Add public comment
			resultService.persistResult(result);
			umlSubmission = (UmlSubmission) exercisePlayerBusiness.addResultToSubmission(submission, umlSubmission, stage,
					result);
		}

		// REVIEW: The following lines are quite similar to those from RStageBusiness
		baseService.merge(umlSubmission);
		submission = submissionService.mergeSubmission(submission);
		baseService.flushEntityManager();

		exercisePlayerBusiness.expandSubmissionAfterSubmit(submission, stage, umlSubmission);
		baseService.flushEntityManager();

		return umlSubmission;
	}

	private boolean createAndSendGreqlJob(Submission submission, UmlSubmission umlSubmission, UmlStage umlStage) {

		Job job = new Job(submission, umlSubmission, "UmlStage", GREQLUMLCHECKER);
		baseService.persist(job);

		byte[] xmiContents = Iterables.getOnlyElement(umlSubmission.getUploadedFiles()).getContent();
		String rules = exercisePlayerBusiness.resolvePlaceholders(umlStage.getGreqlRules(), submission, umlSubmission,
				umlStage, false);

		GreqlUmlTransport transport = GreqlUmlTransport.newBuilder() //
				.setRuleFileContents(rules) //
				.setXmiFileContents(new String(xmiContents)) //
				.setJobMetaInfo( //
						JobMetaInfo.newBuilder() //
								.setResultTopic(checkerResultsTopic) //
								.setJobId(job.getId()) //
								.setLocale(submission.getExercise().getLanguage())) //
				.build();

		getLogger().info("Sending Job to Topic '" + job.getKafkaTopic() + "'");

		messageBusiness.sendSerializedDtoToKafka(transport.toByteArray(), GREQLUMLCHECKER);

		job.setStarted();
		baseService.merge(job);

		return true;
	}

	@Override
	public boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps)
			throws InternalErrorEvaluatorException, CalculatorException {
		assureCorrectClassUsage(UmlSubmission.class, stagesubmission);
		return evaluatorBusiness.evaluateStageTransitionExpression(transition, evaluatorMaps);
	}

	@Override
	public Map<String, VariableValue> getInputVariables(StageSubmission stagesubmission) {
		assureCorrectClassUsage(UmlSubmission.class, stagesubmission);
		UmlSubmission umlSubmission = (UmlSubmission) stagesubmission;

		Map<String, VariableValue> inputs = new HashMap<>();

		for (Entry<String, String> reportValue : umlSubmission.getReportValues().entrySet()) {
			inputs.put(reportValue.getKey(),
					VariableValueFactory.createVariableValueForOpenMathString(reportValue.getValue()));
		}
		return inputs;
	}

	@Override
	public Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission) {
		return new HashMap<>();
	}

	@Override
	public void handleAsyncCheckerResult(BackendResult backendResult, Job job) {
		String checkerType = backendResult.getJobMetaInfo().getCheckerType();
		if (!GREQLUMLCHECKER.equals(checkerType)) {
			throw new UnsupportedOperationException(
					"Unsupported checker type: '" + checkerType + "' for " + getClass().getSimpleName());
		}
		handleGreqlUmlCheckerResult(backendResult, (UmlSubmission) Hibernate.unproxy(job.getStageSubmission()),
				job.getSubmission());
	}

	private void handleGreqlUmlCheckerResult(BackendResult backendResult, UmlSubmission umlSubmission,
			Submission submission) {
		int jobResult = backendResult.getResult();

		Result result = new Result(umlSubmission);
		result.setPoints(jobResult);
		result.setCheckerLog(backendResult.getBackendLog());
		result.setErrorResult(!backendResult.getJobMetaInfo().getJobSuccessful());

		StringBuilder feedback = new StringBuilder();
		String feedbackPrefix = "";
		for (Feedback feedbackMessage : backendResult.getFeedbackList()) {
			String category = feedbackMessage.getCategory();
			if ("generalInformation".equals(category)) {
				feedbackPrefix = "<h4>" + feedbackMessage.getTitle() + "<h4><p>" + feedbackMessage.getContent()
						+ "</p>";
			} else if ("internalError".equals(category)) {
				feedbackPrefix = "<p><i>" + feedbackMessage.getTitle() + "</i>: " + feedbackMessage.getContent()
						+ "</p>";
			} else if ("errorMessage".equals(category)) {
				feedback.append("<p>" + feedbackMessage.getTitle() + ": " + feedbackMessage.getContent() + "</p>");
			} else if ("reportValue".equals(category)) {
				umlSubmission.addReportValue(feedbackMessage.getTitle(), feedbackMessage.getContent());
			}
		}

		String feedbackString = feedback.toString().strip();

		if (feedbackPrefix.isEmpty() || feedbackString.isEmpty()) {
			result.setPublicComment(feedbackPrefix + feedbackString);
		} else {
			result.setPublicComment(feedbackPrefix + "<h4>Detaillierte Meldungen</h4>" + feedbackString);

		}

		resultService.persistResult(result);
		exercisePlayerBusiness.persistAndAddAsyncResultToSubmission(submission, umlSubmission, result);
	}

	@Override
	public StageSubmission updateStatus(StageSubmission stageSubmission, Submission submission) {
		assureCorrectClassUsage(UmlSubmission.class, stageSubmission);
		UmlSubmission umlSubmission = (UmlSubmission) stageSubmission;

		UmlStage umlStage = baseService.findById(UmlStage.class, umlSubmission.getStageId(), false)
				.orElseThrow(VerifyException::new);

		Optional<Result> mayBeResult = umlSubmission.getResults().stream().findFirst();

		if (mayBeResult.isPresent()) {
			umlSubmission.setHasPendingChecks(false);
			Result result = mayBeResult.get();
			umlSubmission.setPoints(result.getPoints());
			if (umlStage.isPropagateInternalErrors()) {
				umlSubmission.setHasInternalErrors(result.isErrorResult());
			}
		} else {
			umlSubmission.setHasPendingChecks(true);
			umlSubmission.setHasInternalErrors(false);
		}

		return stageSubmissionService.mergeStageSubmission(umlSubmission);
	}


}
