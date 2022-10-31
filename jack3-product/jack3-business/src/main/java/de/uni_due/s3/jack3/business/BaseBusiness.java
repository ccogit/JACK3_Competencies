package de.uni_due.s3.jack3.business;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.services.BaseService;

/**
 * This service provides basic methods for database transactions and can be injected in view classes where no
 * entity-specific operations are needed. This just delegates to our baseservice to not violate the design principle
 * that views should never inject services.
 *
 * @author benjamin.otto
 */
@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class BaseBusiness extends AbstractBusiness {

	@Inject
	BaseService baseService;

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
		baseService.flushEntityManager();
	}

	/**
	 * Make an instance of AbstractEntity managed and persistent.
	 *
	 * @see EntityManager#persist(Object)
	 */
	public <T extends AbstractEntity> void persist(T abstractEntity) {
		baseService.persist(abstractEntity);
	}

	/**
	 * Merge the state of the given Entity into the current persistence context.
	 *
	 * @see EntityManager#merge(Object)
	 */
	public <T extends AbstractEntity> T merge(T abstractEntity) {
		return baseService.merge(abstractEntity);
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
		return baseService.findById(clazz, id, eager);
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
		return baseService.findAll(clazz);
	}

	/**
	 * Counts all entities of a class.
	 *
	 * @see #findAll(Class)
	 */
	public <T extends AbstractEntity> long countAll(Class<T> clazz) {
		return baseService.countAll(clazz);
	}

	/**
	 * Deletes an entity from database.
	 *
	 * @throws IllegalArgumentException
	 *             If the entity is transient or the entity does not exist anymore.
	 * @see EntityManager#remove(Object)
	 */
	public <T extends AbstractEntity> void deleteEntity(T entity) {
		baseService.deleteEntity(entity);
	}
}
