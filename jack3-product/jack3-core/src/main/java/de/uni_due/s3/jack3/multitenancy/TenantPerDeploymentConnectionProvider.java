package de.uni_due.s3.jack3.multitenancy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * This class implements a hibernate connection provider that allows one tenant per deployment.
 * The tenant's name is used as its identifier. In order to use this class you have to
 * <ol>
 *   <li>set the hibernate property {@code hibernate.multiTenancy} to the value
 *     &quot;DATABASE&quot;</li>
 *   <li>set the hibernate property {@code hibernate.multi_tenant_connection_provider} to this
 *     class' fully qualified name.</li>
 *   <li>set the hibernate property {@code hibernate.tenant_identifier_resolver} to the class
 *     {@link TenantPerDeploymentIdentifierResolver}'s fully qualified name.</li>
 * </ol>
 * You can set these properties for example in your persistence.xml or your hibernate.properties
 * files.
 *
 * This implementation is thread safe.
 *
 * @see TenantPerDeploymentIdentifierResolver
 * @author Bj&ouml;rn Zurmaar
 */
public final class TenantPerDeploymentConnectionProvider
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl
		implements MultiTenantConnectionProvider, ServiceRegistryAwareService {
	/** The generated serial version UID. */
	private static final long serialVersionUID = 3860821229490715524L;

	/**
	 * The key for obtaining the EntityManagerFactory's name from the hibernate
	 * configuration.
	 */
	private static final String ENTITYMANAGER_FACTORY_NAME = "hibernate.ejb.entitymanager_factory_name";

	/** This string identifies our current tenant. */
	private String tenantIdentifier;

	/** The datasource that serves our current tenant. */
	private DataSource dataSource = null;

	/** This lock object is used for synchronization. */
	private final Object lock = new Object();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final DataSource selectAnyDataSource() {
		synchronized (lock) {
			if (dataSource != null) {
				return dataSource;
			}

			throw new IllegalStateException("DataSource has not yet been aquired.");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final DataSource selectDataSource(final String tenantIdentifier) {
		ensureTenantIdentifierIsValid(tenantIdentifier);
		return selectAnyDataSource();
	}

	/**
	 * Throws an {@link IllegalArgumentException} if {@code tenantIdentifier}
	 * does not equal the expected tenant identifier stored in
	 * {@link #tenantIdentifier}. Otherwise this methods silently returns.
	 *
	 * @param tenantIdentifier
	 *            The tenant identifier to be checked.
	 */
	private final void ensureTenantIdentifierIsValid(final String tenantIdentifier) {
		synchronized (lock) {
			if (!tenantIdentifier.equals(this.tenantIdentifier)) {
				throw new IllegalArgumentException(String.format(
					"Only tenant \"%s\" can be served, but \"%s\" requested.",
					this.tenantIdentifier,tenantIdentifier));
			}
		}
	}

	/**
	 * {@inheritDoc} This implementation uses the serviceRegistry's
	 * configuration service in order to guess the module's name and then
	 * acquire the corresponding datasource.
	 */
	@Override
	public final void injectServices(final ServiceRegistryImplementor serviceRegistry) {
		synchronized (lock) {
			tenantIdentifier = aquireTenantName(serviceRegistry.getService(ConfigurationService.class));
			dataSource = acquireDataSource(tenantIdentifier);
		}
	}

	/**
	 * Extracts the current tenant's name from the given configuration service. This is done by
	 * cutting out the relevant part of the name of the {@link EntityManagerFactory}.
	 *
	 * @param service
	 *            The configuration service to read the entitymanagerfactory's name from.
	 * @return The name of the tenant we want to serve.
	 */
	private final String aquireTenantName(final ConfigurationService service) {

		final String entitiyManagerFactoryName =
			service.getSetting(ENTITYMANAGER_FACTORY_NAME,StandardConverters.STRING);
		final String deploymentName = extractDeploymentName(entitiyManagerFactoryName);
		final String tenantIdentifier = TenantIdentifier.fromDeploymentName(deploymentName);

		LoggerProvider.get(TenantPerDeploymentConnectionProvider.class).infof(
				"Using tenant identifier \"%s\" for entity manager factory name \"%s\".",
				tenantIdentifier,
				entitiyManagerFactoryName);

		return tenantIdentifier;
	}

	/**
	 * Extracts the deployment's name out of the entity manager factory's name. This methods assumes
	 * that the entity manager factory's name consists of the deployment name with one of the file
	 * extensions .jar, .war or .ear followed by a sharp (#) and the persistence unit's name.
	 *
	 * @param entityManagerFactoryName
	 *            The name of the entity manager factory to be analyzed.
	 * @return The deployment name extracted from the entity manager factory name.
	 * @throws IllegalArgumentException
	 *             If the name of the entity manager factory is in an unexpected format.
	 */
	private final String extractDeploymentName(final String entityManagerFactoryName) {
		final Pattern factoryNamePattern = Pattern.compile("(?<deploymentName>(.+))\\.[jwe]ar#.+");
		final Matcher matcher = factoryNamePattern.matcher(entityManagerFactoryName);

		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Unexpected entity manager factory name: \"" + entityManagerFactoryName + "\"");
		}

		return matcher.group("deploymentName");
	}

	/**
	 * Returns the datasource for the given tenant identifier. This method generates the
	 * datasource's name by capitalizing the tenant's identifier and appending the suffix
	 * &quot;DS&quot;. The datasource is then acquired by a JNDI lookup.
	 *
	 * @param tenantIdentifier
	 *            The identifier of the tenant the datasource is requested for.
	 * @return The datasource for the tenant identified by the given string.
	 */
	private final DataSource acquireDataSource(final String tenantIdentifier) {
		try {
			final String jndiName =
				TenantPerDeploymentDatasourceIdentifier.getDataSourceJndiName(tenantIdentifier);
			final DataSource dataSource = InitialContext.doLookup(jndiName);

			LoggerProvider.get(TenantPerDeploymentConnectionProvider.class).infof(
				"Using datasource \"%s\" for tenant \"%s\"", jndiName, tenantIdentifier);

			return dataSource;
		} catch (final NamingException e) {
			throw new IllegalStateException(
				"No datasource for tenant \"" + tenantIdentifier + "\" available.", e);
		}
	}
}