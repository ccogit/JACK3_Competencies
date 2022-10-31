package de.uni_due.s3.jack3.business.exceptions;

public class WrongStageBusinessException extends IllegalArgumentException {

	private static final long serialVersionUID = -5711221156290178773L;

	public WrongStageBusinessException(Class<?> which, Class<?> expected) {
		super(which.getName() + " must be used with " + expected.getName() + " instances only");
	}
}
