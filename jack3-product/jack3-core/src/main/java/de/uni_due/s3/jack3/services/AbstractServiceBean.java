package de.uni_due.s3.jack3.services;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * Abstract class for all services, provides an {@link EntityManager} and a {@link Logger}.
 */
public abstract class AbstractServiceBean {

	private final Logger logger;

	@PersistenceContext(name = "jack3-core")
	private EntityManager entityManager;

	protected AbstractServiceBean() {
		logger = LoggerProvider.get(getClass());
	}

	protected AbstractServiceBean(final Logger logger) {
		this.logger = logger;
	}

	protected EntityManager getEntityManager() {
		return entityManager;
	}

	protected Logger getLogger() {
		return logger;
	}

}
