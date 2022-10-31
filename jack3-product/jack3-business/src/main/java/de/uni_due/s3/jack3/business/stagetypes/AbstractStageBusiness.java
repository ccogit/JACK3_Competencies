package de.uni_due.s3.jack3.business.stagetypes;

import java.util.Map;

import javax.inject.Inject;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.services.BaseService;

public abstract class AbstractStageBusiness extends AbstractBusiness {

	@Inject
	protected BaseService baseService;

	/**
	 * This method can be called after a new plain stage submission is created and before it is displayed to the user.
	 * This method may perform stage type specific preparations such as shuffling answer options.
	 *
	 * @param submission
	 *            The exercise submission which contains the stage submission
	 * @param stage
	 *            The stage the new submission belongs to
	 * @param stagesubmission
	 *            The new stage submission
	 * @return The (altered or unchanged) stage submission
	 */
	public abstract StageSubmission prepareSubmission(Submission submission, Stage stage,
			StageSubmission stagesubmission);

	/**
	 * This method can be called after a stage submission has been filled with
	 * user input. This method may perform any operation that triggers a grading
	 * process for that submission. The method is not required to actually
	 * append any result to the submission, as this may happen later in case of
	 * asynchronous grading.
	 *
	 * @param submission
	 *            The exercise submission which contains the stage submission
	 * @param stage
	 *            The stage the stage submission belongs to
	 * @param stagesubmission
	 *            The stage submission to be graded
	 * @param evaluatorMaps
	 *            The meta object containing variable values
	 * @return The (altered or unchanged) stage submission
	 */
	public abstract StageSubmission startGrading(Submission submission, Stage stage, StageSubmission stagesubmission,
			EvaluatorMaps evaluatorMaps);

	/**
	 * This method can be called at any time. It must perform any operation that
	 * is necessary to determine the overall status of a stage submission, i.e.
	 * whether or not is has pending checks, internal errors and similar. The
	 * method must store the updated entity in the database and return it.
	 *
	 * @param stagesubmission
	 *            The stagesubmission to be updated
	 * @param submission
	 *            The submission, if we need to add (failure-) SubmissionLog entries
	 * @return The (altered or unchanged) stage submission
	 */
	public abstract StageSubmission updateStatus(StageSubmission stagesubmission, Submission submission);

	/**
	 * This method can be called to find out whether the given submission triggers the given transition. The method must
	 * not change the submission or transition entity.
	 *
	 * @param submission
	 *            The exercise submission which contains the stage submission
	 * @param stage
	 *            The stage the stage submission belongs to
	 * @param stagesubmission
	 *            The stage submission that may or may not trigger the transition
	 * @param transition
	 *            The transition to be checked
	 * @param evaluatorMaps
	 *            The meta object containing variable values
	 * @return True if the submission triggers the transition, false otherwise
	 */
	public abstract boolean evaluateTransition(Submission submission, Stage stage, StageSubmission stagesubmission,
			StageTransition transition, EvaluatorMaps evaluatorMaps)
			throws InternalErrorEvaluatorException, CalculatorException;

	/**
	 * This method can be called to retrieve a set of variables representing the user input from the given submission.
	 * The map keys are the names of the input fields or similar references depending on the actual stage type. The map
	 * may be empty, but must not be null.
	 *
	 * @param stagesubmisson
	 *            The stage submission that is asked for user input
	 * @return A (possibly empty) map of user inputs
	 */
	public abstract Map<String, VariableValue> getInputVariables(StageSubmission stagesubmisson);

	/**
	 * This method can be called to retrieve a set of meta variables about the given submission. A suitable naming
	 * schema (e.g. using a stage specific prefix) should be used for the keys of the map. The map may be empty, but
	 * must not be null.
	 *
	 * @param stagesubmission
	 *            The stage submission that is asked for meta variables
	 * @return A (possibly empty) map of user inputs
	 */
	public abstract Map<String, VariableValue> getMetaVariables(StageSubmission stagesubmission);

	/**
	 * If you need to execute initialization code when creating your stage in exerciseEditView your business may
	 * overwrite this method to do the initialization.
	 *
	 * @param stage
	 *            The stage to do initialization code for
	 */
	public void setViewDefaults(Stage stage) {
		// Overwrite this if you need to do initializing when creating your stage in exerciseEditView!
	}

	/**
	 * If your stage should handle evaluating code through a live console you have to overwrite this method. Then you
	 * usually need to do the following:
	 * <ul>
	 * <li>Create a service that listens on MY_KAFKA_TOPIC and sends results back to the topic set in the config of:
	 * TenantConfigSource.getValue(TenantConfigSource.CHECKER_TOPIC_KEY)
	 * using the existing DTOS ConsoleEvaluationRequest and ConsoleEvaluationResponse (see: <a
	 * href="https://s3gitlab.paluno.uni-due.de/JACK/dto">https://s3gitlab.paluno.uni-due.de/JACK/dto</a>)</li>
	 * <li>Create and persist a new ConsoleResult entity in the testCode method below with the available
	 * information already set (see {@link RStageBusiness#testCode(StageSubmission, String)}) and use the id of that new
	 * entity as the id of the ConsoleEvaluationRequest id.
	 * </li>
	 * <li>Overwrite {@link #hasCodeTestCapability()} to return true (if the kafka topic above is also set)</li>
	 * <li>Optionally overwrite {@link #formatConsoleResponse(String)}</li>
	 * </ul>
	 *
	 * @param stageSubmission
	 * @param initiatingUserString
	 * @return
	 */
	public long testCode(StageSubmission stageSubmission, String initiatingUserString) {
		throw new UnsupportedOperationException(
				"Business " + getClass().getSimpleName() + " can not (yet?) handle testing code." + stageSubmission);
	}

	/**
	 * If you overwrite this in your business class and return true here, a "Test code" button and console output
	 * element gets rendered in the exercise player view for your stage. In this case you also need to overwrite
	 * {@link #testCode(StageSubmission, String)} to evaluate the given code.
	 *
	 * @return true, if your stage can handle testing code in a live console and a console result topic is configured,
	 *         false otherwise
	 */
	public boolean hasCodeTestCapability() {
		return false;
	}

	/**
	 * If the response of your console service needs some formatting before it will be shown to the user, you can do
	 * that here. It might be better to not let your service do the formatting so the response can be saved exactly as
	 * it was obtained by the service (to allow for later correction).
	 *
	 * @param consoleResponse
	 *            The unformatted output of the service exactly as it was obtained.
	 * @return The formatted output that will be shown to the user (e.g. the R formatter will cut all lines following
	 *         the line "$visible" amongst other changes)
	 */
	public String formatConsoleResponse(String consoleResponse) {
		return consoleResponse;
	}

	/**
	 * If your stage has to handle async checker results, your stageBusiness must override this method. To make this
	 * work, you need to set the basename of your stage business as "HandlerBusiness" in backendResult.getJobMetaInfo()
	 * in your checker service (without the "Business"-suffix, e.g. "RStage").
	 *
	 * If you need to handle different types of checks (e.g. static / dynamic), you can then differentiate that in your
	 * business according to backendResult.getJobMetaInfo().getCheckerType()
	 *
	 * After your done calculating feedback you have to call
	 * {@link ExercisePlayerBusiness#persistAndAddAsyncResultToSubmission(Submission, StageSubmission,
	 * de.uni_due.s3.jack3.entities.tenant.Result)}
	 *
	 * @param backendResult
	 *            ProtocolBuffer Object received from the checker-service
	 * @param job
	 *            The grading job this result belongs to
	 */
	public void handleAsyncCheckerResult(BackendResult backendResult, Job job) {
		throw new UnsupportedOperationException(
				"Business " + getClass().getSimpleName() + " can not (yet?) handle async results " + backendResult);
	}
}
