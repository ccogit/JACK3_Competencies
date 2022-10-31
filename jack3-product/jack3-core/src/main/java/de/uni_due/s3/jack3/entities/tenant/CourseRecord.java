package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.TestableSubmission;
import de.uni_due.s3.jack3.services.RevisionService;

/**
 * <p>
 * This class represents a course participation where a student can submit exercises. It saves the {@link User} whose
 * participation it is, the {@link CourseOffer} (optional), the {@link Course} for this participation and all other
 * things that belong to the participation.
 * </p>
 * <p>
 * A course record is not deleted after completion of the participation. Instead, in case of a completed or cancelled
 * participation we talk about <em>closed</em> course records. To determine whether a CourseRecord is closed, the
 * following attributes must be used:
 * </p>
 * <ul>
 * <li>{@link #endTime} and {@link #manuallyClosed} indicate that a course record was closed at a certain point of time
 * by the participant or an authorized user.</li>
 * <li>{@link #automaticallyClosed} if the closing was not initiated by a user but by the system.</li>
 * <li>Each course record has an individual {@link #individualDeadline}. After that deadline, a course record is closed
 * and the student is no longer allowed to make any submissions.</li>
 * <li>Also check the {@link #courseOffer} because it defines a global visibility end time and a global deadline.</li>
 * </ul>
 * <p>
 * Therefore, database queries excluding/including closed course records depend on these attributes. These attributes
 * are set depending the reason for closing the course record. <strong>There is no single attribute, which definitely
 * indicates whether a course record is closed or open.</strong>
 * </p>
 * <p>
 * There is the possibility to open a course record without a course offer. In this case it is a test record.
 * </p>
 */
// NOTE: For queries that consider frozen courses, we need a sub-query to retrieve the IDs of the frozen courses:
// "SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId" (see QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@Audited
@NamedQuery(
	name = CourseRecord.BY_ID_WITH_EXERCISES,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "LEFT JOIN FETCH cr.exercises "
			+ "WHERE cr.id = :id")
@NamedQuery( //
	name = CourseRecord.ALL_COURSERECORDS_FOR_COURSE_ORDERBY_STARTTIME, //
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.course = :course " //
			+ "ORDER BY cr.startTime DESC")
@NamedQuery( //
	name = CourseRecord.ALL_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS_ORDERBY_STARTTIME, //
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS //
			+ "ORDER BY cr.startTime DESC")
@NamedQuery( //
	name = CourseRecord.ALL_COURSERECORDS_FOR_COURSEOFFER_ORDERBY_STARTTIME, //
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer " //
			+ "ORDER BY cr.startTime DESC")
@NamedQuery( //
	name = CourseRecord.ALL_COURSERECORDS_FOR_COURSEOFFERS_ORDERBY_STARTTIME, //
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer IN (:courseOffers) " //
			+ "ORDER BY cr.startTime DESC")
@NamedQuery( //
	name = CourseRecord.OPEN_COURSERECORDS_FOR_USER_ORDERBY_COURSEOFFER, //
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.user = :user " //
			+ "AND cr.isTestSubmission IS FALSE " //
			+ "AND " + CourseRecord.QUERY_OPEN_COURSERECORD //
			+ "ORDER BY cr.courseOffer.name ASC")
@NamedQuery( //
	name = CourseRecord.COUNT_COURSERECORDS_FOR_COURSEOFFER_AND_USER, //
	query = "SELECT COUNT(cr) FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer "
			+ "AND cr.isTestSubmission IS FALSE "
			+ "AND cr.user = :user")
@NamedQuery(
	name = CourseRecord.CLOSED_COURSERECORDS_FOR_USER_COURSEOFFER_ORDERBY_STARTTIME,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer " //
			+ "AND cr.user = :user " //
			+ "AND NOT " + CourseRecord.QUERY_OPEN_COURSERECORD //
			+ "ORDER BY cr.startTime DESC")
@NamedQuery(
	name = CourseRecord.OPEN_COURSERECORD_FOR_USER_AND_COURSEOFFER,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer " //
			+ "AND cr.user = :user " //
			+ "AND " + CourseRecord.QUERY_OPEN_COURSERECORD)
@NamedQuery(
	name = CourseRecord.ALL_COURSERECORDS_FOR_COURSE_AND_FROZEN_COURSES,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.course.id = :courseId " //
			+ "OR cr.course.id IN (SELECT c.id FROM FrozenCourse c WHERE c.proxiedCourseId = :courseId)")
@NamedQuery(
	name = CourseRecord.ALL_COURSERECORDS_FOR_USER,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE cr.user = :user ")
@NamedQuery(
	name = CourseRecord.ALL_EMPTY_COURSERECORDS_FOR_USER,
	query = "SELECT cr FROM CourseRecord cr " //
			+ "WHERE  cr.user = :user " //
			+ "AND NOT EXISTS" //
			+ "(SELECT s FROM Submission s WHERE s.courseRecord = cr) ")
@NamedQuery(
	name = CourseRecord.COUNT_COURSERECORDS_FOR_COURSEOFFER,
	query = "SELECT COUNT(cr) FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer")
// For aggregate functions (except COUNT) we need COALESCE, otherwise null will be returned in case of empty list
@NamedQuery(
	name = CourseRecord.AVG_SCORE_FOR_COURSEOFFER,
	query = "SELECT COALESCE(avg(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer")
@NamedQuery(
	name = CourseRecord.HIGHEST_SCORE_FOR_COURSEOFFER,
	query = "SELECT COALESCE(max(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer")
@NamedQuery(
	name = CourseRecord.LOWEST_SCORE_FOR_COURSEOFFER,
	query = "SELECT COALESCE(min(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.courseOffer = :courseOffer")
@NamedQuery(
	name = CourseRecord.COUNT_NONTESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COUNT(cr) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission IS FALSE "
			+ "AND " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@NamedQuery(
	name = CourseRecord.COUNT_TESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COUNT(cr) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission IS TRUE "
			+ "AND " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@NamedQuery(
	name = CourseRecord.AVG_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COALESCE(avg(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission IS FALSE "
			+ "AND " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@NamedQuery(
	name = CourseRecord.HIGHEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COALESCE(max(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission IS FALSE "
			+ "AND " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@NamedQuery(
	name = CourseRecord.LOWEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COALESCE(min(cr.resultPoints),0) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission IS FALSE "
			+ "AND " + CourseRecord.QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@Entity
public class CourseRecord extends AbstractEntity implements TestableSubmission {

	private static final long serialVersionUID = -6660650217749844324L;

	// #########################################################################
	// Queries
	// #########################################################################

	public static final String BY_ID_WITH_EXERCISES = "CourseRecord.byIdWithExercises";
	public static final String ALL_COURSERECORDS_FOR_COURSE_ORDERBY_STARTTIME = "CourseRecord.allCourseRecordsForCourseOrderByStartTime";
	public static final String ALL_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS_ORDERBY_STARTTIME = "CourseRecord.allCourseRecordsForCourseIncludingFrozenRevisionsOrderByStartTime";
	public static final String ALL_COURSERECORDS_FOR_COURSEOFFER_ORDERBY_STARTTIME = "CourseRecord.allCourseRecordsForCourseOfferOrderByStartTime";
	public static final String ALL_COURSERECORDS_FOR_COURSEOFFERS_ORDERBY_STARTTIME = "CourseRecord.allCourseRecordsForCourseOffersOrderByStartTime";
	public static final String COUNT_COURSERECORDS_FOR_COURSEOFFER_AND_USER = "CourseRecord.countCourseRecordsForCourseOfferAndUserIgnoringTestSubmissions";
	public static final String OPEN_COURSERECORDS_FOR_USER_ORDERBY_COURSEOFFER = "CourseRecord.openCourseRecordsForUserOrderByCourseOffer";
	public static final String OPEN_COURSERECORD_FOR_USER_AND_COURSEOFFER = "CourseRecord.openCourseRecordForUserAndCourseOffer";
	public static final String CLOSED_COURSERECORDS_FOR_USER_COURSEOFFER_ORDERBY_STARTTIME = "CourseRecord.closedCourseRecordsForUserAndCourseOfferOrderByStartTime";
	public static final String ALL_COURSERECORDS_FOR_COURSE_AND_FROZEN_COURSES = "CourseRecord.allCourseRecordsForCourseAndFrozenCourses";
	public static final String ALL_COURSERECORDS_FOR_USER = "CourseRecord.allCourseRecordsForUser";
	public static final String ALL_EMPTY_COURSERECORDS_FOR_USER = "CourseRecord.allEmptyCourseRecordsForUser";
	// Aggregate queries for course offers
	public static final String COUNT_COURSERECORDS_FOR_COURSEOFFER = "CourseRecord.countNonTestingCourseRecordsForCourseOffer";
	public static final String AVG_SCORE_FOR_COURSEOFFER = "CourseRecord.averageScoreForCourseOffer";
	public static final String HIGHEST_SCORE_FOR_COURSEOFFER = "CourseRecord.highestScoreForCourseOffer";
	public static final String LOWEST_SCORE_FOR_COURSEOFFER = "CourseRecord.lowestScoreForCourseOffer";

	// Aggregate queries for courses
	// NOTE: In the queries we pass a list of course IDs that is retrieved from the query
	// 'FrozenCourse.FROZEN_COURSE_IDS_FOR_COURSE' because we have to include all frozen courses
	public static final String COUNT_NONTESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "CourseRecord.countNonTestingCourseRecordsForCourseIncludingFrozenRevisions";
	public static final String COUNT_TESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "CourseRecord.countTestingCourseRecordsForCourseIncludingFrozenRevisions";
	public static final String AVG_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS = "CourseRecord.averageNonTestingScoreForCourseIncludingFrozenRevisions";
	public static final String HIGHEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS = "CourseRecord.highestNonTestingScoreForCourseIncludingFrozenRevisions";
	public static final String LOWEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS = "CourseRecord.lowestNonTestingScoreForCourseIncludingFrozenRevisions";

	// Course record has not been closed and has no deadline or deadline is not over
	public static final String QUERY_OPEN_COURSERECORD =
			// not manually or automatically exited
			"((cr.endTime IS NULL) "

					// not after visibility end time
					+ "AND (cr.courseOffer.visibilityEndTime IS NULL OR current_timestamp() < cr.courseOffer.visibilityEndTime) "

					// not after submission deadline
					+ "AND (cr.courseOffer.submissionDeadline IS NULL OR current_timestamp() < cr.courseOffer.submissionDeadline) "

					// not after individual deadline
					+ "AND (cr.individualDeadline IS NULL OR current_timestamp() < cr.individualDeadline)) ";

	// Condition that matches course records for the (given) course id, including frozen courses for this course
	// The sub-query needs "cr" (CourseRecord) and the parameter "courseId"
	public static final String QUERY_MATCH_COURSE_AND_FROZENREVISIONS = "(cr.course.id = :courseId OR cr.course.id IN (SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId)) ";

	// #########################################################################
	// Details about the participation
	// #########################################################################

	/** The participant */
	@ToString
	@ManyToOne(optional = false)
	private User user;

	/** The <strong>optional</strong> corresponding course offer. */
	@ToString
	@ManyToOne(optional = true)
	private CourseOffer courseOffer;

	/**
	 * The corresponding course that saves the exercises for this provider. A course record <strong>must</strong> have a
	 * course!
	 */
	@ManyToOne(optional = false)
	private AbstractCourse course;

	/** The revision ID of the corresponding {@link #course}. */
	@Column(nullable = false)
	private int courseRevisionId;

	/** If the course record is not a real participation but a test record by a lecturer. */
	@Column(nullable = false)
	private boolean isTestSubmission;

	/** The ID of the exercise currently displayed in the CourseRecord. 0 in case there is no exercise active. */
	@Column(nullable = false)
	private long currentExerciseId;

	/** How many points the student currently earns for the exercises of the course. Depends on the course modus. */
	@Column(nullable = false)
	private int resultPoints;

	/**
	 * A feedback that is shown to the participant after finishing the course. The feedback text with the condition is
	 * specified at {@link AbstractCourse#resultFeedbackMappings}.
	 */
	@Column
	@Type(type = "text")
	private String courseFeedback;

	/** Comments of the participant for this course record. */
	@OneToMany(cascade = CascadeType.ALL)
	// REVIEW lg - unbenutzt: Brauchen wir hier überhaupt eine Möglichkeit für Kommentare? Soweit ich das sehe, ist die
	// Kommentar-Funktionalität in "Submission" umgesetzt.
	private List<Comment> studentComments = new ArrayList<>();

	/**
	 * List of all exercises of the course record, regardless of wether they have been submitted, not submitted or have
	 * been submitted several times.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	private Set<AbstractExercise> exercises = new HashSet<>();

	// #########################################################################
	// Timestamps
	// #########################################################################

	/** Actual start of working on a course. This corresponds to the time of creation. */
	@Column(nullable = false)
	private LocalDateTime startTime;

	/**
	 * Actual end of working on a course or null if user has not explicitly left the course. After this timestamp, the
	 * course record is closed.
	 */
	@Column
	private LocalDateTime endTime;

	/**
	 * Inividual deadline regarding the course offer's time limit or null for no time limit. <strong>Note: {@code NULL}
	 * does not mean that there is no deadline. It only indicates the <em>individual</em> deadline (start time + time
	 * limit)!</strong>
	 */
	@Column
	private LocalDateTime individualDeadline;

	/** When the course record was last accessed by the participant. */
	@Column
	private LocalDateTime lastVisit;

	// #########################################################################
	// Details about opening / closing
	// #########################################################################

	/** Who has closed the course record (if manually closed by a lecturer) */
	@ManyToOne
	private User closedByLecturer;

	/** An optional explanation for closing (if manually closed by a lecturer) */
	@Column
	@Type(type = "text")
	private String closedByLecturerExplanation;

	/** If the course record was manually closed at a certain point of time. */
	@Column(nullable = false)
	private boolean manuallyClosed;

	/** If the system has closed the course record at a certain point of time. */
	@Column(nullable = false)
	private boolean automaticallyClosed;

	// #########################################################################
	// Transient attributes
	// #########################################################################

	@Transient
	private long commentsOnSubmissions;

	@Transient
	private long unreadCommentsOnSubmissions;

	// #########################################################################
	// Constructors
	// #########################################################################

	public CourseRecord() {
		// Empty constructor for Hibernate
	}

	/**
	 * Opens a new regular course record.
	 * 
	 * @param user
	 *            The participant
	 * @param courseOffer
	 *            The course offer
	 * @param course
	 *            The course
	 */
	public CourseRecord(User user, CourseOffer courseOffer, AbstractCourse course) {
		this.user = Objects.requireNonNull(user);
		this.courseOffer = Objects.requireNonNull(courseOffer);
		this.course = Objects.requireNonNull(course);

		RevisionService revisionService = CDI.current().select(RevisionService.class).get();

		courseRevisionId = revisionService.getProxiedOrLastPersistedRevisionId(course);
		startTime = LocalDateTime.now();
		if (courseOffer.getTimeLimit() != null && !courseOffer.getTimeLimit().isZero()) {
			// Calculate the user's individual deadline (start time + time limit) and save it
			individualDeadline = startTime.plus(courseOffer.getTimeLimit());
		}
		isTestSubmission = false;
	}

	/**
	 * Opens a new test record.
	 * 
	 * @param user
	 *            The participant
	 * @param course
	 *            The course
	 */
	public CourseRecord(User user, AbstractCourse course) {
		this.user = Objects.requireNonNull(user);
		this.course = Objects.requireNonNull(course);

		RevisionService revisionService = CDI.current().select(RevisionService.class).get();
		courseRevisionId = revisionService.getProxiedOrLastPersistedRevisionId(course);
		startTime = LocalDateTime.now();
		isTestSubmission = true;
	}

	// #########################################################################
	// Getters & Setters
	// #########################################################################

	public User getUser() {
		return user;
	}

	@Override
	@Nonnull
	public Optional<CourseOffer> getCourseOffer() {
		return Optional.ofNullable(courseOffer);
	}

	public AbstractCourse getCourse() {
		return course;
	}

	public int getCourseRevisionId() {
		return courseRevisionId;
	}

	@Override
	public boolean isTestSubmission() {
		return isTestSubmission;
	}

	public long getCurrentExerciseId() {
		return currentExerciseId;
	}

	public void setCurrentExercise(AbstractExercise currentExercise) {
		this.currentExerciseId = currentExercise == null ? 0 : currentExercise.getId();
	}

	public int getResultPoints() {
		return resultPoints;
	}

	public void setResultPoints(int resultPoints) {
		this.resultPoints = resultPoints;
	}

	public String getCourseFeedback() {
		return courseFeedback;
	}

	public void setCourseFeedback(String courseFeedback) {
		this.courseFeedback = courseFeedback;
	}

	public List<Comment> getStudentComments() {
		return studentComments;
	}

	public void addStudentComment(Comment studentComment) {
		studentComments.add(studentComment);
	}

	public Set<AbstractExercise> getExercises() {
		return exercises;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public LocalDateTime getLastVisit() {
		return lastVisit;
	}

	public void setLastVisit(LocalDateTime lastVisit) {
		this.lastVisit = lastVisit;
	}

	// https://vladmihalcea.com/the-best-way-to-map-a-java-1-8-optional-entity-attribute-with-jpa-and-hibernate/
	public Optional<User> getClosedByLecturer() {
		return Optional.ofNullable(closedByLecturer);
	}

	public String getClosedByLecturerExplanation() {
		return closedByLecturerExplanation;
	}

	public boolean isManuallyClosed() {
		return manuallyClosed;
	}

	public boolean isAutomaticallyClosed() {
		return automaticallyClosed;
	}

	public long getCommentsOnSubmissions() {
		return commentsOnSubmissions;
	}

	public void setCommentsOnSubmissions(long commentsOnSubmissions) {
		this.commentsOnSubmissions = commentsOnSubmissions;
	}

	public long getUnreadCommentsOnSubmissions() {
		return unreadCommentsOnSubmissions;
	}

	public void setUnreadCommentsOnSubmissions(long unreadCommentsOnSubmissions) {
		this.unreadCommentsOnSubmissions = unreadCommentsOnSubmissions;
	}

	// #########################################################################
	// Methods & computed values
	// #########################################################################

	/**
	 * Determines the dynamic deadline considering
	 * <ul>
	 * <li>course offer visibility end</li>
	 * <li>course offer submission deadline</li>
	 * <li>individual deadline (course record start time + course offer time limit)</li>
	 * </ul>
	 * 
	 * @return Deadline of this course record or <code>NULL</code> if the course record does not have one.
	 * @see #isClosed()
	 */
	@CheckForNull
	public LocalDateTime getDeadline() {

		if (courseOffer == null) {
			// Only look at individual deadline if the course record does not have a linked course offer i.e. it is a
			// test record
			return individualDeadline;
		}

		Set<LocalDateTime> values = new HashSet<>();
		if (courseOffer.getSubmissionDeadline() != null) {
			values.add(courseOffer.getSubmissionDeadline());
		}
		if (courseOffer.getVisibilityEndTime() != null) {
			values.add(courseOffer.getVisibilityEndTime());
		}
		if (individualDeadline != null) {
			values.add(individualDeadline);
		}

		return values.isEmpty() ? null : Collections.min(values);
	}

	/**
	 * Determines if the course record is exited.
	 * 
	 * @return TRUE if the course record was manually / automatically exited or the deadline is over.
	 * @see #getDeadline()
	 */
	public boolean isClosed() {
		// TODO course registration - Kann man hier auf die drei Abfragen verzichten und prüfen, ob eine Endzeit
		// eingetragen
		// ist? Genau dann müsste der CR ja geschlossen sein.
		if (isManuallyClosed() || isAutomaticallyClosed() || getClosedByLecturer().isPresent()) {
			// Manually / automatically closed
			return true;
		}

		final LocalDateTime dynamicDeadline = getDeadline();
		// Closed via deadline
		return dynamicDeadline != null && LocalDateTime.now().isAfter(dynamicDeadline);
	}
	
	public LocalDateTime getClosedTimestamp() {
		//if the courseRecord is not closed there is no end time
		if(!isClosed()) {
			return null;
		}
		
		//if the user was logged in when the courseRecor got closed we have the endtime and cansimply return this
		if(endTime != null) {
			return endTime;
		}
		
		//if the user was not logged in when the courseRecord got closed (for example because the time run out) we have to reconstruct the endtime 
		return getDeadline();
	}

	/**
	 * The user closes the course record manually.
	 */
	public void closeManually() {
		manuallyClosed = true;
		endTime = LocalDateTime.now();
	}

	/**
	 * A user closes the course record manually.
	 *
	 * @param closingUser
	 *            The user who closes the course record.
	 * @param explanation
	 *            Optional explanation for closing
	 */
	public void closeManually(User closingUser, String explanation) {
		closeManually();

		if (!closingUser.equals(this.user)) {
			// The course record was closed manually by a lecturer
			closedByLecturer = closingUser;
			this.closedByLecturerExplanation = explanation;
		}
	}

	/**
	 * The course record is closed automatically.
	 */
	public void closeAutomatically() {
		automaticallyClosed = true;
		endTime = LocalDateTime.now();
	}
	
	/**
	 * Artificial Key for grouping courseRecods in courseRecordDatatable.xhtml to distinguish frozen and non-frozen
	 * Revisions
	 * 
	 */
	public int getGroupKey() {
		if (course.isFrozen()) {
			return this.getCourseRevisionId() * 10 + 5;
		} else {
			return this.getCourseRevisionId() * 10;
		}
	}
}
