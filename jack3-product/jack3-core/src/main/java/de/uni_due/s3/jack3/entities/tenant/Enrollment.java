package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;

/**
 * <p>
 * This class stores the enrollment status of a student in a course (offer).
 * </p>
 * <p>
 * Enrollment objects will be not deleted after finishing or sign-off, only after deleting the linked course offer or
 * the user. Instead, the status is set to <em>disenrolled</em>. There may only be one enrollment object per user and
 * course offer at any time! Changes and different states can be tracked using Hibernate Envers.
 * </p>
 * 
 * @author lukas.glaser
 */
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_USER,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.user = :user "
			+ "ORDER BY e.lastChange DESC")
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_USER_AND_STATUS,
	query = "SELECT e FROM Enrollment e " 
			+ "WHERE e.user = :user " 
			+ "AND e.status = :status "
			+ "ORDER BY e.lastChange DESC")
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFER,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.courseOffer = :courseOffer "
			+ "ORDER BY e.user.loginName ASC")
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFERS,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.courseOffer IN (:courseOffers) "
			+ "ORDER BY e.user.loginName ASC")
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_USER_AND_FOLDERS_UNORDERED,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.user = :user "
			+ "AND e.courseOffer.folder IN (:folders)")
@NamedQuery(
	name = Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.courseOffer = :courseOffer "
			+ "AND e.status = :status "
			+ "ORDER BY e.user.loginName ASC")
@NamedQuery(
	name = Enrollment.COUNT_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS,
	query = "SELECT COUNT(e) FROM Enrollment e "
			+ "WHERE e.courseOffer = :courseOffer "
			+ "AND e.status = :status")
@NamedQuery( // This query also counts all participants for a course offer due to the unique constraint
	name = Enrollment.COUNT_ENROLLMENTS_FOR_COURSEOFFER,
	query = "SELECT COUNT(e) FROM Enrollment e "
			+ "WHERE e.courseOffer = :courseOffer")
@NamedQuery(
	name = Enrollment.ONE_ENROLLMENT_FOR_USER_AND_COURSEOFFER,
	query = "SELECT e FROM Enrollment e "
			+ "WHERE e.courseOffer = :courseOffer "
			+ "AND e.user = :user")
// Only one enrollment per user and course offer!
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "courseOffer_id" }))
@Entity
@Audited
public class Enrollment extends AbstractEntity {

	private static final long serialVersionUID = 5324481543855062471L;

	public static final String ALL_ENROLLMENTS_FOR_USER = "Enrollment.allEnrollmentsForUser";
	public static final String ALL_ENROLLMENTS_FOR_USER_AND_STATUS = "Enrollment.allEnrollmentsForUserAndStatus";
	public static final String ALL_ENROLLMENTS_FOR_COURSEOFFER = "Enrollment.allEnrollmentsForCourseOffer";
	public static final String ALL_ENROLLMENTS_FOR_COURSEOFFERS = "Enrollment.allEnrollmentsForCourseOffers";
	public static final String ALL_ENROLLMENTS_FOR_USER_AND_FOLDERS_UNORDERED = "Enrollment.allEnrollmentsForUserAndFoldersUnordered";
	public static final String ALL_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS = "Enrollment.allEnrollmentsForCourseOfferAndStatus";
	public static final String COUNT_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS = "Enrollment.countEnrollmentsForCourseOfferAndStatus";
	public static final String COUNT_ENROLLMENTS_FOR_COURSEOFFER = "Enrollment.countEnrollmentsForCourseOffer";
	public static final String ONE_ENROLLMENT_FOR_USER_AND_COURSEOFFER = "Enrollment.oneEnrollmentForUserAndCourseOffer";

	/** The participant */
	@ToString
	@ManyToOne(optional = false)
	private User user;

	/** The course offer */
	@ToString
	@ManyToOne(optional = false)
	private CourseOffer courseOffer;

	/**
	 * The enrollment status of the participant. The status can change over time, either by direct actions of a user or
	 * automatically by the system. Once an enrollment was created, the status must not become {@code null}.
	 */
	@ToString
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private EEnrollmentStatus status;

	/** When the last change to the status took place. */
	@Column(nullable = false)
	private LocalDateTime lastChange;

	/**
	 * Which user has changed the status by his action. Can be {@code null}, in this case the status was automatically
	 * updated by the system.
	 */
	@ManyToOne
	private User lastChangedBy;

	/** An optional explanation of the reason for the last status change. */
	@Column
	@Type(type = "text")
	private String explanation;

	public Enrollment() {
		// Empty constructor for Hibernate
	}

	/**
	 * Creates a new enrollment based on a user or a system action.
	 * 
	 * @param user
	 *            Participant (required)
	 * @param offer
	 *            Linked course offer (required)
	 * @param status
	 *            Enrollment status (required)
	 * @param createdBy
	 *            The user who performed the action or {@code NULL} if the system performed the action
	 * @param explanation
	 *            Optional explanation for the state change
	 */
	public Enrollment(User user, CourseOffer offer, EEnrollmentStatus status, User createdBy, String explanation) {
		this.user = Objects.requireNonNull(user);
		this.courseOffer = Objects.requireNonNull(offer);
		updateStatus(status, createdBy, explanation);
	}

	public User getUser() {
		return user;
	}

	public CourseOffer getCourseOffer() {
		return courseOffer;
	}

	public EEnrollmentStatus getStatus() {
		return status;
	}

	public LocalDateTime getLastChange() {
		return lastChange;
	}

	public User getLastChangedBy() {
		return lastChangedBy;
	}

	public String getExplanation() {
		return explanation;
	}

	/**
	 * Changes the enrollment status.
	 * 
	 * @param status
	 *            New enrollment status (required)
	 * @param changedBy
	 *            The user who performed the action or {@code NULL} if the system performed the action
	 * @param explanation
	 *            Optional explanation for the state change
	 */
	public void updateStatus(EEnrollmentStatus status, User changedBy, String explanation) {
		this.status = Objects.requireNonNull(status);
		this.lastChange = LocalDateTime.now();
		this.lastChangedBy = changedBy;
		this.explanation = explanation;
	}

}
