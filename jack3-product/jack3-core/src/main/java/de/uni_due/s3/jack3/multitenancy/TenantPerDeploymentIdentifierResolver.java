package de.uni_due.s3.jack3.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * This class determines the identifier of the current tenant which corresponds to the current
 * module name. For an explanation of how to put this class to use see the documentation on
 * {@link TenantPerDeploymentConnectionProvider}.
 *
 * This implementation is thread safe.
 * @author Bj&ouml;rn Zurmaar
 */
public final class TenantPerDeploymentIdentifierResolver
	implements CurrentTenantIdentifierResolver
{
	/** The identifier of the tenant we detected. */
	private String tenantIdentifier;

	/** This serves as our locking object. */
	private final Object lock = new Object();

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns the current module name, i.e. the deployment name, stripped off
	 * file extensions and version information.
	 */
	@Override
	public final String resolveCurrentTenantIdentifier() {
		synchronized (lock) {
			if (tenantIdentifier == null)
				tenantIdentifier = TenantIdentifier.get();

			return tenantIdentifier;
		}
	}

	/**
	 * {@inheritDoc}
	 * This implementation returns {@code false} as the tenant's identifier does not change within
	 * a deployment.
	 */
	@Override
	public final boolean validateExistingCurrentSessions() {
		return false;
	}
}