package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.comparators.TimeComparator;
import de.uni_due.s3.jack3.interfaces.TestableSubmission;
import de.uni_due.s3.jack3.services.RevisionService;

/**
 * Representation of a Submission. Containing only Meta-Data and referring to StageSubmission for concrete,
 * exercise-specific Submission.
 */
// NOTE: For queries that consider frozen courses, we need a sub-query to retrieve the IDs of the frozen courses:
// "SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId" (see QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@Audited
@NamedQuery(
		name = Submission.ALL_SUBMISSIONS_FOR_USER,
		query = "SELECT s FROM Submission s " //
		+ "WHERE s.author = :author")
@NamedQuery(
		name = Submission.SUBMISSIONS_WITH_COMMENTS_EAGER_BY_SUBMISSIONS_ID,
		query = "SELECT s FROM Submission s " //
		+ "LEFT JOIN FETCH s.comments " //
		+ "WHERE s.id = :id")
@NamedQuery(
		name = Submission.SUBMISSIONS_WITH_LAZY_DATA_BY_SUBMISSIONS_ID,
		query = "SELECT s FROM Submission s " //
		+ "LEFT JOIN FETCH s.attributes " //
		+ "LEFT JOIN FETCH s.submissionResources " //
		+ "LEFT JOIN FETCH s.comments " //
		+ "LEFT JOIN FETCH s.submissionLog " //
		+ "LEFT JOIN FETCH s.variableValues " //
		+ "WHERE s.id = :id")
@NamedQuery(
		name = Submission.COUNT_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE,
		query = "SELECT COUNT(s) " //
		+ "FROM Submission s " //
		+ "WHERE s.courseRecord=:courseRecord " //
		+ "AND s.exercise=:exercise")
@NamedQuery(
		name = Submission.ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_IN_DESCENDING_ORDER,
		query = "SELECT DISTINCT s FROM Submission s " //
		+ "LEFT JOIN FETCH s.attributes " //
		+ "LEFT JOIN FETCH s.submissionResources " //
		+ "LEFT JOIN FETCH s.comments " //
		+ "LEFT JOIN FETCH s.submissionLog " //
		+ "LEFT JOIN FETCH s.variableValues " //
		+ "WHERE s.courseRecord=:courseRecord " //
		+ "AND s.exercise=:exercise " //
		+ "ORDER BY s.id DESC")
@NamedQuery(
		name = Submission.ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_ORDERD_BY_RESULT_IN_DESCENDING_ORDER,
		query = "SELECT DISTINCT s FROM Submission s " //
		+ "LEFT JOIN FETCH s.attributes " //
		+ "LEFT JOIN FETCH s.submissionResources " //
		+ "LEFT JOIN FETCH s.comments " //
		+ "LEFT JOIN FETCH s.submissionLog " //
		+ "LEFT JOIN FETCH s.variableValues " //
		+ "WHERE s.courseRecord=:courseRecord " //
		+ "AND s.exercise=:exercise " //
		+ "ORDER BY s.resultPoints DESC")
@NamedQuery(
		name = Submission.ALL_SUBMISSIONS_FOR_COURSERECORD,
		query = "SELECT s FROM Submission s " //
		+ "WHERE s.courseRecord=:courseRecord " //
		+ "ORDER BY s.id DESC")
@NamedQuery(
		name = Submission.ALL_SUBMISSIONS_FOR_EXERCISE_AND_FROZEN_EXERCISES,
		query = "SELECT s FROM Submission s " //
		+ "WHERE s.exercise = :exercise " //
		+ "OR s.exercise IN (SELECT fe FROM FrozenExercise fe WHERE fe.proxiedExerciseId = :proxiedExerciseId)"
		+ "ORDER BY s.id DESC")
@NamedQuery(
		name = Submission.COUNT_NONTESTING_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS,
		query = "SELECT COUNT(s) FROM Submission s " //
		+ "WHERE s.isTestSubmission IS FALSE "
		+ "AND (s.exercise.id = :exerciseId OR s.exercise.id IN (SELECT fe.id FROM FrozenExercise fe WHERE fe.proxiedExerciseId = :exerciseId))")
@NamedQuery(
		name = Submission.COUNT_ALL_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS,
		query = "SELECT COUNT(s) FROM Submission s " //
		+ "WHERE (s.exercise.id = :exerciseId OR s.exercise.id IN (SELECT fe.id FROM FrozenExercise fe WHERE fe.proxiedExerciseId = :exerciseId))")
@NamedQuery(
		name = Submission.COUNT_SUBMISSIONS_FOR_COURSEOFFER,
		query = "SELECT COUNT(s) FROM Submission s " //
		+ "WHERE s.courseRecord.courseOffer = :courseOffer") // Also handles NULL for s.courseRecord correctly
@NamedQuery(
		name = Submission.COUNT_NONTESTING_SUBMISSIONS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
		query = "SELECT COUNT(s) FROM Submission s " //
		+ "WHERE s.isTestSubmission IS FALSE "
		// Also handles NULL for s.courseRecord correctly
		+ "AND (s.courseRecord.course.id = :courseId OR s.courseRecord.course.id IN (SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId))")
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Submission extends AbstractEntity implements TestableSubmission {

	private static final long serialVersionUID = 7694360018101221552L;

	/** Name of the query that returns all submissions for given user. */
	public static final String ALL_SUBMISSIONS_FOR_USER = "Submission.allSubmissionsForUser";

	/** Name of the query that loads the submission with lazy data by id. */
	public static final String SUBMISSIONS_WITH_LAZY_DATA_BY_SUBMISSIONS_ID = "Submission.submissionWithLazyDataBySubmissionId";

	/** Name of the query that loads the submission by id and only eagerly gets comments */
	public static final String SUBMISSIONS_WITH_COMMENTS_EAGER_BY_SUBMISSIONS_ID = "Submission.submissionWithCommentsEagerBySubmissionId";

	/** Name of the query that counts submissions for given course record and exercise. */
	public static final String COUNT_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE = "Submission.countSubmissionsForCourseRecordAndExercise";

	public static final String ALL_SUBMISSIONS_FOR_COURSERECORD = "Submission.allSubmissionsForCourseRecord";

	public static final String ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_IN_DESCENDING_ORDER = "Submission.latestSubmissionForCourseRecordAndExercise";

	public static final String ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_ORDERD_BY_RESULT_IN_DESCENDING_ORDER = "Submission.bestSubmissionForCourseRecordAndExercise";

	/** Name of the query that counts all submissions for a given course offer. */
	public static final String COUNT_SUBMISSIONS_FOR_COURSEOFFER = "Submission.countNontestingSubmissionsForCourseOffer";

	/** Name of the query that counts all non-testing submissions for the given course IDs. */
	public static final String COUNT_NONTESTING_SUBMISSIONS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "Submission.countNontestingSubmissionsForCourseIncludingFrozenRevisions";

	public static final String ALL_SUBMISSIONS_FOR_EXERCISE_AND_FROZEN_EXERCISES = "Submission.allSubmissionsForExerciseAndFrozenExercise";
	public static final String COUNT_NONTESTING_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS = "Submission.countNontestingSubmissionsForExerciseIncludingFrozenRevisions";
	public static final String COUNT_ALL_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS = "Submission.countAllSubmissionsForExerciseIncludingFrozenRevisions";

	@Column
	private LocalDateTime creationTimestamp;

	@ToString
	@Column(nullable = false)
	private int resultPoints = 0;

	/**
	 * Flag indicating whether the user has completed the exercise in this submission. The exercise is considered
	 * completed if a stage transition has been activated which produced no more stage to be answered.
	 */
	@ToString
	@Column
	private boolean isCompleted = false;

	@Column
	private boolean isTestSubmission = false;

	// TODO Belongs to a feature that is not implemented yet. If generated feedback is only visible to students after
	// manual release, this value stores if the submission was reviewed. The value is independent of whether a manual
	// feedback has been set.
	@Column
	@ColumnDefault(value = "false")
	private boolean reviewed = false;

	@ToString
	@Column
	private boolean hasPendingStageChecks = false;

	@ToString
	@Column
	private boolean hasInternalErrors = false;

	@ManyToOne(optional = false)
	private User author;

	/**
	 * The exercise this submission belongs to. The specific revision of this exercise that was presented to the user is
	 * stored in {@code shownExerciseRevisionId}.
	 */
	@ToString
	@ManyToOne(optional = false)
	private AbstractExercise exercise;

	/**
	 * The exercise revision that was shown to the user when this submission was created. Usually, this is also the
	 * revision that is used to check this submission for correctness.
	 */
	@Column
	private int shownExerciseRevisionId;

	/**
	 * The exercise revision that was used to re-check the correctness of this submission. Needs only to be set if this
	 * submission was checked with a different exercise revision than the one stored in {@code shownExerciseRevisionId}.
	 */
	@Column
	private long checkedExerciseRevisionId;

	/**
	 * The course record for the course that was active when this submission was created. May be {@literal null} if this
	 * submission was created by testing the exercise directly from the edit page.
	 */
	@ManyToOne(optional = true)
	private CourseRecord courseRecord;

	@OneToMany
	private Set<SubmissionLogEntry> submissionLog = new HashSet<>();

	@ManyToMany(targetEntity = VariableValue.class)
	@MapKeyColumn(length = 10485760)
	private Map<String, VariableValue> variableValues = new HashMap<>();

	// REVIEW lg - Dieses Attribut wird nicht benutzt, zusammen mit der Klasse "SubmissionAttribute"
	@OneToMany(cascade = CascadeType.ALL)
	private Set<SubmissionAttribute> attributes = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL)
	// REVIEW lg - Dieses Attribut wird nicht benutzt, zusammen mit der Klasse "SubmissionResource"
	private Set<SubmissionResource> submissionResources = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL)
	private Set<Comment> comments = new HashSet<>();

	public Submission() {
		super();
	}

	public Submission(User author, AbstractExercise shownExerciseRevision) {
		this(author, shownExerciseRevision, null, false);
	}

	public Submission(User author, AbstractExercise shownExerciseRevision, CourseRecord courseRecord,
			boolean isTestSubmission) {
		this.author = Objects.requireNonNull(author, "You must speficy an author.");
		exercise = Objects.requireNonNull(shownExerciseRevision, "You must specify an exercise revision.");
		RevisionService revisionService = CDI.current().select(RevisionService.class).get();
		shownExerciseRevisionId = revisionService.getProxiedOrLastPersistedRevisionId(shownExerciseRevision);
		this.courseRecord = courseRecord;
		this.isTestSubmission = isTestSubmission;
		creationTimestamp = LocalDateTime.now();
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
	}

	public int getShownExerciseRevisionId() {
		return shownExerciseRevisionId;
	}

	public void setShownExerciseRevisionId(int shownExerciseRevisionId) {
		this.shownExerciseRevisionId = shownExerciseRevisionId;
	}

	public long getCheckedExerciseRevisionId() {
		return checkedExerciseRevisionId;
	}

	public void setCheckedExerciseRevision(long checkedExerciseRevisionId) {
		this.checkedExerciseRevisionId = checkedExerciseRevisionId;
	}

	public CourseRecord getCourseRecord() {
		return courseRecord;
	}

	/** Abbreviation for getting the course offer for a submission */
	@Override
	@Nonnull
	public Optional<CourseOffer> getCourseOffer() {
		return Optional.ofNullable(courseRecord).flatMap(CourseRecord::getCourseOffer);
	}

	public void setCourseRecord(CourseRecord courseRecord) {
		this.courseRecord = courseRecord;
	}

	public Optional<VariableValue> getVariableValueForName(String name) {
		for (Entry<String, VariableValue> var : getVariableValues().entrySet()) {
			if (name.equals(var.getKey())) {
				return Optional.of(var.getValue());
			}
		}
		return Optional.empty();
	}

	/*
	 * @return unmodifiableMap of variable values
	 */
	public Map<String, VariableValue> getVariableValues() {
		return Collections.unmodifiableMap(variableValues);
	}

	public void addVariableValue(String name, VariableValue value) {
		variableValues.put(name, value);
	}

	/**
	 * Clears all variable values stored in this submission and replaces them by those given as a parameter.
	 */
	public void resetVariableValues(Map<String, VariableValue> newVariableValues) {
		variableValues.clear();
		variableValues.putAll(newVariableValues);
	}

	/*
	 * @return unmodifiableSet of attributes
	 */
	public Set<SubmissionAttribute> getAttributes() {
		return Collections.unmodifiableSet(attributes);
	}

	public void addAttribute(SubmissionAttribute attribute) {
		attributes.add(attribute);
	}

	/*
	 * @return unmodifiableSet of submissionResources
	 */
	public Set<SubmissionResource> getSubmissionResources() {
		return Collections.unmodifiableSet(submissionResources);
	}

	public void addSubmissionResource(SubmissionResource resource) {
		submissionResources.add(resource);
	}

	/*
	 * @return unmodifiableSet of comments
	 */
	public Set<Comment> getComments() {
		return Collections.unmodifiableSet(comments);
	}

	public void addComment(Comment comment) {
		comments.add(comment);
	}

	/*
	 * @return unmodifiableSet of submissionLog
	 */
	public Set<SubmissionLogEntry> getSubmissionLog() {
		return Collections.unmodifiableSet(submissionLog);
	}

	public void addSubmissionLogEntry(SubmissionLogEntry entry) {
		submissionLog.add(entry);
	}

	public LocalDateTime getCreationTimestamp() {
		return creationTimestamp;
	}

	public int getResultPoints() {
		return resultPoints;
	}

	public void setResultPoints(int resultPoints) {
		this.resultPoints = resultPoints;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public void setIsCompleted(boolean isCompleted) {
		this.isCompleted = isCompleted;
	}

	@Override
	public boolean isTestSubmission() {
		return isTestSubmission;
	}

	public void setIsTestSubmission(boolean isTestSubmission) {
		this.isTestSubmission = isTestSubmission;
	}

	public boolean isReviewed() {
		return reviewed;
	}

	public void setReviewed(boolean reviewed) {
		this.reviewed = reviewed;
	}

	public boolean hasPendingStageChecks() {
		return hasPendingStageChecks;
	}

	public boolean hasInternalErrors() {
		return hasInternalErrors;
	}

	public void setHasPendingStageChecks(boolean hasPendingStageChecks) {
		this.hasPendingStageChecks = hasPendingStageChecks;
	}

	public void setHasInternalErrors(boolean hasInternalErrors) {
		this.hasInternalErrors = hasInternalErrors;
	}

	public List<SubmissionLogEntry> getSubmissionLogAsSortedList() {
		final List<SubmissionLogEntry> list = new LinkedList<>(submissionLog);
		list.sort(new TimeComparator<>());
		return list;
	}

	public AbstractExercise getExercise() {
		return exercise;
	}

	public void setExercise(AbstractExercise exercise) {
		this.exercise = exercise;
	}

	public boolean hasComments() {
		return !comments.isEmpty();
	}

	public boolean hasUnreadComments() {
		return comments.stream().anyMatch(comment -> !comment.isRead());
	}

	public int getGroupKey() {
		if(exercise.isFrozen()) {
			return (getShownExerciseRevisionId()*10)+5;
		}
		return getShownExerciseRevisionId()*10;
	}
}
