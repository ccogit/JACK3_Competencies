package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZero;
import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing {@link CourseRecord} entities.
 */
@Stateless
public class CourseRecordService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;
	
	@Inject
	private SubmissionService submissionService;

	/**
	 * Get all course records, that point to the course. The result is ordered by start time in descending order.
	 * <strong>NOTE: This query does not include course records for frozen versions of the course!</strong>
	 * 
	 * @see #getAllCourseRecordsForCourseIncludingFrozenCourses(Course)
	 */
	/*
	 * REVIEW lg - Schaut man sich die Aufrufer an, wird deutlich, dass diese Methode ausschließlich im Kontext 'Gib mir
	 * alle Bearbeitungen zu einem Kurs' benutzt wird. Dies ist problematisch, weil die zugehörige HQL-Abfrage nur nach
	 * Verweisen auf "Course"-IDs schaut, IDs von "FrozenCourses" jedoch auslässt - auch wenn die Methode gemäß
	 * Spezifikation korrekt arbeitet.
	 * 
	 * Eine Abhilfe könnte sein:
	 * a) Falls wir wirklich den Fall brauchen, nur Bearbeitungen zum Kurs zu erhalten, diese Methode umzubenennen.
	 * b) Diese Methode zu entfernen.
	 * Als Alternative kann die nächste Methode genutzt werden, diese inkludiert auch Bearbeitungen zu eingefrorenen
	 * Versionen.
	 */
	public List<CourseRecord> getAllCourseRecordsForCourse(AbstractCourse course) {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseRecord> q = em.createNamedQuery(
				CourseRecord.ALL_COURSERECORDS_FOR_COURSE_ORDERBY_STARTTIME, CourseRecord.class);
		q.setParameter("course", course);
		return q.getResultList();
	}

	/**
	 * Returns all course records for a course including testing submissions and all course records for frozen versions
	 * of the course. The result is ordered by start time in descending order.
	 */
	public List<CourseRecord> getAllCourseRecordsForCourseIncludingFrozenCourses(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.ALL_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS_ORDERBY_STARTTIME, CourseRecord.class)
				.setParameter("courseId", course.getId())
				.getResultList();
	}

	/**
	 * Get all course records, that point to a course offer. The result is ordered by start time in descending order.
	 */
	public List<CourseRecord> getAllCourseRecordsForCourseOfferOrderedByStarttime(CourseOffer courseOffer) {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseRecord> q = em.createNamedQuery(
				CourseRecord.ALL_COURSERECORDS_FOR_COURSEOFFER_ORDERBY_STARTTIME, CourseRecord.class);
		q.setParameter("courseOffer", courseOffer);
		return q.getResultList();
	}

	/**
	 * Get all course records, that point to any course offer in the passed list. The result is ordered by start time in
	 * descending order.
	 */
	public List<CourseRecord> getAllCourseRecordsForCourseOffersOrderedByStarttime(List<CourseOffer> courseOffers) {
		if (courseOffers.isEmpty())
			return Collections.emptyList();
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseRecord> q = em.createNamedQuery(
				CourseRecord.ALL_COURSERECORDS_FOR_COURSEOFFERS_ORDERBY_STARTTIME, CourseRecord.class);
		q.setParameter("courseOffers", courseOffers);
		return q.getResultList();
	}

	/**
	 * Deletes all course records from database that point to the given course.
	 */
	public void removeAllCourseRecordsForCourse(AbstractCourse course) {
		final EntityManager em = getEntityManager();
		List<CourseRecord> courseRecordsToDelete = getAllCourseRecordsForCourse(course);

		for (CourseRecord currentCourseRecord : courseRecordsToDelete) {
			// Calling baseService.deleteEntity would be overhead here as the list above already contain "fresh"
			// entities and only includes existing entities
			em.remove(currentCourseRecord);
		}
	}
	
	/**
	 * Deletes given course record AND associated Submissions
	 * @param courseRecord
	 * 			CourseRecord, which should be Deleted
	 */
	public void removeCourseRecordAndAttachedSubmissions(CourseRecord courseRecord) {
		
		List<Submission> submissionList = submissionService.getAllSubmissionsForCourseRecord(courseRecord);
		
		for(Submission submission : submissionList) {
			submissionService.deleteSubmissionAndDependentEntities(submission);
		}
		
		baseService.deleteEntity(courseRecord);
	}

	public long countAllCourseRecordsForCourseOfferAndUser(CourseOffer courseOffer, User user) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> q = em.createNamedQuery(CourseRecord.COUNT_COURSERECORDS_FOR_COURSEOFFER_AND_USER,
				Long.class);
		q.setParameter("courseOffer", courseOffer);
		q.setParameter("user", user);
		return q.getSingleResult();
	}

	public Optional<CourseRecord> getCourseRecordById(long id) {
		return baseService.findById(CourseRecord.class, id, false);
	}

	/**
	 * Returns the course record with fetched exercises.
	 */
	public Optional<CourseRecord> getCourseRecordWithExercises(long id) {
		var query = getEntityManager()
				.createNamedQuery(CourseRecord.BY_ID_WITH_EXERCISES, CourseRecord.class)
				.setParameter("id", id);
		return getOneOrZeroRemovingDuplicates(query);
	}

	/**
	 * Gets all open course records for a student, excluding test records. The result list is ordered by the name of the
	 * course offer.
	 */
	public List<CourseRecord> getOpenCourseRecords(User user) {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseRecord> query = em
				.createNamedQuery(CourseRecord.OPEN_COURSERECORDS_FOR_USER_ORDERBY_COURSEOFFER, CourseRecord.class);
		query.setParameter("user", user);
		return query.getResultList();
	}

	/**
	 * Returns <strong>one</strong> open course record for a given user that belongs to a course offer. <strong>Each
	 * user may only have one open course record for a course offer at a time.</strong>
	 * 
	 * @throws NonUniqueResultException
	 *             If more than one result was found.
	 * @see DBHelper#getOneOrZero(TypedQuery)
	 */
	public Optional<CourseRecord> getOpenCourseRecordFor(User user, CourseOffer offer) {
		final EntityManager em = getEntityManager();
		TypedQuery<CourseRecord> query = em.createNamedQuery(CourseRecord.OPEN_COURSERECORD_FOR_USER_AND_COURSEOFFER,
				CourseRecord.class);
		query.setParameter("user", user);
		query.setParameter("courseOffer", offer);

		return getOneOrZero(query);
	}

	/**
	 * Lists all course records for a user and a course offer that are not opened.
	 */
	public List<CourseRecord> getClosedCourseRecords(User user, CourseOffer offer) {
		final EntityManager em = getEntityManager();
		TypedQuery<CourseRecord> query = em.createNamedQuery(
				CourseRecord.CLOSED_COURSERECORDS_FOR_USER_COURSEOFFER_ORDERBY_STARTTIME,
				CourseRecord.class);
		query.setParameter("user", user);
		query.setParameter("courseOffer", offer);

		return query.getResultList();
	}

	/**
	 * Counts all comments for a given course record, regardless of whether they are read or unread.
	 */
	public long countCommentsForCourseRecord(CourseRecord courseRecord) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(Comment.COUNT_COMMENTS_FOR_COURSERECORD, Long.class);
		query.setParameter("courseRecord", courseRecord);
		return query.getSingleResult();
	}

	public long countUnreadCommentsForCourseRecord(CourseRecord courseRecord) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(Comment.COUNT_UNREAD_COMMENTS_FOR_COURSERECORD,
				Long.class);
		query.setParameter("courseRecord", courseRecord);
		return query.getSingleResult();
	}

	public void persistCourseRecord(CourseRecord courseRecord) {
		baseService.persist(courseRecord);
	}

	public CourseRecord mergeCourseRecord(CourseRecord courseRecord) {
		return baseService.merge(courseRecord);
	}

	public List<CourseRecord> getAllEmptyCourseRecordsForUser(User user) {
		final EntityManager em = getEntityManager();
		TypedQuery<CourseRecord> query = em.createNamedQuery(CourseRecord.ALL_EMPTY_COURSERECORDS_FOR_USER,
				CourseRecord.class);
		query.setParameter("user", user);

		return query.getResultList();
	}

	/**
	 * Counts all course records belonging to a given course offer. Since only non-testing records are linked to course
	 * offers, this query ignores all testing records.
	 */
	public long countCourseRecordsForCourseOffer(CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.COUNT_COURSERECORDS_FOR_COURSEOFFER, Long.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult();
	}

	/**
	 * Returns the average score of all non-testing course records belonging to a course offer.
	 */
	public double getAverageScoreForCourseOffer(CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.AVG_SCORE_FOR_COURSEOFFER, Double.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult();
	}

	/**
	 * Returns the highest score of all non-testing course records belonging to a course offer.
	 */
	public int getHighestScoreForCourseOffer(CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.HIGHEST_SCORE_FOR_COURSEOFFER, Integer.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult();
	}

	/**
	 * Returns the lowest score of all non-testing course records belonging to a course offer.
	 */
	public int getLowestScoreForCourseOffer(CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.LOWEST_SCORE_FOR_COURSEOFFER, Integer.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult();
	}

	/**
	 * Counts all non-testing course records belonging to a course, including records for frozen courses.
	 */
	public long countCourseRecordsForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.COUNT_NONTESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS, Long.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	/**
	 * Counts all testing course records belonging to a course, including records for frozen courses.
	 */
	public long countTestCourseRecordsForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.COUNT_TESTING_COURSERECORDS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
						Long.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	/**
	 * Returns the average score of all non-testing course records belonging to a course, including records for frozen
	 * courses.
	 */
	public double getAverageScoreForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.AVG_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS, Double.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	/**
	 * Returns the highest score of all non-testing course records belonging to a course, including records for frozen
	 * courses.
	 */
	public int getHighestScoreForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.HIGHEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS, Integer.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	/**
	 * Returns the lowest score of all non-testing course records belonging to a course, including records for frozen
	 * courses.
	 */
	public int getLowestScoreForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(CourseRecord.LOWEST_NONTESTING_SCORE_FOR_COURSE_INCLUDING_FROZENREVISIONS, Integer.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

}
