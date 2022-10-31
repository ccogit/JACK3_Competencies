package de.uni_due.s3.jack3.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;

import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing {@link Enrollment} entities.
 * 
 * @author lukas.glaser
 */
@Stateless
public class EnrollmentService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	public void persistEnrollment(Enrollment enrollment) {
		baseService.persist(enrollment);
	}

	public Enrollment mergeEnrollment(Enrollment enrollment) {
		return baseService.merge(enrollment);
	}

	public void deleteEnrollment(Enrollment enrollment) {
		baseService.deleteEntity(enrollment);
	}

	public void deleteAllEnrollmentsForUser(User user) {
		getEnrollments(user).forEach(baseService::deleteEntity);
	}

	/**
	 * Returns all enrollments for a user, ordered by the last change (desc).
	 */
	public List<Enrollment> getEnrollments(User user) {
		return getEntityManager()
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_USER, Enrollment.class)
				.setParameter("user", user)
				.getResultList();
	}

	/**
	 * Returns all enrollments for a user, which have the given status, ordered by the last change (desc).
	 */
	public List<Enrollment> getEnrollments(User user, EEnrollmentStatus status) {
		return getEntityManager() //
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_USER_AND_STATUS, Enrollment.class) //
				.setParameter("user", user)
				.setParameter("status", status)
				.getResultList();
	}

	/**
	 * Returns all enrollments for a course offer, ordered alphabetically by the username.
	 */
	public List<Enrollment> getEnrollments(CourseOffer offer) {
		return getEntityManager()
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFER, Enrollment.class)
				.setParameter("courseOffer", offer)
				.getResultList();
	}

	/**
	 * Returns all enrollments for a course offer list, ordered alphabetically by the username.
	 */
	public List<Enrollment> getEnrollments(List<CourseOffer> offers) {
		if (offers.isEmpty())
			return Collections.emptyList();
		return getEntityManager()
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFERS, Enrollment.class)
				.setParameter("courseOffers", offers)
				.getResultList();
	}

	/**
	 * Returns all enrollments for a user in any of a Presentation Folder in the passed list (not ordered).
	 */
	public List<Enrollment> getEnrollmentsForUserAndFoldersUnordered(User user, List<PresentationFolder> folders) {
		if (folders.isEmpty())
			return Collections.emptyList();
		return getEntityManager()
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_USER_AND_FOLDERS_UNORDERED, Enrollment.class)
				.setParameter("user", user)
				.setParameter("folders", folders)
				.getResultList();
	}

	/**
	 * Returns all enrollments for a course offer that have a certain status, ordered alphabetically by the username.
	 */
	public List<Enrollment> getEnrollments(CourseOffer offer, EEnrollmentStatus status) {
		return getEntityManager()
				.createNamedQuery(Enrollment.ALL_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS, Enrollment.class)
				.setParameter("courseOffer", offer)
				.setParameter("status", status)
				.getResultList();
	}

	/**
	 * Counts all enrollments for a course offer that have a certain status.
	 */
	public long countEnrollments(CourseOffer offer, EEnrollmentStatus status) {
		return getEntityManager()
				.createNamedQuery(Enrollment.COUNT_ENROLLMENTS_FOR_COURSEOFFER_AND_STATUS, Long.class)
				.setParameter("courseOffer", offer)
				.setParameter("status", status)
				.getSingleResult();
	}

	/**
	 * Counts all enrollments for a course offer.
	 */
	public long countEnrollments(CourseOffer offer) {
		return getEntityManager()
				.createNamedQuery(Enrollment.COUNT_ENROLLMENTS_FOR_COURSEOFFER, Long.class)
				.setParameter("courseOffer", offer)
				.getSingleResult();
	}

	/**
	 * Returns one enrollment for a user and a course offer or empty {@link Optional} if no one was found.
	 * 
	 * @throws NonUniqueResultException
	 *             if multiple enrollments were found. This must not happen due to the constraints.
	 */
	public Optional<Enrollment> getEnrollment(User user, CourseOffer offer) {
		return DBHelper.getOneOrZero(getEntityManager()
				.createNamedQuery(Enrollment.ONE_ENROLLMENT_FOR_USER_AND_COURSEOFFER, Enrollment.class)
				.setParameter("user", user)
				.setParameter("courseOffer", offer));
	}

}
