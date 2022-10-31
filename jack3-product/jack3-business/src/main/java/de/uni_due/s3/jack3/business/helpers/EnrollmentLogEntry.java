package de.uni_due.s3.jack3.business.helpers;

import java.io.Serializable;
import java.time.LocalDateTime;

import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Represents a single enrollment log entry for a user.
 * 
 * @see EnrollmentBusiness#getEnrollmentLog(User, de.uni_due.s3.jack3.entities.tenant.CourseOffer)
 */
public class EnrollmentLogEntry implements Serializable {

	private static final long serialVersionUID = -3434988075261773918L;

	public EnrollmentLogEntry(User user, EEnrollmentStatus status, LocalDateTime timestamp, User changedBy,
			String reason) {
		this.user = user;
		this.status = status;
		this.timestamp = timestamp;
		this.changedBy = changedBy;
		this.reason = reason;
	}

	private final User user;
	private final EEnrollmentStatus status;
	private final LocalDateTime timestamp;
	private final User changedBy;
	private final String reason;

	public User getUser() {
		return user;
	}

	public EEnrollmentStatus getStatus() {
		return status;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public User getChangedBy() {
		return changedBy;
	}

	public String getReason() {
		return reason;
	}

}
