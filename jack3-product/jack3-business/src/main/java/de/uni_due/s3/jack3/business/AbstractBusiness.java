package de.uni_due.s3.jack3.business;

import java.util.Objects;

import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.business.exceptions.WrongStageBusinessException;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * Provides methods common to all classes in the BusinessLogicLayer
 */
public abstract class AbstractBusiness {

	private final Logger logger;

	protected AbstractBusiness() {
		logger = LoggerProvider.get(getClass());
	}

	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Helper method to check whether a method is called with the correct sub-type of the Class. Throws a
	 * WrongStageBusinessException if this requirement is not met.
	 *
	 * @param expected
	 *            The expected class
	 * @param actual
	 *            The actual object to be checked
	 */
	protected void assureCorrectClassUsage(Class<? extends AbstractEntity> expected, AbstractEntity actual) {
		if (!expected.isInstance(actual)) {
			throw new WrongStageBusinessException(this.getClass(), expected);
		}
	}

	/**
	 * Throws a {@link NullPointerException} if the String {@code s} is {@code null} or an
	 * {@link IllegalArgumentException} if {@code s} is empty or consists solely of whitespaces. This method is intended
	 * for use in parameter validation.
	 * 
	 * @see String#isBlank()
	 */
	protected String requireIdentifier(final String string, final String message) {
		Objects.requireNonNull(string, message);

		if (string.isBlank()) {
			throw new IllegalArgumentException(message);
		}

		return string;
	}

}
