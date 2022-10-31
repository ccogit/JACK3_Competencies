package de.uni_due.s3.jack3.business.helpers;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Encapsulation class for exchanging information about an existing {@link Enrollment} of a student together with a
 * possibly existing open {@link CourseRecord}. Also provides some helper methods for time constraints and enrollment
 * states.
 * 
 * @author lukas.glaser
 */
public class CourseParticipation implements Serializable {

	private static final long serialVersionUID = -2231549009874890553L;

	public CourseParticipation(Enrollment enrollment) {
		this.enrollment = Objects.requireNonNull(enrollment);
		this.openCourseRecord = null;
	}

	public CourseParticipation(Enrollment enrollment, CourseRecord openCourseRecord) {
		this.enrollment = Objects.requireNonNull(enrollment);
		this.openCourseRecord = openCourseRecord;
	}

	private final Enrollment enrollment;
	private final CourseRecord openCourseRecord;

	@Nonnull
	public User getStudent() {
		return enrollment.getUser();
	}

	@Nonnull
	public Enrollment getEnrollment() {
		return enrollment;
	}

	@Nonnull
	public CourseOffer getCourseOffer() {
		return enrollment.getCourseOffer();
	}

	@Nonnull
	public String getCourseOfferName() {
		return enrollment.getCourseOffer().getName();
	}

	/**
	 * @return An open course record, if the user has one.
	 */
	@CheckForNull
	public CourseRecord getOpenCourseRecord() {
		return openCourseRecord;
	}

	/**
	 * @return When the course record started or {@code null} if the user has no open course record.
	 */
	@CheckForNull
	public LocalDateTime getStartTime() {
		return openCourseRecord == null ? null : openCourseRecord.getStartTime();
	}

	/**
	 * @return Deadline of the course record or {@code null} if the user has no open course record or the open course
	 *         record has no deadline.
	 */
	@CheckForNull
	public LocalDateTime getDeadline() {
		return openCourseRecord == null ? null : openCourseRecord.getDeadline();
	}

	/**
	 * @return When the course record was last visited by the user or {@code null} if the user has no open course
	 *         record.
	 */
	@CheckForNull
	public LocalDateTime getLastVisit() {
		return openCourseRecord == null ? null : openCourseRecord.getLastVisit();
	}

	public boolean isExplicitlyEnrolled() {
		return enrollment.getCourseOffer().isExplicitEnrollment()
				&& enrollment.getStatus() == EEnrollmentStatus.ENROLLED;
	}

	public boolean isOnWaitingList() {
		return enrollment.getCourseOffer().isExplicitEnrollment()
				&& enrollment.getStatus() == EEnrollmentStatus.ON_WAITINGLIST;
	}

	public boolean isExplicitlyDisenrolled() {
		return enrollment.getCourseOffer().isExplicitEnrollment()
				&& enrollment.getStatus() == EEnrollmentStatus.DISENROLLED;
	}

}
