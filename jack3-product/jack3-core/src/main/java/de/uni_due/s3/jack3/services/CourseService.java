package de.uni_due.s3.jack3.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.*;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing entities derived from {@link AbstractCourse}.
 */
@Stateless
// REVIEW - Is @LocalBean obsolete?
@LocalBean // So this Service can be injected as implementation not only as Interface
public class CourseService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	@Inject
	private RevisionService revisionService;

	@Inject
	private CourseRecordService courseRecordService;

	public void deleteCourse(AbstractCourse course) {
		courseRecordService.removeAllCourseRecordsForCourse(course);
		baseService.deleteEntity(course);
	}

	/**
	 * Returns the course with lazy data by id.
	 *
	 * @return Found course with lazy data
	 */
	public Optional<Course> getCourseWithLazyDataByCourseID(long courseID) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em.createNamedQuery(Course.COURSE_WITH_LAZY_DATA_BY_COURSE_ID, Course.class)
				.setParameter("id", courseID);

		return DBHelper.getOneOrZero(query);
	}

	/**
	 * Returns the course by id.
	 *
	 * @return Found course <strong>without</strong> lazy data
	 */
	public Optional<Course> getCourseByCourseID(long courseID) {
		return baseService.findById(Course.class, courseID, false);
	}

	public void persistCourse(AbstractCourse course) {
		baseService.persist(course);
	}

	/**
	 * Returns all courses from the database where the user has rights on the parent folder of the course.
	 *
	 * @return Course list without lazy data, alphabetically ordered
	 */
	public List<Course> getAllCoursesForUser(User user) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em.createNamedQuery(Course.ALL_COURSES_FOR_USER, Course.class);
		query.setParameter("user", user);
		return query.getResultList();
	}

	/**
	 * Returns all courses from the database where the user group has rights on the parent folder of the course.
	 *
	 * @return Course list without lazy data, alphabetically ordered
	 */
	public List<Course> getAllCoursesForUser(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em.createNamedQuery(Course.ALL_COURSES_FOR_USERGROUP, Course.class);
		query.setParameter("group", userGroup);
		return query.getResultList();
	}

	/**
	 * Returns all courses that are direct children of a folder in the folderList.
	 *
	 * @param folderList
	 *            The list of folders where the courses need to be in
	 * @return Course list without lazy data, alphabetically ordered
	 */
	public List<Course> getAllCoursesForContentFolderList(List<ContentFolder> folderList) {
		if (folderList.isEmpty()) {
			// TODO ns Throw AssertionError later
			return new ArrayList<>();
		}
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em.createNamedQuery(Course.ALL_COURSES_FOR_CONTENT_FOLDER_LIST, Course.class);
		query.setParameter("folderList", folderList);
		return query.getResultList();
	}

	public AbstractCourse mergeCourse(AbstractCourse course) {
		return baseService.merge(course);
	}

	/**
	 * Returns a course revision with the given real revision ID.
	 *
	 * @return Found course <strong>without</strong> lazy data
	 * @see RevisionService#getRevisionOfEntity(de.uni_due.s3.jack3.entities.AbstractEntity, int)
	 */
	public Optional<AbstractCourse> getRevisionOfCourse(AbstractCourse course, int revisionId) {
		Optional<AbstractCourse> revisionOfEntity = revisionService.getRevisionOfEntity(course, revisionId);

		revisionOfEntity //
				.filter(abstractCourse -> (abstractCourse instanceof Course)) //
				.ifPresent(abstractCourse -> ((Course) abstractCourse).setFromEnvers(true));

		return revisionOfEntity;
	}

	/**
	 * Returns a course revision with the given real revision ID.
	 *
	 * @return Found course with lazy data
	 * @see RevisionService#getRevisionOfEntityWithLazyData(de.uni_due.s3.jack3.entities.AbstractEntity, int)
	 */
	public Optional<AbstractCourse> getRevisionOfCourseWithLazyData(AbstractCourse course, int revisionId) {
		Optional<AbstractCourse> revisionOfEntityWithLazyData = revisionService.getRevisionOfEntityWithLazyData(course,
				revisionId);

		revisionOfEntityWithLazyData //
				.filter(abstractCourse -> (abstractCourse instanceof Course)) //
				.ifPresent(abstractCourse -> ((Course) abstractCourse).setFromEnvers(true));

		return revisionOfEntityWithLazyData;
	}

	public FrozenCourse mergeFrozenCourse(FrozenCourse frozenCourse) {
		return baseService.merge(frozenCourse);
	}

	/**
	 * Get all frozen course revisions for a given course by its ID.
	 *
	 * @param proxiedCourseId
	 *            The ID of the proxied ("real") course.
	 * @return Frozen course list <strong>without</strong> lazy data.
	 */
	public List<FrozenCourse> getFrozenRevisionsForCourse(long proxiedCourseId) {
		if (proxiedCourseId < 1) {
			throw new IllegalArgumentException("Id must be greater than zero: " + proxiedCourseId);
		}
		final EntityManager em = getEntityManager();
		final TypedQuery<FrozenCourse> q = em.createNamedQuery(FrozenCourse.FROZEN_REVISIONS_FOR_COURSE,
				FrozenCourse.class);
		q.setParameter("proxiedCourseId", proxiedCourseId);
		return q.getResultList();
	}

	/**
	 * Returns all frozen course IDs for a given course.
	 */
	public List<Long> getFrozenCourseIdsForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(FrozenCourse.FROZEN_COURSE_IDS_FOR_COURSE, Long.class)
				.setParameter("proxiedCourseId", course.getId())
				.getResultList();
	}

	/**
	 * Returns all IDs of courses belonging to this course. This includes the ID of the passed course itself and all IDs
	 * of frozen versions of the course.
	 *
	 * @see #getFrozenCourseIdsForCourse(Course)
	 */
	public List<Long> getAllIdsForCourse(Course course) {
		final List<Long> ids = getEntityManager()
				.createNamedQuery(FrozenCourse.FROZEN_COURSE_IDS_FOR_COURSE, Long.class)
				.setParameter("proxiedCourseId", course.getId())
				.getResultList();
		ids.add(course.getId());
		return ids;
	}

	/**
	 * Get a frozen revision for a course.
	 *
	 * @param proxiedCourseId
	 *            The ID of the proxied ("real") course.
	 * @param proxiedCourseRevisionId
	 *            The revision ID of the corresponding course
	 * @return Frozen course with lazy data
	 * @see #getFrozenCourseByProxiedIds(long, int)
	 */
	public Optional<FrozenCourse> getFrozenCourseByProxiedIdsWithLazyData(long proxiedCourseId,
			int proxiedCourseRevisionId) {
		final EntityManager em = getEntityManager();
		final TypedQuery<FrozenCourse> query = em.createNamedQuery( //
				FrozenCourse.FROZEN_COURSE_BY_PROXIED_IDS_WITH_LAZY_DATA, FrozenCourse.class);
		query.setParameter("proxiedCourseId", proxiedCourseId);
		query.setParameter("proxiedCourseRevisionId", proxiedCourseRevisionId);

		return DBHelper.getOneOrZeroRemovingDuplicates(query);
	}

	/**
	 * Get a frozen revision for a course.
	 *
	 * @param proxiedCourseId
	 *            The ID of the proxied ("real") course.
	 * @param proxiedCourseRevisionId
	 *            The revision ID of the corresponding course
	 * @return Frozen course
	 * @see #getFrozenCourseByProxiedIdsWithLazyData(long, int)
	 */
	public Optional<FrozenCourse> getFrozenCourseByProxiedIds(long proxiedCourseId, int proxiedCourseRevisionId) {
		final EntityManager em = getEntityManager();
		final TypedQuery<FrozenCourse> query = em.createNamedQuery( //
				FrozenCourse.FROZEN_COURSE_BY_PROXIED_IDS, FrozenCourse.class);
		query.setParameter("proxiedCourseId", proxiedCourseId);
		query.setParameter("proxiedCourseRevisionId", proxiedCourseRevisionId);

		return DBHelper.getOneOrZero(query);
	}

	/**
	 * Get a frozen course by its ID.
	 *
	 * @param frozenCourseId
	 *            ID of the frozen course entity
	 * @return Frozen course <strong>without</strong> lazy data
	 */
	public Optional<FrozenCourse> getFrozenCourse(long frozenCourseId) {
		return baseService.findById(FrozenCourse.class, frozenCourseId, false);
	}

	/**
	 * Returns all abstract courses that use a certain exercise in a {@link FixedListExerciseProvider}.
	 *
	 * @return Course list <strong>without</strong> lazy data
	 */
	public List<AbstractCourse> getAbstractCoursesReferencingExerciseExerciseProvider(AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<AbstractCourse> query = em
				.createNamedQuery(AbstractCourse.ABSTRACTCOURSES_REFERENCING_EXERCISE_BY_EXERCISE_PROVIDER, AbstractCourse.class);
		query.setParameter("abstractExerciseId", exercise.getId());
		return query.getResultList();
	}

	/**
	 * Returns all courses that use a certain exercise in a {@link FixedListExerciseProvider}.
	 *
	 * @return Course list
	 */
	public List<Course> getCoursesReferencingExerciseExerciseProvider(AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em
				.createNamedQuery(Course.COURSES_REFERENCING_EXERCISE_PROVIDER, Course.class);
		query.setParameter("abstractExerciseId", exercise.getId());
		return query.getResultList();
	}

	/**
	 * Returns all courses that use at least one of the folders in a {@link FolderExerciseProvider}.
	 *
	 * @return Course list <strong>without</strong> lazy data
	 */
	public List<AbstractCourse> getAbstractCoursesReferencingContentFoldersByFolderProvider(List<ContentFolder> folderList) {
		final EntityManager em = getEntityManager();
		final TypedQuery<AbstractCourse> query = em
				.createNamedQuery(AbstractCourse.ABSTRACTCOURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER,
						AbstractCourse.class);
		query.setParameter("folderList", folderList);
		return query.getResultList();
	}

	/**
	 * Returns all courses that use at least one of the folders in a {@link FolderExerciseProvider}.
	 *
	 * @return Course list <strong>without</strong> lazy data
	 */
	public List<Course> getCoursesReferencingContentFoldersByFolderProvider(List<ContentFolder> folderList) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em
				.createNamedQuery(Course.COURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER,
						Course.class);
		query.setParameter("folderList", folderList);
		return query.getResultList();
	}

	public Optional<FrozenCourse> getFrozenCourseWithLazyData(long courseId) {
		final EntityManager em = getEntityManager();
		final TypedQuery<FrozenCourse> query = em.createNamedQuery( //
				FrozenCourse.FROZEN_COURSE_BY_ID_WITH_LAZY_DATA, FrozenCourse.class);
		query.setParameter("id", courseId);
		return DBHelper.getOneOrZeroRemovingDuplicates(query);
	}

	/**
	 * Returns all courses that reference a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
	 *
	 * @return Course list
	 */
	public List<Course> getCoursesReferencingSubject(Subject subject) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> query = em.createNamedQuery( //
				Course.COURSES_REFERENCING_SUBJECT, Course.class);
		query.setParameter("subject", subject);
		return query.getResultList();
	}
}