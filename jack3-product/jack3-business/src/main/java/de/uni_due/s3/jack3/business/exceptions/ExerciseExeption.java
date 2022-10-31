package de.uni_due.s3.jack3.business.exceptions;

public class ExerciseExeption extends ActionNotAllowedException {

	private static final long serialVersionUID = 255038710801556547L;

	private final EType type;
	
	public ExerciseExeption(EType type) {
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
		/** A User tries to delete an Exercise with student submissions */
		EXERCISE_HAS_SUBMISSION,
		/** A User tries to delete an Exercise that is used by at least one Course with a fixed Exercise Provider */
		EXERCISE_IS_REFERENCED;
	}
	
}
