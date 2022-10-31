package de.uni_due.s3.jack3.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * Utility service for Hibernate Envers, managing revisions and lazy data.
 *
 * @author Benjamin.Otto
 */
@SuppressWarnings("unchecked")
@Stateless
public class RevisionService extends AbstractServiceBean {

	// TODO bo: private machen und dann servicespezifischen code nur die jeweiligen services machen lassen
	public <E extends AbstractExercise> int getProxiedOrLastPersistedRevisionId(E abstractExercise) {
		Objects.requireNonNull(abstractExercise);
		if (abstractExercise instanceof Exercise) {
			if (abstractExercise.isTransient()) {
				getLogger().warn("Asked a transient AbstractExercise, which Revision it is: " + abstractExercise
						+ " returning 0!");
				return 0;
			}
			if (((Exercise) abstractExercise).isFromEnvers()) {
				throw new IllegalArgumentException(
						"Getting the latest revision of an Exercise from the audit table is not well defined! "
								+ abstractExercise);
			}

			List<Integer> revisions = this.getRevisionNumbersFor(abstractExercise);
			return revisions.get(revisions.size() - 1);
		}
		if (abstractExercise instanceof FrozenExercise) {
			return ((FrozenExercise) abstractExercise).getProxiedExerciseRevisionId();
		}
		throw new UnsupportedOperationException("Type of AbstractExercise is not yet supported: " + abstractExercise);
	}

	// TODO bo: private machen und dann servicespezifischen code nur die jeweiligen services machen lassen
	public <F extends AbstractCourse> int getProxiedOrLastPersistedRevisionId(F abstractCourse) {
		Objects.requireNonNull(abstractCourse);
		if (abstractCourse instanceof Course) {
			if (abstractCourse.isTransient()) {
				getLogger().warn("Asked a transient AbstractExercise, which Revision it is: " + abstractCourse
						+ " returning 0!");
				return 0;
			}
			if (((Course) abstractCourse).isFromEnvers()) {
				throw new IllegalArgumentException(
						"Getting the latest revision of a Course from the audit table is not well defined! "
								+ abstractCourse);
			}
			List<Integer> revisions = this.getRevisionNumbersFor(abstractCourse);
			return revisions.get(revisions.size() - 1);
		}
		if (abstractCourse instanceof FrozenCourse) {
			return ((FrozenCourse) abstractCourse).getProxiedCourseRevisionId();
		}
		throw new UnsupportedOperationException("Type of AbstractCourse is not yet supported: " + abstractCourse);
	}

	/**
	 * We get a List of Numbers that correspond to the revisions of an entity. Those numbers are sorted in ascending
	 * order (so older revisions come first). These numbers are not necessarily starting at 0 and may contain gaps!
	 */
	public <T extends AbstractEntity> List<Integer> getRevisionNumbersFor(T entity) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		List<Number> revisions = auditReader.getRevisions(entity.getClass(), entity.getId());

		// Convert to Integer-List, because even though {@link AuditReaderFactory::find} takes a Number argument as
		// revision, an Exception is thrown if it is not an Integer (and the database revision table is of type Integer).
		// The stream gurantees the same order as the original list!
		return revisions.stream().map(Number::intValue).collect(Collectors.toList());
	}

	/**
	 * Get a specific revision of given entity. You have to make sure before calling the revision-number is contained in
	 * {@link #getRevisionNumbersFor(AbstractEntity)}!
	 */
	public <T extends AbstractEntity> T getRevisionOfEntity(Class<T> clazz, long entityId, int revisionId) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		return auditReader.find(clazz, entityId, revisionId);
	}

	/**
	 * Get a specific revision of given entity and initialize them using our helper method. You have to make sure before
	 * calling the revision-number is contained in {@link #getRevisionNumbersFor(AbstractEntity)}!
	 */
	public <T extends AbstractEntity> T getRevisionOfEntityWithLazyData(Class<T> clazz, long entityId, int revisionId) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		T result = auditReader.find(clazz, entityId, revisionId);
		EntityReflectionHelper.hibernateInitializeObjectGraph(result);
		return result;
	}

	/**
	 * Convenience method for {@link RevisionService#getRevisionOfEntity(Class, long, int)}
	 */
	public <T extends AbstractEntity> Optional<T> getRevisionOfEntity(T entity, int revisionId) {
		T revisionOfEntity = (T) getRevisionOfEntity(entity.getClass(), entity.getId(), revisionId);
		if (revisionOfEntity == null) {
			return Optional.empty();
		}
		return Optional.of(revisionOfEntity);
	}

	/**
	 * Convenience method for {@link RevisionService#getRevisionOfEntityWithLazyData(Class, long, int)}
	 */
	public <T extends AbstractEntity> Optional<T> getRevisionOfEntityWithLazyData(T entity, int revisionId) {
		T revisionOfEntityWithLazyData = (T) getRevisionOfEntityWithLazyData(entity.getClass(), entity.getId(),
				revisionId);
		if (revisionOfEntityWithLazyData == null) {
			return Optional.empty();
		}
		return Optional.of(revisionOfEntityWithLazyData);
	}

	/**
	 * Returns a list of all revisions of the given entity.
	 */
	public <T extends AbstractEntity> List<T> getAllRevisionsForEntity(T entity) {
		List<T> resultList = AuditReaderFactory.get(getEntityManager()) //
				.createQuery() //
				.forRevisionsOfEntity(entity.getClass(), true, true) //
				.add(AuditEntity.id().eq(entity.getId())) //
				.getResultList();
		return resultList;
	}

	/**
	 * Returns a list of all revisions of the given entity which are fully initialized by hibernate. This can
	 * potentially be slow!
	 */
	public <T extends AbstractEntity> List<T> getAllRevisionsForEntityWithLazyData(T entity) {
		List<T> resultList = AuditReaderFactory //
				.get(getEntityManager()) //
				.createQuery() //
				.forRevisionsOfEntity(entity.getClass(), true, true) //
				.add(AuditEntity.id().eq(entity.getId())) //
				.getResultList();

		for (int i = 0; i < resultList.size(); i++) {
			EntityReflectionHelper.hibernateInitializeObjectGraph(resultList.get(i));
		}

		return resultList;
	}

	private <T extends AbstractEntity> List<T> getFilteredRevisionsOf(T entity, int first, int pageSize,
			String sortField, String sortOrderString, boolean withLazyData) {
		AuditQuery auditQuery = AuditReaderFactory //
				.get(getEntityManager()) //
				.createQuery() //
				.forRevisionsOfEntity(entity.getClass(), true, true) //
				.add(AuditEntity.id().eq(entity.getId())) //
				.setFirstResult(first).setMaxResults(pageSize);

		if (JackStringUtils.isNotBlank(sortField)) {
			if (sortOrderString.equalsIgnoreCase("ASC")) {
				auditQuery.addOrder(AuditEntity.property(sortField).asc());
			} else {
				auditQuery.addOrder(AuditEntity.property(sortField).desc());
			}
		}

		List<T> resultList = auditQuery.getResultList();

		// REVIEW bo: diese das funktioniert hier so, wir müssen aber mal über eine
		// bessere Art nachdenken, auf
		// bestimmte Entitys anders zu reagieren (das ist hier an vielen Stellen im Code so).
		// Das Problem hier ist: Einerseits soll redundanter Code vermieden werden, andererseits ist die
		// Hibernate-Session in aufrufenden Entitys dann schon immer geschlossen, d.h. wenn Lazy-Daten initialisiert
		// werden sollen, muss das hier passieren. Als ausweg fallen mir nur mehr Übergabe-Parameter ein, oder den
		// ganzen Code redundant an die entsprechenden Stellen zu kopieren. Hat jemand noch ne bessere Idee
		// (Behavioural-Parametrizierung oder so?)

		if (withLazyData) {
			for (int i = 0; i < resultList.size(); i++) {
				EntityReflectionHelper.hibernateInitializeObjectGraph(resultList.get(i));
			}
		}
		return resultList;
	}

	/**
	 * Works like {@link #getAllRevisionsForEntityWithLazyData(AbstractEntity)} but you can
	 * <ul>
	 * <li>page the returned List by providing the first index and a pagesize
	 * <li>specify ascending or descending order in sortOrderString (if sortField is set to a non empty value)
	 * <li>TODO: apply filters
	 * </ul>
	 * This method is useful for DataTables of revisions with paging.
	 */
	public <T extends AbstractEntity> List<T> getFilteredRevisionsOfEntityWithLazyData(T entity, int first,
			int pageSize, String sortField, String sortOrderString) {
		return getFilteredRevisionsOf(entity, first, pageSize, sortField, sortOrderString, true);
	}

	public <T extends AbstractEntity> List<T> getFilteredRevisionsOfEntity(T entity, int first, int pageSize,
			String sortField, String sortOrderString) {
		return getFilteredRevisionsOf(entity, first, pageSize, sortField, sortOrderString, false);
	}

	/**
	 * This method returns the given revision as the newest revision of an Entity. Needs to be merged by the caller!
	 */
	public <T extends AbstractEntity> T resetToRevisionOfEntity(T entity, int revisionIndex) {
		entity = getRevisionOfEntityWithLazyData(entity, revisionIndex).orElseThrow(IllegalArgumentException::new);

		entity.setUpdateTimeStampToNow();

		if (entity instanceof Course) {
			entity = handleCourseExerciseProvider((Course) entity);
			entity = deepCopyCourseResources((Course) entity);
		}

		return entity;
	}

	/**
	 * To prevent bug #260 from happening, we explictly deep copy the resources, since the resources seem to still
	 * reference the audit tables
	 */
	private <T extends AbstractEntity> T deepCopyCourseResources(Course course) {
		for (CourseResource courseResource : course.getCourseResources()) {
			course.removeCourseResource(courseResource);
			course.addCourseResource(courseResource.deepCopy());
		}
		course.getCourseResources();
		return (T) course;
	}

	/**
	 * If we want to reset a course we have to handle the ExerciseProvider separately. In case it has an Id that is
	 * not in the main DB anymore (e.g. the referenced FixedListExerciseProvider from Envers was replaced by a
	 * FolderExerciseProvider). This prevents us from getting an "EntityId not found"-error.
	 */
	private <T extends AbstractEntity> T handleCourseExerciseProvider(Course course) {

		AbstractExerciseProvider abstractExerciseProviderHibernateProxy = course.getContentProvider();

		if (abstractExerciseProviderHibernateProxy != null) {
			AbstractExerciseProvider abstractExerciseProvider = //
					(AbstractExerciseProvider) Hibernate.unproxy(abstractExerciseProviderHibernateProxy);

			boolean exerciseProviderAlreadyInDB = getEntityManager().find(AbstractExerciseProvider.class,
					abstractExerciseProvider.getId()) != null;
			if (!exerciseProviderAlreadyInDB) {
				getLogger().debug("Did not find Exerciseprovider " + abstractExerciseProvider
						+ " in Database, setting ID to 0, so it will be inserted as a new one");
				abstractExerciseProvider.markTransient();
			} else {
				getLogger().debug("Found Exerciseprovider " + abstractExerciseProvider + " in Database, updating...");
			}
			course.setContentProvider(abstractExerciseProvider);
		}
		return (T) course;
	}

	public <T extends AbstractEntity> T getRevisionOfEntityByTypeAndIdAndTimeStamp(Class<T> theClass, long id,
			LocalDateTime timeStamp) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		Number revisionNumber = auditReader
				.getRevisionNumberForDate(Date.from(timeStamp.atZone(ZoneId.systemDefault()).toInstant()));
		T temp = auditReader.find(theClass, id, revisionNumber);
		EntityReflectionHelper.hibernateInitializeObjectGraph(temp);
		return temp;
	}

	public <T extends AbstractEntity> T getRevisionOfEntityByTimeStamp(T entity, LocalDateTime timeStamp) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		Number revisionNumber = auditReader
				.getRevisionNumberForDate(Date.from(timeStamp.atZone(ZoneId.systemDefault()).toInstant()));
		T temp = (T) auditReader.find(entity.getClass(), entity.getId(), revisionNumber);
		EntityReflectionHelper.hibernateInitializeObjectGraph(temp);
		return temp;
	}

	public <T extends AbstractEntity> T getDeletedEntityById(Class<T> clazz, Long id) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		Object deletedEntityObj = auditReader.createQuery().forRevisionsOfEntity(clazz, true, true)
				.add(AuditEntity.id().eq(id)).add(AuditEntity.revisionType().eq(RevisionType.DEL))
				// TODO get last deleted version of Entity
				.getSingleResult();
		EntityReflectionHelper.hibernateInitializeObjectGraph(deletedEntityObj);
		return (T) deletedEntityObj;
	}
}
