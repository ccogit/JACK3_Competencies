package de.uni_due.s3.jack3.services;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * This service provides basic methods for database transactions and can be injected in business classes where no
 * entity-specific operations are needed.
 *
 * @author benjamin.otto
 */
@Stateless
public class BaseService extends AbstractServiceBean {
	/**
	 * Synchronize the persistence context to the underlying database.
	 * <p>
	 * This method is mainly for debugging purposes, when you want to ensure that dirty hibernate caches are persisted.
	 * Otherwise you can observe unexpected behaviour like INSERT statements when you are calling a SELECT query.
	 * </p>
	 *
	 * <p>
	 * Also calling this seems to be necessary when a Singleton like the Kafka-Reciever is used on entitys that might
	 * still have dirty caches.
	 * </p>
	 */
	public void flushEntityManager() {
		getEntityManager().flush();
	}

	/**
	 * Make an instance of AbstractEntity managed and persistent.
	 *
	 * @see EntityManager#persist(Object)
	 */
	public <T extends AbstractEntity> void persist(T abstractEntity) {
		abstractEntity.setUpdateTimeStampToNow();
		getEntityManager().persist(abstractEntity);
	}

	/**
	 * Merge the state of the given Entity into the current persistence context. In case of audited entities, a new
	 * revision is created.
	 *
	 * @see EntityManager#merge(Object)
	 */
	public <T extends AbstractEntity> T merge(T abstractEntity) {
		/*
		 * NOTE: Updating the updateTimestamp is not only for better traceability but NECESSARY to create a new
		 * revision. This is because Hibernate Envers compares the entity with the saved state in the database. A new
		 * revision is only created if at least one of the entity's property is changed.
		 *
		 * Without this line, merging an Entity would not create a new revision. However, this is unexpected behaviour
		 * in the case of an entity that is not itself being changed, but an object referenced by the entity is changing
		 * (e.g. an AbstractExerciseProvider referenced by a course). If we merge the course with a changed provider, a
		 * new revision should be created ALWAYS.
		 *
		 * See:
		 * - JACK/jack3-core#984 for discussion
		 * - JACK/jack3-core#168 for a test case ("ExerciseProviderRevisionTest")
		 */
		abstractEntity.setUpdateTimeStampToNow();

		return getEntityManager().merge(abstractEntity);
	}

	/**
	 * Finds an {@link AbstractEntity} by the ID.
	 *
	 * @param <T>
	 *            Returning type
	 * @param clazz
	 *            Entity class
	 * @param id
	 *            Primary key
	 * @param eager
	 *            If the whole object graph is to be loaded
	 * @return Optional with the entity if found, empty Optional otherwise.
	 * @see EntityManager#find(Class, Object)
	 */
	public <T extends AbstractEntity> Optional<T> findById(Class<T> clazz, long id, boolean eager) {
		T entity = getEntityManager().find(clazz, id);
		if (entity == null) {
			return Optional.empty();
		}

		if (eager) {
			EntityReflectionHelper.hibernateInitializeObjectGraph(entity);
		}

		return Optional.of(clazz.cast(entity));
	}

	/**
	 * Returns a List of all entitys of type clazz (lazy loaded).
	 *
	 * @param <T>
	 *            Type of entity to get all instances of.
	 * @param clazz
	 *            Class representation of T
	 * @return (possibly empty) List of all entitys of type T that are persisted in the DB
	 */
	public <T extends AbstractEntity> List<T> findAll(Class<T> clazz) {
		return getEntityManager() //
				.createQuery("FROM " + clazz.getSimpleName(), clazz) //
				.getResultList();
	}

	/**
	 * Returns a List of all entitys of type clazz in the range from "first" to "first + max - 1" (lazy loaded) sorted
	 * by id in the given order. If you need more filter capabilitys, you should do this in a dedicated service!
	 * See: {@link #findAll(Class)}
	 */
	public <T extends AbstractEntity> List<T> findAllInRange(Class<T> clazz, int first, int max, String sortOrder) {
		if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
			throw new IllegalArgumentException(
					"'SortOrder' can only have the values 'asc' or 'desc'! Was: " + sortOrder);
		}

		return getEntityManager() //
				.createQuery("FROM " + clazz.getSimpleName() + " ORDER BY id " + sortOrder, clazz) //
				.setFirstResult(first)
				.setMaxResults(max)
				.getResultList();
	}

	/**
	 * Counts all entities of a class.
	 *
	 * @see #findAll(Class)
	 */
	public <T extends AbstractEntity> long countAll(Class<T> clazz) {
		return getEntityManager() //
				.createQuery("SELECT COUNT(*) FROM " + clazz.getSimpleName(), Long.class) //
				.getSingleResult();
	}

	/**
	 * Deletes an entity from database.
	 *
	 * @throws IllegalArgumentException
	 *             If the entity is transient or the entity does not exist anymore.
	 * @see EntityManager#remove(Object)
	 */
	public <T extends AbstractEntity> void deleteEntity(T entity) {
		if (entity.isTransient()) {
			throw new IllegalArgumentException("Tried to delete a transient entity: " + entity);
		}

		final EntityManager entityManager = getEntityManager();
		if (entityManager.contains(entity)) {
			entityManager.remove(entity);
		} else {
			Class<? extends AbstractEntity> clazz = ((AbstractEntity) Hibernate.unproxy(entity)).getClass();
			var entityToRemove = findById(clazz, entity.getId(), false)
					.orElseThrow(() -> new IllegalArgumentException("Tried to delete non-existing entity: " + entity));
			entityManager.remove(entityToRemove);
		}
	}
}
