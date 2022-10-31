package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZero;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;

import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Service for managing {@link User} entities.
 */
@Stateless
public class UserService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	@Inject
	private EnrollmentService enrollmentService;

	/**
	 * Counts all existing users in the database regardless wether they are active or locked.
	 */
	public long countUser() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(User.QUERY_COUNT, Long.class);
		return query.getSingleResult(); // We don't need an Optional here, since count() returns at least 0.
	}

	public void persistUser(User user) {
		baseService.persist(user);
	}

	/**
	 * Delete user and all enrollments
	 *
	 * @param user
	 */
	public void removeUser(User user) {
		enrollmentService.deleteAllEnrollmentsForUser(user);
		baseService.deleteEntity(user);
		getLogger().infof("Deleted User with username: %s and id: %s", user.getLoginName(), user.getId());
	}

	/**
	 * Lists all users in the database, ordered by login name.
	 */
	public List<User> getAllUsers() {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> q = em.createNamedQuery(User.ALL_USERS, User.class);

		return q.getResultList();
	}

	/**
	 * Lists all users with edit rights, ordered by login name.
	 */
	public List<User> getAllUsersWithEditRights() {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> q = em.createNamedQuery(User.ALL_USERS_WITH_EDIT_RIGHTS, User.class);

		return q.getResultList();
	}

	/**
	 * Lists all users without edit rights, ordered by login name.
	 */
	public List<User> getAllUsersWithoutEditRights() {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> q = em.createNamedQuery(User.ALL_USERS_WITHOUT_EDIT_RIGHTS, User.class);

		return q.getResultList();
	}

	/**
	 * Returns the user entity for the given screen name if a user with this name exists. Before querying the database
	 * the username is converted to lowercase so that calling this method with "admin" and "AdMiN" will result in the
	 * same user being returned.
	 *
	 * @param name
	 *            The screen name of the requested user entity
	 * @return User entity or empty optional if user with this screen name does not exist
	 */
	public Optional<User> getUserByName(String name) {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> query = em.createNamedQuery(User.USER_BY_NAME, User.class);
		query.setParameter("loginName", name.toLowerCase());

		return getOneOrZero(query);
	}

	/**
	 * Returns the user entity for the given email if a user with this email exists. Before querying the database
	 * the email is converted to lowercase so that calling this method with "foo@b.ar" and "fOo@B.aR" will result
	 * in the same user being returned.
	 *
	 * @param email
	 *            The email address of the requested user entity
	 * @return User entity or empty optional if user with this email does not exist
	 */
	public Optional<User> getUserByEmail(final String email) {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> query = em.createNamedQuery(User.USER_BY_EMAIL, User.class);
		query.setParameter("email", email.toLowerCase());

		return getOneOrZero(query);
	}

	public Optional<User> getUserById(long id) {
		return baseService.findById(User.class, id, false);
	}

	/**
	 * Searches for a user with the given pseudonym.
	 *
	 * @param pseudonym
	 * @return {@code true} in case there is a user with the given pseudonym, {@code false} otherwise.
	 */
	public boolean hasPseudonym(final String pseudonym) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(User.EXISTS_PSEUDONYM, Long.class);
		query.setParameter("pseudonym", pseudonym);
		return query.getSingleResult() > 0;
	}

	/**
	 * @return The user who owns the passed folder as his/her personal folder.
	 */
	public Optional<User> getUserOwningThisFolder(Folder folder) {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> query = em.createNamedQuery(User.USER_OWNING_THIS_FOLDER, User.class);
		query.setParameter("folder", folder);

		return getOneOrZero(query);
	}
	
	/**
	 * @return The user who owns the passed folder as his/her personal folder. The user will be loaded form envers.
	 */
	public Optional<User> getUserOwningFolderFromEnvers(Folder folder){
		final AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		
		Object[] userResult = (Object[]) auditReader.createQuery()
				.forRevisionsOfEntity(User.class, false, true)
				.add(AuditEntity.property("personalFolder").eq(folder))
				.setMaxResults(1)
				.getSingleResult();
		
		return Optional.of((User) (userResult[0]));
	}

	/**
	 * Returns true if no user is saved in database.
	 *
	 * @see #countUser()
	 */
	public boolean hasNoUser() {
		return countUser() < 1;
	}

	/**
	 * Fetches all users that have participated in the passed course, test submissions will be ignored. This query
	 * includes all users who participated in a frozen version of the course.
	 */
	public List<User> getAllParticipantsForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(User.ALL_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS, User.class)
				.setParameter("courseId", course.getId())
				.getResultList();
	}

	/**
	 * Counts all distinct users that have participated in the passed course including all frozen versions of the
	 * course, test submissions will be ignored.
	 *
	 * @see #getAllParticipantsForCourse(Course)
	 */
	public long countAllParticipantsForCourse(Course course) {
		return getEntityManager()
				.createNamedQuery(User.COUNT_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS, Long.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	public User mergeUser(User user) {
		return baseService.merge(user);
	}
}
