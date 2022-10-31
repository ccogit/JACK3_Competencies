package de.uni_due.s3.jack3.business;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.Hibernate;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.CalculatorException;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.stagetypes.AbstractStageBusiness;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlStage;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.JSXGraph;
import de.uni_due.s3.jack3.entities.tenant.ManualResult;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableUpdate;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.ResultService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionLogEntryService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.services.utils.ClassFinder;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class ExercisePlayerBusiness extends AbstractBusiness {

	/**
	 * Businesses {@link ApplicationScoped}
	 */
	@Inject
	private CoursePlayerBusiness coursePlayerBusiness;

	@Inject
	private CalculatorBusiness calculatorBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	/**
	 * Services {@link Stateless}
	 */
	@Inject
	private ResultService resultService;

	@Inject
	private BaseService baseService;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private SubmissionLogEntryService submissionLogEntryService;

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private RevisionService revisionService;

	private static List<Class<? extends Stage>> registeredStageTypes;

	static {
		registeredStageTypes = ClassFinder.find("de.uni_due.s3.jack3.entities.stagetypes", Stage.class);

		// Fallback implementation for cases in which ClassFinder.find will
		// fail due to read restrictions on the underlying file system.
		if (registeredStageTypes.isEmpty()) {
			registeredStageTypes.add(MCStage.class);
			registeredStageTypes.add(FillInStage.class);
			registeredStageTypes.add(RStage.class);
			registeredStageTypes.add(UmlStage.class);
			registeredStageTypes.add(JavaStage.class);
			registeredStageTypes.add(PythonStage.class);
			registeredStageTypes.add(MoleculeStage.class);
			// More stage types can be added here
		}
		registeredStageTypes = Collections.unmodifiableList(registeredStageTypes);
	}

	/**
	 * Adds the given result to the given submission and updates the submission log of the given submission accordingly.
	 * Saves both updates to database and returns the updated submission.
	 *
	 * @param submission
	 * @param stageSubmission
	 * @param result
	 */
	public void persistAndAddAsyncResultToSubmission(Submission submission, StageSubmission stageSubmission,
			Result result) {
		baseService.persist(result);
		submission = baseService.merge(submission);
		stageSubmission = baseService.merge(stageSubmission);

		// Hibernate proxy has no session here, re-getting
		stageSubmission = baseService.findById(StageSubmission.class, stageSubmission.getId(), false).orElseThrow();
		Stage stage = baseService.findById(Stage.class, stageSubmission.getStageId(), false).orElseThrow();

		addResultToSubmission(submission, stageSubmission, stage, result);
	}

	public Submission updateTotalResult(Submission submission) {
		// Store current result points
		final int oldResult = submission.getResultPoints();

		// Generate list of last submission per stage
		final List<StageSubmission> submissionPath = generateSubmissionPath(submission);
		long lastStageIdInPath = 0;
		if (!submissionPath.isEmpty()) {
			lastStageIdInPath = submissionPath.get(submissionPath.size() - 1).getStageId();
		}

		// We first propagate internal errors and pending checks from submissions to the submission
		propagateInternalErrorsAndPendingChecks(submissionPath, submission);

		// We only do more updates if there are no errors or pending checks
		if (!submission.hasInternalErrors() && !submission.hasPendingStageChecks()) {
			calculatedWeigthsAndPoints(submission, submissionPath, lastStageIdInPath);
		}

		// We save the submission in any case
		submission = submissionService.mergeSubmission(submission);

		// If points have changed and we are not just testing the exercise, we update the course record
		if ((submission.getCourseRecord() != null) && (submission.getResultPoints() != oldResult)) {
			coursePlayerBusiness.updateCourseResult(submission.getCourseRecord());
		}

		return submission;
	}

	private void calculatedWeigthsAndPoints(Submission submission, final List<StageSubmission> submissionPath,
			long lastStageIdInPath) {
		AbstractExercise exercise = submission.getExercise();

		// In case of asyncronous results we'd get a LazyInitializationException here, regetting from database...
		exercise = baseService.findById(AbstractExercise.class, exercise.getId(), false).orElseThrow();

		// Create temp storage for stage weights
		Map<Long, Integer> stageWeights = new HashMap<>();
		for (Stage stage : exercise.getStages()) {
			stageWeights.put(stage.getId(), stage.getWeight());
		}

		// Calculate total weighted points
		double sumOfPointsAllStages = 0.0;
		double sumOfWeightsAllStages = 0.0;
		for (StageSubmission stageSubmission : submissionPath) {
			stageSubmission = stageSubmissionService.getStageSubmissionWithLazyData(stageSubmission.getId())
					.orElseThrow(NoSuchJackEntityException::new);

			int points = getManualOrAutomaticPoints(stageSubmission, exercise);
			Integer stageWeight = stageWeights.get(stageSubmission.getStageId());

			if (stageWeight == null) {
				throw new VerifyException(
						"Stage ID: " + stageSubmission.getStageId() + " not found in weights map" + stageWeights);
			}

			sumOfPointsAllStages += points * stageWeight;
			sumOfWeightsAllStages += stageWeight;
		}

		// Calculate remaining path weights (but only if submission is not yet completed - see #1002)
		int remainingWeights = 0;
		if (!submission.isCompleted()) {
			Map<Long, Integer> weightsMap = exercise.getSuffixWeights();
			if (weightsMap.containsKey(lastStageIdInPath)) {
				// We need that check, because stages may have no suffix at all if an exercise is circular (see #563)
				int relevantSuffixWeights = weightsMap.get(lastStageIdInPath);
				remainingWeights = relevantSuffixWeights - stageWeights.get(lastStageIdInPath);
			}
		}

		// Calculate and set result
		double totalWeights = sumOfWeightsAllStages + remainingWeights;
		if (totalWeights > 0) {
			submission.setResultPoints((int) Math.round((sumOfPointsAllStages / totalWeights)));
		} else {
			submission.setResultPoints(100);
		}
	}

	/**
	 * Iterates over submissions and looks if hasInternalErrors() or hasPendingChecks() returns true on one of them. If
	 * so, the corresponding flag is also set for the submission.
	 *
	 * @param stagesubmissions
	 * @param submission
	 */
	private void propagateInternalErrorsAndPendingChecks(final Collection<StageSubmission> stagesubmissions,
			Submission submission) {
		submission.setHasInternalErrors(false);
		submission.setHasPendingStageChecks(false);

		for (StageSubmission stageSubmission : stagesubmissions) {
			if (stageSubmission.hasInternalErrors()) {
				submission.setHasInternalErrors(true);
			}

			Stage stage = baseService.findById(Stage.class, stageSubmission.getStageId(), false)
					.orElseThrow(() -> new VerifyException(
							"Cannot find Stage Id refenrenced from stageSubmission in Database: " + stageSubmission));
			if (stageSubmission.hasPendingChecks() && stage.mustWaitForPendingJobs()) {
				submission.setHasPendingStageChecks(true);
			}
		}
	}

	/**
	 * Returns the score of a stage submission taking into account deductions for hints and a possible manual result. If
	 * the stage submission has a manual result, it returns the manually given points, otherwise the points from the
	 * automatic checks with possible deductions for hints.
	 */
	public int getManualOrAutomaticPoints(StageSubmission stageSubmission, AbstractExercise exercise) {
		final Optional<ManualResult> manualResult = stageSubmission.getManualResult();
		if (manualResult.isPresent()) {
			return manualResult.get().getPoints();
		}
		return getPointsWithHintMalus(stageSubmission, exercise);
	}

	/**
	 * Iterates the submission log of the given submission and constructs a sequence of stage submissions that are
	 * relevant for grading. In particular, all stage submissions that are overwritten due to repeated stages or due to
	 * using of the eraser are ignored in this sequence.
	 */
	public List<StageSubmission> generateSubmissionPath(Submission submission) {
		List<StageSubmission> path = new LinkedList<>();
		boolean replace = false;

		final List<SubmissionLogEntry> submissionLog = submission.getSubmissionLogAsSortedList();
		for (final SubmissionLogEntry currentSubmissionLogEntry : submissionLog) {
			if (currentSubmissionLogEntry.getType() == ESubmissionLogEntryType.ENTER) {
				if (replace) {
					path.remove(path.size() - 1);
				}
				path.add(currentSubmissionLogEntry.getSubmission());
				replace = true;
			}
			if ((currentSubmissionLogEntry.getType() == ESubmissionLogEntryType.SUBMIT)
					|| (currentSubmissionLogEntry.getType() == ESubmissionLogEntryType.SKIP)) {
				if (replace) {
					path.remove(path.size() - 1);
					replace = false;
				}
				path.add(currentSubmissionLogEntry.getSubmission());
			}
			if (currentSubmissionLogEntry.getType() == ESubmissionLogEntryType.REPEAT) {
				replace = true;
			}
			if (currentSubmissionLogEntry.getType() == ESubmissionLogEntryType.ERASE) {
				StageSubmission erasedSubmission = currentSubmissionLogEntry.getSubmission();
				path = path.subList(0, path.indexOf(erasedSubmission));
				replace = false;
			}
		}

		return path;
	}

	/**
	 * Adds the given failure message to the given submission and updates the submission log of the given submission
	 * accordingly. Saves both updates to database and returns the updated submission.
	 *
	 * @param submission
	 * @param stageSubmission
	 * @return
	 */
	public StageSubmission addFailureToSubmission(Submission submission, StageSubmission stageSubmission,
			String failureMessage, Throwable cause) {
		if (cause != null) {
			failureMessage += " This was caused by an exception: " + cause.getMessage();
		}

		// Create log entry for failure
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmissionAndText(ESubmissionLogEntryType.FAIL, stageSubmission,
						failureMessage);
		submission.addSubmissionLogEntry(logEntry);
		submission = submissionService.mergeSubmission(submission);

		// Set submission to error state
		stageSubmission.setHasInternalErrors(true);
		stageSubmission = stageSubmissionService.mergeStageSubmission(stageSubmission);

		// Update submission to propagate error state
		updateTotalResult(submission);

		return stageSubmission;
	}

	public void doVariableUpdates(Submission submission, StageSubmission stageSubmission, Stage stage,
			List<VariableUpdate> updates) {
		// If the list of updates is empty, there is nothing to do
		if (updates.isEmpty()) {
			return;
		}

		try {
			final EvaluatorMaps evaluatorMaps = prepareEvaluatorMaps(submission, stageSubmission, stage, false);
			Map<String, VariableValue> evaluatedVariables = calculatorBusiness.variableUpdate(updates, evaluatorMaps);
			// TODO: Sort log entries in the same order as variables were evaluated
			for (Entry<String, VariableValue> variable : evaluatedVariables.entrySet()) {
				// Store the new value
				baseService.persist(variable.getValue());
				submission.addVariableValue(variable.getKey(), variable.getValue());
				// If we are in an active submission, we also log the update
				if (stageSubmission != null) {
					String updateText = variable.getKey() + " = " + variable.getValue().getContent();
					updateText = updateText.replace(
							"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\">",
							"");
					updateText = updateText.replace("</oo:OpenObject>", "");
					SubmissionLogEntry varLogEntry = submissionLogEntryService
							.persistSubmissionLogEntryWithStageSubmissionAndText(ESubmissionLogEntryType.VAR_UPDATE,
									stageSubmission, updateText);
					submission.addSubmissionLogEntry(varLogEntry);
				}
			}
		} catch (InternalErrorEvaluatorException e) {
			// TODO: Can we also display log entries for successful updates before?
			if (stageSubmission != null) {
				// Logging if we are in an active submission
				String error = "Cannot evaluate update code for variables in stage with id "
						+ stageSubmission.getStageId() + ". This will also be logged in the submission protocol.";
				getLogger().warn(error);
				SubmissionLogEntry varLogEntry = submissionLogEntryService
						.persistSubmissionLogEntryWithStageSubmissionAndText(ESubmissionLogEntryType.FAIL,
								stageSubmission, "Update failed for variables with message '" + e.getMessage() + "'");
				submission.addSubmissionLogEntry(varLogEntry);
			} else {
				String error = "Cannot evaluate update code for variables in stage with id " + stage.getId()
				+ ". There is no active stage submission and thus this warning will not be logged in the submission protocol.";
				getLogger().warn(error);
			}
			submission.setHasInternalErrors(true);
		}
	}

	/**
	 * Collects exercise variables, input variables and meta variables for the given submission and submission into a
	 * map
	 * container suitable to be passed to the evaluator.
	 *
	 * @param submission
	 *            The submission from which exercise variables are read (depending on the boolean parameter) and for
	 *            which
	 *            meta variables are generated. Must not be null.
	 * @param stageSubmission
	 *            The submission from which exercise variables are read (depending on the boolean parameter), from which
	 *            input variables are read, and for which additional meta variables are generated. May be null.
	 * @param stage
	 *            The stage belonging to the submission. Must not be null if a submission is provided.
	 * @param useExerciseVariablesFromStageSubmission
	 *            Defines whether exercise variables are read from the stage submission (never change during the
	 *            lifecycle of
	 *            a submission) or from the exercise submission (change with every update).
	 * @return An properly initialized instance of {@link EvaluatorMaps}
	 */
	public EvaluatorMaps prepareEvaluatorMaps(Submission submission, StageSubmission stageSubmission, Stage stage,
			boolean useExerciseVariablesFromStageSubmission) {
		final EvaluatorMaps evaluatorMaps = new EvaluatorMaps();

		if ((stageSubmission != null) && (stage != null)) {
			// We use a stage business instance to read input variables and meta variables from the submission and add
			// them to the map.
			AbstractStageBusiness stageBusiness = getStageBusiness(stage);
			evaluatorMaps.setInputVariableMap(stageBusiness.getInputVariables(stageSubmission));
			evaluatorMaps.setMetaVariableMap(stageBusiness.getMetaVariables(stageSubmission));

			if (useExerciseVariablesFromStageSubmission) {
				// If requested, we read exercise variables from the stage submission
				// We clone existing values to avoid confusion between old and new values during update processes (see
				// doVariableAssignment()).
				Map<String, VariableValue> clonedExerciseVarMap = new HashMap<>(stageSubmission.getVariableValues());
				evaluatorMaps.setExerciseVariableMap(clonedExerciseVarMap);
			}
		}

		if (!useExerciseVariablesFromStageSubmission) {
			// We read the variables from the exercise submission.
			// We clone existing values to avoid confusion between old and new values during update processes.
			Map<String, VariableValue> clonedExerciseVarMap = new HashMap<>(submission.getVariableValues());
			evaluatorMaps.setExerciseVariableMap(clonedExerciseVarMap);
		}

		// If possible, we read additional information from the submission and add it to the meta variables
		if (stageSubmission != null) {
			evaluatorMaps.addMetaVariable("stageHints", stageSubmission.getGivenHintCount());
			evaluatorMaps.addMetaVariable("stageCurrentResult",
					getPointsWithHintMalus(stageSubmission, submission.getExercise()));
			evaluatorMaps.addMetaVariable("stageCurrentAttempt", stageSubmission.getAttemptCount());
		}

		// TODO ms: add more meta variables here

		return evaluatorMaps;
	}

	public AbstractStageBusiness getStageBusiness(Stage stage) {
		stage = (Stage) Hibernate.unproxy(stage);
		return getStageBusiness(stage.getClass().getSimpleName());
	}

	public AbstractStageBusiness getStageBusiness(String stageName) {
		final String stageBusinessName = "de.uni_due.s3.jack3.business.stagetypes." + stageName + "Business";

		// Load stage specific business bean
		try {
			final Class<?> stageBusinessClass = getClass().getClassLoader().loadClass(stageBusinessName);
			return (AbstractStageBusiness) (CDI.current().select(stageBusinessClass).get());
		} catch (ClassNotFoundException e) {
			throw new VerifyException("Could not load class '" + stageBusinessName + "'", e);
		}
	}

	/**
	 * Utility method to replace as much placeholders (i. e. variable references, graph tags) as possible that occur in
	 * a text within an exercise (e. g. task description, feedback, hints, ...). Placeholders that cannot be replaced
	 * (e. g. because they refer to a variable that does not exists) remain unchanged.
	 *
	 * @param text
	 *            The text that may contain placeholders.
	 * @param submission
	 *            The current submission the user is working on while seeing the text.
	 * @param stageSubmission
	 *            The current stage submission the user is working on while seeing the text.
	 * @param stage
	 *            The stage belonging to the current stage submission.
	 * @param useExerciseVariableFromSubmission
	 *            If set to {@code true}, variable values are retrieved from the submission (i. e. updates within the
	 *            stage are <u>not</u> considered). If set to {@code false}, variable values are retrieved from the
	 *            submission (i. e. the most current values are used).
	 * @return A string in which as much placeholders as possible are replaced.
	 */
	public String resolvePlaceholders(String text, Submission submission, StageSubmission stageSubmission, Stage stage,
			boolean useExerciseVariableFromSubmission) {
		// Preparations: We load the correct version of the exercise
		AbstractExercise exerciseWithoutLazyData = submission.getExercise();
		AbstractExercise exercise = null;

		if (exerciseWithoutLazyData.isFrozen()) {
			// Frozen exercises have a different query
			exercise = exerciseService.getFrozenExerciseWithLazyDataById(exerciseWithoutLazyData.getId())
					.orElseThrow(NoSuchJackEntityException::new);
		} else {
			// For normal exercises, we must first retrieve the current revision
			exercise = exerciseService.getExerciseByIdWithLazyData(exerciseWithoutLazyData.getId())
					.orElseThrow(NoSuchJackEntityException::new);
			if (revisionService.getProxiedOrLastPersistedRevisionId(exercise) != submission
					.getShownExerciseRevisionId()) {
				// Then we can check whether we actually need an older one
				exercise = exerciseService.getRevisionOfExerciseWithLazyData(submission.getExercise(),
						submission.getShownExerciseRevisionId()).orElseThrow(NoSuchJackEntityException::new);
			}
		}

		// Step 1: We replace any markers for JSX graphs
		text = replaceJSXGraphByJSXGraphName(text, exercise.getJSXGraphs());

		// Step 2: We replace any occurrences of variables
		EvaluatorMaps evaluatorMaps = prepareEvaluatorMaps(submission, stageSubmission, stage,
				useExerciseVariableFromSubmission);
		return converterBusiness.replaceVariablesByVariableName(text, evaluatorMaps);
	}

	private String replaceJSXGraphByJSXGraphName(String inputText, Set<JSXGraph> jsxGraphs) {
		if (inputText == null) {
			return null;
		}
		if (jsxGraphs.isEmpty()) {
			return inputText;
		}

		//TODO MSch: wegen der Schreibweise noch mal nachschlagen
		Pattern jsxGraphPattern = Pattern.compile("\\[(graph)=(\\$)?[a-zA-Z]+[a-zA-Z0-9_]*\\]");

		Matcher matcher = jsxGraphPattern.matcher(inputText);
		while (matcher.find()) {
			String match = matcher.group();

			String jsxGraphName;
			String jsxGraphValue = null;

			jsxGraphName = match.substring(match.indexOf("=") + 1, match.length() - 1);
			for (JSXGraph jsx : jsxGraphs) {
				if (jsx.getName().equals(jsxGraphName)) {
					jsxGraphValue = "<div id='" + jsxGraphName + "' class='jxgbox' style='width:" + jsx.getWidth()
					+ "px; height:" + jsx.getHeight() + "px;'></div><script type='text/javascript'>"
					+ jsx.getText() + "</script>";
				}
			}
			if (jsxGraphValue != null) {
				inputText = inputText.replace(match, jsxGraphValue);
			}
		}
		return inputText;
	}

	public Submission initSubmissionForExercisePlayer(Submission submission) {
		if (submission.getSubmissionLog().isEmpty() && !submission.hasInternalErrors()) {
			// 1. load exercise or frozenExercise with lazydata
			AbstractExercise exerciseWithoutLazyData = submission.getExercise();
			AbstractExercise exercise = null;

			if (exerciseWithoutLazyData.isFrozen()) {
				exercise = exerciseService.getFrozenExerciseWithLazyDataById(exerciseWithoutLazyData.getId())
						.orElseThrow(NoSuchJackEntityException::new);
			} else {
				exercise = exerciseService.getExerciseByIdWithLazyData(exerciseWithoutLazyData.getId())
						.orElseThrow(NoSuchJackEntityException::new);
			}

			// 2. Initialize variables
			boolean variableInitializationFailed = false;

			try {
				Map<String, VariableValue> evaluatedVariables = calculatorBusiness
						.initVariables(exercise.getVariableDeclarations());
				for (Entry<String, VariableValue> entry : evaluatedVariables.entrySet()) {
					// Store the new value
					baseService.persist(entry.getValue());
					submission.addVariableValue(entry.getKey(), entry.getValue());
				}
			} catch (InternalErrorEvaluatorException e) {
				getLogger().warn("Cannot evaluate initialization code exercise " + submission.getExercise()
				+ ". This will also been logged in the submission protocol.", e);
				SubmissionLogEntry varLogEntry = submissionLogEntryService.persistSubmissionLogEntryWithText(
						ESubmissionLogEntryType.FAIL, "Initialization failed for exercise due to " + e.getMessage());
				submission.addSubmissionLogEntry(varLogEntry);
				variableInitializationFailed = true;
			}

			if (variableInitializationFailed) {
				submission.setHasInternalErrors(true);
				submission = submissionService.mergeSubmission(submission);
			} else {
				// 3. Append first (empty) stage submission for start stage
				submission = expandSubmission(submission, exercise.getStartStage(), null);
			}
		}
		return submission;
	}

	public Submission performStageSkip(Submission submission, Stage stage, StageSubmission stageSubmission)
			throws IllegalAccessException {
		// Security check
		if (!stage.getAllowSkip()) {
			throw new IllegalAccessException("Stage does not allow to skip.");
		}

		// Create log entry for skip action
		SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.SKIP, stageSubmission);
		submission.addSubmissionLogEntry(logEntry);
		submission = submissionService.mergeSubmission(submission);

		// Prepare skip message
		String skipMessage = stage.getSkipMessage();
		skipMessage = resolvePlaceholders(skipMessage, submission, stageSubmission, stage, false);

		// Use skip message as feedback
		Result result = new Result(stageSubmission);
		result.setPoints(0);
		result.setPublicComment(skipMessage);
		resultService.persistResult(result);

		// Clear old results (may exist if stage was repeated)
		stageSubmission.clearResults();

		// Add new result, set points and adjust submission status
		stageSubmission.addResult(result);
		stageSubmission.setHasInternalErrors(false);
		stageSubmission.setHasPendingChecks(false);
		stageSubmission.setPoints(result.getPoints());

		// Store changes to database
		stageSubmission = stageSubmissionService.mergeStageSubmission(stageSubmission);

		// Create log entry for check
		logEntry = submissionLogEntryService.persistSubmissionLogEntryWithResult(ESubmissionLogEntryType.CHECK, result,
				stageSubmission);
		submission.addSubmissionLogEntry(logEntry);
		submission = submissionService.mergeSubmission(submission);

		return expandSubmissionAfterSkip(submission, stage, stageSubmission);
	}

	public Submission performStageSubmit(Submission submission, Stage stage, StageSubmission stageSubmission) {
		// Merge stage submission to make sure any input is stored
		stageSubmission = stageSubmissionService.mergeStageSubmission(stageSubmission);

		// Create log entry for submit action
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.SUBMIT, stageSubmission);
		submission.addSubmissionLogEntry(logEntry);
		submission = submissionService.mergeSubmission(submission);

		// Get stage business bean
		AbstractStageBusiness stageBusiness = getStageBusiness(stage);

		// Calculate variable updates (phase "before check")
		doVariableUpdates(submission, stageSubmission, stage, stage.getVariableUpdatesBeforeCheck());

		// Save results so far
		submission = submissionService.mergeSubmission(submission);

		// Abort process (i. e. don't start grading) if variable update cause errors
		if (submission.hasInternalErrors()) {
			return submission;
		}

		// Trigger grading activity
		EvaluatorMaps evaluatorMaps = prepareEvaluatorMaps(submission, stageSubmission, stage, false);
		stageBusiness.startGrading(submission, stage, stageSubmission, evaluatorMaps);

		// Get potentially updated submission from database
		return submissionService.getSubmissionnWithLazyDataBySubmissionId(submission.getId())
				.orElseThrow(VerifyException::new);
	}

	public Submission performStageHintRequest(Submission submission, Stage stage, StageSubmission stageSubmission) {
		final int availableHints = stage.getHints().size();

		if (availableHints == 0) {
			// If no hints are available at all, we just return the unchanged
			// submission.
			return submission;
		}

		int hintsGiven = stageSubmission.getGivenHintCount();

		if (availableHints <= hintsGiven) {
			// If no more hints are available, we also just return the unchanged
			// submission.
			return submission;
		}

		// A hint is available, so we retrieve it and assign it to the submission.
		final StageHint nextHint = stage.getHints().get(hintsGiven);
		final String hintText = resolvePlaceholders(nextHint.getText(), submission, stageSubmission, stage, true);
		stageSubmission.addHint(hintText, nextHint.getMalus());
		stageSubmission = stageSubmissionService.mergeStageSubmission(stageSubmission);

		// Create log entry for hint request
		final String hintMessage = hintText + " {id=" + nextHint.getId() + "}";
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmissionAndText(ESubmissionLogEntryType.HINT, stageSubmission,
						hintMessage);
		submission.addSubmissionLogEntry(logEntry);

		// Return the updated submission that now contains the additional hint.
		return submissionService.mergeSubmission(submission);
	}

	public Submission performExit(Submission submission, StageSubmission stageSubmission) {
		// TODO: Not yet triggered by any UI action
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.EXIT, stageSubmission);
		submission.addSubmissionLogEntry(logEntry);
		return submissionService.mergeSubmission(submission);
	}

	public Submission eraseSubmission(Submission submission, Stage stage, StageSubmission stageSubmission)
			throws ActionNotAllowedException {

		// Check if the action is allowed (we need a fresh submission object from DB because the course offer's settings
		// may have changed)
		if (!baseService.findById(Submission.class, submission.getId(), false) //
				.orElseThrow(NoSuchJackEntityException::new) //
				.getCourseOffer() //
				.map(CourseOffer::isAllowStageRestart) //
				.orElse(true)) {
			if (!submission.hasInternalErrors()) {
				throw new ActionNotAllowedException();
			}
		}

		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ERASE, stageSubmission);
		submission.addSubmissionLogEntry(logEntry);

		// When the student erases something, the submission is no longer completed
		submission.setIsCompleted(false);

		// We reset the variables to the values they had at the stage to which we go back
		submission.resetVariableValues(stageSubmission.getVariableValues());

		// Since a user can only break out of a stage that has produced an internal error by clicking "eraseSubmission"
		// we know we are not in a path with a stage that produced an internal error. Fixes #590
		submission.setHasInternalErrors(false);

		// Store submission
		submission = submissionService.mergeSubmission(submission);

		// Expand submission and update result
		submission = expandSubmission(submission, stage, stageSubmission);

		return updateTotalResult(submission);
	}

	/**
	 * Returns the score of a stage submission taking into account deductions for hints.
	 */
	public int getPointsWithHintMalus(StageSubmission stageSubmission, AbstractExercise exercise) {
		if (exercise.getHintMalusType() == null) {
			return stageSubmission.getPoints();
		}

		int malusToActual = 0;
		int malusToMaximum = 0;

		switch (exercise.getHintMalusType()) {
		case CUT_ACTUAL:
			malusToActual = stageSubmission.getCumulatedHintMalus();
			break;
		case CUT_MAXIMUM:
			malusToMaximum = stageSubmission.getCumulatedHintMalus();
			break;
		default:
			throw new UnsupportedOperationException("Unsupported hint malus type: " + exercise.getHintMalusType());
		}

		int rawPoints = stageSubmission.getPoints();
		int maximum = Math.max(0, 100 - malusToMaximum);
		return Math.max(0, (int) Math.round((Math.min(maximum, rawPoints) * (100 - malusToActual)) / 100.0));
	}

	/**
	 * Sets the manual result for a stage submission and re-calculates all points (submission and course record) if
	 * necessary.
	 *
	 * ************************ Usercode must check if this action is allowed! *****************************************
	 *
	 * @param acting
	 *            Which user requests the action.
	 * @param stageSubmission
	 *            The stage submission whose manual result should be edited.
	 * @param submission
	 *            To which exercise submission the stage submission belongs.
	 * @param manualResult
	 *            The manual result to set or <code>null</code> if the manual result should be unset.
	 * @param accessPath
	 *            From which course offer the user requests the action or <code>null</code> if the user does not come
	 *            from a course offer.
	 */
	// REVIEW bo: Wenn ich die Doku zu @CheckForNull richtig verstehe, soll diese Annotation anzeigen dass der UserCode
	// diese Werte auf null überprüfen soll. Nicht nur passiert das hier nicht, sondern der Usercode will das explizit
	// auf null setzen können, um ein manuelles Result resetten zu können.
	public void updateManualResult(User acting, StageSubmission stageSubmission, Submission submission,
			@CheckForNull ManualResult manualResult, @CheckForNull CourseOffer accessPath) {

		stageSubmission = baseService.findById(StageSubmission.class, stageSubmission.getId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
		submission = submissionService.getSubmissionWithLazyDataBySubmissionIdFromEnvers(submission.getId())
				.orElseThrow(NoSuchJackEntityException::new);
		final int oldPoints = getManualOrAutomaticPoints(stageSubmission, submission.getExercise());
		stageSubmission.setManualResult(manualResult);
		stageSubmission = stageSubmissionService.mergeStageSubmission(stageSubmission);
		final int newPoints = getManualOrAutomaticPoints(stageSubmission, submission.getExercise());

		// Initiate recalculation of the points if necessary
		if (oldPoints != newPoints) {
			updateTotalResult(submission);
		}
	}

	private Submission expandSubmissionAfterSkip(Submission submission, Stage currentStage,
			StageSubmission currentStageSubmission) {
		// First step: Use default transition
		StageTransition stageTransition = currentStage.getDefaultTransition();

		// Second step: Check skip transitions if available
		boolean evaluationFailed = false;
		if (!currentStage.getSkipTransitions().isEmpty()) {
			try {
				stageTransition = findSkipTransition(submission, currentStageSubmission, currentStage, stageTransition);
			} catch (InternalErrorEvaluatorException e) {
				getLogger().warn(
						"Cannot evaluate conditional expression for skip transition in stage " + currentStage + ".", e);
				SubmissionLogEntry varLogEntry = submissionLogEntryService.persistSubmissionLogEntryWithText(
						ESubmissionLogEntryType.FAIL, "Evaluation of skip transitions failed: " + e.getMessage());
				submission.addSubmissionLogEntry(varLogEntry);
				evaluationFailed = true;
			}
		}

		if (evaluationFailed) {
			submission.setHasInternalErrors(true);
			submission = submissionService.mergeSubmission(submission);
		} else {
			// Handle transition in case it defines a stage repetition
			// Notably, this case is actually somewhat awkward. Authors are discouraged from creating exercises in which
			// skipping a stage means repeating the stage. However, that case can occur anyway.
			if (stageTransition.isRepeat()) {
				return handleRepeatTransition(submission, currentStage, currentStageSubmission);
			}

			// Handle transition in all other cases
			// Calculate variable updates (final phase for skip)
			doVariableUpdates(submission, currentStageSubmission, currentStage,
					currentStage.getVariableUpdatesOnSkip());
			submission = submissionService.mergeSubmission(submission);

			// Only continue if variable update caused no errors
			if (!submission.hasInternalErrors()) {
				submission = expandSubmission(submission, stageTransition.getTarget(), null);
			}
		}
		return submission;
	}

	public Submission expandSubmissionAfterSubmit(Submission submission, Stage currentStage,
			StageSubmission currentStageSubmission) {

		if ((currentStageSubmission.hasPendingChecks() && currentStage.mustWaitForPendingJobs())
				|| submission.hasInternalErrors() //
				|| stageIsAlreadyExpanded(currentStageSubmission, submission)) {
			// We don't need to calculate the next stage (and do variable updates) in these cases, so we just return
			return submission;
		}
		boolean evaluationFailed = false;
		StageTransition stageTransition = null;
		try {
			stageTransition = findStageTransition(submission, currentStage, currentStageSubmission);
		} catch (CalculatorException | InternalErrorEvaluatorException e) {
			getLogger().warn(
					"Cannot evaluate conditional expression for stage transition in stage " + currentStage + ".", e);
			SubmissionLogEntry varLogEntry = submissionLogEntryService.persistSubmissionLogEntryWithText(
					ESubmissionLogEntryType.FAIL, "Evaluation of stage transitions failed: " + e.getMessage());
			submission.addSubmissionLogEntry(varLogEntry);
			evaluationFailed = true;
		}

		if (evaluationFailed || (stageTransition == null)) {
			submission.setHasInternalErrors(true);
			submission = submissionService.mergeSubmission(submission);
		} else {
			// Handle transition in case it defines a stage repetition
			if (stageTransition.isRepeat()) {
				return handleRepeatTransition(submission, currentStage, currentStageSubmission);
			}

			// Handle transition in all other cases
			// Calculate variable updates (final phase for normal exit)
			doVariableUpdates(submission, currentStageSubmission, currentStage,
					currentStage.getVariableUpdatesOnNormalExit());
			submission = submissionService.mergeSubmission(submission);

			// Only continue if variable update caused no errors
			if (!submission.hasInternalErrors()) {
				submission = expandSubmission(submission, stageTransition.getTarget(), null);
			}
		}

		return submission;
	}

	public Submission handleRepeatTransition(Submission submission, Stage currentStage,
			StageSubmission currentStageSubmission) {
		// Write log entry
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.REPEAT, currentStageSubmission);
		submission.addSubmissionLogEntry(logEntry);
		submission = submissionService.mergeSubmission(submission);

		// Update variables (final phase for repeat)
		doVariableUpdates(submission, currentStageSubmission, currentStage, currentStage.getVariableUpdatesOnRepeat());
		submission = submissionService.mergeSubmission(submission);

		// Abort process if variable update cause errors
		if (submission.hasInternalErrors()) {
			return submission;
		}

		return expandSubmission(submission, currentStage, currentStageSubmission);
	}

	/**
	 * Checks if the given submission has already been expanded.
	 *
	 * @param stagesubmission
	 *            The stageSubmission referencing the stage we want to check if it is expanded
	 * @param submission
	 *            The submission to get the sorted submissionLog from
	 * @return true, if the stage referenced from the given submission has already been expanded false, otherwise
	 */
	private boolean stageIsAlreadyExpanded(StageSubmission stagesubmission, Submission submission) {
		// If the submission is completed, the submission is definitely expanded
		if (submission.isCompleted()) {
			return true;
		}

		// If the most recent ENTER event references another submission, the given submission has already been expanded.
		List<SubmissionLogEntry> enterEvents = submission.getSubmissionLogAsSortedList() //
				.stream() //
				.filter(logEntry -> ESubmissionLogEntryType.ENTER.equals(logEntry.getType())) //
				.collect(Collectors.toList());

		// We should have at least one ENTER before this is called.
		SubmissionLogEntry lastEnterLogEntry = enterEvents.get(enterEvents.size() - 1);

		return !stagesubmission.equals(lastEnterLogEntry.getSubmission());
	}

	/**
	 * Returns the relevant stageTransition for this stage, which is either the DefaultTransition or the result of
	 * {@link #findStageSpecificTransition(Submission, StageSubmission, Stage, StageTransition)}
	 *
	 *
	 * @throws CalculatorException
	 * @throws InternalErrorEvaluatorException
	 */
	public StageTransition findStageTransition(Submission submission, Stage currentStage,
			StageSubmission currentStageSubmission) throws CalculatorException, InternalErrorEvaluatorException {
		// First step: Use default transition
		StageTransition stageTransition = currentStage.getDefaultTransition();

		// Second step: Check stage specific transitions if required
		if (!currentStage.getStageTransitions().isEmpty()) {
			stageTransition = findStageSpecificTransition(submission, currentStageSubmission, currentStage,
					stageTransition);
		}
		return stageTransition;
	}

	private StageTransition findSkipTransition(Submission submission, StageSubmission stageSubmission,
			final Stage currentStage, StageTransition stageTransition) throws InternalErrorEvaluatorException {
		// If there are no skip transitions, we return the given (default) stage transition
		if (currentStage.getSkipTransitions().isEmpty()) {
			return stageTransition;
		}

		final EvaluatorMaps evaluatorMaps = prepareEvaluatorMaps(submission, stageSubmission, currentStage, false);

		for (StageTransition skipTransition : currentStage.getSkipTransitions()) {
			if (calculatorBusiness.evaluateStageTransitionCondition(skipTransition, evaluatorMaps)) {
				stageTransition = skipTransition;
				break;
			}
		}
		return stageTransition;
	}

	/**
	 * Finds out using a specific stageBusiness method if a transition in the list of stageTransitions has to be used as
	 * the current stageTransition and returns it.
	 *
	 * @throws CalculatorException
	 * @throws InternalErrorEvaluatorException
	 */
	private StageTransition findStageSpecificTransition(Submission submission, StageSubmission stageSubmission,
			final Stage currentStage, StageTransition stageTransition)
					throws CalculatorException, InternalErrorEvaluatorException {
		// If there are no stage specific transitions, we return the given (default) stage transition
		if (currentStage.getStageTransitions().isEmpty()) {
			return stageTransition;
		}

		AbstractStageBusiness stageBusiness = getStageBusiness(currentStage);

		// REVIEW: sollte hier nicht lieber ne exception fliegen?
		if (stageBusiness == null) {
			return stageTransition;
		}

		final EvaluatorMaps evaluatorMaps = prepareEvaluatorMaps(submission, stageSubmission, currentStage, false);

		for (final StageTransition otherTransition : currentStage.getStageTransitions()) {
			// This stageBusiness method finds out whether the given submission triggers the given transition.
			boolean evaluateTransition = stageBusiness.evaluateTransition(submission, currentStage, stageSubmission,
					otherTransition, evaluatorMaps);
			boolean otherConditionEmptyOrTrue = calculatorBusiness.evaluateStageTransitionCondition(otherTransition,
					evaluatorMaps);
			if (evaluateTransition && otherConditionEmptyOrTrue) {
				stageTransition = otherTransition;
				break;
			}
		}
		return stageTransition;
	}

	public Submission expandSubmission(Submission submission, Stage stage, StageSubmission copyFromStageSubmission) {
		// Handle special case of finished exercise
		if (stage == null) {
			// Mark exercise completed
			submission.setIsCompleted(true);

			// Create log entry
			final SubmissionLogEntry logEntry = submissionLogEntryService
					.persistSubmissionLogEntry(ESubmissionLogEntryType.END, 0);

			submission.addSubmissionLogEntry(logEntry);
			submission = submissionService.mergeSubmission(submission);

			// We need to trigger an update here again, because the fact the the submission is marked as completed is
			// only known since now.
			return updateTotalResult(submission);
		}
		stage = (Stage) Hibernate.unproxy(stage);

		// Find names of stage specific entities and beans
		final String stageSubmissionTypeName = stage.getClass().getName().replace("Stage", "Submission");

		// 1. Create new instance of stage submission
		StageSubmission stageSubmission = null;
		try {
			@SuppressWarnings("unchecked")
			final Class<? extends StageSubmission> submissionTypeClass = (Class<? extends StageSubmission>) this
			.getClass().getClassLoader().loadClass(stageSubmissionTypeName);
			stageSubmission = submissionTypeClass.newInstance();
			stageSubmission.setStageId(stage.getId());
			stageSubmissionService.persistStageSubmission(stageSubmission);
		} catch (final ClassNotFoundException e) {
			getLogger().error("Class '" + stageSubmissionTypeName + "' not found!", e);
			submission.setHasInternalErrors(true);
			return submissionService.mergeSubmission(submission);
		} catch (final InstantiationException | IllegalAccessException e) {
			getLogger().error("Could not create instance of stage submission type", e);
			submission.setHasInternalErrors(true);
			return submissionService.mergeSubmission(submission);
		}

		// 2. Calculate variable updates (phase "on enter")
		doVariableUpdates(submission, stageSubmission, stage, stage.getVariableUpdatesOnEnter());

		// 3. Do some general stuff to initialize stage submission (i.e.
		// copy current variable values to this submission)
		stageSubmission.addVariableValues(submission.getVariableValues());

		// 4. Perform stage specific initialization
		AbstractStageBusiness stageBusiness = getStageBusiness(stage);
		stageSubmission = stageBusiness.prepareSubmission(submission, stage, stageSubmission);

		// Return early if preparation fails
		if (submission.hasInternalErrors()) {
			return baseService.merge(submission);
		}

		// 5. Copy settings from another submission if required and set attempt counter
		if (copyFromStageSubmission != null) {
			copyFromStageSubmission = stageSubmissionService //
					.getStageSubmissionWithLazyData(copyFromStageSubmission.getId()) //
					.orElseThrow(VerifyException::new);

			stageSubmission.copyFromStageSubmission(copyFromStageSubmission);
			stageSubmission.copyHints(copyFromStageSubmission);
			stageSubmission.setAttemptCount(copyFromStageSubmission.getAttemptCount() + 1);
		} else {
			stageSubmission.setAttemptCount(1);
		}

		// 6. Add initialized stage submission to submission entity
		final SubmissionLogEntry logEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithStageSubmission(ESubmissionLogEntryType.ENTER, stageSubmission);
		submission.addSubmissionLogEntry(logEntry);

		// 7. Persist and return readily prepared submission
		return baseService.merge(submission);
	}

	/**
	 * Wether a submission is unprocessed i.e. if it does not contain a SUBMIT, SKIP or HINT event.
	 */
	public boolean isSubmissionUnprocessed(Submission submission) {
		submission = submissionService.getSubmissionnWithLazyDataBySubmissionId(submission.getId())
				.orElseThrow(NoSuchJackEntityException::new);
		return submission.getSubmissionLog().stream()
				.noneMatch(logEntry -> (logEntry.getType() == ESubmissionLogEntryType.SUBMIT)
						|| (logEntry.getType() == ESubmissionLogEntryType.SKIP)
						|| (logEntry.getType() == ESubmissionLogEntryType.HINT));
	}

	/**
	 * Returns a list of all stage types available for this tenant. The returned list is immutable.
	 *
	 * @return A list of stage types available for this tenant.
	 */
	public List<Class<? extends Stage>> getRegisteredStagetypes() {
		return registeredStageTypes;
	}

	public long testCode(Stage stageFromCache, StageSubmission currentStageSubmission, String initiatingUserString) {
		AbstractStageBusiness stageBusiness = getStageBusiness(stageFromCache);
		return stageBusiness.testCode(currentStageSubmission, initiatingUserString);
	}

	public boolean hasCodeTestCapability(Stage stageFromCache) {
		AbstractStageBusiness stageBusiness = getStageBusiness(stageFromCache);
		return stageBusiness.hasCodeTestCapability();

	}

	/**
	 * Adds the given result to the given submission and updates the submission log of the given submission accordingly.
	 * Saves both updates to database and returns the updated submission.
	 *
	 * @param submission
	 * @param stageSubmission
	 * @param result
	 * @return
	 */
	public StageSubmission addResultToSubmission(Submission submission, StageSubmission stageSubmission, Stage stage,
			Result result) {

		// Replace variables in feedback
		String publicComment = result.getPublicComment();
		publicComment = resolvePlaceholders(publicComment, submission, stageSubmission, stage, false);
		result.setPublicComment(publicComment);

		// Add the result
		stageSubmission.addResult(result);

		// Write log entry
		final SubmissionLogEntry resultEntry = submissionLogEntryService
				.persistSubmissionLogEntryWithResult(ESubmissionLogEntryType.CHECK, result, stageSubmission);
		submission.addSubmissionLogEntry(resultEntry);
		submission = submissionService.mergeSubmission(submission);

		// Trigger status update for stage submission
		AbstractStageBusiness stageBusiness = getStageBusiness(stage);
		stageSubmission = stageBusiness.updateStatus(stageSubmission, submission);

		if (!stageSubmission.hasPendingChecks() && !stageSubmission.hasInternalErrors()) {
			// Calculate variable updates (phase "after check")
			doVariableUpdates(submission, stageSubmission, stage, stage.getVariableUpdatesAfterCheck());
			submission = submissionService.mergeSubmission(submission);
		}

		// Trigger status update for submission
		submission = updateTotalResult(submission);

		// Abort process if variable update cause errors
		if (submission.hasInternalErrors()) {
			return stageSubmission;
		}

		// Trigger calculation of next stage
		expandSubmissionAfterSubmit(submission, stage, stageSubmission);

		return stageSubmission;
	}

}
