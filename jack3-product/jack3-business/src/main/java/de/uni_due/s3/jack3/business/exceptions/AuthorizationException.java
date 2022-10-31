package de.uni_due.s3.jack3.business.exceptions;

import de.uni_due.s3.jack3.entities.AccessRight;

public class AuthorizationException extends ActionNotAllowedException {

	private static final long serialVersionUID = -5009170694330640708L;

	private final EType type;

	public AuthorizationException(EType type) {
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
		//TODO #585 Die ersten drei Gründe sollten in die DragDropException.
		/** A movement operation will change Rights */
		RIGHTS_WILL_CHANGE,
		/** User has no {@link AccessRight#WRITE} right on the drop target */
		DROP_TARGET_RIGHT_IS_NOT_WRITE,
		/** User has no {@link AccessRight#WRITE} right on the dragged element */
		DRAG_TARGET_RIGHT_IS_NOT_WRITE,
		//TODO #585 Dieser Grund kann durch eine generische ActionNotAllowedException ersetzt werden.
		/** Not sufficient Rights for an Action */
		INSUFFICIENT_RIGHT;
	}
}
