package de.uni_due.s3.jack3.business.exceptions;

public class CourseException extends ActionNotAllowedException {

	private static final long serialVersionUID = -761611004007192183L;

	private final EType type;

	public CourseException(EType type) {
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

	public enum EType {
		/** A User tries to delete a Course with student submissions */
		COURSE_NOT_EMPTY,
		/** A User tries to delete a Course that is referenced by at least one Course Offer */
		COURSE_IS_REFERENCED;
	}

}
