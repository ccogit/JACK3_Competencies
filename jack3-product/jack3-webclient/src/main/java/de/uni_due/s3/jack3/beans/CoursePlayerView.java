package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.EJBTransactionRolledbackException;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Hibernate;

import de.uni_due.s3.jack3.beans.ViewId.Builder;
import de.uni_due.s3.jack3.beans.data.CoursePlayerSubmissionCache;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.CoursePlayerBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.enums.ESubmissionStatus;
import de.uni_due.s3.jack3.utils.JackStringUtils;
import de.uni_due.s3.jack3.utils.StopWatch;

@Named
@ViewScoped
public class CoursePlayerView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -2572578747971527899L;

	private AbstractCourse course;

	private CourseOffer courseOffer;
	private CourseRecord courseRecord;

	private AbstractExercise currentExercise;
	private Submission currentSubmission;

	/** Temporary variable for saving the student's comment and adding it */
	private String newComment;
	/** If e-mail address should be shown with the comment to the lecturer */
	private boolean emailVisible;

	/** If restarting exercises is allowed in general. */
	private boolean allowExerciseRestart;
	/** If restarting the course is allowed in general */
	private boolean allowCourseOfferRestart;
	private boolean showFeedbackImmediately;
	private boolean showResultImmediately;
	private ECourseOfferReviewMode reviewMode;
	private boolean showFeedbackInCourseResults;
	private boolean showExerciseAndSubmissionInCourseResults;
	private boolean showResultInCourseResults;
	private boolean showStatus;
	private boolean showDifficulty;
	private boolean showMyResult;
	private boolean showLeaveCourseOffer;

	private String hyperlinkToExercise;

	// - - - - - - - - - - Cache - - - - - - - - - -

	/** All exercises that the course's provider contain */
	private List<AbstractExercise> exercises;

	/** What exercises are frozen (Only available for a course with a fixed-list provider) */
	private Map<Long, Boolean> cachedFrozenExerciseIDs;

	/** How many points the exercises give (Only available for a course with a fixed-list provider) */
	private Map<AbstractExercise, Integer> cachedExerciseWeights;

	/** Cache submission data */
	private Map<AbstractExercise, CoursePlayerSubmissionCache> cachedSubmissions;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private CoursePlayerBusiness coursePlayerBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private ExercisePlayerView exercisePlayer;

	/**
	 * Inits the course player for testing an existing course. If called, this view is embedded in a
	 * {@link CourseTestView}. We don't check any rights because a lecturer is allowed everything when testing the
	 * course.
	 *
	 * @param courseId
	 *            The ID of the {@link Course} to test.
	 *
	 * @throws IOException
	 *             If no course with the given ID exists.
	 */
	public void initForTesting(long courseId) throws IOException {
		try {
			course = courseBusiness.getCourseWithLazyDataByCourseID(courseId);
		} catch (EJBTransactionRolledbackException e) {
			sendErrorResponse(400, "Course with given courseId does not exist in database");
			return;
		}
		setAllowExerciseRestart(true);
		setAllowCourseOfferRestart(true);
		setShowFeedbackImmediately(true);
		setShowResultImmediately(true);
		setReviewMode(ECourseOfferReviewMode.ALWAYS);
		setShowFeedbackInCourseResults(true);
		setShowExerciseAndSubmissionInCourseResults(true);
		setShowResultInCourseResults(true);
		setShowStatus(true);
		setShowDifficulty(true);
		setShowMyResult(true);
		setShowLeaveCourseOffer(true);

		initCourseSettings();
	}

	/**
	 * Inits the course player for use in an existing {@link CourseOffer} with an existing {@link CourseRecord}. If
	 * called, this view is embedded in a {@link ShowCourseRecordView}. Several rights are checked (e.g. allow exercise
	 * restart) to display the page correctly.
	 *
	 * @param courseOffer
	 *            The offer, which is to "play".
	 */
	public void initForCourseOffer(CourseOffer courseOffer) {
		// TODO ms: This method assumes that courseRecord has been set before this method is called. We should check that
		// explicitly.

		this.courseOffer = courseOffer;
		course = courseOffer.getCourse();

		setShowMyResult(true);

		setAllowExerciseRestart(courseOffer.isAllowExerciseRestart());
		setAllowCourseOfferRestart(!courseOffer.isOnlyOneParticipation());
		setShowFeedbackImmediately(courseOffer.isShowFeedbackImmediately());
		setShowResultImmediately(courseOffer.isShowResultImmediately());
		setShowDifficulty(courseOffer.isShowDifficulty());
		setReviewMode(courseOffer.getReviewMode());
		setShowFeedbackInCourseResults(courseOffer.isShowFeedbackInCourseResults());
		setShowExerciseAndSubmissionInCourseResults(courseOffer.isShowExerciseAndSubmissionInCourseResults());
		setShowResultInCourseResults(courseOffer.isShowResultInCourseResults());
		setShowLeaveCourseOffer(true);

		if (ECourseOfferReviewMode.ALWAYS.equals(courseOffer.getReviewMode())
				&& (courseOffer.isShowExerciseAndSubmissionInCourseResults()
						|| courseOffer.isShowResultInCourseResults())) {
			setShowStatus(true);
		} else if (ECourseOfferReviewMode.NEVER.equals(courseOffer.getReviewMode())) {
			setShowStatus(false);
		} else if (ECourseOfferReviewMode.AFTER_EXIT.equals(courseOffer.getReviewMode())) {
			setShowStatus(courseRecord.isClosed());
		} else if (ECourseOfferReviewMode.AFTER_END.equals(courseOffer.getReviewMode())) {
			setShowStatus((courseOffer.getSubmissionDeadline() != null)
					&& LocalDateTime.now().isAfter(courseOffer.getSubmissionDeadline()));
		}

		initCourseSettings();

	}

	/**
	 * Sets the parameters that only depend on the underlying course
	 */
	private void initCourseSettings() {

		loadExercisesFromCourse();
		reloadSubmissionsCache();

		currentExercise = null;
	}

	/**
	 * Loads the exercise list from the course's exercise provider.
	 */
	private void loadExercisesFromCourse() {

		final var sw = new StopWatch().start();
		if (!Hibernate.isInitialized(courseRecord.getExercises())) {
			courseRecord = courseBusiness.getCourseRecordWithExercisesById(courseRecord.getId());
		}
		getLogger().debugf("Loading course record exercises took %s", sw.stop().getElapsedMilliseconds());
		final int exerciseCount = courseRecord.getExercises().size();
		// Invalidate exercise cache
		exercises = new ArrayList<>(exerciseCount);
		cachedFrozenExerciseIDs = new HashMap<>(exerciseCount);
		cachedExerciseWeights = new HashMap<>(exerciseCount);

		// if we have a FixedListExerciseProvider we must cache the possible frozen id's and the weights
		// FolderExerciseProvder doesn't use frozen exercises or weights so nothing to do in this case
		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			// We can just take the course entry list
			final FixedListExerciseProvider flep = (FixedListExerciseProvider) course.getContentProvider();
			for (final CourseEntry courseEntry : flep.getCourseEntries()) {
				AbstractExercise toAdd = courseEntry.getFrozenExercise();
				if (toAdd == null) {
					toAdd = courseEntry.getExercise();
				}

				// Fill exercise cache
				cachedFrozenExerciseIDs.put(toAdd.getId(), toAdd.isFrozen());
				cachedExerciseWeights.put(toAdd, courseEntry.getPoints());
			}
		}
		// load exercises from courseRecord
		exercises.addAll(courseRecord.getExercises());
		// Initial sorting
		courseBusiness.sortExercisesForStudent(exercises, courseRecord);

		currentExercise = null;
	}

	/**
	 * Loads all submissions from database. The submission cache will be updated.
	 */
	private void reloadSubmissionsCache() {
		cachedSubmissions = new HashMap<>();

		// We first collect all submissions, later we filter out all those that belong to exercises that are no longer
		// part of this course
		List<Submission> allSubmissions = courseBusiness.getAllSubmissionsForCourseRecord(courseRecord);
		Set<AbstractExercise> exercisesWithSubmissions = allSubmissions.stream().map(Submission::getExercise).collect(Collectors.toSet());

		for (AbstractExercise exercise : exercisesWithSubmissions) {
			List<Submission> sortedSubmissionsForExercise = allSubmissions
					.stream()
					.filter(submission -> submission.getExercise().equals(exercise))
					.sorted(Comparator.comparing(Submission::getId).reversed()) // Sort by ID = Sort by creation
					.collect(Collectors.toList());
			cachedSubmissions.put(exercise,
					new CoursePlayerSubmissionCache(sortedSubmissionsForExercise, !exercises.contains(exercise)));
		}

		// Ensure that there is a cache entry for each listed exercise, not only for them with submissions
		for (AbstractExercise exercise : exercises) {
			cachedSubmissions.putIfAbsent(exercise, new CoursePlayerSubmissionCache(Collections.emptyList(), false));
		}
	}

	/**
	 * Reloads the current submission. Called from ExercisePlayer via remote command when the user submits an exercise or
	 * performs other actions that have an impact on the score.
	 */
	public void updateCurrentSubmissionPoints() {
		currentSubmission = exerciseBusiness.refreshSubmissionFromDatabase(currentSubmission);
		courseRecord = courseBusiness.getCourseRecordById(courseRecord.getId());

		final CoursePlayerSubmissionCache cache = cachedSubmissions.get(currentExercise);
		cache.setLastSubmission(currentSubmission);

		// Update best submission if current result is better than the actual result
		final Optional<Submission> cachedBestSubmission = cache.getBestSubmission();
		if (!cachedBestSubmission.isPresent()
				|| (cachedBestSubmission.get().getResultPoints() <= currentSubmission.getResultPoints())) {
			cache.setBestSubmission(currentSubmission);
		}
	}

	public AbstractCourse getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public CourseOffer getCourseOffer() {
		return courseOffer;
	}

	public void setCourseOffer(CourseOffer courseOffer) {
		this.courseOffer = courseOffer;
	}

	/**
	 * Returns the list with exercises.
	 */
	public List<AbstractExercise> getExercises() {
		return exercises;
	}

	public CourseRecord getCourseRecord() {
		return courseRecord;
	}

	public void setCourseRecord(CourseRecord courseRecord) {
		this.courseRecord = courseRecord;
	}

	public AbstractExercise getCurrentExercise() {
		return currentExercise;
	}

	public Submission getCurrentSubmission() {
		return currentSubmission;
	}

	/**
	 * Loads an exercise into the foreground so that the user sees the exercise player for this exercise. Is called when
	 * the user selects or restarts an exercise or if this view was initialized while the user is currently in an
	 * exercise.
	 *
	 * @param exerciseId
	 *            ID of the exercise currently being processed.
	 */
	public void setCurrentExercise(long exerciseId) {
		currentExercise = null;

		try {
			Boolean isExerciseFrozen = cachedFrozenExerciseIDs.get(exerciseId);
			if ((isExerciseFrozen != null) && isExerciseFrozen) {
				currentExercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(exerciseId);
			} else {
				currentExercise = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exerciseId);
			}
		} catch (EJBTransactionRolledbackException e) {
			// Nothing to do here. currentExercise just stays empty, which is handled properly later on.
		}

		// Refresh courseRecord, since its result may have changed and would be overwritten without refresh
		courseRecord = courseBusiness.getCourseRecordById(courseRecord.getId());

		if ((currentExercise == null) || !exercises.contains(currentExercise)) {
			// Special case: Exercise has been deleted or removed from the course
			currentExercise = null;
			courseRecord.setCurrentExercise(null);
			courseRecord = coursePlayerBusiness.updateCourseRecord(courseRecord);
		} else {
			courseRecord.setCurrentExercise(currentExercise);
			courseRecord = coursePlayerBusiness.updateCourseRecord(courseRecord);

			Optional<Submission> foundSubmission = courseBusiness.getLatestSubmissionForCourseRecordAndExercise(courseRecord,
					currentExercise);
			if (foundSubmission.isPresent()) {
				// Just go to the existing submission
				currentSubmission = foundSubmission.get();
			} else {
				// Create a new submission
				try {
					currentSubmission = exerciseBusiness.createSubmissionForCourseRecord(currentExercise, getCurrentUser(),
							courseRecord, courseOffer == null, false);
				} catch (SubmissionException e) {
					addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "exercisePlayer.pastDeadlineMessage");
					clearCurrentExercise();
					return;
				}
			}

			exercisePlayer.initPlayer(currentSubmission);
		}

		if (courseOffer != null) {
			exercisePlayer.setShowFeedback(courseOffer.isShowFeedbackImmediately());
			exercisePlayer.setShowResult(courseOffer.isShowResultImmediately());
			exercisePlayer.setAllowsHints(courseOffer.isAllowHints());
			exercisePlayer.setAllowStudentComments(courseOffer.isAllowStudentComments());
		} else {
			// Default values
			exercisePlayer.setShowFeedback(true);
			exercisePlayer.setShowResult(true);
			exercisePlayer.setAllowsHints(true);
			exercisePlayer.setAllowStudentComments(true);
		}

		reloadSubmissionsCache();
		updateHyperlinkToExercise();
	}

	/**
	 * Loads the course overview if the user goes back and leaves the current exercise.
	 */
	public void clearCurrentExercise() {
		// Refresh courseRecord, since its result may have changed and would be overwritten without refresh
		courseRecord = courseBusiness.getCourseRecordById(courseRecord.getId());

		if (!course.isFrozen()) {
			// If course is not frozen, we refresh the list of available exercises
			course = courseBusiness.getCourseWithLazyDataByCourseID(course.getId());
			loadExercisesFromCourse();
		}

		currentExercise = null;
		currentSubmission = null;
		courseRecord.setCurrentExercise(null);
		courseRecord = coursePlayerBusiness.updateCourseRecord(courseRecord);

		reloadSubmissionsCache();
	}

	public boolean isShowDifficulty() {
		return showDifficulty;
	}

	public void setShowDifficulty(boolean showDifficulty) {
		this.showDifficulty = showDifficulty;
	}

	public boolean isShowMyResult() {
		return showMyResult;
	}

	public void setShowMyResult(boolean showMyResult) {
		this.showMyResult = showMyResult;
	}

	public boolean isAllowCourseOfferRestart() {
		return allowCourseOfferRestart;
	}

	public void setAllowCourseOfferRestart(boolean allowCourseOfferRestart) {
		this.allowCourseOfferRestart = allowCourseOfferRestart;
	}

	public boolean isAllowExerciseRestart() {
		return allowExerciseRestart;
	}

	public void setAllowExerciseRestart(boolean allowExerciseRestart) {
		this.allowExerciseRestart = allowExerciseRestart;
	}

	public boolean isShowFeedbackImmediately() {
		return showFeedbackImmediately;
	}

	public void setShowFeedbackImmediately(boolean showFeedbackImmediately) {
		this.showFeedbackImmediately = showFeedbackImmediately;
	}

	public boolean isShowResultImmediately() {
		return showResultImmediately;
	}

	public void setShowResultImmediately(boolean showResultImmediately) {
		this.showResultImmediately = showResultImmediately;
	}

	public boolean isShowFeedbackInCourseResults() {
		return showFeedbackInCourseResults;
	}

	public void setShowFeedbackInCourseResults(boolean showFeedbackInCourseResults) {
		this.showFeedbackInCourseResults = showFeedbackInCourseResults;
	}

	public boolean isShowExerciseAndSubmissionInCourseResults() {
		return showExerciseAndSubmissionInCourseResults;
	}

	public void setShowExerciseAndSubmissionInCourseResults(boolean showExerciseAndSubmissionInCourseResults) {
		this.showExerciseAndSubmissionInCourseResults = showExerciseAndSubmissionInCourseResults;
	}

	public boolean isShowResultInCourseResults() {
		return showResultInCourseResults;
	}

	public void setShowResultInCourseResults(boolean showResultInCourseResults) {
		this.showResultInCourseResults = showResultInCourseResults;
	}

	public ECourseOfferReviewMode getReviewMode() {
		return reviewMode;
	}

	public void setReviewMode(ECourseOfferReviewMode reviewMode) {
		this.reviewMode = reviewMode;
	}

	public boolean isShowStatus() {
		return showStatus;
	}

	public void setShowStatus(boolean showStatus) {
		this.showStatus = showStatus;
	}

	public boolean isShowLeaveCourseOffer() {
		return showLeaveCourseOffer;
	}

	public void setShowLeaveCourseOffer(boolean showLeaveCourseOffer) {
		this.showLeaveCourseOffer = showLeaveCourseOffer;
	}

	public boolean isAllowExerciseRestart(AbstractExercise exercise) {
		if ((exercise == null) || !allowExerciseRestart) {
			return false;
		}

		if ((courseOffer == null) || (courseOffer.getMaxSubmissionsPerExercise() == 0)) {
			return true;
		}

		return cachedSubmissions.get(exercise).countAllSubmissions() < courseOffer.getMaxSubmissionsPerExercise();
	}

	public boolean isShowRestartCount() {
		return ((courseOffer != null) && (courseOffer.getMaxSubmissionsPerExercise() != 0));
	}

	public int getRemainingExerciseRestartCount(AbstractExercise exercise) {
		if ((exercise == null) || !allowExerciseRestart) {
			return 0;
		}

		if ((courseOffer == null) || (courseOffer.getMaxSubmissionsPerExercise() == 0)) {
			return Integer.MAX_VALUE;
		}

		return Math.max(0,
				courseOffer.getMaxSubmissionsPerExercise() - cachedSubmissions.get(exercise).countAllSubmissions());
	}

	public void restartExercise(AbstractExercise exercise) {

		if (courseRecord.isClosed()) {
			// Restarting an exercise is not allowed in a closed CourseRecord
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "coursePlayer.restartNotAllowed",
					"exercisePlayer.pastDeadlineMessage", (Object[]) null);
			return;
		}

		if (isAllowExerciseRestart(exercise)) {
			setCurrentExercise(exercise.getId());
			try {
				currentSubmission = exerciseBusiness.createSubmissionForCourseRecord(currentExercise, getCurrentUser(),
						courseRecord, (courseOffer == null), false);
			} catch (SubmissionException e) {
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, null, "coursePlayer.restartNotAllowed");
				reloadSubmissionsCache();
				clearCurrentExercise();
				return;
			}
			exercisePlayer.initPlayer(currentSubmission);
			//update total resultPoints
			courseRecord = coursePlayerBusiness.updateCourseResult(courseRecord);
			reloadSubmissionsCache();
		}
	}

	/**
	 * Lists all submissions that are not part of the latest processing or belong to exercises that are no longer
	 * available because they have been removed from the course.
	 *
	 * @return List with submissions, ordered by the start (DESC).
	 */
	public List<Submission> getOldSubmissionsList() {

		// Show old submissions only if user can always see the submissions
		// While testing the Course, courseOffer can be null
		if ((courseOffer != null) && (courseOffer.getReviewMode() != ECourseOfferReviewMode.ALWAYS)) {
			// All other modes other than ALWAYS define that there is no review in the current course before the
			// submission was finished.
			return new LinkedList<>();
		}

		return cachedSubmissions
				.values()
				.stream()
				.flatMap(cache -> cache.getOldSubmissions().stream())
				.sorted(Comparator.comparing(Submission::getId).reversed())
				.collect(Collectors.toList());
	}

	/**
	 * Calculates the last score for a given exercise.
	 *
	 * @param exercise
	 *            Exercise
	 * @return HTML String with the result points of the latest submission or a localized note that the exercise has not
	 *         yet been processed.
	 */
	public String getLastScoreForExercise(AbstractExercise exercise) {

		String result;
		Optional<Submission> lastSubmission = cachedSubmissions.get(exercise).getLastSubmission();

		if (lastSubmission.isPresent()) {
			result = Integer.toString(lastSubmission.get().getResultPoints()) + "%";
		} else {
			result = "<span title=\"" + getLocalizedMessage("coursePlayer.noSubmission") + "\">"
					+ getLocalizedMessage("global.notAvailable.short") + "</span>";
		}

		return result;
	}

	/**
	 * Calculates the best score for a given exercise.
	 *
	 * @param exercise
	 *            Exercise
	 * @return HTML String with the result points of the best submission or a localized note that the exercise has not yet
	 *         been processed.
	 */
	public String getBestScoreForExercise(AbstractExercise exercise) {

		String result;
		Optional<Submission> bestSubmission = cachedSubmissions.get(exercise).getBestSubmission();

		if (bestSubmission.isPresent()) {
			result = Integer.toString(bestSubmission.get().getResultPoints()) + "%";
		} else {
			result = "<span title=\"" + getLocalizedMessage("coursePlayer.noSubmission") + "\">"
					+ getLocalizedMessage("global.notAvailable.short") + "</span>";
		}

		return result;
	}

	/**
	 * Calculates the total score for this course record. For courses with a {@link FolderExerciseProvider}, only the
	 * stored score from {@link CourseRecord#getResultPoints()} is returned. For a {@link FixedListExerciseProvider},
	 * the returned value consists of the points sum, the maximum achievable score (sum of all exercise weights) and the
	 * resulting percentage.
	 *
	 * @return HTML String for use in the summary row.
	 */
	public String getTotalScore() {
		StringBuilder outputBuilder = new StringBuilder();

		if (course.getScoringMode() == ECourseScoring.BEST) {
			outputBuilder.append("<span title=\"");
			outputBuilder.append(getLocalizedMessage("coursePlayer.bestSubmissionTooltip"));
			outputBuilder.append("\">");
		}

		int totalPointsNormalized = courseRecord.getResultPoints();
		if (cachedExerciseWeights.isEmpty()) {
			// In a folder provider we can takeover the result points
			outputBuilder.append("<span style=\"font-size:x-large\">");
			outputBuilder.append(totalPointsNormalized);
			outputBuilder.append('%');
			outputBuilder.append("</span>");
		} else {
			// Calculate the sum of all submission points
			double totalPoints = cachedSubmissions
					.entrySet()
					.stream()
					.map(entry -> entry.getValue().getSubmission(course.getScoringMode()))
					.filter(Optional::isPresent)
					.map(Optional::get)
					.mapToDouble(submission -> ((double) submission.getResultPoints() / 100)
							* cachedExerciseWeights.get(submission.getExercise()))
					.sum();

			// In a fixed-list provider we must offset the result points with the exercise weight:
			int weightSum = cachedExerciseWeights.values().stream().mapToInt(value -> value).sum();
			outputBuilder.append("<span style=\"font-size:x-large\">");
			outputBuilder.append(totalPointsNormalized);
			outputBuilder.append("%");
			outputBuilder.append("</span><br />(");
			outputBuilder.append(formatLocalizedMessage("global.pointsOf",
					new Object[] { String.format("%.2f", totalPoints), weightSum }).replace(" ", "&nbsp;"));
			outputBuilder.append(")");
		}

		if (course.getScoringMode() == ECourseScoring.BEST) {
			outputBuilder.append("</span>");
		}

		return outputBuilder.toString();
	}

	/**
	 * Returns a shortened version of the score for a given exercise containing both best and latest result points.
	 *
	 * @param exercise
	 * @return HTML String with the current score or a localized note that the exercise has not yet been processed.
	 * @see #getLastScoreForExercise(AbstractExercise)
	 * @see #getBestScoreForExercise(AbstractExercise)
	 */
	public String getShortScoreForExercise(AbstractExercise exercise) {
		Optional<Submission> lastSubmission = cachedSubmissions.get(exercise).getLastSubmission();
		Optional<Submission> bestSubmission = cachedSubmissions.get(exercise).getBestSubmission();

		if (!lastSubmission.isPresent()) {
			// Not yet submitted
			return "<span title=\"" + getLocalizedMessage("coursePlayer.noSubmission") + "\">"
			+ getLocalizedMessage("global.notAvailable.short") + "</span>";
		}

		StringBuilder outputBuilder = new StringBuilder();

		// Add last submission score
		outputBuilder.append(lastSubmission.get().getResultPoints());
		outputBuilder.append('%');
		if (bestSubmission.isPresent() && (course.getScoringMode() == ECourseScoring.BEST)
				&& (bestSubmission.get().getResultPoints() > lastSubmission.get().getResultPoints())) {
			outputBuilder.append(" (");
			outputBuilder.append(bestSubmission.get().getResultPoints());
			outputBuilder.append("%)");
		}

		return outputBuilder.toString(); //
	}

	public String getCountdownString() {
		LocalDateTime deadline = courseRecord.getDeadline();
		LocalDateTime now = LocalDateTime.now();

		if ((deadline == null) || now.isAfter(deadline)) {
			return "";
		}

		LocalDateTime tempDateTime = LocalDateTime.from(now);
		StringBuilder result = new StringBuilder();

		ChronoUnit[] relevantUnits = { ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS,
				ChronoUnit.MINUTES, ChronoUnit.SECONDS };

		for (ChronoUnit unit : relevantUnits) {
			long diff = tempDateTime.until(deadline, unit);
			tempDateTime = tempDateTime.plus(diff, unit);
			if (diff > 0) {
				result.append('+');
				result.append(diff);
				switch (unit) {
				case YEARS:
					result.append('y');
					break;
				case MONTHS:
					result.append('o');
					break;
				case DAYS:
					result.append('d');
					break;
				case HOURS:
					result.append('h');
					break;
				case MINUTES:
					result.append('m');
					break;
				case SECONDS:
					result.append('s');
					break;
				default:
					break;
				}
				result.append(' ');
			}
		}

		return result.toString().strip();
	}

	public boolean isDeadlineExpired() {
		final LocalDateTime deadline = courseRecord.getDeadline();
		return (deadline != null) && deadline.isBefore(LocalDateTime.now());
	}

	public String getNewComment() {
		return newComment;
	}

	public void setNewComment(String newComment) {
		this.newComment = newComment;
	}

	public void addComment() {
		if (JackStringUtils.isNotBlank(newComment)) {
			currentSubmission = exerciseBusiness.refreshSubmissionFromDatabase(currentSubmission);
			currentSubmission = exerciseBusiness.addCommentToSubmission(currentSubmission, getCurrentUser(), newComment,
					emailVisible);
			newComment = null;
		}
	}

	public String getPublicDescriptionForExercise(AbstractExercise exercise) {
		return JackStringUtils.stripOrNull(exercise.getPublicDescription());
	}

	public boolean isCourseHasFixedListExerciseProvider() {
		return course.getContentProvider() instanceof FixedListExerciseProvider;
	}

	public boolean isCourseHasFolderExerciseProvider() {
		return course.getContentProvider() instanceof FolderExerciseProvider;
	}

	/**
	 * Returns the shown weigh for an exercise.
	 *
	 * @param e
	 * @return "(x points)" for fixed-list exercise providers, otherwise empty String.
	 */
	public String getShownExerciseWeights(AbstractExercise e) {
		Integer points = cachedExerciseWeights.get(e);
		if (points == null) {
			// No cached weight entry available
			return "";
		}

		return String.format(" (%s %s)", points, getLocalizedMessage(points == 1 ? "global.point" : "global.points"));
	}

	/**
	 * Returns the status for a submission
	 */
	public ESubmissionStatus getSubmissionStatus(AbstractExercise exercise) {
		final Optional<Submission> last = cachedSubmissions.get(exercise).getLastSubmission();
		if (!last.isPresent()) {
			return ESubmissionStatus.NOT_STARTED;
		}
		if (last.get().isCompleted()) {
			return ESubmissionStatus.COMPLETED;
		}
		if (exercisePlayerBusiness.isSubmissionUnprocessed(last.get())) {
			return ESubmissionStatus.STARTED_UNPROCESSED;
		}
		return ESubmissionStatus.PARTLY_PROCESSED;
	}

	/**
	 * Wether a submission exists for the given exercise.
	 */
	public boolean isSubmissionPresent(AbstractExercise exercise) {
		return cachedSubmissions.get(exercise).getLastSubmission().isPresent();
	}

	public boolean isEmailVisible() {
		return emailVisible;
	}

	public void setEmailVisible(boolean emailVisible) {
		this.emailVisible = emailVisible;
	}

	public void updateHyperlinkToExercise() {
		Builder viewIdBuilder = viewId.getQuickId().withParam("exerciseId", currentExercise.getId());
		hyperlinkToExercise = getServerUrl() + viewIdBuilder.toActionUrl();
	}

	public String getHyperlinkToExercise() {
		return hyperlinkToExercise;
	}

}
