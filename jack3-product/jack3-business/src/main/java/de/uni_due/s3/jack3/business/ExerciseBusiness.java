package de.uni_due.s3.jack3.business;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.ExerciseExeption;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException.EType;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.JSXGraph;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.Tag;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.exceptions.PreconditionException;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class ExerciseBusiness extends AbstractBusiness {

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private TagService tagService;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private FolderService folderService;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private RevisionService revisionService;

	@Inject
	private BaseService baseService;

	@Inject
	private CourseService courseService;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	// REVIEW lg - We shouldn't save any state in business classes
	Map<String, JSXGraph> jsxGraphMap = new HashMap<>();

	/**
	 * Deletes exercise and all related submissions and frozen revisions.
	 *
	 * @param exercise
	 * @throws ActionNotAllowedException
	 */
	public void deleteExercise(Exercise exercise, User user) throws ActionNotAllowedException {

		if (!authorizationBusiness.isAllowedToEditFolder(user, exercise.getFolder())) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}

		if (hasExerciseNormalSubmissions(exercise)) {
			throw new ExerciseExeption(ExerciseExeption.EType.EXERCISE_HAS_SUBMISSION);
		}

		if (hasCoursesWithExerciseProviderReferencing(exercise)) {
			throw new ExerciseExeption(ExerciseExeption.EType.EXERCISE_IS_REFERENCED);
		}

		// Remove all submissions before deleting the exercise
		deleteAllSubmissionsForExerciseAndFrozenVersions(exercise);
		// Get rid of all the exercise's frozen revisions.
		List<FrozenExercise> frozenrevisions = getFrozenRevisionsForExercise(exercise);
		for (FrozenExercise frozenversion : frozenrevisions) {
			exerciseService.deleteExercise(frozenversion);
		}
		exerciseService.deleteExercise(exercise);
	}

	/**
	 * Exercise is deletable by user if:
	 * <ul>
	 * <li>user has at least write-rights</li>
	 * <li>none or only testsubmissions</li>
	 * <li>no course references the exercise</li>
	 * </ul>
	 *
	 * @param exercise
	 *            for which should be checked, if it is deletable
	 * @param user
	 *            who wants to delete the exercise
	 * @return true when exercise is deletable by the user
	 */
	public boolean isExerciseDeletableByUser(Exercise exercise, User user) {
		return (authorizationBusiness.isAllowedToEditFolder(user, exercise.getFolder())
				&& !hasExerciseNormalSubmissions(exercise) && !hasCoursesReferencing(exercise));
	}

	/**
	 * Updates the folder pointer of the given exercise and triggers an update. This creates a new revision and returns
	 * it.
	 *
	 * @param exercise
	 * @param folder
	 * @return
	 * @throws ActionNotAllowedException
	 */
	public AbstractExercise moveExercise(Exercise exercise, ContentFolder folder, User user)
			throws ActionNotAllowedException {
		//refresh Entities
		exercise = exerciseService.getExerciseByIdWithLazyData(exercise.getId())
				.orElseThrow(IllegalArgumentException::new);
		ContentFolder oldParent = exercise.getFolder();
		oldParent = folderService.getContentFolderWithLazyData(oldParent);
		folder = folderService.getContentFolderWithLazyData(folder);

		authorizationBusiness.ensureIsAllowedToMoveElement(user, exercise.getFolder(), folder);

		oldParent.removeChildExercise(exercise);
		folder.addChildExercise(exercise);

		folderService.mergeContentFolder(oldParent);
		folderService.mergeContentFolder(folder);

		return updateExercise(exercise);
	}

	/**
	 * Checks if new revision for the given exercise entity is needed and then either overrides the current revision or
	 * creates a new one.
	 *
	 * @param exercise
	 * @return
	 */
	public AbstractExercise updateExercise(AbstractExercise exercise) {
		if (exercise.isFrozen()) {
			return exerciseService.mergeExercise(exercise);
		}

		Set<AbstractEntity> entitiesToRemoveBySaving = getListOfEntitiesToRemoveBySaving(exercise);

		AbstractExercise mergedExercise = exerciseService.mergeExercise(exercise);
		if (!entitiesToRemoveBySaving.isEmpty()) {
			exerciseService.removeEntitiesFromStageOrExerciseManually(entitiesToRemoveBySaving);
		}
		return mergedExercise;
	}

	/**
	 * Merges the state of the given exercise into the database.
	 *
	 * @throws ActionNotAllowedException
	 *             If the user does not have edit rights on the exercise's parent folder.
	 */
	public AbstractExercise updateExercise(AbstractExercise exercise, User actingUser)
			throws ActionNotAllowedException {

		// Since we can get a frozen exercise, we must lookup the real exercise from database because frozen revisions
		// don't have a folder
		Exercise realExercise = exerciseService.getExerciseById(exercise.getProxiedOrRegularExerciseId())
				.orElseThrow(() -> new PreconditionException("Proxied exercise was not found: " + exercise));

		if (!authorizationBusiness.isAllowedToEditFolder(actingUser, realExercise.getFolder())) {
			throw new ActionNotAllowedException();
		}

		return updateExercise(exercise);
	}

	/**
	 * Duplicates a Exercise with all settings.
	 *
	 * @param exercise
	 *            The exercise of which a copy is to be made
	 * @param targetFolder
	 *            The Parent Folder for the copy of the Exercise
	 * @param newName
	 *            The name for the copy
	 * @param user
	 *            The user that performs the action.
	 * @return New exercise
	 * @throws ActionNotAllowedException
	 *             If either the user is not allowed to read the Exercise configuration or the user is not allowed to
	 *             place something in the target folder.
	 */
	public Exercise duplicateExercise(Exercise exercise, ContentFolder targetFolder, String newName, User user)
			throws ActionNotAllowedException {
		if (!authorizationBusiness.isAllowedToReadFromFolder(user, exercise.getFolder())) {
			throw new ActionNotAllowedException("User not allowed to read Exercise");
		}
		if (!authorizationBusiness.isAllowedToEditFolder(user, targetFolder)) {
			throw new ActionNotAllowedException("User not allowed place duplicated Exercise in the target folder");
		}

		exercise = getExerciseWithLazyDataByExerciseId(exercise.getId());

		final Exercise copy = exercise.deepCopy();
		copy.setName(newName);

		targetFolder = folderService.getContentFolderWithLazyData(targetFolder);
		targetFolder.addChildExercise(copy);
		folderService.mergeContentFolder(targetFolder);
		exerciseService.persistExercise(copy);
		return copy;
	}

	private Set<AbstractEntity> getListOfEntitiesToRemoveBySaving(AbstractExercise exercise) {
		Set<AbstractEntity> entitiesToRemoveBySaving = new HashSet<>();
		for (Stage stage : exercise.getStages()) {
			entitiesToRemoveBySaving.addAll(stage.getListOfStageEntitiesToRemoveBySaving());
			stage.getListOfStageEntitiesToRemoveBySaving().clear();
		}

		entitiesToRemoveBySaving.addAll(exercise.getListOfExcerciseEntitiesToRemoveBySaving());
		exercise.getListOfExcerciseEntitiesToRemoveBySaving().clear();
		return entitiesToRemoveBySaving;
	}

	/**
	 * Returns true iff the given exercise already declares a variable with the given name.
	 *
	 * @param exercise
	 * @param variableName
	 * @return
	 */
	public boolean variableNameAlreadyExistsForThisExercise(AbstractExercise exercise, String variableName) {
		for (final VariableDeclaration vd : exercise.getVariableDeclarations()) {
			if (vd.getName().equals(variableName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fetches the tag for the given name from the database or creates a new one if none with this name exists. Adds the
	 * tag to the exercise.
	 *
	 * @param exercise
	 * @param newTagName
	 */
	public void addTagToExercise(AbstractExercise exercise, String newTagName) {

		if (JackStringUtils.isBlank(newTagName)) {
			throw new IllegalArgumentException("You must specify a non-emtpy tag name.");
		}

		exercise.addTag(tagService.getOrCreateByName(newTagName));
	}

	public boolean isExercisePointingToTagName(AbstractExercise exercise, String tagName) {
		return exercise.getTagsAsStrings().contains(tagName);
	}

	/**
	 * Removes the tag with the given name from the given exercise. Does not trigger the actual update in the database.
	 *
	 * @param exercise
	 * @param tagName
	 */
	public void removeTagFromExercise(AbstractExercise exercise, String tagName) {
		final Optional<Tag> tag = exercise.getTags().stream().filter(t -> t.getName().equals(tagName)).findFirst();

		if (tag.isPresent()) {
			exercise.removeTag(tag.get());
		}
	}

	/**
	 * Returns all exercises with a tag with the given name. Returns an empty list of no tag with this name exists.
	 *
	 * @param tagName
	 * @return
	 */
	public List<Exercise> getAllExercisesForTagName(String tagName) {
		Optional<Tag> tag = tagService.getTagByName(tagName);

		if (tag.isPresent()) {
			return exerciseService.getAllExercisesForThisTag(tag.get());
		}
		return new LinkedList<>();
	}

	/**
	 * Returns all exercises with a tag with the given name, where the given User has Reading Right. Returns an empty
	 * list of no tag with this name exists.
	 *
	 * @param tagName
	 * @param user
	 * @return
	 */
	public List<Exercise> getAllExercisesForTagNameWhereUserCanRead(String tagName, User user) {
		// Caches for whose Folders the User has at least read permission
		final Map<ContentFolder, Boolean> cachedRights = new HashMap<>();
		final List<Exercise> exercisesWithTag = new LinkedList<>(getAllExercisesForTagName(tagName));
		final ContentFolder personalFolder = user.getPersonalFolder();
		exercisesWithTag.removeIf(exercise -> {
			final ContentFolder folder = exercise.getFolder();

			if (folder.isChildOf(personalFolder)) {
				return false;
			}
			if (!cachedRights.containsKey(folder)) {
				cachedRights.put(folder, authorizationBusiness.isAllowedToReadFromFolder(user, folder));
			}
			return !cachedRights.get(folder);
		});
		return exercisesWithTag;
	}

	public void deleteAllTestSubmissionsForExercise(AbstractExercise exercise) {
		for (Submission submission : submissionService.getAllSubmissionsForExerciseAndFrozenVersions(exercise)) {
			if ((submission.getCourseRecord() == null) && submission.isTestSubmission()) {
				submissionService.deleteSubmissionAndDependentEntities(submission);
			}
		}
	}

	public void deleteAllSubmissionsForExerciseAndFrozenVersions(AbstractExercise exercise) {
		for (Submission submission : submissionService.getAllSubmissionsForExerciseAndFrozenVersions(exercise)) {
			submissionService.deleteSubmissionAndDependentEntities(submission);
		}
	}

	/**
	 * Get all direct child exercises for a content folder.
	 *
	 * @param folder
	 *            Folder containing the exercises to be listed.
	 * @return List with found exercises
	 */
	public List<AbstractExercise> getAllExercisesForContentFolder(ContentFolder folder) {
		return exerciseService.getAllExercisesForContentFolder(folder);
	}

	/**
	 * Get all child exercises for a content folder including exercises in sub folders.
	 *
	 * @param folder
	 *            Folder containing the exercises to be listed.
	 * @return List with found exercises
	 */
	public List<Exercise> getAllExercisesForContentFolderRecursive(ContentFolder folder) {
		List<ContentFolder> folders = folderBusiness.getAllChildContentFolders(folder, true);
		return exerciseService.getAllExercisesForContentFolderList(folders);
	}

	// ************************************
	// Delegate methods for exerciseService

	public Exercise createExercise(String name, User author, ContentFolder folder, String language)
			throws ActionNotAllowedException {
		folder = folderService.getContentFolderWithLazyData(folder);
		if (!authorizationBusiness.isAllowedToEditFolder(author, folder)) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}
		Exercise exercise = new Exercise(name, language);
		folder.addChildExercise(exercise);
		folderService.mergeContentFolder(folder);
		exerciseService.persistExercise(exercise);
		return exercise;
	}

	public List<Exercise> getAllExercisesForUser(User user) {
		return exerciseService.getAllExercisesForUser(user);
	}

	public List<String> getAllTagsAsStrings() {
		return tagService.getAllTagsAsStrings();
	}

	public List<String> getTagsForExerciseAsString(Exercise exercise) {
		return exerciseService.getTagsForExerciseAsString(exercise);
	}

	public Exercise getExerciseWithLazyDataByExerciseId(long exerciseID) {
		return exerciseService.getExerciseByIdWithLazyData(exerciseID).orElseThrow(NoSuchJackEntityException::new);
	}

	/**
	 * Returns the exercise for {@link Exercise} objects or the proxied exercise for {@link FrozenExercise} objects.
	 */
	public Exercise getNonFrozenExercise(AbstractExercise abstractExercise) {
		if (abstractExercise.isFrozen()) {
			return getExerciseById(abstractExercise.getProxiedOrRegularExerciseId()).orElseThrow();
		}
		return (Exercise) abstractExercise;
	}

	// ************************************
	// Delegate methods for submissionService

	/**
	 * Creates a new submission for an existing course record
	 *
	 * @throws SubmissionException
	 *             If the user has reached the limit of submissions or the course record is closed.
	 */
	public Submission createSubmissionForCourseRecord(AbstractExercise shownExerciseRevision, User author,
			CourseRecord courseRecord, boolean isTestSubmission, boolean forceRestart) throws SubmissionException {

		// Test submissions don't have a course offer and should not be checked
		if (!isTestSubmission) {

			final CourseRecord freshRecord = baseService.findById(CourseRecord.class, courseRecord.getId(), false)
					.orElseThrow(
							() -> new PreconditionException("Course record does not exist anymore: " + courseRecord));

			// Fetch and refresh the course offer
			CourseOffer courseOffer = freshRecord.getCourseOffer().orElseThrow(() -> new IllegalStateException(
					freshRecord + " is not a test submission and should be linked to a valid course offer!"));
			courseOffer = baseService.findById(CourseOffer.class, courseOffer.getId(), false)
					.orElseThrow(() -> new PreconditionException(
							"Course offer for the course record does not exist anymore: " + freshRecord));

			if (freshRecord.isClosed()) {
				// Students shouldn't be able to start a submission in a closed course record
				throw new SubmissionException(EType.NO_OPEN_COURSE_RECORD);
			}

			// If no restart is allowed, each student may only have one submission per an exercise ( 0 = unlimited )
			final int maxCount = courseOffer.isAllowExerciseRestart() ? courseOffer.getMaxSubmissionsPerExercise() : 1;
			final long actualCount = submissionService.countAllSubmissionsForCourseRecordAndExercise(freshRecord,
					shownExerciseRevision);

			// 0 means "no limit"
			if ((maxCount > 0) && (actualCount >= maxCount) && !forceRestart) {
				throw new SubmissionException(EType.SUBMISSION_LIMIT_REACHED);
			}
		}

		final Submission submission = new Submission(author, shownExerciseRevision, courseRecord, isTestSubmission);
		submissionService.persistSubmission(submission);
		return submission;
	}

	public Submission createSubmission(AbstractExercise shownExerciseRevision, User author, boolean isTestSubmission) {
		final Submission submission = new Submission(author, shownExerciseRevision);
		submission.setIsTestSubmission(isTestSubmission);
		submissionService.persistSubmission(submission);
		return submission;
	}

	public List<Submission> getAllSubmissionsForExerciseAndFrozenVersions(AbstractExercise exercise) {
		return submissionService.getAllSubmissionsForExerciseAndFrozenVersions(exercise);
	}

	public Optional<Submission> getSubmissionWithLazyDataBySubmissionId(long submissionID) {
		return submissionService.getSubmissionnWithLazyDataBySubmissionId(submissionID);
	}

	public Optional<Submission> getSubmissionWithLazyDataBySubmissionIdFromEnvers(long submissionId) {
		return submissionService.getSubmissionWithLazyDataBySubmissionIdFromEnvers(submissionId);
	}

	public Optional<Submission> getSubmissionWithCommentsEagerBySubmissionId(long submissionID) {
		return submissionService.getSubmissionWithCommentsEagerBySubmissionId(submissionID);
	}

	public Optional<Submission> getSubmissionBySubmissionId(long submissionID) {
		return baseService.findById(Submission.class, submissionID, false);
	}

	/*
	 * Looks up the given submission in the database and returns a freshly loaded copy from there including all lazy
	 * data.
	 * Returns the unchanged parameter otherwise.
	 */
	public Submission refreshSubmissionFromDatabase(Submission submission) {
		return submissionService.getSubmissionnWithLazyDataBySubmissionId(submission.getId()).orElse(submission);
	}

	/**
	 * @return A fresh stage submission based on the ID of the given submission, loaded from database.
	 */
	public StageSubmission getStageSubmissionWithoutLazyData(StageSubmission submission) {
		return baseService.findById(StageSubmission.class, submission.getId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	/**
	 * @return A fresh exercise submission based on the ID of the given submission, loaded from database.
	 */
	public Submission getSubmissionWithoutLazyData(Submission submission) {
		return baseService.findById(Submission.class, submission.getId(), false)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public StageSubmission getStageSubmissionWithLazyData(StageSubmission stagesubmission) {
		return stageSubmissionService.getStageSubmissionWithLazyData(stagesubmission.getId())
				.orElseThrow(VerifyException::new);
	}

	public StageSubmission getStageSubmissionWithLazyDataFromEnvers(StageSubmission stagesubmission) {
		return stageSubmissionService
				.getStageSubmissionWithLazyDataByStageSubmissionIDFromEnvers(stagesubmission.getId())
				.orElseThrow(VerifyException::new);
	}

	public List<Exercise> getAllExercisesForContentFolderList(List<ContentFolder> folderList) {
		return exerciseService.getAllExercisesForContentFolderList(folderList);
	}

	/**
	 * Gets the original author of an exercise (the user who created the exercise. Not the one who updated the
	 * exercise as last one)
	 *
	 * @param exercise
	 * @return
	 *         the name of the author
	 */
	public String getAuthorNameFromExericse(AbstractExercise exercise) {
		return exerciseService.getRevisionOfExercise(exercise, getRevisionNumbersFor(exercise).get(0)) //
				.orElseThrow(NoSuchJackEntityException::new) //
				.getUpdatedBy(); //
	}

	public Optional<AbstractExercise> getRevisionOfExercise(AbstractExercise exercise, int revisionNumber) {
		return exerciseService.getRevisionOfExercise(exercise, revisionNumber);
	}

	public AbstractExercise getRevisionOfExerciseWithLazyData(AbstractExercise exercise, int revisionNumber) {
		return exerciseService.getRevisionOfExerciseWithLazyData(exercise, revisionNumber)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public Optional<AbstractExercise> getRevisionOfExerciseWithLazyData(Long id, int revisionNumber) {
		return exerciseService.getRevisionOfExerciseWithLazyData(id, revisionNumber);
	}

	public AbstractExercise resetToRevision(Exercise newestExercise, int revisionIndex, User actingUser)
			throws ActionNotAllowedException {

		final ContentFolder folder = newestExercise.getFolder();
		if (!authorizationBusiness.isAllowedToEditFolder(actingUser, folder)) {
			throw new ActionNotAllowedException();
		}

		AbstractExercise exerciseAtRevision = exerciseService.resetToRevision(newestExercise, revisionIndex);
		exerciseAtRevision.setFolder(folder);
		exerciseService.mergeExercise(exerciseAtRevision);

		return exerciseAtRevision;
	}

	/**
	 * Returns wether a frozen exercise exists for the id of the proxied ("real") exercise and the revision ID.
	 */
	public boolean frozenExerciseExists(long proxiedExerciseId, int proxiedExerciseRevisionId) {
		return exerciseService.getFrozenRevisionForExercise(proxiedExerciseId, proxiedExerciseRevisionId).isPresent();
	}

	public void createFrozenExercise(AbstractExercise exercise, int revisionId) {
		// Call copy constructor
		FrozenExercise frozenExercise = new FrozenExercise((Exercise) exercise, revisionId);

		// Merge copy to get correct IDs for all copied elements (i.e. stages)
		frozenExercise = exerciseService.mergeFrozenExercise(frozenExercise);

		// Do post-processing that can only be done when all IDs are correct after the merge
		frozenExercise.generateSuffixWeights();

		// Merge for the second time
		frozenExercise = exerciseService.mergeFrozenExercise(frozenExercise);

		getLogger().debug("Frozen Revision \"" + frozenExercise + "\" of exercise successfully created!");
	}

	public List<FrozenExercise> getFrozenRevisionsForExercise(AbstractExercise exercise) {
		return exerciseService.getFrozenRevisionsForExercise(exercise);
	}

	public FrozenExercise getFrozenExerciseWithLazyDataById(long exerciseId) {
		return exerciseService.getFrozenExerciseWithLazyDataById(exerciseId)
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public List<Integer> getRevisionNumbersFor(AbstractExercise exercise) {
		return revisionService.getRevisionNumbersFor(exercise);
	}

	public Integer getNumberOfRevisions(AbstractExercise exercise) {
		return revisionService.getRevisionNumbersFor(exercise).size();
	}

	public int getProxiedOrLastPersistedRevisionId(AbstractExercise abstractExercise) {
		return revisionService.getProxiedOrLastPersistedRevisionId(abstractExercise);
	}

	public int getRevisionIndexForRevisionId(AbstractExercise tmpExercise, int revisionId) {
		List<Integer> revisions = getRevisionNumbersFor(tmpExercise);
		return revisions.indexOf(revisionId);
	}

	public Submission addCommentToSubmission(Submission submission, User author, String text, boolean emailVisible) {
		Comment comment = new Comment(author, text, emailVisible);
		baseService.persist(comment);
		submission.addComment(comment);

		return submissionService.mergeSubmission(submission);
	}

	public Comment updateComment(Comment comment) {
		return baseService.merge(comment);
	}

	/**
	 * Returns all revisions for an {@link Exercise} <strong>without</strong> lazy data.
	 */
	public List<Exercise> getAllRevisionsForExercise(Exercise exercise) {
		var revisions = revisionService.getAllRevisionsForEntity(exercise);
		revisions.forEach(exercize -> exercize.setFromEnvers(true));
		return revisions;
	}

	public List<Exercise> getFilteredRevisionsOfExercise(Exercise exercise, int first, int pageSize, String sortField,
			String sortOrderString) {
		var revisions = revisionService.getFilteredRevisionsOfEntity(exercise, first, pageSize, sortField,
				sortOrderString);
		revisions.forEach(exercize -> exercize.setFromEnvers(true));
		return revisions;
	}

	public Optional<Exercise> getExerciseById(long exerciseId) {
		return exerciseService.getExerciseById(exerciseId);
	}

	public void persistImportedExercise(Exercise exercise, ContentFolder folder) {
		exercise.setFolder(folder);
		exerciseService.persistExercise(exercise);
	}

	public Stage getStageRevisionForStageSubmissionByTimestamp(StageSubmission stageSubmission,
			LocalDateTime timestamp) {
		return revisionService.getRevisionOfEntityByTypeAndIdAndTimeStamp(Stage.class, stageSubmission.getStageId(),
				timestamp);
	}

	public Map<String, JSXGraph> getJsxGraphMap() {
		return jsxGraphMap;
	}

	public void setJsxGraphMap(Map<String, JSXGraph> jsxGraphMap) {
		this.jsxGraphMap = jsxGraphMap;
	}

	public Optional<FrozenExercise> getFrozenExerciseById(long id) {
		return baseService.findById(FrozenExercise.class, id, false);
	}

	/**
	 * Checks if the exercise has normal submissions(not only test submissions).
	 *
	 * @param exercise
	 * @return
	 */
	public boolean hasExerciseNormalSubmissions(AbstractExercise exercise) {
		List<Submission> submissions = getAllSubmissionsForExerciseAndFrozenVersions(exercise);
		for (Submission submission : submissions) {
			if (!submission.isTestSubmission()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the exercise has test-submissions which belong to a course.
	 *
	 * @param exercise
	 * @return true if at least one course test-submission exists
	 */
	public boolean hasExerciseCourseTestSubmissions(AbstractExercise exercise) {
		List<Submission> submissions = getAllSubmissionsForExerciseAndFrozenVersions(exercise);
		for (Submission submission : submissions) {
			if (submission.isTestSubmission() && (submission.getCourseRecord() != null)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if an course contains the exercise.
	 * WARNING: Only Courses with an ExerciseProvider attached are checked!
	 *
	 * @param exercise
	 * @return
	 */
	private boolean hasCoursesWithExerciseProviderReferencing(AbstractExercise exercise) {
		return !courseService.getAbstractCoursesReferencingExerciseExerciseProvider(exercise).isEmpty();
	}

	/**
	 * Check if an course (independent from provider) contains the exercise.
	 *
	 * @param exercise
	 * @return
	 */
	private boolean hasCoursesReferencing(AbstractExercise exercise) {
		return !courseBusiness.getAllCoursesContainingExercise(exercise).isEmpty();
	}

	/**
	 * Performs placeholder replacement on the contents of the exercise resource if necessary and returns the result as
	 * byte array.
	 */
	public byte[] getExerciseResourceContent(ExerciseResource exerciseResource, Submission submission,
			StageSubmission currentStageSubmission, Stage stage) {
		byte[] rawContent = exerciseResource.getContent();

		if (exerciseResource.isReplacePlaceholder()) {
			try {
				String contentString = new String(rawContent, "UTF-8");
				String replacedContentString = exercisePlayerBusiness.resolvePlaceholders(contentString, submission,
						currentStageSubmission, stage, false);
				rawContent = replacedContentString.getBytes("UTF-8");
			} catch (UnsupportedEncodingException uee) {
				getLogger().warn("Failed to replace variables in exerciseResource " + exerciseResource
						+ " due to UnsupportedEncodingException", uee);
			}
		}
		return rawContent;
	}

}
