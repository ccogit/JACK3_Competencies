package de.uni_due.s3.jack3.exceptions;

import de.uni_due.s3.jack3.entities.enums.EDeepCopyExceptionErrorCode;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * This exception indicates that an error occurred while deep dopying an entity.
 * 
 * @author Benjamin.Otto
 * @see DeepCopyable
 */
public class DeepCloningException extends RuntimeException {

	private static final long serialVersionUID = -5088480254886234059L;

	private final EDeepCopyExceptionErrorCode errorcode;

	public DeepCloningException(String message, EDeepCopyExceptionErrorCode errorcode) {
		super(message);
		this.errorcode = errorcode;
	}

	public EDeepCopyExceptionErrorCode getErrorcode() {
		return errorcode;
	}

}
