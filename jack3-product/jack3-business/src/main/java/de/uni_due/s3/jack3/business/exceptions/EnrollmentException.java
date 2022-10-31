package de.uni_due.s3.jack3.business.exceptions;

/**
 * This Exception indicates that an enrollment action performed by a user is not allowed.
 */
public class EnrollmentException extends ActionNotAllowedException {

	private static final long serialVersionUID = -8392721582447103499L;

	private final EType type;

	public EnrollmentException(EType type) {
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
		
		/** The student is already registered in the same course or a linked one */
		ALREADY_ENROLLED,
		
		/** The student is not registered in the course, but a registration is required for the action */
		NOT_ENROLLED,
		
		/** There are free places left in the course */
		COURSE_NOT_FULL,

		/** There are no free places left in the course */
		COURSE_IS_FULL,
		
		/** The registration has not begun yet */
		ENROLLMENT_NOT_STARTED,
		
		/** The registration deadline is over */
		ENROLLMENT_DEADLINE_ELAPSED,
		
		/** Deadline for sign off is over */
		DISENROLLMENT_DEADLINE_ELAPSED,
		
		/** The waiting list is disabled */
		WAITINGLIST_DISABLED,

		/** The entered password is wrong */
		PASSWORD_WRONG,

		/** A user has no rights for the action */
		MISSING_RIGHT,

	}

}
