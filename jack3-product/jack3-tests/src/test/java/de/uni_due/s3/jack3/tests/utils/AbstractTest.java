package de.uni_due.s3.jack3.tests.utils;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.tests.annotations.NeedsEureka;
import de.uni_due.s3.jack3.tests.annotations.NeedsKafka;
import de.uni_due.s3.jack3.tests.arquillian.ArquillianExtension;
import de.uni_due.s3.jack3.tests.arquillian.Deployments;
import de.uni_due.s3.jack3.tests.arquillian.EDeploymentType;
import de.uni_due.s3.jack3.tests.arquillian.ExtendedDeployment;

/**
 * This class is the super class for all tests working with Arquillian. It provides deployment and some basic query
 * tools.
 *
 * @author lukas.glaser
 *
 */
@ExtendedDeployment(EDeploymentType.MULTI)
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractTest {

	protected static final int FIRST_REVISION = 0;
	protected static final int SECOND_REVISION = 1;
	protected static final int THIRD_REVISION = 2;
	protected static final int FOURTH_REVISION = 3;
	protected static final int FIFTH_REVISION = 4;

	@PersistenceContext
	private EntityManager entityManager;

	// Other tests should only have explicit access to business classes.
	@Inject
	private ConfigurationBusiness configBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	protected BaseService baseService;

	private Logger logger;

	protected final Logger getLogger() {
		if (logger == null) {
			logger = LoggerProvider.get(getClass());
		}
		return logger;
	}

	/**
	 * Single deployment for Arquillian
	 */
	@Deployment(testable = true)
	public static WebArchive createDeployment() {
		return Deployments.createSingleDeployment();
	}

	/**
	 * Queries the database and get a result list.
	 */
	protected final <T> List<T> queryResultList(String query, Class<T> clazz) {
		return entityManager.createQuery(query, clazz).getResultList();
	}

	@BeforeEach
	final void initializeExternalConnections() {
		boolean changedConfiguration = false;
		if (getClass().isAnnotationPresent(NeedsEureka.class)) {
			changedConfiguration = ExternalConnectionInitializer.initializeEureka();
		}
		if (getClass().isAnnotationPresent(NeedsKafka.class)) {
			changedConfiguration = ExternalConnectionInitializer.initializeKafka();
		}
		if (changedConfiguration) {
			configBusiness.clearCache();
		}
	}

	/**
	 * Queries the database and get a single result.
	 *
	 * @throws NonUniqueResultException
	 */
	protected final <T> Optional<T> querySingleResult(String query, Class<T> clazz) {
		try {
			return Optional.of(entityManager.createQuery(query, clazz).getSingleResult());
		} catch (NoResultException nre) {
			return Optional.empty();
		}
	}

	// TODO These methods may be moved to a dedicated service or something similar.

	/**
	 * @return An existing user with the given name or a new user without any rights.
	 */
	protected final User getStudent(String name) {
		final Optional<User> found = userBusiness.getUserByName(name);
		if (found.isPresent()) {
			final User user = found.get();
			if (user.isHasAdminRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized with admin rights!");
			if (user.isHasEditRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized with edit rights!");
			return user;
		} else {
			final User user = TestDataFactory.getUser(name, false, false);
			baseService.persist(user);
			return user;
		}
	}

	/**
	 * @return An existing user with the given name or a new user with edit rights.
	 */
	protected final User getLecturer(String name) {
		final Optional<User> found = userBusiness.getUserByName(name);
		if (found.isPresent()) {
			final User user = found.get();
			if (user.isHasAdminRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized with admin rights!");
			if (!user.isHasEditRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized without edit rights!");
			// We don't call baseService.merge(user) because only the business ensures a personal folder
			return user;
		} else {
			final User user = TestDataFactory.getUser(name, false, true);
			baseService.persist(user);
			// Business ensures a personal folder
			return userBusiness.updateUser(user);
		}
	}

	/**
	 * @return An existing user with the given name or a new user with edit and admin rights.
	 */
	protected final User getAdmin(String name) {
		final Optional<User> found = userBusiness.getUserByName(name);
		if (found.isPresent()) {
			final User user = found.get();
			if (!user.isHasAdminRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized without admin rights!");
			if (!user.isHasEditRights())
				throw new AssertionError(
						"The student with the name " + name + " was previously initialized without edit rights!");
			return user;
		} else {
			final User user = TestDataFactory.getUser(name, true, true);
			baseService.persist(user);
			// Business ensures a personal folder
			return userBusiness.updateUser(user);
		}
	}

	/**
	 * @return An existing user group with the given name or a new user group.
	 */
	protected final UserGroup getUserGroup(String name) {
		final Optional<UserGroup> found = userBusiness.getUserGroup(name);
		if (found.isPresent()) {
			return found.get();
		} else {
			final UserGroup group = TestDataFactory.getUserGroup(name);
			baseService.persist(group);
			return group;
		}
	}

	/**
	 * @return An existing user group with the given name or a new user group. Note that if the user group already
	 *         exists, the members are not added, this has to be done before by the test class!
	 */
	protected final UserGroup getUserGroup(String name, User... members) {
		final Optional<UserGroup> found = userBusiness.getUserGroup(name);
		if (found.isPresent()) {
			// Adding the members here would be too much work.
			return found.get();
		} else {
			final UserGroup group = TestDataFactory.getUserGroup(name, members);
			baseService.persist(group);
			return group;
		}
	}

	/**
	 * @return An existing user group with the given name or a new user group. Note that if the user group already
	 *         exists, the member groups are not added, this has to be done before by the test class!
	 */
	protected final UserGroup getUserGroup(String name, UserGroup... memberGroups) {
		final Optional<UserGroup> found = userBusiness.getUserGroup(name);
		if (found.isPresent()) {
			// Adding the members here would be too much work.
			return found.get();
		} else {
			final UserGroup group = TestDataFactory.getUserGroup(name, memberGroups);
			baseService.persist(group);
			return group;
		}
	}

}
