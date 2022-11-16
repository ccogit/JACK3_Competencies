package de.uni_due.s3.jack3.business.exceptions;

public class CompetenceGoalException extends ActionNotAllowedException {

	private static final long serialVersionUID = 3949806850877293395L;

	private final EType type;

	public CompetenceGoalException(EType type) {
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
		/** An Editor tries to add a new competenceGoal, but competenceGoal referencing the same competence
		 *  already exists for given course or exercise */
		COMPETENCEGOAL_ALREADY_EXISTS;
	}

}
