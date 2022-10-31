package de.uni_due.s3.jack3.beans;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Unmanaged;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.uni_due.s3.jack3.beans.data.VariableTuple;
import de.uni_due.s3.jack3.beans.dialogs.ManualFeedbackDialogView;
import de.uni_due.s3.jack3.beans.stagetypes.AbstractSubmissionView;
import de.uni_due.s3.jack3.business.BaseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.stagetypes.AbstractStageBusiness;
import de.uni_due.s3.jack3.entities.enums.ESubmissionLogEntryType;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ConsoleResult;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.utils.ByteCount;

@Named
@ViewScoped
public class ExercisePlayerView extends AbstractView implements Serializable {

	private static final int CONSOLE_TIMEOUT = 30;
	private static final long serialVersionUID = -6861339599355710446L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	@Inject
	private EvaluatorShellView evaluatorShellView;

	@Inject
	private ManualFeedbackDialogView manualFeedbackDialogView;

	@Inject
	private BaseBusiness baseBusiness;

	private AbstractExercise exercise;
	private Submission submission;

	private List<SubmissionLogEntry> submissionLog;
	private String lastFailureLogEntry;
	private List<StageSubmission> stageSubmissionList = new LinkedList<>();
	private StageSubmission currentStageSubmission;
	private StageSubmission stagesubmissionDetailsSelection;
	private AbstractSubmissionView currentStageSubmissionViewBean;
	private final Map<StageSubmission, AbstractSubmissionView> stageSubmissionViews = new HashMap<>();
	/**
	 * Maps stage ids to the stage object. This is necessary because a submission object only saves the stage id, not
	 * the stage object
	 */
	private final Map<Long, Stage> stageCache = new HashMap<>();

	private boolean reviewMode = false;
	private boolean allowedToGiveManualFeedback;
	private boolean allowsHints;
	private boolean allowStudentComments;
	private boolean showFeedback;
	private boolean showResult;
	private boolean showVariablesAndLogs;
	private Map<StageSubmission, List<VariableTuple>> preVariableValues = new HashMap<>();
	private List<VariableValue> filteredPreVariablesValues;
	private Map<StageSubmission, List<VariableTuple>> variableValues = new HashMap<>();
	private List<VariableValue> filteredVariablesValues;

	private CourseOffer accessPath;

	private LocalDateTime consoleRequestStartedAt;
	private long consoleResponseId;
	private String consoleLog;

	// REVIEW bo: Wieso wird hier nicht das @Injected Business genommen?
	protected ExerciseBusiness exerciseBusiness() {
		return CDI.current().select(ExerciseBusiness.class).get();
	}

	/**
	 * Loads contents of exercise from database.
	 */
	public void initPlayer(Submission submission) {
		if ((submission == null) || (submission.getId() == 0)) {
			throw new IllegalArgumentException(
					"ExercisePlayer must be initialized with an existing submission entity from the database.");
		}

		if (reviewMode) {
			loadExerciseReviewMode(submission);
		} else {
			loadExerciseCourseMode(submission);
		}

		this.submission = exercisePlayerBusiness.initSubmissionForExercisePlayer(submission);
		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();
		updateAllVariableValues();
	}

	/**
	 * Reloads the content from database. This method assumes that the view has been fully loaded before.
	 */
	public void reloadPlayer() {
		submission = exerciseBusiness.getSubmissionWithLazyDataBySubmissionIdFromEnvers(submission.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		submissionLog = submission.getSubmissionLogAsSortedList();
		createViewBeans();
		updateAllVariableValues();
	}

	private void loadExerciseCourseMode(Submission submission) {
		if (submission.getExercise().isFrozen()) {
			exercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(submission.getExercise().getId());
		} else {
			exercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(submission.getExercise().getId());
		}
	}

	private void loadExerciseReviewMode(Submission submission) {
		if (submission.getExercise().isFrozen()) {
			exercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(submission.getExercise().getId());
		} else {
			exercise = exerciseBusiness.getRevisionOfExerciseWithLazyData( //
					submission.getExercise(), submission.getShownExerciseRevisionId());
		}
	}

	/**
	 * Called via ajax to check for updates due to async checks
	 */
	public void refreshContent() {
		submission = exerciseBusiness.refreshSubmissionFromDatabase(submission);
		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();
		updateAllVariableValues();
	}

	public void refreshConsole() {
		ConsoleResult consoleResult = baseBusiness.findById(ConsoleResult.class, consoleResponseId, false) //
				.orElseThrow();

		if (consoleResult.isResponseReceived()) {
			consoleLog = formatOutput(consoleResult);
			stopClientConsolePolling();
			return;
		}

		Duration absDuration = Duration.between(consoleRequestStartedAt, LocalDateTime.now()).abs();
		if (absDuration.toSeconds() > CONSOLE_TIMEOUT) {
			consoleLog = getLocalizedMessage("exercisePlayer.consoleResponseTimeout");
			stopClientConsolePolling();
			return;
		}

		PrimeFaces.current().executeScript("PF('consoleWidget').getJQ().css('background-color', '#D3D3D3');");
	}

	private String formatOutput(ConsoleResult consoleResult) {
		AbstractStageBusiness business = exercisePlayerBusiness.getStageBusiness(consoleResult.getHandlerStageType());
		return business.formatConsoleResponse(consoleResult.getResponse());
	}

	private void stopClientConsolePolling() {
		PrimeFaces.current().executeScript("PF('consolePoll').stop()");
		PrimeFaces.current().executeScript("PF('consoleWidget').getJQ().css('background-color', 'white')");
	}

	/**
	 * Returns false if the JSXGraph list is empty, otherwise true.
	 */
	public boolean isJSXGraphVisible() {
		return ((exercise != null) && ((exercise.getJSXGraphs() != null) || !exercise.getJSXGraphs().isEmpty()));
	}

	/**
	 * Returns a list of all stage types used in the current exercise. The returned list is immutable.
	 *
	 * @return A list of all stage types used in the current exercise.
	 */
	public List<?> getStageTypes() {
		if (exercise == null) {
			return Collections.emptyList();
		}
		return exercise.getStages().stream().map(Stage::getType).distinct().collect(Collectors.toUnmodifiableList());
	}

	/**
	 * Not meant for use outside of exercisePlayer.xhtml
	 *
	 * @return Variable values for filtering in exercisePlayer.xhtml
	 */
	public List<VariableValue> getFilteredVariablesValues() {
		return filteredVariablesValues;
	}

	public void setFilteredVariablesValues(List<VariableValue> filteredVariablesValues) {
		this.filteredVariablesValues = filteredVariablesValues;
	}

	public List<VariableValue> getFilteredPreVariablesValues() {
		return filteredPreVariablesValues;
	}

	public void setFilteredPreVariablesValues(List<VariableValue> filteredPreVariablesValues) {
		this.filteredPreVariablesValues = filteredPreVariablesValues;
	}

	public CourseOffer getAccessPath() {
		return accessPath;
	}

	public void setAccessPath(CourseOffer accessPath) {
		this.accessPath = accessPath;
	}

	private boolean isUpToDateAndWithinDeadline() {
		// If exercise is not frozen, we need to check for updates.
		// We return false, if the shown exercise revision is not the most recent one.
		if (!submission.getExercise().isFrozen() && (submission.getShownExerciseRevisionId() != exerciseBusiness
				.getProxiedOrLastPersistedRevisionId(submission.getExercise()))) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_FATAL, "exercisePlayer.outdatedRevisionTitle",
					"exercisePlayer.outdatedRevisionMessage", (Object[]) null);
			// TODO ms: Leave a comment in the log?
			return false;
		}

		// We return true, if there is no course record or a course record without deadline.
		if ((submission.getCourseRecord() == null) || (submission.getCourseRecord().getDeadline() == null)) {
			return true;
		}

		// We return false, if there is a deadline that lies in the past.
		if (LocalDateTime.now().isAfter(submission.getCourseRecord().getDeadline())) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_FATAL, "exercisePlayer.pastDeadlineTitle",
					"exercisePlayer.pastDeadlineMessage", (Object[]) null);
			// TODO ms: Leave a comment in the log?
			return false;
		}

		return true;
	}

	public boolean stageHasCodeTestCapability() {
		if (currentStageSubmission == null) {
			return false;
		}

		Stage stageFromCache = stageCache.get(currentStageSubmission.getStageId());
		if (stageFromCache == null) {
			return false;
		}
		return exercisePlayerBusiness.hasCodeTestCapability(stageFromCache);
	}

	public void testCode() {
		consoleLog = getLocalizedMessage("exercisePlayer.computing");
		Stage stageFromCache = stageCache.get(currentStageSubmission.getStageId());
		consoleResponseId = exercisePlayerBusiness.testCode(stageFromCache, currentStageSubmission,
				getCurrentUser().getLoginName());
		setConsoleRequestStartedAt(LocalDateTime.now());
	}

	/**
	 * Handles click on submit button.
	 *
	 * @throws ActionNotAllowedException
	 */
	public void submitSubmission() throws ActionNotAllowedException {
		if (!isUpToDateAndWithinDeadline()) {
			return;
		}

		if (currentStageSubmission.hasInternalErrors()) {
			throw new ActionNotAllowedException(
					getCurrentUser() + " should not be able to submit an exercise when submission has internal errors");
		}

		// Clear any results that might exist because of stage repetition
		currentStageSubmission.clearResults();

		// Refresh submission object because user might have added comments before submitting
		submission = exerciseBusiness.refreshSubmissionFromDatabase(submission);

		Stage stageFromCache = stageCache.get(currentStageSubmission.getStageId());
		submission = exercisePlayerBusiness.performStageSubmit(submission, stageFromCache, currentStageSubmission);
		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();

		// Update the current and the previous variable values
		final int submissionCount = stageSubmissionList.size();
		if (submissionCount > 0) {
			updateVariableValues(stageSubmissionList.get(submissionCount - 1));
		}
		if (submissionCount > 1) {
			updateVariableValues(stageSubmissionList.get(submissionCount - 2));
		}
	}

	/**
	 * Handles click on skip button.
	 *
	 * @throws ActionNotAllowedException
	 */
	public void skipStage() throws ActionNotAllowedException {
		if (!isUpToDateAndWithinDeadline()) {
			return;
		}

		if (currentStageSubmission.hasInternalErrors()) {
			throw new ActionNotAllowedException(
					getCurrentUser() + " should not be able to skip a stage when submission has internal errors");
		}

		// Clear any results that might exist because of stage repetition
		currentStageSubmission.clearResults();

		try {
			submission = exercisePlayerBusiness.performStageSkip(submission,
					stageCache.get(currentStageSubmission.getStageId()), currentStageSubmission);
		} catch (final IllegalAccessException e) {
			getLogger().error("IllegalAccessException: ", e);
		}

		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();

		// Update the current and the previous variable values
		final int submissionCount = stageSubmissionList.size();
		if (submissionCount > 0) {
			updateVariableValues(stageSubmissionList.get(submissionCount - 1));
		}
		if (submissionCount > 1) {
			updateVariableValues(stageSubmissionList.get(submissionCount - 2));
		}
	}

	/**
	 * Handles click on hint button.
	 *
	 * @throws ActionNotAllowedException
	 */
	public void requestHint() throws ActionNotAllowedException {
		if (!isUpToDateAndWithinDeadline()) {
			return;
		}
		if (currentStageSubmission.hasInternalErrors()) {
			throw new ActionNotAllowedException(
					getCurrentUser() + " should not be able to request a hint when submission has internal errors");
		}

		// Clear any results that might exist because of stage repetition
		currentStageSubmission.clearResults();

		submission = exercisePlayerBusiness.performStageHintRequest(submission,
				stageCache.get(currentStageSubmission.getStageId()), currentStageSubmission);
		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();
		updateVariableValues(currentStageSubmission);
	}

	public void eraseSubmission(StageSubmission stageSubmission) {
		if (!isUpToDateAndWithinDeadline()) {
			return;
		}

		Stage stageFromCache = stageCache.get(stageSubmission.getStageId());
		try {
			submission = exercisePlayerBusiness.eraseSubmission(submission, stageFromCache, stageSubmission);
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exception.actionNotAllowed");
			return;
		}
		submissionLog = submission.getSubmissionLogAsSortedList();

		createViewBeans();
		updateAllVariableValues();
	}

	/**
	 * Iterates over the complete submission log and fills view caches stepwise.
	 */
	private void createViewBeans() {
		// Clear existing caches
		stageSubmissionViews.clear();
		stageSubmissionList.clear();
		stageCache.clear();

		// Prepare temp data storage
		boolean repeat = false;

		// Iterate over log
		for (final SubmissionLogEntry submissionLogEntry : submissionLog) {
			switch (submissionLogEntry.getType()) {
			// Main case: Add a new stage to be displayed
			case ENTER:
				handleEnterSubmissionLogEntry(submissionLogEntry, repeat);
				repeat = false;
				break;
			case HINT:
			case SUBMIT:
			case SKIP:
			case CHECK:
			case VAR_UPDATE:
			case EXIT:
				handleSimpleSubmissionLogEntry(submissionLogEntry);
				break;
			case ERASE:
				handleEraseSubmissionLogEntry(submissionLogEntry);
				break;
			case REPEAT:
				handleRepeatSubmissionLogEntry(submissionLogEntry);
				repeat = true;
				break;
			case END:
				handleEndSubmissionLogEntry(submissionLogEntry);
				break;
			case FAIL:
				lastFailureLogEntry = submissionLogEntry.getText();
				handleSimpleSubmissionLogEntry(submissionLogEntry);
				break;
			}
		}
	}

	private void handleEndSubmissionLogEntry(SubmissionLogEntry endLogEntry) {
		if (currentStageSubmissionViewBean != null) {
			currentStageSubmissionViewBean.addSubmissionLogEntry(endLogEntry);
			currentStageSubmissionViewBean = null;
			currentStageSubmission = null;
		}
	}

	private void handleRepeatSubmissionLogEntry(SubmissionLogEntry submissionLogEntry) {
		stageSubmissionViews.get(submissionLogEntry.getSubmission()).addSubmissionLogEntry(submissionLogEntry);
	}

	/**
	 * This method associates log entries with the detailed log of the respective stage submission
	 *
	 * @param submissionLogEntry
	 *            The log entry to be handled
	 */
	private void handleSimpleSubmissionLogEntry(SubmissionLogEntry submissionLogEntry) {
		if (submissionLogEntry.getSubmission() != null
				// Ignore log entries not related to a stage submission at all
				&& stageSubmissionViews.get(submissionLogEntry.getSubmission()) != null
				// Ignore log entries for variable updates that happen before a stage is entered
				) {
			stageSubmissionViews.get(submissionLogEntry.getSubmission()).addSubmissionLogEntry(submissionLogEntry);
		} else if (currentStageSubmissionViewBean != null
				&& submissionLogEntry.getType() != ESubmissionLogEntryType.VAR_UPDATE) {
			// Fallback for old log entries
			currentStageSubmissionViewBean.addSubmissionLogEntry(submissionLogEntry);
		}
	}

	private void handleEraseSubmissionLogEntry(SubmissionLogEntry submissionLogEntry) {
		stageSubmissionViews.get(submissionLogEntry.getSubmission()).addSubmissionLogEntry(submissionLogEntry);

		// We find out which submissions have to be erased
		StageSubmission erasedStageSubmission = submissionLogEntry.getSubmission();
		int erasedIndex = stageSubmissionList.indexOf(erasedStageSubmission);

		// Make sure we do not try to remove elements twice (see #1022)
		if (erasedIndex > -1) {
			// We keep the submissions that are not erased
			stageSubmissionList = stageSubmissionList.subList(0, erasedIndex);

			// We explicitly do not clear the hint counter here, because otherwise students can get hints for free

			currentStageSubmission = null;
			currentStageSubmissionViewBean = null;
		}
	}

	private void handleEnterSubmissionLogEntry(SubmissionLogEntry submissionLogEntry, boolean repeat) {
		// Store old data from previous stage
		StageSubmission previousStageSubmission = currentStageSubmission;
		AbstractSubmissionView previousStageSubmissionViewBean = currentStageSubmissionViewBean;

		// Retrieve full data for this stage
		StageSubmission stageSubmission = submissionLogEntry.getSubmission();
		if (!reviewMode) {
			// Load from main tables
			stageSubmission = exerciseBusiness.getStageSubmissionWithLazyData(stageSubmission);
		} else {
			// Load from Envers
			stageSubmission = exerciseBusiness.getStageSubmissionWithLazyDataFromEnvers(stageSubmission);
		}

		// Fill data caches
		stageSubmissionList.add(stageSubmission);
		currentStageSubmission = stageSubmission;

		// Find name of stage specific view bean
		final String viewBeanName = "de.uni_due.s3.jack3.beans.stagetypes." + stageSubmission.getClass().getSimpleName()
				+ "View";

		createStageSpecificViewBean(submissionLogEntry, stageSubmission, viewBeanName);

		// Create actual hint texts
		for (String hint : stageSubmission.getHintTexts()) {
			currentStageSubmissionViewBean.addHint(hint);
		}

		// Handle special cases of stage repetition
		// The extra checks are required to ignore spurious submissions (see #997)
		if (repeat && (previousStageSubmissionViewBean != null)
				&& previousStageSubmissionViewBean.getStage().equals(currentStageSubmissionViewBean.getStage())) {
			// Make previous submission invisible
			previousStageSubmissionViewBean.setVisible(false);

			// If no results exist or are expected to come, we copy old results so that they are still visible although
			// the bean has been made invisible in the previous step. These copies are later removed by
			// submitSubmission(), skipStage() or requestHint().
			if (stageSubmission.getResults().isEmpty() && !stageSubmission.hasPendingChecks()) {
				for (Result oldResult : previousStageSubmission.getResults()) {
					stageSubmission.addResult(oldResult);
				}
				stageSubmission.setPoints(previousStageSubmission.getPoints());
			}
		}
	}

	private void createStageSpecificViewBean(SubmissionLogEntry submissionLogEntry, StageSubmission stageSubmission,
			final String viewBeanName) {
		try {
			// Create stage specific view bean
			@SuppressWarnings("unchecked")
			final Class<AbstractSubmissionView> viewBeanClass = (Class<AbstractSubmissionView>) this.getClass()
			.getClassLoader() //
			.loadClass(viewBeanName);

			currentStageSubmissionViewBean = new Unmanaged<>(viewBeanClass) //
					.newInstance() //
					.produce() //
					.inject() //
					.postConstruct() //
					.get();

			// Cache data in view bean
			Stage stage = null;
			long stageId = stageSubmission.getStageId();
			if (stageCache.containsKey(stageId)) {
				stage = stageCache.get(stageId);
			} else {
				if (reviewMode) {
					stage = exerciseBusiness.getStageRevisionForStageSubmissionByTimestamp(stageSubmission,
							submissionLogEntry.getTimestamp());
				} else {
					stage = exercise.getStages().stream() //
							.filter(currentStage -> currentStage.getId() == stageId) //
							.findFirst() //
							.orElseThrow(NoSuchJackEntityException::new);
				}
				stageCache.put(stage.getId(), stage);
			}

			// Create the view
			currentStageSubmissionViewBean.setStageSubmission(submission, stageSubmission, stage);
			currentStageSubmissionViewBean.addSubmissionLogEntry(submissionLogEntry);

			// Store bean in map
			stageSubmissionViews.put(stageSubmission, currentStageSubmissionViewBean);

			// Copy maps to evaluator shell, use submission id as key
			EvaluatorMaps evaluatorMaps = exercisePlayerBusiness.prepareEvaluatorMaps(submission, stageSubmission,
					stage,
					true);
			evaluatorShellView.getRequest(String.valueOf(stageSubmission.getId())).setMaps(evaluatorMaps);

		} catch (final ClassNotFoundException e) {
			getLogger().warn("Class '" + viewBeanName
					+ "' not found. This may be OK, but most likely this causes errors when users interact with exercise stages.");
		}
	}

	public AbstractExercise getExercise() {
		return exercise;
	}

	public Submission getSubmission() {
		return submission;
	}

	public List<StageSubmission> getStageSubmissionList() {
		return stageSubmissionList;
	}

	public int getPointsWithDeductionsForStageSubmission(StageSubmission stageSubmission) {
		return exercisePlayerBusiness.getPointsWithHintMalus(stageSubmission, exercise);
	}

	public boolean isScoreDeductedForHints(StageSubmission stageSubmission) {
		return exercisePlayerBusiness.getPointsWithHintMalus(stageSubmission, exercise) != stageSubmission.getPoints();
	}

	public List<SubmissionLogEntry> getSubmissionLog() {
		return submissionLog;
	}

	public AbstractSubmissionView getStageSubmissionViewBean(StageSubmission stageSubmission) {
		return stageSubmissionViews.get(stageSubmission);
	}

	public Stage getStageByIdFromCache(long id) {
		return stageCache.get(id);
	}

	public StageSubmission getSubmissionDetailsSelection() {
		return stagesubmissionDetailsSelection;
	}

	public void setSubmissionDetailsSelection(StageSubmission stagesubmissionDetailsSelection) {
		this.stagesubmissionDetailsSelection = stagesubmissionDetailsSelection;
	}

	public boolean isFirstStage(StageSubmission stageSubmission) {
		if (stageSubmissionList.isEmpty()) {
			throw new IllegalStateException(
					"Exercise is not initialized properly. List of stage submissions is empty.");
		}

		return stageSubmissionList.get(0).equals(stageSubmission);
	}

	public boolean isLastStage(StageSubmission stageSubmission) {
		if (stageSubmissionList.isEmpty()) {
			throw new IllegalStateException(
					"Exercise is not initialized properly. List of stage submissions is empty.");
		}

		return stageSubmissionList.get(stageSubmissionList.size() - 1).equals(stageSubmission);
	}

	public boolean isCurrentStage(StageSubmission stageSubmission) {
		return (currentStageSubmission != null) && (currentStageSubmission.equals(stageSubmission));
	}

	public boolean isReviewMode() {
		return reviewMode;
	}

	public void setReviewMode(boolean reviewMode) {
		this.reviewMode = reviewMode;
	}

	public boolean isAllowedToGiveManualFeedback() {
		return allowedToGiveManualFeedback;
	}

	public void setAllowedToGiveManualFeedback(boolean allowedToGiveManualFeedback) {
		this.allowedToGiveManualFeedback = allowedToGiveManualFeedback;
	}

	public boolean allowsHints() {
		return allowsHints;
	}

	public void setAllowsHints(boolean allowsHints) {
		this.allowsHints = allowsHints;
	}

	public boolean hasMoreHints(StageSubmission stageSubmission) {
		return stageSubmission.getGivenHintCount() < stageCache.get(stageSubmission.getStageId()).getHints().size();
	}

	public boolean hasAnyHints(StageSubmission stageSubmission) {
		return !stageCache.get(stageSubmission.getStageId()).getHints().isEmpty();
	}

	public boolean stageAllowsSkip(StageSubmission stageSubmission) {
		return stageCache.get(stageSubmission.getStageId()).getAllowSkip();
	}

	public boolean isAllowStudentComments() {
		return allowStudentComments;
	}

	public void setAllowStudentComments(boolean allowStudentComments) {
		this.allowStudentComments = allowStudentComments;
	}

	public boolean isShowFeedback() {
		return showFeedback;
	}

	public void setShowFeedback(boolean showFeedback) {
		this.showFeedback = showFeedback;
	}

	public boolean isShowResult() {
		return showResult;
	}

	public void setShowResult(boolean showResult) {
		this.showResult = showResult;
	}

	public boolean isShowVariablesAndLogs() {
		return showVariablesAndLogs;
	}

	public void setShowVariablesAndLogs(boolean showVariablesAndLogs) {
		this.showVariablesAndLogs = showVariablesAndLogs;
	}

	public String getLastFailureLogEntry() {
		return lastFailureLogEntry;
	}

	/**
	 * Handles file download of a stage resource.
	 */
	public StreamedContent getStageResource(StageResource stageResource) {
		// Need to make the content final explicitly for the following expression
		final byte[] content = exerciseBusiness.getExerciseResourceContent(stageResource.getExerciseResource(),
				submission, currentStageSubmission, getStageByIdFromCache(currentStageSubmission.getStageId()));

		return DefaultStreamedContent.builder() //
				.contentType(stageResource.getExerciseResource().getMimeType()) //
				.name(stageResource.getExerciseResource().getFilename()) //
				.stream(() -> new ByteArrayInputStream(content)).build();
	}

	/**
	 * Gets the size of a stage resource in Bytes with SI prefixes.
	 */
	public String getStageResourceSize(StageResource stageResource) {
		return ByteCount.toSIString(stageResource.getExerciseResource().getSize());
	}

	/**
	 * Returns false if all resource descriptions of a list are empty, otherwise true.
	 */
	public boolean isShowStageResourceDescription(List<StageResource> resources) {
		for (StageResource resource : resources) {
			if ((resource.getDescription() != null) && !resource.getDescription().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts the value of a variable value object into LaTeX
	 */
	public String getLaTeXRepresentation(VariableValue variableValue) {
		try {
			return "$" + converterBusiness.convertToLaTeX(variableValue) + "$";
		} catch (Exception e) {
			return getLocalizedMessage("global.notAvailable.short");
		}
	}

	/**
	 * Returns the variable values that belong to a given submission, representing the state after some interaction
	 * (submit, skip, hint request) has happened.
	 */
	// Used by the UI
	public List<VariableTuple> getVariableValues(StageSubmission stagesubmission) {
		// Only update variables if shown
		if (!showVariablesAndLogs) {
			Collections.emptyList();
		}

		// Compute the variable values if not available
		variableValues.putIfAbsent(stagesubmission, computeVariables(stagesubmission));
		return variableValues.get(stagesubmission);
	}

	/**
	 * Returns the variable values that belong to a given submission, containing only the exercise variable values used
	 * to generate the stage content.
	 */
	// Used by the UI
	public List<VariableTuple> getPreVariableValues(StageSubmission submission) {
		// Only update variables if shown
		if (!showVariablesAndLogs) {
			Collections.emptyList();
		}

		// Compute the variable values if not available
		preVariableValues.putIfAbsent(submission, computePreVariables(submission));
		return preVariableValues.get(submission);
	}

	/**
	 * Updates variable values for all submissions.
	 */
	private void updateAllVariableValues() {
		// Only update variables if shown
		if (!showVariablesAndLogs) {
			return;
		}

		preVariableValues.clear();
		variableValues.clear();
		for (StageSubmission currStageSubmission : stageSubmissionList) {
			preVariableValues.put(currStageSubmission, computePreVariables(currStageSubmission));
			variableValues.put(currStageSubmission, computeVariables(currStageSubmission));
		}
	}

	/**
	 * Updates variable values for a given submission.
	 */
	private void updateVariableValues(StageSubmission stagesubmission) {
		// Only update variables if shown
		if (!showVariablesAndLogs) {
			return;
		}

		variableValues.put(stagesubmission, computeVariables(stagesubmission));
	}

	/**
	 * Computes sorted variables for a given stage submission.
	 */
	private List<VariableTuple> computeVariables(StageSubmission stagesubmission) {
		final Stage stage = stageCache.get(stagesubmission.getStageId());
		List<VariableTuple> result = new ArrayList<>();

		final EvaluatorMaps allVars = exercisePlayerBusiness.prepareEvaluatorMaps(submission, stagesubmission, stage,
				true);

		// We iterate over the declarations because otherwise we get a list with a wrong order. We don't sort the
		// exercise variables by name because the order is important for initialization!
		// see JACK/jack3-core#462
		for (VariableDeclaration variableDeclaration : exercise.getVariableDeclarations()) {
			final String varName = variableDeclaration.getName();
			result.add(new VariableTuple(varName, allVars.getExerciseVariableMap().get(varName),
					VariableTuple.EVariableType.VAR));
		}

		allVars.getInputVariableMap().entrySet().stream() //
		.map(entry -> new VariableTuple(entry.getKey(), entry.getValue(), VariableTuple.EVariableType.INPUT)) //
		.sorted(Comparator.comparing(VariableTuple::getName)) //
		.forEach(result::add);

		allVars.getMetaVariableMap().entrySet().stream() //
		.map(entry -> new VariableTuple(entry.getKey(), entry.getValue(), VariableTuple.EVariableType.META)) //
		.sorted(Comparator.comparing(VariableTuple::getName)) //
		.forEach(result::add);

		return result;
	}

	private List<VariableTuple> computePreVariables(StageSubmission stageSubmission) {
		final Stage stage = stageCache.get(stageSubmission.getStageId());
		List<VariableTuple> result = new ArrayList<>();

		final EvaluatorMaps allVars = exercisePlayerBusiness.prepareEvaluatorMaps(submission, stageSubmission, stage,
				true);

		// We iterate over the declarations because otherwise we get a list with a wrong order. We don't sort the
		// exercise variables by name because the order is important for initialization!
		// see JACK/jack3-core#462
		for (VariableDeclaration variableDeclaration : exercise.getVariableDeclarations()) {
			final String varName = variableDeclaration.getName();
			result.add(new VariableTuple(varName, allVars.getExerciseVariableMap().get(varName),
					VariableTuple.EVariableType.VAR));
		}

		return result;
	}

	public VariableTuple.EVariableType[] getVariableTypeValues() {
		return VariableTuple.EVariableType.values();
	}

	/**
	 * @return The exercise variables from the {@link Submission} object. This represents the latest variable values
	 *         within an exercise.
	 */
	public List<VariableTuple> getLatestExerciseVariables() {
		List<VariableTuple> result = new ArrayList<>();
		for (VariableDeclaration variableDeclaration : exercise.getVariableDeclarations()) {
			final String name = variableDeclaration.getName();
			Optional<VariableValue> value = submission.getVariableValueForName(name);
			// It may be the case that a variable is declared but no value is available, e.g. if there was an error
			// while initializing. Therefore we only show the variable if a value is present.
			if (value.isPresent()) {
				result.add(new VariableTuple(name, value.get(), VariableTuple.EVariableType.VAR));
			}
		}
		return result;
	}

	/**
	 * If the student is allowed to erase submissions ("Start over from here"). This is the case when we are in testing
	 * mode or the course offer allows erasing the submission.
	 */
	public boolean isAllowStageRestart() {
		return submission.getCourseOffer().map(CourseOffer::isAllowStageRestart).orElse(true);
	}

	public void loadManualFeedbackDialog(StageSubmission stageSubmission) {
		manualFeedbackDialogView.load(stageSubmission, submission, accessPath);
	}

	public String getConsoleLog() {
		return consoleLog;
	}

	public void setConsoleLog(String consoleLog) {
		this.consoleLog = consoleLog;
	}

	public LocalDateTime getConsoleRequestStartedAt() {
		return consoleRequestStartedAt;
	}

	public void setConsoleRequestStartedAt(LocalDateTime consoleRequestStartedAt) {
		this.consoleRequestStartedAt = consoleRequestStartedAt;
	}
}
