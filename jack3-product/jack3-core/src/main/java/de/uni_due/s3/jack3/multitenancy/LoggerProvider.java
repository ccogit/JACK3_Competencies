package de.uni_due.s3.jack3.multitenancy;

import org.jboss.logging.Logger;

/**
 * This utility class provides loggers on a per class and tenant instance.
 */
public final class LoggerProvider {

	private static final String SUFFIX = "@" + TenantIdentifier.get();

	/**
	 * Returns a logger for the given class. The logger's name is generated by appending the class'
	 * simple name to the current tenant's identifier separated by a slash.
	 * @param c The class the logger is requested for.
	 * @return A logger suitable for the provided class.
	 */
	public static final Logger get(final Class<?> c) {

		if (c.isAnonymousClass()) {
			return get(c.getEnclosingClass());
		}

		return Logger.getLogger(c.getName() + SUFFIX);
	}

	private LoggerProvider() {
		throw new AssertionError("This class shouldn\'t be instantiated.");
	}
}
