package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Result;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.entities.tenant.SubmissionLogEntry;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;

/**
 * Service for managing {@link Submission} entities.
 */
@Stateless
public class SubmissionService extends AbstractServiceBean {

	@Inject
	private JobService jobService;

	@Inject
	private BaseService baseService;

	public void persistSubmission(Submission submission) {
		baseService.persist(submission);
	}

	public List<Submission> getAllSubmissionsForExerciseAndFrozenVersions(AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> q = em
				.createNamedQuery(Submission.ALL_SUBMISSIONS_FOR_EXERCISE_AND_FROZEN_EXERCISES, Submission.class);
		q.setParameter("exercise", exercise);
		q.setParameter("proxiedExerciseId", exercise.getProxiedOrRegularExerciseId());

		return q.getResultList();
	}

	/**
	 * Loads the submission with lazy data by submission id from database.
	 */
	public Optional<Submission> getSubmissionnWithLazyDataBySubmissionId(long submissionID) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> query = em.createNamedQuery(Submission.SUBMISSIONS_WITH_LAZY_DATA_BY_SUBMISSIONS_ID,
				Submission.class);
		query.setParameter("id", submissionID);
		Optional<Submission> submission = getOneOrZeroRemovingDuplicates(query);
		submission.ifPresent(subm -> Hibernate.initialize(subm.getExercise()));
		return submission;
	}

	/**
	 * Loads the submission with lazy data by submission id from envers.
	 */
	public Optional<Submission> getSubmissionWithLazyDataBySubmissionIdFromEnvers(long submissionId) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		List<Number> revisionNumbers = auditReader.getRevisions(Submission.class, submissionId);
		Submission submission = auditReader.find(Submission.class, submissionId,
				revisionNumbers.get(revisionNumbers.size() - 1));

		if (submission == null) {
			return Optional.empty();
		}

		// REVIEW bo: m.E. braucht man die ganzen ID-getter hier nicht aufrufen, weil primitive datentypen immer eager
		// geladen werden
		// Initialize fields
		submission.getAuthor().getId();
		submission.getExercise().getId();
		submission.getVariableValues().size();
		for (SubmissionLogEntry submissionLogEntry : submission.getSubmissionLog()) {
			Result result = submissionLogEntry.getResult();
			if (result != null) {
				result.getPoints();
			}
			StageSubmission stageSubmission = submissionLogEntry.getSubmission();
			if (stageSubmission != null) {
				stageSubmission.getId();
				stageSubmission.getStageId();
			}
		}
		CourseRecord courseRecord = submission.getCourseRecord();
		if (courseRecord != null) {
			Optional<CourseOffer> courseOffer = courseRecord.getCourseOffer();
			if (courseOffer.isPresent()) {
				courseOffer.get().getId();
				courseOffer.get().getFolder().getId();
			}
		}
		submission.getComments().size();

		return Optional.of(submission);
	}

	/**
	 * Loads the submission by submission id and only eager loads the comments. This provides a significant performance
	 * boost for the submission view. Comments must still be eagerly loaded, because this is exactly the place where a
	 * user with extended view should see user comments.
	 */
	public Optional<Submission> getSubmissionWithCommentsEagerBySubmissionId(long submissionID) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> query = em.createNamedQuery(Submission.SUBMISSIONS_WITH_COMMENTS_EAGER_BY_SUBMISSIONS_ID,
				Submission.class);
		query.setParameter("id", submissionID);
		Optional<Submission> submission = getOneOrZeroRemovingDuplicates(query);
		return submission;
	}

	/**
	 * Lists all submissions that belong to a given course record
	 * <strong>Do not use this query for count all submissions!</strong>
	 *
	 * @return Submission list <strong>without</strong> lazy data
	 */
	public List<Submission> getAllSubmissionsForCourseRecord(CourseRecord courseRecord) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> q = em.createNamedQuery(Submission.ALL_SUBMISSIONS_FOR_COURSERECORD,
				Submission.class);
		q.setParameter("courseRecord", courseRecord);

		return q.getResultList();
	}

	/**
	 * Counts all submissions that belong to a given exercise AND a given course record.
	 */
	public long countAllSubmissionsForCourseRecordAndExercise(CourseRecord courseRecord, AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> q = em.createNamedQuery(Submission.COUNT_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE,
				Long.class);
		q.setParameter("exercise", exercise);
		q.setParameter("courseRecord", courseRecord);

		return q.getSingleResult(); // We don't need an Optional here, since count() returns at least 0.
	}

	/**
	 * Returns the submission that was started last for a given exercise AND a given course record.
	 *
	 * @return Submission with lazy data or empty {@link Optional} if no submission was found.
	 * @see #getBestSubmissionForCourseRecordAndExercise(CourseRecord, AbstractExercise)
	 */
	public Optional<Submission> getLatestSubmissionForCourseRecordAndExercise(CourseRecord courseRecord,
			AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> query = em.createNamedQuery(
				Submission.ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_IN_DESCENDING_ORDER, Submission.class);
		query.setParameter("exercise", exercise);
		query.setParameter("courseRecord", courseRecord);
		// TODO Maybe it is better for performance to set a row limit of 1 since all other rows are ignored.

		// We don't use "getOneOrZero" here because result list may contain several elements
		Optional<Submission> submission = query.getResultStream().findFirst();
		submission.ifPresent(sol -> Hibernate.initialize(sol.getExercise()));
		return submission;
	}

	/**
	 * Returns the submission with the best result for a given exercise AND a given course record.
	 *
	 * @return Submission with lazy data or empty {@link Optional} if no submission was found.
	 * @see #getBestSubmissionForCourseRecordAndExercise(CourseRecord, AbstractExercise)
	 */
	public Optional<Submission> getBestSubmissionForCourseRecordAndExercise(CourseRecord courseRecord,
			AbstractExercise exercise) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Submission> query = em.createNamedQuery(
				Submission.ALL_SUBMISSIONS_FOR_COURSERECORD_AND_EXERCISE_ORDERD_BY_RESULT_IN_DESCENDING_ORDER,
				Submission.class);
		query.setParameter("exercise", exercise);
		query.setParameter("courseRecord", courseRecord);
		// TODO Maybe it is better for performance to set a row limit of 1 since all other rows are ignored.

		// We don't use "getOneOrZero" here because result list may contain several elements
		Optional<Submission> submission = query.getResultStream().findFirst();
		submission.ifPresent(sol -> Hibernate.initialize(sol.getExercise()));
		return submission;
	}

	/**
	 * Counts all submissions belonging to a given course offer. Since only non-testing submissions are linked to course
	 * offers, this query ignores all testing submissions.
	 */
	public long countSubmissionsForCourseOffer(final CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(Submission.COUNT_SUBMISSIONS_FOR_COURSEOFFER, Long.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult(); // No Optional, count() returns at least 0.
	}

	/**
	 * Counts all non-testing submissions for a given course including frozen revisions of the course.
	 */
	public long countSubmissionsForCourse(final Course course) {
		return getEntityManager()
				.createNamedQuery(Submission.COUNT_NONTESTING_SUBMISSIONS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
						Long.class)
				.setParameter("courseId", course.getId())
				.getSingleResult(); // No Optional, count() returns at least 0.
	}

	/**
	 * Counts all non-testing submissions for a given exercise including frozen revisions of the exercise.
	 */
	public long countSubmissionsForExercise(final Exercise exercise) {
		return getEntityManager()
				.createNamedQuery(Submission.COUNT_NONTESTING_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS,
						Long.class)
				.setParameter("exerciseId", exercise.getId())
				.getSingleResult(); // No Optional, count() returns at least 0.
	}

	/**
	 * Counts all submissions for a given exercise including frozen revisions of the exercise and testing submissions.
	 */
	public long countAllSubmissionsForExercise(final Exercise exercise) {
		return getEntityManager()
				.createNamedQuery(Submission.COUNT_ALL_SUBMISSIONS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS, Long.class)
				.setParameter("exerciseId", exercise.getId())
				.getSingleResult(); // No Optional, count() returns at least 0.
	}

	/**
	 * Lists all submissions that belong to a given user
	 *
	 * @return Submission list <strong>without</strong> lazy data
	 */
	public List<Submission> getAllSubmissionsForUser(User user) {
		return getEntityManager() //
				.createNamedQuery(Submission.ALL_SUBMISSIONS_FOR_USER, Submission.class) //
				.setParameter("author", user) //
				.getResultList();
	}

	public Submission mergeSubmission(Submission submission) {
		return baseService.merge(submission);
	}

	/**
	 * Deletes given Submission and dependent Entities
	 * Deletion order: submissions, results, stage-submissions, variable values, submission log entries
	 *
	 * @param submission
	 *            Submission, which is due for deletion
	 */
	public void deleteSubmissionAndDependentEntities(final Submission submission) {
		Submission updatedSubmission = getSubmissionWithLazyDataBySubmissionIdFromEnvers(submission.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		// Also delete dependent entities
		// Deletion order: submissions, results, jobs, stage-submissions, variable values, submission log entries
		final Set<SubmissionLogEntry> submissionLog = new HashSet<>(updatedSubmission.getSubmissionLog());
		final Collection<VariableValue> variableValues = updatedSubmission.getVariableValues().values();
		final Set<StageSubmission> stagesubmissions = new HashSet<>();

		//get jobs before deleting submission to avoid referencing error
		final List<Job> jobs = jobService.getAllJobsForSubmission(submission);

		baseService.deleteEntity(updatedSubmission);


		final Set<Result> results = new HashSet<>();
		for (SubmissionLogEntry submissionLogEntry : submissionLog) {
			stagesubmissions.add(submissionLogEntry.getSubmission());
			results.add(submissionLogEntry.getResult());
		}

		results.stream() //
		.filter(Objects::nonNull) //
		.forEach(baseService::deleteEntity);

		jobs.stream() //
		.forEach(baseService::deleteEntity);

		stagesubmissions.stream() //
		.filter(Objects::nonNull) //
		.forEach(baseService::deleteEntity);

		variableValues.stream() //
		.forEach(baseService::deleteEntity);

		submissionLog.stream() //
		.forEach(baseService::deleteEntity);
	}

}
