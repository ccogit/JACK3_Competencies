package de.uni_due.s3.jack3.services;

import java.util.Objects;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Utility service for the ID generator.
 *
 * @author Bj&ouml;rn Zurmaar
 */
@Stateless
public class IdentifierService extends AbstractServiceBean {

	/**
	 * Returns the maximum entity IDs from Hibernate and Envers.
	 */
	public long getMaximumEntityId() {
		// This stream iterates over all types managed by hibernate and ...
		// 1. filters all managed types not being entities
		// 2. maps the managed types to their java pendants
		// 3. filters out managed types without a java pendant
		// 4. filters all values not being a subclass of AbstractEntity
		// 5. retrieves the maximum id for the remaining types
		return getEntityManager().getMetamodel().getManagedTypes().stream()
			.filter(type -> type.getPersistenceType() == PersistenceType.ENTITY)
			.map(ManagedType::getJavaType)
			.filter(Objects::nonNull)
			.filter(AbstractEntity.class::isAssignableFrom)
			.mapToLong(this::getMaximumId)
			.max().orElse(0);
	}

	private long getMaximumId(final Class<?> type) {
		return Math.max(getMaximumHibernateId(type),getMaximumEnversId(type));
	}

	private long getMaximumHibernateId(final Class<?> type) {
		// We first setup some hibernate objects we need.
		final EntityManager entityManager = getEntityManager();
		final Session session = entityManager.unwrap(org.hibernate.Session.class);
		final String idName = session.getEntityManagerFactory()
			.getMetamodel()
			.entity(type)
			.getId(Long.class)
			.getName();

		// We then prepare the query string.
		final String query = "select max("	+ idName + ") "
			+ "from " + type.getSimpleName();

		// Finally we fire the query and return its result.
		final Long maximumId = (Long) getEntityManager().createQuery(query)
														.getSingleResult();
		return maximumId != null ? maximumId : 0;
	}

	private long getMaximumEnversId(final Class<?> type) {
		// Some entities may not be audited. We _must_ not execute the query in this case.
		if (!type.isAnnotationPresent(Audited.class)) {
			return 0;
		}

		final AuditQuery query = AuditReaderFactory.get(getEntityManager()).createQuery()
			.forRevisionsOfEntity(type, true, true)
			.addProjection(AuditEntity.id().max());
		final Long maximumId = (Long)query.getSingleResult();
		return maximumId != null ? maximumId : 0;
	}
}
