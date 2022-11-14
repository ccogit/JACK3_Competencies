package de.uni_due.s3.jack3.business.exceptions;

public class CompetenceException extends ActionNotAllowedException {

	private static final long serialVersionUID = 3949806850877293395L;

	private final EType type;

	public CompetenceException(EType type) {
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
		/** An Administrator tries to delete a Competence that is referenced by at least one Course or Exercise */
		COMPETENCE_IS_REFERENCED,
		/** An Administrator tries to add a new Competence, but Competence with same name already exists for given subject */
		COMPETENCE_ALREADY_EXISTS;
	}

}
