package de.uni_due.s3.jack3.services;

import java.util.List;

import javax.ejb.Stateless;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

/**
 * Service for development purposes.
 */
@Stateless
public class DevelopmentService extends AbstractServiceBean {

	public enum EDatabaseType {
		H2, POSTGRES
	}

	/**
	 * Deletes all entities from database. After deleting, it is required to execute the First Time Setup again to
	 * recreate root folders.
	 *
	 * @param type
	 *            Database which is used.
	 */
	public void deleteTenantDatabase(EDatabaseType type) {
		final EntityManager em = getEntityManager();

		String deleteAllQuery = null;
		switch (type) {
		case H2:
			deleteAllQuery = getTableDeleteStringH2();
			break;

		case POSTGRES:
			deleteAllQuery = getTableDeleteStringPostgres();
			break;

		default:
			throw new AssertionError("Unsupported DatabaseType to delete: " + type);
		}

		getLogger().debug(deleteAllQuery);

		em.createNativeQuery(deleteAllQuery).executeUpdate();
	}

	private String getTableDeleteStringPostgres() {
		final EntityManager em = getEntityManager();
		final StringBuilder deleteTableSB = new StringBuilder();

		// Get all tables
		final String tableQuery = "SELECT tablename FROM pg_catalog.pg_tables WHERE tableowner = 'jack';";

		@SuppressWarnings("unchecked") // We know, we're getting a list of strings here
		final List<String> tables = em.createNativeQuery(tableQuery).getResultList();

		// We set and reset the replica roles to disable all triggers (this is how foreign key constraints are enforced
		// in postgres). This requires superuser privileges! To set those for user jack, use something like:
		// "ALTER USER jack WITH SUPERUSER;"
		// This should only be done on a development server!
		deleteTableSB.append("SET session_replication_role = 'replica';");

		// Iterate over all tables and add DELETE statements
		tables.forEach(tableName -> deleteTableSB.append("DELETE FROM ").append(tableName).append(';'));

		deleteTableSB.append("SET session_replication_role = 'origin';");

		return deleteTableSB.toString();
	}

	private String getTableDeleteStringH2() {
		final EntityManager em = getEntityManager();
		final StringBuilder deleteTableSB = new StringBuilder();

		// Get all tables
		final String tableQuery = "SELECT TABlE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC';";

		@SuppressWarnings("unchecked") // We know, we're getting a list of strings here
		List<String> tables = em.createNativeQuery(tableQuery).getResultList();

		deleteTableSB.append("SET FOREIGN_KEY_CHECKS = 0;");
		tables.forEach(tableName -> deleteTableSB.append("DELETE FROM ").append(tableName).append(';'));
		deleteTableSB.append("SET FOREIGN_KEY_CHECKS = 1;");

		return deleteTableSB.toString();
	}

	public List<ContentFolder> getAllContentFolders() {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> q = em.createQuery("FROM ContentFolder", ContentFolder.class);
		return q.getResultList();
	}

	public List<CourseOffer> getAllCourseOffers() {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseOffer> q = em.createQuery("FROM CourseOffer", CourseOffer.class);
		return q.getResultList();
	}

	public List<CourseRecord> getAllCourseRecords() {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseRecord> q = em.createQuery("FROM CourseRecord", CourseRecord.class);
		return q.getResultList();

	}

	public List<CourseResource> getAllCourseResources() {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseResource> q = em.createQuery("FROM CourseResource", CourseResource.class);
		return q.getResultList();
	}

	public List<Course> getAllCourses() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Course> q = em.createQuery("FROM Course", Course.class);
		return q.getResultList();
	}

	public List<Exercise> getAllExercises() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Exercise> q = em.createQuery("FROM Exercise", Exercise.class);
		return q.getResultList();
	}

	public List<PresentationFolder> getAllPresentationFolders() {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> q = em.createQuery("FROM PresentationFolder", PresentationFolder.class);
		return q.getResultList();
	}

	public List<Submission> getAllSubmissions() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> q = em.createQuery("FROM Submission", Submission.class);
		return q.getResultList();

	}

	public List<UserGroup> getAllUserGroups() {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> q = em.createQuery("FROM UserGroup ", UserGroup.class);
		return q.getResultList();
	}

	public List<User> getAllUsers() {
		final EntityManager em = getEntityManager();
		final TypedQuery<User> q = em.createQuery("FROM User ", User.class);
		return q.getResultList();
	}

	public List<CourseEntry> getAllCourseEntries() {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseEntry> q = em.createQuery("FROM CourseEntry ", CourseEntry.class);
		return q.getResultList();
	}

	public List<AbstractExerciseProvider> getAllExerciseProviders() {
		final EntityManager em = getEntityManager();
		final TypedQuery<AbstractExerciseProvider> q = em.createQuery("FROM AbstractExerciseProvider ",
				AbstractExerciseProvider.class);
		return q.getResultList();
	}

	public List<StageSubmission> getAllStageSubmissions() {
		final EntityManager em = getEntityManager();
		final TypedQuery<StageSubmission> q = em.createQuery("FROM StageSubmission ", StageSubmission.class);
		return q.getResultList();
	}

	public void testMethod() {
		// You can use this method to test stuff and trigger it from the development view, but don't commit it!
	}

	// You may call this method in the test method but DO NOT commit it!
	@SuppressWarnings("unused")
	private void deleteAllLoadtestRecords() {
		// NOTE: You have to call the method multiple times.
		var crs = getEntityManager()
				.createQuery("FROM CourseRecord cr WHERE cr.closedByLecturerExplanation = 'Closed by loadtest-script'",
						CourseRecord.class)
				.setMaxResults(1000)
				.getResultList();
		int i = 0;
		for (CourseRecord cr : crs) {
			var service = CDI.current().select(CourseRecordService.class).get();
			service.removeCourseRecordAndAttachedSubmissions(cr);
			if (++i % 50 == 0) {
				getLogger().infof("Deleted %s course records.", i);
			}
		}
		getLogger().infof("Deleted %s course records.", i);
		getLogger().info("This was it (partial...).");
	}

}
