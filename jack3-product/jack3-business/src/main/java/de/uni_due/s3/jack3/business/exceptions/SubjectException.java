package de.uni_due.s3.jack3.business.exceptions;

public class SubjectException extends ActionNotAllowedException {

	private static final long serialVersionUID = -6479624431697982026L;

	private final EType type;

	public SubjectException(EType type) {
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
		/** An Administrator tries to delete a Subject that is referenced by at least one Course or Exercise */
		SUBJECT_IS_REFERENCED,
		/** An Administrator tries to add a new Subject, but Subject with same name already exists */
		SUBJECT_ALREADY_EXISTS;
	}

}
