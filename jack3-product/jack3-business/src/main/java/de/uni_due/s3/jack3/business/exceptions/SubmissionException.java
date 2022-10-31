package de.uni_due.s3.jack3.business.exceptions;

/**
 * This Exception is thrown if a submission action performed by a user is not allowed.
 */
public class SubmissionException extends ActionNotAllowedException {

	private static final long serialVersionUID = 7554911785257637261L;

	private final EType type;

	public SubmissionException(EType type) {
		super();
		this.type = type;
	}

	public EType getType() {
		return type;
	}

	@Override
	public String getMessage() {
		return type == null ? null : type.toString();
	}

	/**
	 * Different error types
	 */
	public enum EType {

		/** The student has already reached the limit of submissions for an exercise */
		SUBMISSION_LIMIT_REACHED,

		/** The student has already attended the course and may not repeat it */
		ALREADY_PARTICIPATED,

		/** There is no course for performing this action */
		NO_COURSE,

		/** The user has no open course record */
		NO_OPEN_COURSE_RECORD,

		/** The user has an open course record */
		OPEN_COURSE_RECORD,

		/** Submission period has not begun yet */
		SUBMISSION_NOT_STARTED,

		/** Submission deadline is over */
		SUBMISSION_DEADLINE_ELAPSED,

		/** The student is not enrolled in the course, but an enrollment is required for the action */
		NOT_ENROLLED,

		/** The entered password is wrong */
		PASSWORD_WRONG

	}

}