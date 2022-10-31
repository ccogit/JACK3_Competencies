package de.uni_due.s3.jack3.multitenancy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.uni_due.s3.jack3.entities.tenant.LtiLaunch;

/**
 * This is a utility class that allows clients to obtain the name of the current tenant.
 * @author Bj&ouml;rn Zurmaar
 */
public final class TenantIdentifier
{
	private static final String JNDI_MODULE_NAME = "java:module/ModuleName";

	private static final String IDENTIFIER = lookupIdentifier();

	private static final String QUALIFIER = IDENTIFIER + "/";

	private static String lookupIdentifier() {
		try {
			final String moduleName = InitialContext.doLookup(JNDI_MODULE_NAME);
			return fromDeploymentName(moduleName);
		}
		catch (final NamingException e) {
			throw new AssertionError("Failed to lookup \"" + JNDI_MODULE_NAME + "\".");
		}
	}

	static final String fromDeploymentName(final String deploymentName) {
		final Pattern identifierPattern = Pattern.compile("([a-zA-Z0-9_-]+)-\\d+\\.\\d+\\.\\d+.*");
		final Matcher matcher = identifierPattern.matcher(deploymentName);

		if (matcher.matches())
			return matcher.group(1);

		return deploymentName;
	}

	private TenantIdentifier() {
		throw new AssertionError("You shouldn\'t instantiate this class.");
	}

	/**
	 * Returns the current tenant's identifier. It corresponds to the current module name
	 * stripped off any version information.
	 * @return The current tenant's identifier.
	 */
	public static final String get() {
		return IDENTIFIER;
	}

	/**
	 * Qualifies the given username by prepending it with the current tenant's identifier
	 * and a slash.
	 * @param userName The name to be qualified.
	 * @return The username including the tenant identifier and a slash.
	 */
	public static final String qualify(final String userName) {
		return QUALIFIER.concat(userName);
	}

	/**
	 * Qualifies the given username by
	 * <ol>
	 *   <li>prepending it with the current tenant's identifier and a slash.</li>
	 *   <li>appending the LTI launch identifier.</li>
	 * </ol>
	 * @param userName The name to be qualified.
	 * @param ltiLaunch The LTI launch the identifier should be taken from.
	 * @return The given username qualified with the tenant identifier and and the LTI launch id.
	 */
	public static final String qualify(final String userName,final LtiLaunch ltiLaunch) {
		return qualify(ltiLaunch.getLoginName());
	}

	/**
	 * Dequalifies the given username by stripping off the tenant identifier with its slash
	 * and also removing LTI launch information if present.
	 * @param userName
	 * @return The dequalified username.
	 */
	public static final String dequalify(final String userName) {
		if (!userName.startsWith(QUALIFIER))
			throw new IllegalArgumentException("Username does not start with a tenant identifier.");

		final String nameWithoutIdentifier = userName.substring(QUALIFIER.length());

		final int idSeparatorPos = nameWithoutIdentifier.indexOf(LtiLaunch.ID_SEPARATOR);
		if (idSeparatorPos >= 0) {
			return nameWithoutIdentifier.substring(0,idSeparatorPos);
		}

		return nameWithoutIdentifier;
	}
}
