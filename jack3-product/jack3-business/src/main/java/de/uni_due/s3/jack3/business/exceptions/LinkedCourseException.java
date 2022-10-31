package de.uni_due.s3.jack3.business.exceptions;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * Indicates that an action failed because one is already enrolled in a linked course.
 */
public class LinkedCourseException extends EnrollmentException {

	private static final long serialVersionUID = -3402481828681446895L;

	private final CourseOffer registeredIn;

	public LinkedCourseException(CourseOffer registeredIn) {
		super(EType.ALREADY_ENROLLED);
		this.registeredIn = registeredIn;
	}

	/**
	 * @return The linked course in which the user is already enrolled
	 */
	public CourseOffer getLinkedCourse() {
		return registeredIn;
	}

}
