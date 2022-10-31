package de.uni_due.s3.jack3.services;

import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.Query;

/**
 * Service for development purposes that executes direct queries from a given {@link String}.
 */
@Stateless
public class LiveHqlService extends AbstractServiceBean {

	public List<?> getResultList(final String query) {
		return createQuery(query).getResultList();
	}

	public List<?> getSingleResult(final String query) {
		final Object result = createQuery(query).getSingleResult();
		return Collections.singletonList(result);
	}

	private final Query createQuery(final String query) {
		return getEntityManager().createQuery(query);
	}
}
